# Survey Module

## 1. General Description
The survey collects the user data after the first application running, and it consists of 4 screens (fragments).
Also, the 2nd and 3rd screens can be used again in the "Edit Mode" (isEditMode), when the user changes his data.

| Screen | Class                 | ViewBinding | Purpose                                                                   |
|--------|-----------------------|---|---------------------------------------------------------------------------|
| 1      | `SurveyPage1Fragment` | `SurveyPage1Binding` | Welcoming, continue next button                                           |
| 2      | `SurveyPage2Fragment` | `SurveyPage2Binding` | Name, Date of Birth, Gendre                                               |
| 3      | `SurveyPage3Fragment` | `SurveyPage3Binding` | Trainings goals (6 cards + own goal)                                      |
| 4      | `SurveyPage4Fragment` | `SurveyPage4Binding` | Finalising: saving the profile to DB and redirect to home page (calendar) |

Packet: `com.example.fitnesscalendar.logic.survey`

---

## 2. Files and connections
Key idea: **all screens utilise one common `SurveyViewModel`**, which is linked to Activity (`new ViewModelProvider(requireActivity())`).
While the user switches the screens, the data is stored there, only after pressing the button in the end or **Save** in edit mode the new data is written in the DB.

```mermaid
classDiagram
    class SurveyPage1Fragment {
        -SurveyPage1Binding binding
        -SurveyViewModel viewModel
    }
    class SurveyPage2Fragment {
        -boolean isEditMode
        -User currentUser
        -handleGenderSelection(MaterialButton, String)
        -restoreGenderUI(String)
        -saveUserDataToDatabase()
    }
    class SurveyPage3Fragment {
        -boolean isEditMode
        -Long currentUserId
        -toggleGoalSelection(MaterialCardView, ImageView, String)
        -restoreSelectionUI()
        -updateState(MaterialCardView, ImageView, boolean)
        -saveGoalsToDatabase()
    }
    class SurveyPage4Fragment {
        -SurveyViewModel viewModel
    }
    class SurveyViewModel {
        -String name
        -Date birthDate
        -String gender
        -Set~String~ selectedGoals
        -String customGoal
        +toggleGoal(String)
        +saveUserProfileToDatabase()
    }
    class UserViewModel {
        -UserRepository repository
        +getLoggedInUser() LiveData~UserWithGoals~
        +getProfileData() LiveData~UserWithGoals~
        +updateUser(User)
        +updateUserGoals(Long, List, String)
    }
    class UserRepository {
        -UserDao userDao
        -GoalDao goalDao
        -ExecutorService databaseExecutor
        +GOAL_LOSE_WEIGHT : String
        +GOAL_BUILD_MUSCLE : String
        +GOAL_GET_STRONGER : String
        +GOAL_STAY_FIT : String
        +GOAL_RECOVER_INJURY : String
        +GOAL_STAY_ACTIVE : String
        -GOAL_SUBTITLES : Map~String,String~
        +insertUserWithGoals(User, List~Goal~)
        +getLatestUser() LiveData~UserWithGoals~
        +updateGoal(Goal)
        +updateUser(User)
        +hasUser() boolean
        +updateUserGoals(Long, List, String)
    }
    class UserDao {
        +insert(User) long
        +update(User)
        +delete(User)
        +deleteUserById(long)
        +getUserWithGoals(long) LiveData~UserWithGoals~
        +getLatestUser() LiveData~UserWithGoals~
        +getAllUsers() LiveData~List~User~~
        +getUserById(long) User
        +getUserCount() int
    }
    class GoalDao {
        +insert(Goal) long
        +update(Goal)
        +getGoalsForUser(long) List~Goal~
        +getGoalById(long) Goal
        +deleteGoalsByUserId(long)
    }
    class AppDatabase {
        +getDatabase(Context) AppDatabase
        +databaseWriteExecutor : ExecutorService
        +userDao() UserDao
        +goalDao() GoalDao
    }
    class User
    class Goal
    class UserWithGoals
    class SharedPreferences
    class ProfileScreenFragment {
        -GoalAdapter goalAdapter
        -UserViewModel viewModel
    }
    class GoalAdapter {
        +setGoals(List~Goal~)
    }

    SurveyPage1Fragment ..> SurveyViewModel : receives, but not uses
    SurveyPage2Fragment --> SurveyViewModel : name, birthDate, gender
    SurveyPage2Fragment --> UserViewModel : prefilling and saving
    SurveyPage3Fragment --> SurveyViewModel : selectedGoals, customGoal
    SurveyPage3Fragment --> UserViewModel : prefilling and saving
    SurveyPage3Fragment ..> UserRepository : goals constants
    SurveyPage4Fragment --> SurveyViewModel : saveUserProfileToDatabase
    SurveyPage4Fragment ..> SharedPreferences : is_survey_completed
    SurveyViewModel --> UserRepository : insertUserWithGoals
    UserViewModel --> UserRepository : updateUser, updateUserGoals, getLatestUser
    UserRepository --> UserDao
    UserRepository --> GoalDao
    UserRepository ..> AppDatabase : receives DAO and executor
    UserRepository ..> User
    UserRepository ..> Goal
    UserDao ..> UserWithGoals : returns
    ProfileScreenFragment --> UserViewModel : getProfileData
    ProfileScreenFragment --> GoalAdapter : shows the goals list
    ProfileScreenFragment ..> SurveyPage2Fragment : opens with isEditMode=true
    ProfileScreenFragment ..> SurveyPage3Fragment : opens with isEditMode=true
```

