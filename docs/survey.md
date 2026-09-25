# Survey Module

## 1. General Description
The survey collects the user data after the first application running and it consists of 4 screens (fragments).
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
        name
        birthDate
        gender
        selectedGoals
        customGoal
        toggleGoal(String)
        saveUserProfileToDatabase()
    }
    class UserViewModel {
        getLoggedInUser()
        updateUser(User)
        updateUserGoals(Long, List, String)
    }
    class UserRepository {
        GOAL_LOSE_WEIGHT
        GOAL_BUILD_MUSCLE
        GOAL_GET_STRONGER
        GOAL_STAY_FIT
        GOAL_RECOVER_INJURY
        GOAL_STAY_ACTIVE
    }
    class User
    class Goal
    class SharedPreferences

    SurveyPage1Fragment ..> SurveyViewModel : receives, but not uses
    SurveyPage2Fragment --> SurveyViewModel : name, birthDate, gender
    SurveyPage2Fragment --> UserViewModel : prefilling and saving
    SurveyPage3Fragment --> SurveyViewModel : selectedGoals, customGoal
    SurveyPage3Fragment --> UserViewModel : prefilling and saving
    SurveyPage3Fragment ..> UserRepository : goals constants
    SurveyPage4Fragment --> SurveyViewModel : saveUserProfileToDatabase
    SurveyPage4Fragment ..> SharedPreferences : is_survey_completed
    UserViewModel ..> User
    UserViewModel ..> Goal
```