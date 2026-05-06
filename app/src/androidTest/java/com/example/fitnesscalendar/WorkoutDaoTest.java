package com.example.fitnesscalendar;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.dao.WorkoutDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.entities.Workout;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WorkoutDaoTest {
    private AppDatabase db;
    private WorkoutDao workoutDao;
    private long userId;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        workoutDao = db.workoutDao();
        UserDao userDao = db.userDao();

        // user is required for workout creation
        User user = new User();
        user.name = "Test User";
        userId = userDao.insert(user);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertAndReadWorkout() {
        Workout workout = new Workout();
        workout.title = "Morning Strength";
        workout.ownerId = userId;
        workout.colour = 0xFF4CAF50;

        workoutDao.insert(workout);

        List<Workout> allWorkouts = workoutDao.getAllWorkouts();
        Assert.assertFalse(allWorkouts.isEmpty());
        Assert.assertEquals("Morning Strength", allWorkouts.get(0).title);
    }

    @Test
    public void updateWorkout() {
        Workout workout = new Workout();
        workout.title = "Old Title";
        workout.ownerId = userId;
        long id = workoutDao.insert(workout);

        Workout retrieved = workoutDao.getAllWorkouts().get(0);
        retrieved.setTitle("New Title");
        workoutDao.update(retrieved);

        Workout updated = workoutDao.getAllWorkouts().get(0);
        Assert.assertEquals("New Title", updated.title);
    }

    @Test
    public void deleteWorkout() {
        Workout workout = new Workout();
        workout.title = "To Delete";
        workout.ownerId = userId;
        workoutDao.insert(workout);

        Workout retrieved = workoutDao.getAllWorkouts().get(0);
        workoutDao.delete(retrieved);

        List<Workout> allWorkouts = workoutDao.getAllWorkouts();
        Assert.assertTrue(allWorkouts.isEmpty());
    }
}