# Navigation between the screens
The transitions are described in navigation graph through actions:

| From where | To where         | Action                                   |
|------------|------------------|------------------------------------------|
| Page 1     | Page 2           | `action_SurveyPage1_to_SurveyPage2`      |
| Page 2     | Page 3           | `action_SurveyPage2_to_SurveyPage3`      |
| Page 3     | Page 4           | `action_SurveyPage3_to_SurveyPage4`      |
| Page 4     | CalendarHomePage | `action_SurveyPage4_to_CalendarHomePage`  (с `popUpTo`)|

The button Back calls navigateUp() on the 2 and 3 screens. The button Back in absent on the 4th screen.

```mermaid
flowchart TD
Start([Survey run]) --> P1["Page 1: Welcoming"]
P1 -->|"Button"| P2["Page 2: Name, date of birth, gender"]
P2 -.->|"Back"| P1
P2 --> V2{"All fields are valid?"}
V2 -- No --> P2
V2 -- Yes --> P3["Page 3: Goals"]
P3 --> V3{"At least one goal is chosen?"}
P3 -.->|"Back"| P2
V3 -- No --> P3
V3 -- Yes --> P4["Page 4: Finish"]
P4 -->|"Continue"| Save[("Profile saving to DB")]
Save --> Flag["SharedPreferences: is_survey_completed = true"]
Flag --> Home([CalendarHomePage])
```
For transition to `CalendarHomePage` is used `setPopUpTo(R.id.SurveyPage1, true)`. This removes all survey screens (including the first) from the stack.

---

## 4. Two modes: onboarding and edit
The screens 2 and 3 read argument `isEditMode` from `getArguments()` (false by default)

|                         | Onboarding (`isEditMode = false`)                     | Edit (`isEditMode = true`)                                                      |
|-------------------------|-------------------------------------------------------|---------------------------------------------------------------------------------|
| Title                   | XML                                                   | "Edit my data" (screen 2), "Edit goals" (screen 3)                              |
| Button `continueButton` | "Continue" leads to the next screen                   | "Save", saves to DB                                                             |
| Button Back             | is visible                                            | `INVISIBLE`                                                                     |
| Background              | initial XML                                           | other colour (`#FFFAFA` on screen 2, `home_page_background_colour` on screen 3) |
| The values source       | from `SurveyViewModel` (if user already entered smth) | from DB through `UserViewModel.getLoggedInUser()`                               |
| After tapping button    | transition to the next screen                         | writing to DB, clearing `SurveyViewModel`, Toast, `navigateUp()`                |

---

## 5. Survey screens
### Screen 1: Welcoming

**Files:** `SurveyPage1Fragment.java`, `survey_page_1.xml`

The only action: button `binding.button` which redirects to screen 2 `action_SurveyPage1_to_SurveyPage2`. `SurveyViewModel` can be received but not used. A common ViewModel is created in Activity before the second screen.

---
