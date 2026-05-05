package com.example.fitnesscalendar.logic.survey;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;

import com.example.fitnesscalendar.entities.Goal;
import com.example.fitnesscalendar.repository.UserRepository;
import com.example.fitnesscalendar.entities.User;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

public class SurveyViewModel extends AndroidViewModel {
    @Setter
    @Getter
    private String name;
    @Setter
    @Getter
    private Date birthDate;
    @Setter
    @Getter
    private String gender;
    @Setter
    @Getter
    private Set<String> selectedGoals = new HashSet<>();
    @Setter
    @Getter
    private String customGoal;

    private final UserRepository repository;

    public SurveyViewModel(@NotNull Application app) {
        super(app);
        repository = new UserRepository(app);
    }

    // for testing
    public SurveyViewModel(@NotNull Application app, UserRepository repository) {
        super(app);
        this.repository = repository;
    }

    public void toggleGoal(String goals) {
        if (selectedGoals.contains(goals)) {
            selectedGoals.remove(goals);
        } else {
            selectedGoals.add(goals);
        }
    }

    public void saveUserProfileToDatabase() {
        User newUser = new User();
        newUser.setName(this.getName());
        newUser.setBirthDate(this.getBirthDate());
        newUser.setGender(this.getGender());

        ArrayList<Goal> goalEntities = new ArrayList<>();

        if (this.getSelectedGoals() != null) {
            for (String goalText : this.getSelectedGoals()) {
                Goal goalEntry = new Goal();
                goalEntry.setGoalTitle(goalText);
                goalEntry.setCustom(false);
                goalEntities.add(goalEntry);
            }
        }

        if (this.getCustomGoal() != null && !this.getCustomGoal().isEmpty()) {
            Goal customEntry = new Goal();
            customEntry.setGoalTitle(this.getCustomGoal());
            customEntry.setCustom(true);
            goalEntities.add(customEntry);
        }
        
        repository.insertUserWithGoals(newUser, goalEntities);
    }
}
