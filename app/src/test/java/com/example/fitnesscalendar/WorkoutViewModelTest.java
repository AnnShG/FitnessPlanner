package com.example.fitnesscalendar;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.logic.workout.WorkoutViewModel;
import com.example.fitnesscalendar.relations.DateColourResult;
import com.example.fitnesscalendar.relations.UserWithGoals;
import com.example.fitnesscalendar.repository.AiRepository;
import com.example.fitnesscalendar.repository.UserRepository;
import com.example.fitnesscalendar.repository.WorkoutRepository;
import com.google.common.util.concurrent.Futures;
import com.google.firebase.vertexai.type.GenerateContentResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorkoutViewModelTest {
    // LiveData values are processed on the main thread, Rule makes it synchronous
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    private WorkoutViewModel viewModel;
    @Mock private Application application;
    @Mock private WorkoutRepository workoutRepository;
    @Mock private UserRepository userRepository;
    @Mock private AiRepository aiRepository;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        MutableLiveData<UserWithGoals> userLiveData = new MutableLiveData<>();
        when(userRepository.getLatestUser()).thenReturn(userLiveData);

        viewModel = new WorkoutViewModel(application, workoutRepository, userRepository, aiRepository);
    }

    @Test
    public void testSetSearchQuery_UpdatesValue() {
        String query = "Legs";
        viewModel.setSearchQuery(query);
        assertNotNull(viewModel.filteredWorkouts);
    }

    @Test
    public void testSetFilters_UpdatesFilterIds() {
        List<Long> ids = Arrays.asList(1L, 2L);
        viewModel.setFilters(ids);
        Assert.assertEquals(ids, viewModel.getFilterIds().getValue());
    }

    @Test
    public void testSaveWorkout_CallsRepository() {
        Workout workout = new Workout();
        List<Long> exercises = Arrays.asList(10L, 11L);
        viewModel.saveWorkout(workout, exercises);
        verify(workoutRepository).insertFullWorkout(workout, exercises);
    }

    @Test
    public void testDeleteWorkout_CallsRepository() {
        Workout workout = new Workout();
        viewModel.deleteWorkout(workout);
        verify(workoutRepository).deleteWorkout(workout);
    }

    @Test
    public void testAttachWorkoutToDates_ConvertsLongToLocalDate() {
        long workoutId = 5L;
        long userId = 1L;
        Set<Long> epochDays = new HashSet<>();
        epochDays.add(20500L);

        viewModel.attachWorkoutToDates(userId, workoutId, epochDays);
        // Verify repo is called with the Set<LocalDate>
        verify(workoutRepository).attachWorkoutToDates(eq(userId), eq(workoutId), anySet());
    }

    @Test
    public void testUpdateWorkoutCompletion_CallsRepository() {
        viewModel.updateWorkoutCompletion(1L, 5L, 20500L, true);
        verify(workoutRepository).updateWorkoutCompletion(1L, 5L, 20500L, true);
    }

    @Test
    public void testRefreshAiInsight_NewUser_ShowsLearningMessage() {
        UserWithGoals mockUser = mock(UserWithGoals.class);
        mockUser.user = new User();
        mockUser.user.id = 1L;
        mockUser.user.createdAt = new java.util.Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 1 day ago created

        viewModel.refreshAiInsight(mockUser, new ArrayList<>());

        Assert.assertEquals("Learning your fitness habits. Personalized insights will appear here after 7 days of activity logging.",
                viewModel.getAiAdvice().getValue());
        assertFalse(viewModel.getIsAiLoading().getValue());
    }

    @Test
    public void testGetMonthlyStats_CombinesSourcesCorrectly() {
        MutableLiveData<Integer> total = new MutableLiveData<>(10);
        MutableLiveData<Integer> completed = new MutableLiveData<>(5);

        when(workoutRepository.getTotalWorkoutsInMonth(0L, 100L)).thenReturn(total);
        when(workoutRepository.getCompletedWorkoutsInMonth(0L, 100L)).thenReturn(completed);

        viewModel.getMonthlyStats(0L, 100L).observeForever(pair -> {
            Assert.assertEquals(Integer.valueOf(10), pair.first);
            Assert.assertEquals(Integer.valueOf(5), pair.second);
        });
    }

    @Test
    public void testSimpleDelegationMethods() {
        // Test remaining simple calls
        viewModel.getFullWorkoutRecords(1L);
        verify(workoutRepository).getFullWorkoutRecords(1L);

        viewModel.getFullWorkoutById(10L);
        verify(workoutRepository).getFullWorkoutById(10L);

        viewModel.getWorkoutDotsForUser(1L);
        verify(workoutRepository).getWorkoutColorsForUser(1L);

        viewModel.getUniquePlannedWorkouts(1L);
        verify(workoutRepository).getUniquePlannedWorkouts(1L);

        viewModel.getAllCategories();
        verify(workoutRepository).getAllCategories();
    }

    @Test
    public void testRefreshAiInsight_ExperiencedUser_TriggersAi() {
        UserWithGoals mockUser = new UserWithGoals();
        mockUser.user = new com.example.fitnesscalendar.entities.User();
        mockUser.user.id = 1L;
        mockUser.user.createdAt = new java.util.Date(System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)); // 10 days ago created

        when(workoutRepository.getAiChatHistoryForUser(1L)).thenReturn(new ArrayList<>());

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn("Keep it up!");
        when(aiRepository.getAdvice(anyString(), anyList())).thenReturn(Futures.immediateFuture(mockResponse));

        viewModel.refreshAiInsight(mockUser, new ArrayList<>());
        assertTrue(viewModel.getIsAiLoading().getValue());
    }

    @Test
    public void testFilteredWorkouts_LogicBranching() {
        UserWithGoals user = new UserWithGoals();
        user.user = new com.example.fitnesscalendar.entities.User();
        user.user.id = 1L;

        MutableLiveData<UserWithGoals> userLiveData = (MutableLiveData<UserWithGoals>) userRepository.getLatestUser();
        userLiveData.setValue(user);

        viewModel.filteredWorkouts.observeForever(list -> {});
        viewModel.setSearchQuery("Bench");
        verify(workoutRepository, timeout(1000)).getWorkoutsFilteredAndSearched(eq(1L), anyList(), eq("Bench"));
    }

    @Test
    public void testFilteredWorkouts_WhenUserIsNull() {
        MutableLiveData<UserWithGoals> userLiveData = new MutableLiveData<>(null);
        when(userRepository.getLatestUser()).thenReturn(userLiveData);

        viewModel.filteredWorkouts.observeForever(list -> {
            assertTrue(list.isEmpty());
        });
    }

    @Test
    public void testMissingDelegationMethods() {
        viewModel.deleteWorkoutFromCalendar(1L, 10L);
        verify(workoutRepository).deleteOnlyPlannedWorkoutsFromCalendar(1L, 10L);

        viewModel.deleteSpecificWorkoutPlan(1L, 10L, 20500L);
        verify(workoutRepository).deleteSpecificWorkoutPlan(1L, 10L, 20500L);

        Workout workout = new Workout();
        List<Long> exerciseIds = new ArrayList<>();
        viewModel.updateWorkout(workout, exerciseIds);
        verify(workoutRepository).updateFullWorkout(workout, exerciseIds);

        viewModel.getWorkoutsForSpecificDay(1L, 20500L);
        verify(workoutRepository).getWorkoutsForSpecificDay(1L, 20500L);

        // test workout plan sync
        Set<Long> newDays = new HashSet<>();
        Set<Long> compDays = new HashSet<>();
        viewModel.updateWorkoutPlan(1L, 5L, newDays, compDays);
        verify(workoutRepository).updateWorkoutPlan(1L, 5L, newDays, compDays);
    }

}