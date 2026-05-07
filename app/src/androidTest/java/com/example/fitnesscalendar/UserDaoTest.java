package com.example.fitnesscalendar;

import android.content.Context;
import android.icu.util.Calendar;

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

import java.util.Date;
import java.util.List;

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
        // Insert two users to test user linit - 1 per device
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

    @Test
    public void update_modifiesExistingUserFields() throws InterruptedException {
        User user = new User("Name");
        user.birthDate = new Date(); // default
        user.gender = "Other";

        long id = userDao.insert(user);
        user.id = id;

        // Specific date to test
        Calendar calendar = Calendar.getInstance();
        calendar.set(1995, Calendar.MAY, 5);
        Date testDate = calendar.getTime();

        // Modify the object and call update
        user.setName("Updated Name");
        user.setBirthDate(testDate);
        user.setGender("Female");
        userDao.update(user);

        User updatedUser = userDao.getUserById(id);

        Assert.assertNotNull(updatedUser);
        Assert.assertEquals("Updated Name", updatedUser.getName());
        Assert.assertEquals(testDate, updatedUser.getBirthDate());
        Assert.assertEquals("Female", updatedUser.getGender());
        Assert.assertEquals((Long)id, updatedUser.getId()); // ID must not change
    }

    @Test
    public void update_doesNotCreateDuplicateUser() throws InterruptedException {
        User user = new User("User A", "2000-01-01", "Male");
        long id = userDao.insert(user);
        user.setId(id);

        user.setName("User B");
        userDao.update(user);

        // Check total count of users
        List<User> allUsers = LiveDataTestUtil.getOrAwaitValue(userDao.getAllUsers());
        Assert.assertEquals("Database should still contain only 1 user", 1, allUsers.size());
    }

    @Test
    public void getUserCount_returnsCorrectNumber() {
        // Should be 0 initially
        Assert.assertEquals(0, userDao.getUserCount());

        User user1 = new User("Name 1");
        userDao.insert(user1);
        Assert.assertEquals(1, userDao.getUserCount());

        User user2 = new User("Name 2");
        userDao.insert(user2);
        Assert.assertEquals(2, userDao.getUserCount());

        user1.setName("Alice Cooper");
        userDao.update(user1);
        Assert.assertEquals("Updating a user should not change the row count",
                2, userDao.getUserCount());
    }

}
