package com.example.fitnesscalendar;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.GoalDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Goal;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.relations.UserWithGoals;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UserDaoTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private UserDao userDao;
    private GoalDao goalDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        userDao = db.userDao();
        goalDao = db.goalDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void getUserWithGoals_returnsCombinedData() throws InterruptedException {
        User user = new User();
        user.name = "John Doe";
        long userId = userDao.insert(user);

        goalDao.insert(new Goal(userId, "Muscle Gain", false));
        goalDao.insert(new Goal(userId, "Weight Loss", false));

        // use LiveDataTestUtil because there is a LiveData transaction
        UserWithGoals result = LiveDataTestUtil.getOrAwaitValue(userDao.getUserWithGoals(userId));

        Assert.assertNotNull(result);
        Assert.assertEquals("John Doe", result.user.name);
        Assert.assertEquals(2, result.goals.size()); // Proves the 1:M link worked
    }

    @Test
    public void getLatestUser_returnsFirstAvailableUser() throws InterruptedException {
        // Insert two users to test user linit - 1 per device)
        User user1 = new User();
        user1.name = "First User";
        userDao.insert(user1);

        User user2 = new User();
        user2.name = "Second User";
        userDao.insert(user2);

        UserWithGoals latest = LiveDataTestUtil.getOrAwaitValue(userDao.getLatestUser());

        Assert.assertNotNull(latest);
        Assert.assertEquals("First User", latest.user.name); // the 1st user should be inserted
    }

}
