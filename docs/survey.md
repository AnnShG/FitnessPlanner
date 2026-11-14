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

## 3. Navigation between the screens
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

## 3.1. Where to access edit mode? (`ProfileScreenFragment`)

The answer on the question «Where to access `isEditMode`»: from the profile screen, `ProfileScreenFragment` (package `com.example.fitnesscalendar.logic.profile`). Exactly this file puts `isEditMode = true` in `Bundle` and passes it as arguments to the `SurveyPage2` fragment during navigation which is this - `getArguments().getBoolean("isEditMode", false)`, which read 2 and 3 screens.

**Profile screen demonstrates:**

- username (`binding.userNameTitle`) and the list of goals — through `RecyclerView` with `LinearLayoutManager` and with adapter `GoalAdapter` (`setGoals(List<Goal>)`). It is a separate adapter, not linked with `Adapter` for `GridView` of the calendar, it has another base (`RecyclerView.Adapter`, not `BaseAdapter`), but the role is the same - convert a list of data into rows on the screen.
- data is loaded via `viewModel.getProfileData().observe(...)` — the same `UserViewModel`, that is used by 2 and 3 screens for edit mode 

**Two separate entrance for editing:**

| Screen element           | Opens                 | Action | With                |
|--------------------------|-----------------------| --- |---------------------|
| `binding.userData`       | `SurveyPage2Fragment` | `action_ProfileFragment_to_SurveyPage2` | `isEditMode = true` |
| `binding.editGoalButton` | `SurveyPage3Fragment` | `action_ProfileFragment_to_SurveyPage3` | `isEditMode = true` |

That means that personal data (name, date of birth, gender) and goals are edited separately. Each screen of the survey is opened - after saving the user redirects back to the profile via `navigateUp()` (see part 5).

**Other navigation options from this screen** (they are not linked to the survey, but visible in the same file): `allExercisesRow` → `action_NavigationProfile_to_ExercisesList`, `allWorkoutsRow` → `action_NavigationProfile_to_WorkoutsList`.

```mermaid
flowchart TD
    Profile["ProfileScreenFragment"] -->|"tap on userData"| P2["SurveyPage2Fragment (isEditMode=true)"]
    Profile -->|"tap on editGoalButton"| P3["SurveyPage3Fragment (isEditMode=true)"]
    P2 -->|"Save"| Profile
    P3 -->|"Save"| Profile
```

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

### Screen 2: Personal data
**Files:** `SurveyPage2Fragment.java`, `survey_page_2.xml`
**The requested data:**

| Field         | View | Validation | Where stored |
|---------------|---|---|--------------|
| Name          | `userInputName` (EditText) | the field cannot be empty `trim()`, otherwise `setError("Please enter your name")` | `surveyViewModel.setName()` upon clicking Continue |
| Date of Birth | `userInputBirthDate` (upon clicking `DatePickerDialog` is opened) | must be selected, otherwise `setError("Please select your birth date")` | `surveyViewModel.setBirthDate()` immediately after selecting the date | 
| Gender        | `buttonMale`, `buttonFemale`, `buttonNoAnswer` (MaterialButton) | must be selected, otherwise Toast "Please select a gender" | `surveyViewModel.setGender()` just after clicking |

The values of the gender: `"Male"`, `"Female"`, `"Prefer not to say"`.

