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
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        AppDatabase.setTestInstance(db);

        workoutDao = db.workoutDao();
        UserDao userDao = db.userDao();
        calendarDayDao = db.calendarDayDao();
        
        repository = new WorkoutRepository((Application) context);
        
        User user = new User();
        user.name = "Repo Test User";
        userId = userDao.insert(user);
    }

    @After
    public void tearDown() {
        db.close();
        AppDatabase.setTestInstance(null);
    }

    @Test
    public void testInsertFullWorkout() throws InterruptedException {
        Workout workout = new Workout();
        workout.title = "Full Workout Test";
        workout.ownerId = userId;
        
        List<Long> exerciseIds = new ArrayList<>();
        repository.insertFullWorkout(workout, exerciseIds);
        
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

        TimeUnit.MILLISECONDS.sleep(500);

        Long dayId = calendarDayDao.getDayIdByDate(userId, today.toEpochDay());
        Assert.assertNotNull("CalendarDay should have been created", dayId);

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

        repository.updateWorkoutCompletion(userId, workoutId, epochDay, true);
        TimeUnit.MILLISECONDS.sleep(500);
    }
}
