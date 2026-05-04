package com.example.fitnesscalendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Application;

import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.logic.profile.UserViewModel;
import com.example.fitnesscalendar.repository.UserRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

public class UserViewModelTest {
    private UserViewModel viewModel;
    @Mock
    private Application application;
    @Mock
    private UserRepository userRepository;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        viewModel = new UserViewModel(application, userRepository);
    }

    @Test
    public void getLoggedInUser_delegatesToRepository() {
        viewModel.getLoggedInUser();
        verify(userRepository).getLatestUser();
    }

    @Test
    public void getProfileData_delegatesToRepository() {
        viewModel.getProfileData();
        verify(userRepository).getLatestUser();
    }

    @Test
    public void updateUser_delegatesToRepository() {
        User user = new User();
        viewModel.updateUser(user);
        verify(userRepository).updateUser(user);
    }

    @Test
    public void updateUserGoals_validId_delegatesToRepository() {
        Long userId = 1L;
        List<String> titles = Arrays.asList("Goal 1", "Goal 2");
        String customGoal = "Custom Goal";

        viewModel.updateUserGoals(userId, titles, customGoal);

        verify(userRepository).updateUserGoals(userId, titles, customGoal);
    }

    @Test
    public void updateUserGoals_nullId_doesNotCallRepository() {
        viewModel.updateUserGoals(null, Arrays.asList("Goal"), "Custom");

        verify(userRepository, never()).updateUserGoals(anyLong(), any(), anyString());
    }
}
