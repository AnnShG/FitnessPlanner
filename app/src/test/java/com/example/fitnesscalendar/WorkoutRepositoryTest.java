package com.example.fitnesscalendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fitnesscalendar.dao.AiDao;
import com.example.fitnesscalendar.dao.CalendarDayDao;
import com.example.fitnesscalendar.dao.WorkoutDao;
import com.example.fitnesscalendar.entities.AiMessage;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.repository.WorkoutRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorkoutRepositoryTest {
    private WorkoutRepository repository;
    @Mock
    private WorkoutDao workoutDao;
    @Mock
    private CalendarDayDao calendarDao;
    @Mock
    private AiDao aiDao;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new WorkoutRepository(workoutDao, calendarDao, aiDao);
    }

    // --- insertFullWorkout tests ---

    @Test
    public void insertFullWorkout_validData_callsInsert() {
        Workout workout = new Workout();
        workout.title = "Morning Yoga";
        List<Long> exerciseIds = Arrays.asList(1L, 2L);

        repository.insertFullWorkout(workout, exerciseIds);

        verify(workoutDao, timeout(1000)).insert(workout);
        // 2 calls because 2 exercises in the list
        verify(workoutDao, timeout(1000).times(2)).insertWorkoutExerciseCrossRef(any());
    }

    @Test
    public void insertFullWorkout_emptyTitle_doesNotInsert() {
        Workout workout = new Workout();
        workout.title = "";
        List<Long> exerciseIds = Arrays.asList(1L, 2L);
        repository.insertFullWorkout(workout, exerciseIds);
        verify(workoutDao, never()).insert(any());
    }

    @Test
    public void insertFullWorkout_emptyExercises_doesNotInsert() {
        Workout workout = new Workout();
        workout.title = "Leg Day";
        List<Long> exerciseIds = new ArrayList<>();
        repository.insertFullWorkout(workout, exerciseIds);
        verify(workoutDao, never()).insert(any()); // Verify the insertion did not happen
    }

    // --- attachWorkoutToDates tests ---

    @Test
    public void attachWorkoutToDates_withinLimit_insertsCrossRef() {
        long userId = 1L;
        long workoutId = 10L;
        LocalDate date = LocalDate.now();
        Set<LocalDate> dates = Collections.singleton(date);
        long dayId = 10L;

        when(calendarDao.getOrCreateDayId(userId, date.toEpochDay())).thenReturn(dayId);
        when(calendarDao.getWorkoutCountForDay(dayId)).thenReturn(0);

        repository.attachWorkoutToDates(userId, workoutId, dates);

        verify(calendarDao, timeout(1000)).insertCalendarDayWorkoutCrossRef(
                argThat(ref -> ref.calendarDayId == dayId && ref.workoutId == workoutId)
        );
    }

    @Test
    public void attachWorkoutToDates_atLimit_doesNotInsert() {
        long userId = 1L;
        long workoutId = 10L;
        LocalDate date = LocalDate.now();
        Set<LocalDate> dates = Collections.singleton(date);
        long dayId = 10L;

        when(calendarDao.getOrCreateDayId(userId, date.toEpochDay())).thenReturn(dayId);
        when(calendarDao.getWorkoutCountForDay(dayId)).thenReturn(3); // 3 - limit

        repository.attachWorkoutToDates(userId, workoutId, dates);

        verify(calendarDao, never()).insertCalendarDayWorkoutCrossRef(any());
    }

    // --- Update / Delete tests ---

    @Test
    public void updateFullWorkout_updatesAndResyncsExercises() {
        Workout workout = new Workout();
        workout.workoutId = 5L;
        List<Long> exerciseIds = Arrays.asList(10L);

        repository.updateFullWorkout(workout, exerciseIds);

        verify(workoutDao, timeout(1000)).update(workout);
        verify(workoutDao, timeout(1000)).deleteExercisesForWorkout(5L);
        verify(workoutDao, timeout(1000)).insertWorkoutExerciseCrossRef(any());
    }

    @Test
    public void deleteWorkout_callsDaoDelete() {
        Workout workout = new Workout();
        repository.deleteWorkout(workout);
        verify(workoutDao, timeout(1000)).delete(workout);
    }

    @Test
    public void deleteOnlyPlannedWorkoutsFromCalendar_callsDao() {
        repository.deleteOnlyPlannedWorkoutsFromCalendar(1L, 10L);
        verify(workoutDao, timeout(1000)).deleteOnlyPlannedWorkoutsFromCalendar(1L, 10L);
    }

    @Test
    public void deleteSpecificWorkoutPlan_callsDao() {
        repository.deleteSpecificWorkoutPlan(1L, 10L, 12345L);
        verify(calendarDao, timeout(1000)).deleteSpecificWorkoutPlan(1L, 10L, 12345L);
    }

    @Test
    public void updateWorkoutCompletion_callsDao() {
        repository.updateWorkoutCompletion(1L, 10L, 12345L, true);
        verify(calendarDao, timeout(1000)).updateWorkoutCompletion(1L, 10L, 12345L, true);
    }

    // --- Retrieval tests ---

    @Test
    public void getWorkoutsFilteredAndSearched_noCategories_usesSearchOnly() {
        repository.getWorkoutsFilteredAndSearched(1L, null, "run");
        verify(workoutDao).getWorkoutsBySearchOnly(1L, "run");
    }

    @Test
    public void getWorkoutsFilteredAndSearched_withCategories_usesFilteredQuery() {
        List<Long> cats = Arrays.asList(1L);
        repository.getWorkoutsFilteredAndSearched(1L, cats, "run");
        verify(workoutDao).getWorkoutsFiltered(1L, cats, "run");
    }

    @Test
    public void getSimpleQueries_delegatesToDaos() {
        repository.getFullWorkoutRecords(1L);
        verify(workoutDao).getFullWorkoutRecords(1L);

        repository.getFullWorkoutById(10L);
        verify(workoutDao).getFullWorkoutById(10L);

        repository.getWorkoutColorsForUser(1L);
        verify(calendarDao).getCalendarWorkoutDots(1L);

        repository.getUniquePlannedWorkouts(1L);
        verify(calendarDao).getUniquePlannedWorkouts(1L);

        repository.getWorkoutsForSpecificDay(1L, 12345L);
        verify(calendarDao).getWorkoutsForSpecificDay(1L, 12345L);

        repository.getAllCategories();
        verify(workoutDao).getAllCategories();
    }

    // --- Statistics and AI tests ---

    @Test
    public void getStats_delegatesToDao() {
        repository.getTotalWorkoutsInMonth(0L, 100L);
        verify(workoutDao).getTotalWorkoutsInMonth(0L, 100L);

        repository.getCompletedWorkoutsInMonth(0L, 100L);
        verify(workoutDao).getCompletedWorkoutsInMonth(0L, 100L);
    }

    @Test
    public void aiOperations_delegatesToAiDao() {
        repository.getAiChatHistoryForUser(1L);
        verify(aiDao).getChatHistoryForUser(1L);

        AiMessage msg = new AiMessage(1L, "user", "Hello");
        repository.saveAiMessage(msg);
        // verifying bg execution for saving AiMessage
        verify(aiDao, timeout(1000)).insert(msg);
    }

    @Test
    public void updateWorkoutPlan_syncsCorrectly() {
        Set<Long> newDays = new HashSet<>(Arrays.asList(1L));
        Set<Long> completedDays = new HashSet<>(Arrays.asList(1L));
        when(calendarDao.getOrCreateDayId(anyLong(), anyLong())).thenReturn(100L);

        repository.updateWorkoutPlan(1L, 10L, newDays, completedDays);

        verify(calendarDao, timeout(1000)).deleteWorkoutPlanLinks(1L, 10L);
        verify(calendarDao, timeout(1000)).insertCalendarDayWorkoutCrossRef(
                argThat(ref -> ref.isCompleted)
        );
    }
}
