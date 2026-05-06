package com.example.fitnesscalendar;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.CategoryDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Category;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CategoryDaoTest {
    // This rule is mandatory when testing LiveData
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule(); // for this test, run everything immediately on one thread
    private AppDatabase db;
    private CategoryDao categoryDao;

    @Before
    public void createDb() {
        // Use an in-memory database so the data is wiped after the test
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        categoryDao = db.categoryDao();
    }
    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertAndCountCatetgories() {
        Category cat1 = new Category(null, "Strength", "TYPE");
        Category cat2 = new Category(null, "Cardio", "TYPE");

        categoryDao.insert(cat1);
        categoryDao.insert(cat2);

        int count = categoryDao.getCategoryCount();
        Assert.assertEquals(2, count);
    }

    @Test
    public void insertAllAndGetAll_returnsCorrectList() throws InterruptedException {
        List<Category> list = Arrays.asList(
                new Category(null, "Yoga", "BASIC"),
                new Category(null, "HIIT", "TYPE")
        );

        categoryDao.insertAll(list);

        List<Category> result = LiveDataTestUtil.getOrAwaitValue(categoryDao.getAllCategories());

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream().anyMatch(c -> c.getName().equals("Yoga")));
    }

    @Test
    public void updateAndDeleteCategory() throws InterruptedException {
        Category cat = new Category(null, "Initial Name", "BASIC");
        categoryDao.insert(cat);

        List<Category> initialList = LiveDataTestUtil.getOrAwaitValue(categoryDao.getAllCategories());
        Category savedCat = initialList.get(0);

        savedCat.setName("New Name");
        categoryDao.update(savedCat);

        List<Category> updatedList = LiveDataTestUtil.getOrAwaitValue(categoryDao.getAllCategories());
        Assert.assertEquals("New Name", updatedList.get(0).getName());

        categoryDao.delete(savedCat);
        Assert.assertEquals(0, categoryDao.getCategoryCount());
    }
}
