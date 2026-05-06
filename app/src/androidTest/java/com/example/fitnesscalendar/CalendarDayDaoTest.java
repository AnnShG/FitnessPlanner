package com.example.fitnesscalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.CalendarDayDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.dao.WorkoutDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.relations.CalendarDayWorkoutCrossRef;
import com.example.fitnesscalendar.relations.DateColourResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CalendarDayDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private CalendarDayDao calendarDayDao;
    private WorkoutDao workoutDao;
    private long testUserId;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        calendarDayDao = db.calendarDayDao();
        workoutDao = db.workoutDao();
        UserDao userDao = db.userDao();

        // user is required for having the calendar
        User user = new User();
        user.name = "Test User";
        testUserId = userDao.insert(user);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void getOrCreateDayId_CreatesNewDayIfMissing() {
        long epochDay = 12345L;
        long dayId = calendarDayDao.getOrCreateDayId(testUserId, epochDay);
        
        assertTrue(dayId > 0);
        
        Long retrievedId = calendarDayDao.getDayIdByDate(testUserId, epochDay);
        assertEquals(Long.valueOf(dayId), retrievedId);
    }

    @Test
    public void getOrCreateDayId_ReturnsExistingDay() {
        long epochDay = 12345L;
        long firstId = calendarDayDao.getOrCreateDayId(testUserId, epochDay);
        long secondId = calendarDayDao.getOrCreateDayId(testUserId, epochDay);
        
        assertEquals(firstId, secondId);
    }

    @Test
    public void insertAndGetWorkoutCount_WorksCorrectly() {
        long epochDay = 20000L;
        long dayId = calendarDayDao.getOrCreateDayId(testUserId, epochDay);
        
        Workout workout = new Workout();
        workout.title = "Test Workout";
        workout.ownerId = testUserId;
        long workoutId = workoutDao.insert(workout);

        CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef();
        ref.calendarDayId = dayId;
        ref.workoutId = workoutId;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref);

        int count = calendarDayDao.getWorkoutCountForDay(dayId);
        assertEquals(1, count);
    }

    @Test
    public void updateWorkoutCompletion_UpdatesFlag() throws InterruptedException {
        long epochDay = 30000L;
        long dayId = calendarDayDao.getOrCreateDayId(testUserId, epochDay);
        
        Workout workout = new Workout();
        workout.title = "Cardio";
        workout.ownerId = testUserId;
        long workoutId = workoutDao.insert(workout);

        CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef();
        ref.calendarDayId = dayId;
        ref.workoutId = workoutId;
        ref.isCompleted = false;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref);

        calendarDayDao.updateWorkoutCompletion(testUserId, workoutId, epochDay, true);

        // Сheck via the query that returns completion status
        // Use LiveDataTestUtil to observe the value from the LiveData object
        List<DateColourResult> results = LiveDataTestUtil.getOrAwaitValue(calendarDayDao.getWorkoutsForSpecificDay(testUserId, epochDay));
        assertEquals(1, results.size());
        assertTrue(results.get(0).isCompleted);
    }

    @Test
    public void deleteSpecificWorkoutPlan_RemovesOnlyThatLink() throws InterruptedException {
        long epochDay = 40000L;
        long dayId = calendarDayDao.getOrCreateDayId(testUserId, epochDay);
        
        Workout workout = new Workout();
        workout.title = "Yoga";
        workout.ownerId = testUserId;
        long workoutId = workoutDao.insert(workout);

        CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef();
        ref.calendarDayId = dayId;
        ref.workoutId = workoutId;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref);

        calendarDayDao.deleteSpecificWorkoutPlan(testUserId, workoutId, epochDay);
        
        int count = calendarDayDao.getWorkoutCountForDay(dayId);
        assertEquals(0, count);
    }
}
