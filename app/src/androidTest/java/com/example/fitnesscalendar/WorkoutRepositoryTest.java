package com.example.fitnesscalendar;

import android.app.Application;
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
import com.example.fitnesscalendar.repository.WorkoutRepository;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WorkoutRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private WorkoutRepository repository;
    private WorkoutDao workoutDao;
    private CalendarDayDao calendarDayDao;
    private long userId;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        // Use a real in-memory DB for repo testing to ensure DAOs work together
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        // Inject the in-memory database into the AppDatabase singleton for the Repository to pick up
        AppDatabase.setTestInstance(db);

        workoutDao = db.workoutDao();
        UserDao userDao = db.userDao();
        calendarDayDao = db.calendarDayDao();
        
        repository = new WorkoutRepository((Application) context);
        
        // Setup initial user
        User user = new User();
        user.name = "Repo Test User";
        userId = userDao.insert(user);
    }

    @After
    public void tearDown() {
        db.close();
        // Reset the singleton after test
        AppDatabase.setTestInstance(null);
    }

    @Test
    public void testInsertFullWorkout() throws InterruptedException {
        Workout workout = new Workout();
        workout.title = "Full Workout Test";
        workout.ownerId = userId;
        
        List<Long> exerciseIds = new ArrayList<>();
        // In a real scenario we'd insert exercises first
        
        repository.insertFullWorkout(workout, exerciseIds);
        
        // Since repo uses an executor, we need to wait or use a synchronous one.
        // For now, let's wait a bit.
        TimeUnit.MILLISECONDS.sleep(500);

        List<Workout> workouts = workoutDao.getAllWorkouts();
        Assert.assertFalse(workouts.isEmpty());
        Assert.assertEquals("Full Workout Test", workouts.get(0).title);
    }

    @Test
    public void testAttachWorkoutToDates() throws InterruptedException {
        Workout workout = new Workout();
        workout.title = "Plan Test";
        workout.ownerId = userId;
        long workoutId = workoutDao.insert(workout);

        Set<LocalDate> dates = new HashSet<>();
        LocalDate today = LocalDate.now();
        dates.add(today);

        repository.attachWorkoutToDates(userId, workoutId, dates);

        // Wait for executor
        TimeUnit.MILLISECONDS.sleep(500);

        // Verify CalendarDay creation
        Long dayId = calendarDayDao.getDayIdByDate(userId, today.toEpochDay());
        Assert.assertNotNull("CalendarDay should have been created", dayId);

        // Verify workout was linked to the day
        int count = calendarDayDao.getWorkoutCountForDay(dayId);
        Assert.assertEquals("Day should have 1 workout attached", 1, count);
    }

    @Test
    public void testDeleteSpecificWorkoutPlan() throws InterruptedException {
        Workout workout = new Workout();
        workout.title = "Delete Plan Test";
        workout.ownerId = userId;
        long workoutId = workoutDao.insert(workout);

        LocalDate today = LocalDate.now();
        Set<LocalDate> dates = new HashSet<>();
        dates.add(today);
        repository.attachWorkoutToDates(userId, workoutId, dates);
        TimeUnit.MILLISECONDS.sleep(500);

        repository.deleteSpecificWorkoutPlan(userId, workoutId, today.toEpochDay());
        TimeUnit.MILLISECONDS.sleep(500);

        Long dayId = calendarDayDao.getDayIdByDate(userId, today.toEpochDay());
        int count = calendarDayDao.getWorkoutCountForDay(dayId);
        Assert.assertEquals("Workout should be removed from the day", 0, count);
    }

    @Test
    public void testUpdateWorkoutCompletion() throws InterruptedException {
        Workout workout = new Workout();
        workout.title = "Completion Test";
        workout.ownerId = userId;
        long workoutId = workoutDao.insert(workout);

        LocalDate today = LocalDate.now();
        long epochDay = today.toEpochDay();

        Set<LocalDate> dates = new HashSet<>();
        dates.add(today);
        repository.attachWorkoutToDates(userId, workoutId, dates);
        TimeUnit.MILLISECONDS.sleep(500);

        // Trigger completion update
        repository.updateWorkoutCompletion(userId, workoutId, epochDay, true);
        TimeUnit.MILLISECONDS.sleep(500);

        // We can verify this by checking the cross-ref table via a LiveData query
        // or by adding a direct query to CalendarDayDao.
        // For now, this verifies the repository call doesn't crash and the flow is executed.
    }
}
