package com.example.fitnesscalendar;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.AiDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.AiMessage;
import com.example.fitnesscalendar.entities.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

/**
 * Instrumentation test for AiDao.
 * Verifies that AI messages are correctly stored, ordered, and retrieved per user.
 */
@RunWith(AndroidJUnit4.class)
public class AiDaoTest {
    private AppDatabase db;
    private AiDao aiDao;
    private UserDao userDao;
    private long testUserId;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        // Create an in-memory database for testing
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        aiDao = db.aiDao();
        userDao = db.userDao();

        User user = new User();
        user.name = "Test User";
        user.createdAt = new Date();
        testUserId = userDao.insert(user);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertAndGetHistory_ReturnsCorrectDataInDescOrder() throws InterruptedException {

        AiMessage first = new AiMessage(testUserId, "user", "Hello");
        aiDao.insert(first);

        Thread.sleep(100); // Create messages with a small delay to ensure distinct timestamps

        AiMessage second = new AiMessage(testUserId, "model", "Hi there!");
        aiDao.insert(second);

        List<AiMessage> history = aiDao.getChatHistoryForUser(testUserId);

        assertEquals(2, history.size());
        assertEquals("model", history.get(0).getRole());
        assertEquals("Hi there!", history.get(0).getContent());

        assertEquals("user", history.get(1).getRole());
        assertEquals("Hello", history.get(1).getContent());
    }

}
