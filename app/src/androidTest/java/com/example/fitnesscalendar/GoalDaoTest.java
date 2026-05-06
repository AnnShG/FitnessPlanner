package com.example.fitnesscalendar;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.GoalDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Goal;
import com.example.fitnesscalendar.entities.User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class GoalDaoTest {
    private AppDatabase db;
    private GoalDao goalDao;
    private UserDao userDao;
    private long userId;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        goalDao = db.goalDao();
        userDao = db.userDao();

        // Create a test user
        User user = new User();
        user.setName("John Doe");
        userId = userDao.insert(user);
    }

    @Test
    public void insertMultipleGoalsForSingleUser() {
        Goal g1 = new Goal(userId, "Lose 5kg", false);
        Goal g2 = new Goal(userId, "Run 5km", false);
        Goal g3 = new Goal(userId, "Eat more protein", true);

        goalDao.insert(g1);
        goalDao.insert(g2);
        goalDao.insert(g3);

        List<Goal> results = goalDao.getGoalsForUser(userId);

        Assert.assertEquals("Should have 3 goals saved", 3, results.size());
        Assert.assertTrue(results.stream().anyMatch(g -> g.goalTitle.equals("Run 5km")));
    }

    @Test
    public void syncGoals_deletesOldAndInsertsNew() {
        goalDao.insert(new Goal(userId, "Old Goal 1", false));
        goalDao.insert(new Goal(userId, "Old Goal 2", false));

        goalDao.deleteGoalsByUserId(userId);

        goalDao.insert(new Goal(userId, "Brand New Goal", true));

        List<Goal> currentGoals = goalDao.getGoalsForUser(userId);
        Assert.assertEquals(1, currentGoals.size());
        Assert.assertEquals("Brand New Goal", currentGoals.get(0).goalTitle);

        Assert.assertFalse(currentGoals.stream().anyMatch(g -> g.goalTitle.contains("Old")));
    }

    @Test
    public void deleteUser_automaticallyDeletesAllGoals() {
        goalDao.insert(new Goal(userId, "Should be deleted", false));

        User userToDelete = new User();
        userToDelete.id = userId;
        userDao.delete(userToDelete);

        List<Goal> orphanGoals = goalDao.getGoalsForUser(userId);
        Assert.assertTrue("Goals should be gone because their owner was deleted", orphanGoals.isEmpty());
    }

}
