package com.example.fitnesscalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import android.app.Application;

import com.example.fitnesscalendar.logic.survey.SurveyViewModel;
import com.example.fitnesscalendar.repository.UserRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

public class SurveyViewModelTest {
    private SurveyViewModel viewModel;
    @Mock
    private Application application;
    @Mock
    private UserRepository userRepository;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        viewModel = new SurveyViewModel(application, userRepository);
    }

    @Test
    public void toggleGoal_addsGoalIfNotPresent() {
        String goal = "Lose Weight";
        viewModel.toggleGoal(goal);
        
        assertTrue(viewModel.getSelectedGoals().contains(goal));
        assertEquals(1, viewModel.getSelectedGoals().size());
    }

    @Test
    public void toggleGoal_removesGoalIfPresent() {
        String goal = "Build Muscle";
        viewModel.toggleGoal(goal);
        assertTrue(viewModel.getSelectedGoals().contains(goal));
        
        viewModel.toggleGoal(goal);
        assertFalse(viewModel.getSelectedGoals().contains(goal));
        assertEquals(0, viewModel.getSelectedGoals().size());
    }

    @Test
    public void saveUserProfileToDatabase_mapsDataCorrectly() {
        viewModel.setName("John Doe");
        Date birthDate = new Date();
        viewModel.setBirthDate(birthDate);
        viewModel.setGender("Male");
        viewModel.toggleGoal("Stay Fit");
        viewModel.setCustomGoal("Walk 10k steps");

        viewModel.saveUserProfileToDatabase();

        verify(userRepository).insertUserWithGoals(
                argThat(user -> "John Doe".equals(user.getName()) &&
                                birthDate.equals(user.getBirthDate()) &&
                                "Male".equals(user.getGender())),
                argThat(goals -> goals.size() == 2 &&
                                goals.stream().anyMatch(g -> "Stay Fit".equals(g.getGoalTitle()) && !g.isCustom()) &&
                                goals.stream().anyMatch(g -> "Walk 10k steps".equals(g.getGoalTitle()) && g.isCustom()))
        );
    }

    @Test
    public void saveUserProfileToDatabase_handlesNullOrEmptyData() {
        viewModel.setName(null);
        viewModel.setSelectedGoals(null);
        viewModel.setCustomGoal("");

        viewModel.saveUserProfileToDatabase();

        verify(userRepository).insertUserWithGoals(any(), anyList());
    }
}
