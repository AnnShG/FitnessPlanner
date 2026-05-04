package com.example.fitnesscalendar;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fitnesscalendar.dao.GoalDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.entities.Goal;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.repository.UserRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

public class UserRepositoryTest {
    private UserRepository repository;
    @Mock
    private UserDao userDao;
    @Mock
    private GoalDao goalDao;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new UserRepository(userDao, goalDao);
    }

    @Test
    public void insertUserWithGoals_validData_callsInserts() {
        User user = new User();
        user.name = "Test User";
        
        Goal goal1 = new Goal();
        goal1.setGoalTitle(UserRepository.GOAL_BUILD_MUSCLE);
        goal1.setCustom(false);
        
        List<Goal> goals = Arrays.asList(goal1);

        when(userDao.insert(user)).thenReturn(1L);

        repository.insertUserWithGoals(user, goals);

        // Verify user insert
        verify(userDao, timeout(1000)).insert(user);
        
        // Verify goal insert with correctly mapped subtitle
        verify(goalDao, timeout(1000)).insert(any(Goal.class));
    }

    @Test
    public void updateUser_callsDaoUpdate() {
        User user = new User();
        user.id = 1L;
        
        repository.updateUser(user);
        
        verify(userDao, timeout(1000)).update(user);
    }

    @Test
    public void updateGoal_callsDaoUpdate() {
        Goal goal = new Goal();
        goal.setGoalId(1L);
        
        repository.updateGoal(goal);
        
        verify(goalDao, timeout(1000)).update(goal);
    }

    @Test
    public void hasUser_returnsCorrectValue() {
        when(userDao.getUserCount()).thenReturn(1);
        assertTrue(repository.hasUser());
        verify(userDao).getUserCount();
    }

    @Test
    public void getLatestUser_delegatesToDao() {
        repository.getLatestUser();
        verify(userDao).getLatestUser();
    }

    @Test
    public void updateUserGoals_syncsGoalsCorrectly() {
        long userId = 1L;
        List<String> titles = Arrays.asList(UserRepository.GOAL_LOSE_WEIGHT);
        String customText = "My Custom Goal";

        repository.updateUserGoals(userId, titles, customText);

        verify(goalDao, timeout(1000)).deleteGoalsByUserId(userId);
        
        // should insert the predefined goal AND the custom goal
        verify(goalDao, timeout(1000).times(2)).insert(any(Goal.class));
    }
    
    @Test
    public void updateUserGoals_noCustomGoal_onlyInsertsPredefined() {
        long userId = 1L;
        List<String> titles = Arrays.asList(UserRepository.GOAL_STAY_FIT);

        repository.updateUserGoals(userId, titles, "");

        verify(goalDao, timeout(1000)).deleteGoalsByUserId(userId);
        verify(goalDao, timeout(1000).times(1)).insert(any(Goal.class));
    }
}
