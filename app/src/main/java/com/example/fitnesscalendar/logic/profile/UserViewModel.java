package com.example.fitnesscalendar.logic.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.fitnesscalendar.repository.UserRepository;
import com.example.fitnesscalendar.relations.UserWithGoals;
import com.example.fitnesscalendar.entities.User;

import java.util.List;

public class UserViewModel extends AndroidViewModel {

    private final UserRepository repository;

    public UserViewModel(@NonNull Application application) {
        super(application);
        this.repository = new UserRepository(application);
    }

    // constructor for testing
    public UserViewModel(@NonNull Application application, UserRepository repository) {
        super(application);
        this.repository = repository;
    }

    public LiveData<UserWithGoals> getLoggedInUser() {
        return repository.getLatestUser();
    }

    public LiveData<UserWithGoals> getProfileData() {
        return repository.getLatestUser();
    }

    public void updateUser(User user) {
        repository.updateUser(user);
    }

    public void updateUserGoals(Long userId, List<String> goalTitles, String customGoalText) {
        if (userId != null) {
            repository.updateUserGoals(userId, goalTitles, customGoalText);
        }
    }
}