**Highlighting of the selected gender** (`handleGenderSelection(MaterialButton selectedButton, String genderValue)`: the selected button gets an orange border, other buttons get a gray border.

**DatePicker:** the starting date is taken from the ViewModel (if it was already taken), otherwise today's date. Maximum date is today (`setMaxDate(System.currentTimeMillis())`). Display format: `dd/MM/yyyy`.

**State Restoration / Pre-filling:**

```mermaid
flowchart TD
    A["User opens SurveyPage2"] --> B{"Edit Mode?"}

    B -- No --> C["Restore existing ViewModel values to UI if not null"]
    B -- Yes --> D["Observe logged-in user from DB"]

    D --> E{"ViewModel field is null?"}
    E -- Yes --> F["Copy DB value into ViewModel"]
    E -- No --> G["Keep existing ViewModel value"]
    F --> H["Display values in UI"]
    G --> H

    C --> I["User interacts with fields"]
    H --> I

    I --> J["User selects birth date"]
    I --> K["User selects gender"]
    I --> L["User types name"]

    J --> M["Immediately save date to ViewModel"]
    K --> N["Immediately save gender to ViewModel"]
    L --> O["Name remains in UI temporarily"]

    M --> P["User clicks Continue / Save"]
    N --> P
    O --> P

    P --> Q["Read name from UI and save to ViewModel"]
    Q --> R{"All fields valid?"}

    R -- No --> S["Show validation errors"]
    R -- Yes --> T{"Edit Mode?"}

    T -- No --> U["Navigate to SurveyPage3"]
    T -- Yes --> V["Update currentUser with ViewModel values"]
    V --> W["Update database"]
    W --> X["Clear SurveyViewModel"]
    X --> Y["Navigate back"]
```

**Prefilling gender:** `restoreGenderUI(String gender)` is a method that passes the stored gender value in SurveyViewModel to `handleGenderSelection` to highlight the selected button. 
Is useful in the cases when the user returns to the 2 screen, to restore selected gender.

**Tapping Continue / Save:**

```mermaid
flowchart TD
    A([continueButton]) --> B[Validate the name, birthDate and genre]
    B --> C{"Data valid?"}
    C -- No --> D["Show error messages"]
    D --> Z(["End"])
    
    C -- Yes --> E{"isEditMode?"}
    E -- No --> F["navigate: Page 2 -> Page 3"]
    F --> Z
    
    E -- Yes --> G["saveUserDataToDatabase"]
    G --> H{"currentUser != null?"}
    H -- No --> I["Toast: Error: User session lost"]
    I --> Z
    
    H -- Yes --> J["Update currentUser with ViewModel values"]
    J --> K["Call userViewModel.updateUser(currentUser)"]
    K --> L["Clear SurveyViewModel fields (set to null)"]
    L --> M["Show Toast: Data updated!"]
    M --> N["Navigate back (navigateUp)"]
    N --> Z
    
```

---

### Screen 3: Goals
**Files:** `SurveyPage3Fragment.java`, `survey_page_3.xml`

**The screen task:** the user can select multiple cards and type his/her own goal. At leat one card must be select for passing it to the prompt to Gemini.

| Card         | Indicator | Constant                                            |
|--------------|-----------|-----------------------------------------------------|
| `option1` | `circle1` | `UserRepository.GOAL_LOSE_WEIGHT`                   |
| `option2` | `circle2` | `UserRepository.GOAL_BUILD_MUSCLE`                  |
| `option3` | `circle3` | `UserRepository.GOAL_GET_STRONGER`                  |
| `option4` | `circle4` | `UserRepository.GOAL_STAY_FIT`                      |
| `option5` | `circle5` | `UserRepository.GOAL_RECOVER_INJURY`                |
| `option6` | `circle6` | `UserRepository.GOAL_STAY_ACTIVE`                   |
| `userInputGoal` (EditText) |  | own goal, is stored in `surveyViewModel.customGoal` |

**How the selection works:**

```mermaid
flowchart TB
    A(["User clicks a goal card"]) --> B["toggleGoalSelection(card, circle, goalValue)"]
    B --> C["surveyViewModel.toggleGoal(goalValue)"]
    C --> D["Add goal if not selected, otherwise remove it"]
    D --> E["Check contains(goalValue)"]
    E --> F["updateState(card, circle, isSelected)"]
    F --> G["Update card border and circle appearance"]
```

**Own goal:** `TextWatcher` writes text in `surveyViewModel.setCustomGoal()` with every change. The typed text is not lost if the user leaves the screen.

**State restoration:** when creating a screen `restoreSelectionUI()` draws the cards again based on the current state in `selectedGoals` in ViewModel.

**Tapping Continue / Save:**

```mermaid
flowchart TB
    A(["User clicks Continue"]) --> B{"isEditMode?"}
    B -- Yes --> C["saveGoalsToDatabase()"]
    C --> E{"currentUserId is valid?"}
    E -- No --> F["Toast. Error: User session lost"]
    E -- Yes --> G["Call updateUserGoals"]
    
    G --> H["Clear selectedGoals and customGoal in SurveyViewModel"]
    H --> I["Toast: Goals updated!"]
    I --> J["Navigate back"]
    
    B -- No --> D["Get text from EditText"]
    D --> K["Save custom goal in SurveyViewModel"]
    K --> L{"selectedGoals empty?"}
    L -- Yes --> M["Show error message"]
    L -- No --> N["Navigate to SurveyPage4"]

```

---

### Screen 4: Finalising
**Files:** `SurveyPage4Fragment.java`, `survey_page_4.xml`

Tapping `continueButton`:

1. `viewModel.saveUserProfileToDatabase()`: saves all answers to the DB that were collected on 2 and 3 screens.
2. `SharedPreferences("app_prefs")`: `is_survey_completed = true`. The flag determines that the survey is finished
3. Toast "Profile Saved Successfully!".
4. Transition to `CalendarHomePage` with clearing the survey steak


```mermaid

flowchart TD
    A(["User clicks Continue"]) --> B["Get shared SurveyViewModel"]
    B --> C["Call saveUserProfileToDatabase()"]

    C --> E["Copy name, birth date and gender from ViewModel"]
    E --> F["Create empty goalEntities list"]

    F --> G{"selectedGoals is not null?"}
    G -- Yes --> H["Loop through selected goals"]
    H --> I["Create Goal object for each goal"]
    I --> J["Set  isCustom = false"]
    J --> K["Add Goal object to goalEntities"]
    K --> H
    G -- No --> L{"Custom goal exists and is not empty?"}
    H --> L

    L -- Yes --> M["Create custom Goal object"]
    M --> N["Set isCustom = true"]
    N --> O["Add custom Goal to goalEntities"]
    O --> P["Call repository.insertUserWithGoals(newUser, goalEntities)"]
    L -- No --> P

    P --> Q["Save is_survey_completed = true in SharedPreferences"]
    Q --> R["Show Toast: Profile Saved Successfully!"]
    R --> S["Navigate to CalendarHomePage"]
    S --> T["Remove survey screens from back stack"]
    T --> U(["Survey flow finished"])

```

---

## 6. How data flows through SurveyViewModel

The fields `SurveyViewModel` (`name`, `birthDate`, `gender`, `selectedGoals`, `customGoal`) are implemented using Lombok (`@Getter`/`@Setter`), the class — `AndroidViewModel`

| Field           | Writes | When                                     | Reads                                        |
|-----------------|--------|------------------------------------------|----------------------------------------------|
| `name`          | Page 2 | tapping Continue (or prefilling in edit) | Page 2 (edit), `saveUserProfileToDatabase()` |
| `birthDate`     | Page 2 | choosing date in DatePicker              | Page 2 (edit), `saveUserProfileToDatabase()` |
| `gender`        | Page 2 | tapping on gender button                 | Page 2 (edit), `saveUserProfileToDatabase()` |
| `selectedGoals` | Page 3 | tapping on card (`toggleGoal`)           | Page 3 (edit), `saveUserProfileToDatabase()` |
| `customGoal`    | Page 3 | each text changing (`TextWatcher`)       | Page 3 (edit), `saveUserProfileToDatabase()` |

**`saveUserProfileToDatabase()` (is called on screen 4):**

1. The `User` is created with `name`, `birthDate`, `gender`.
2. For each row in `selectedGoals` creates `Goal` with `isCustom = false` flag.
3. If `customGoal` is not empty, adds another one `Goal` with `isCustom = true` flag.
4. Passes `User` and the list `Goal` in `UserRepository.insertUserWithGoals(...)`.

**`UserRepository.insertUserWithGoals(User, List<Goal>)`:**

1. It is executed in the background thread `databaseExecutor`, because the Room prohibits database operations on the main thread.
2. `userDao.insert(user)` returns `newUserId`.
3. For each goal: affixes `userId = newUserId`; if the goal is custom (`!isCustom()`) and its name is in `GOAL_SUBTITLES`, adds subtitle (e.g., for `"Lose Weight"` — *"And see what the discipline can create"*).
4. Retains the goal through `goalDao.insert(goal)`.

Thus, the goal subtitle is generated not within a fragment or a ViewModel, but in the repository, using a static map `GOAL_SUBTITLES`.

---

## 6.1. Storing: entity `User` и `Goal`

Both tables are parts of the common DB `AppDatabase` (Room, `version = 31`), where, in addition to them, 10 other application entities are registered (`CalendarDay`, `Workout`, `Exercise`, etc.). Only two of these (the ones used by the survey) are described here. The whole DB description is here `docs/database.md`.

```mermaid
erDiagram
    USERS ||--o{ GOALS : "user_id" 
    USERS {
        long user_id PK
        string name
        date birthdate
        string gender
        date createdAt
 }
    GOALS {
        long goal_id PK
        long user_id FK
        string goal_title
        string goal_subtitle
        boolean is_custom
    }
```

**`User`** (`@Entity(tableName = "users")`): `id` (`user_id`, auto-generation), `name`, `birthDate` (`birth_date`), `gender`, `createdAt` (not related to screen 2, is affixed automatically during new object creation). The class utilises Lombok `@Data` + `@AllArgsConstructor`, getters/setters are generated, not written by hand (`currentUser.setName(...)`, `newUser.setBirthDate(...)` they are called in fragment and viewModel code.

**`Goal`** (`@Entity(tableName = "goals")`, connection 1:М with `User`): `goalId` (PK), `userId` (FK on `users.user_id`, `onDelete = CASCADE` — when a user is deleted, all their goals are automatically deleted), `goalTitle`, `goalSubtitle`, `isCustom`. Index on `user_id` (`@Index("user_id")`) speeds up the `getGoalsForUser`.

**`UserWithGoals`** (is utilised in `getLatestUser()` / `getUserWithGoals()`) — `@Relation`- Room wrapper, which pulls up the user with his list of the goals with one request, exactly its fields `user` and `goals` read survey fragments (`userWithGoals.user`, `userWithGoals.goals`).

**DAO, which the survey utilises:**

| Method | Class | Where called                                                                                           |
|---|---|--------------------------------------------------------------------------------------------------------|
| `insert(User)` | `UserDao` | `insertUserWithGoals` (screen 4, onboarding)                                                           |
| `update(User)` | `UserDao` | `UserRepository.updateUser` (screen 2, edit)                                                           |
| `getLatestUser()` | `UserDao` | `UserViewModel.getLoggedInUser/getProfileData`                                                         |
| `insert(Goal)` | `GoalDao` | `insertUserWithGoals`, `updateUserGoals`                                                               |
| `deleteGoalsByUserId(long)` | `GoalDao` | `updateUserGoals` (screen 3, edit — old goals are removed before adding new ones) |

---

### First survey completion
```mermaid
sequenceDiagram
    actor U as User
    participant P1 as SurveyPage1Fragment
    participant P2 as SurveyPage2Fragment
    participant P3 as SurveyPage3Fragment
    participant P4 as SurveyPage4Fragment
    participant VM as SurveyViewModel
    participant Repo as UserRepository
    participant DAO as UserDao / GoalDao
    participant SP as SharedPreferences

    U->>P1: taps the button
    P1->>P2: navigate (action_SurveyPage1_to_SurveyPage2)
    U->>P2: enters name, date of birth, selects gender
    P2->>VM: setBirthDate, setGender, setName
    P2->>P3: navigate (action_SurveyPage2_to_SurveyPage3)
    U->>P3: selects goals, enters his own goal
    P3->>VM: toggleGoal, setCustomGoal
    P3->>P4: navigate (action_SurveyPage3_to_SurveyPage4)
    U->>P4: taps Continue
    P4->>VM: saveUserProfileToDatabase()
    VM->>VM: collect User and List<Goal>
    VM->>Repo: insertUserWithGoals(user, goals)
    Note over Repo: background thread (databaseExecutor)
    Repo->>DAO: userDao.insert(user)
    DAO-->>Repo: newUserId
    loop for each goal
        Repo->>Repo: affix userId, subtitle from GOAL_SUBTITLES
        Repo->>DAO: goalDao.insert(goal)
    end
    P4->>SP: is_survey_completed = true
    P4->>U: Toast, transition to CalendarHomePage
```

---

### Data editing (screen 2, edit-mode)
```mermaid
sequenceDiagram
    actor U as User
    participant P2 as SurveyPage2Fragment
    participant UVM as UserViewModel
    participant VM as SurveyViewModel

    P2->>UVM: getLoggedInUser().observe
    UVM-->>P2: UserWithGoals
    P2->>VM: fill in name, birthDate, gender, if they not null
    P2->>U: display current data
    U->>P2: change data, tap Save
    P2->>P2: fields validation
    P2->>UVM: updateUser(currentUser)
    P2->>VM: clear name, birthDate, gender
    P2->>U: Toast "Data updated!", navigateUp
```

Screen 3 in edit-mode works the same, but instead of`updateUser` it calls `updateUserGoals(currentUserId, selectedTitles, customGoal)`.

---

## 8. Common pattern of all the fragments

- `onCreateView`: a `binding` is created via ViewBinding.
- `onViewCreated`: receiving of mutual ViewModel, buttons binding, state restoration.
- `onDestroyView`: `binding = null`, to avoid keeping references to a View after it has been destroyed (protection against memory leaks).
