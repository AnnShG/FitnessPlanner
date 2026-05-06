package com.example.fitnesscalendar;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.fitnesscalendar.dao.CategoryDao;
import com.example.fitnesscalendar.dao.ExerciseDao;
import com.example.fitnesscalendar.dao.StepDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Step;
import com.example.fitnesscalendar.relations.ExerciseCategoryCrossRef;
import com.example.fitnesscalendar.relations.FullExerciseRecord;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ExerciseDaoTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private ExerciseDao exerciseDao;
    private CategoryDao categoryDao;
    private StepDao stepDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        exerciseDao = db.exerciseDao();
        categoryDao = db.categoryDao();
        stepDao = db.stepDao();
    }

    @Test
    public void insertCategoryLink_RetrievesCorrectly() throws InterruptedException {
        long exId = exerciseDao.insert(new Exercise("Pushup"));
        long catId = categoryDao.insert(new Category(null, "Strength", "TYPE"));

        ExerciseCategoryCrossRef ref = new ExerciseCategoryCrossRef();
        ref.exerciseId = exId;
        ref.categoryId = catId;
        exerciseDao.insertCategoryCrossRef(ref);

        // 3. Verify: Use a 'FullRecord' query to see if the category is now attached
        FullExerciseRecord record = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getExerciseById(exId));

        Assert.assertNotNull(record);
        Assert.assertEquals(1, record.categories.size());
        Assert.assertEquals("Strength", record.categories.get(0).name);
    }

    @Test
    public void testFilteredAndSearchedQuery() throws InterruptedException {
        long ex1 = exerciseDao.insert(new Exercise("Squat"));
        long ex2 = exerciseDao.insert(new Exercise("Bench Press"));
        long catLegs = categoryDao.insert(new Category(null, "Legs", "BASIC"));
        long catChest = categoryDao.insert(new Category(null, "Chest", "BASIC"));

        // Link Squat to Legs
        exerciseDao.insertCategoryCrossRef(new ExerciseCategoryCrossRef(ex1, catLegs));
        // Link Bench to Chest
        exerciseDao.insertCategoryCrossRef(new ExerciseCategoryCrossRef(ex2, catChest));

        // Search for "Squat" in "Legs" category
        List<Long> filter = Arrays.asList(catLegs);
        List<FullExerciseRecord> results = LiveDataTestUtil.getOrAwaitValue(
                exerciseDao.getExercisesFilteredAndSearched(filter, "Squat")
        );

        Assert.assertEquals(1, results.size());
        Assert.assertEquals("Squat", results.get(0).exercise.title);
    }

    @Test
    public void deleteStepsByExerciseId_CleansUpOrphans() {
        long id = exerciseDao.insert(new Exercise("Cleanup Test"));

        stepDao.insert(new Step(id, 1, "Testing Delete"));

        exerciseDao.deleteStepsByExerciseId(id);

        List<Step> remainingSteps = stepDao.getStepsForExercise(id);
        Assert.assertTrue(remainingSteps.isEmpty());
    }

}
