package com.example.fitnesscalendar;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.fitnesscalendar.dao.CategoryDao;
import com.example.fitnesscalendar.dao.ExerciseDao;
import com.example.fitnesscalendar.dao.StepDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Step;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.relations.ExerciseCategoryCrossRef;
import com.example.fitnesscalendar.relations.ExerciseSummary;
import com.example.fitnesscalendar.relations.FullExerciseRecord;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExerciseDaoTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private ExerciseDao exerciseDao;
    private CategoryDao categoryDao;
    private StepDao stepDao;
    private UserDao userDao;
    private long userId;


    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        exerciseDao = db.exerciseDao();
        categoryDao = db.categoryDao();
        stepDao = db.stepDao();
        userDao = db.userDao();
        // user is required for exercise creation
        User user = new User();
        user.name = "Test User";
        userId = userDao.insert(user);
    }

    @Test
    public void insertCategoryLink_RetrievesCorrectly() throws InterruptedException {
        long exId = exerciseDao.insert(new Exercise("Pushup"));
        long catId = categoryDao.insert(new Category(null, "Strength", "TYPE"));

        ExerciseCategoryCrossRef ref = new ExerciseCategoryCrossRef();
        ref.exerciseId = exId;
        ref.categoryId = catId;
        exerciseDao.insertExerciseCategoryCrossRef(ref);

        // is category attached?
        FullExerciseRecord record = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getFullExerciseById(exId));

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
        exerciseDao.insertExerciseCategoryCrossRef(new ExerciseCategoryCrossRef(ex1, catLegs));
        // Link Bench to Chest
        exerciseDao.insertExerciseCategoryCrossRef(new ExerciseCategoryCrossRef(ex2, catChest));

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

    @Test
    public void exerciseFullLifecycleTest() throws InterruptedException {
        Category category = new Category(null, "Strength", "TYPE");
        long catId = categoryDao.insert(category);

        Exercise exercise = new Exercise();
        exercise.title = "Push Up";
        exercise.ownerId = userId;
        long exId = exerciseDao.insert(exercise);

        // Link cat with ex
        exerciseDao.insertExerciseCategoryCrossRef(new ExerciseCategoryCrossRef(exId, catId));

        List<FullExerciseRecord> records = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getFullExerciseRecords());

        Assert.assertEquals(1, records.size());
        Assert.assertEquals("Push Up", records.get(0).exercise.title);
        // Verify the @Transaction relation works
        Assert.assertEquals(1, records.get(0).categories.size());
        Assert.assertEquals("Strength", records.get(0).categories.get(0).name);

        // Change the title
        Exercise exerciseToUpdate = records.get(0).exercise;
        exerciseToUpdate.title = "Diamond Push Up";
        exerciseDao.update(exerciseToUpdate);

        // Verify Update
        List<FullExerciseRecord> updatedRecords = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getFullExerciseRecords());
        Assert.assertEquals("Diamond Push Up", updatedRecords.get(0).exercise.title);

        // Remove the exercise
        exerciseDao.delete(updatedRecords.get(0).exercise);

        // Verify Delete
        List<FullExerciseRecord> finalRecords = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getFullExerciseRecords());
        Assert.assertTrue("List should be empty after deletion", finalRecords.isEmpty());
    }

    // no insert if exercise id already exists
    @Test
    public void update_doesNotInsertNewRow() throws InterruptedException {
        Exercise exercise = new Exercise();
        exercise.title = "Push Up";
        exercise.ownerId = userId;
        long exId = exerciseDao.insert(exercise);

        exercise.exerciseId = exId;
        exercise.title = "New Title";
        exerciseDao.update(exercise);

        List<Exercise> all = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getAllExercisesOnly());
        Assert.assertEquals("Database should still only have 1 row", 1, all.size());
    }

    @Test
    public void getFullExerciseRecords_returnsMultipleStepsAndCategories() throws InterruptedException {
        Exercise exercise = new Exercise();
        exercise.title = "Push Up";
        exercise.ownerId = userId;
        long exId = exerciseDao.insert(exercise);

        stepDao.insert(new Step(exId, 1, "Drop to floor"));
        stepDao.insert(new Step(exId, 2, "Push up"));
        stepDao.insert(new Step(exId, 3, "Jump back up"));

        long catId1 = categoryDao.insert(new Category(null, "Strength", "TYPE"));
        long catId2 = categoryDao.insert(new Category(null, "Full Body", "BASIC"));

        exerciseDao.insertExerciseCategoryCrossRef(new ExerciseCategoryCrossRef(exId, catId1));
        exerciseDao.insertExerciseCategoryCrossRef(new ExerciseCategoryCrossRef(exId, catId2));

        List<FullExerciseRecord> results = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getFullExerciseRecords());

        Assert.assertEquals(1, results.size()); // one exercise row created
        FullExerciseRecord record = results.get(0);

        Assert.assertEquals(3, record.steps.size());
        Assert.assertEquals("Drop to floor", record.steps.get(0).description);

        Assert.assertEquals(2, record.categories.size());
    }

    @Test
    public void getFullExerciseRecords_cleansUpWhenUserDeleted() throws InterruptedException {
        Exercise exercise = new Exercise();
        exercise.title = "Push Up";
        exercise.ownerId = userId;
        long exId = exerciseDao.insert(exercise);

        Assert.assertFalse(LiveDataTestUtil.getOrAwaitValue(exerciseDao.getFullExerciseRecords()).isEmpty());

        // Delete the User (the parent)
        userDao.deleteUserById(userId);

        List<FullExerciseRecord> results = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getFullExerciseRecords());
        Assert.assertTrue("Exercise should be automatically deleted via Cascade", results.isEmpty());
    }

    @Test
    public void getFullExerciseRecords_returnsEmptyListWhenNoData() throws InterruptedException {
        List<FullExerciseRecord> results = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getFullExerciseRecords());
        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getExerciseSummaries_returnsCorrectTitleAndImage() throws InterruptedException {
        Exercise exercise = new Exercise();
        exercise.title = "Push Up";
        exercise.mediaUri = "content://images/1";
        exercise.ownerId = userId;
        exerciseDao.insert(exercise);

        List<ExerciseSummary> summaries = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getExerciseSummaries());

        Assert.assertEquals(1, summaries.size());
        ExerciseSummary summary = summaries.get(0);

        Assert.assertEquals("Push Up", summary.exercise.title);
        Assert.assertEquals("content://images/1", summary.exercise.mediaUri);
    }

    @Test
    public void getExerciseSummaries_returnsMultipleItems() throws InterruptedException {
        Exercise exercise1 = new Exercise();
        exercise1.title = "Push Up";
        exercise1.ownerId = userId;
        long exId1 = exerciseDao.insert(exercise1);

        Exercise exercise2 = new Exercise();
        exercise2.title = "Dips";
        exercise2.ownerId = userId;
        long exId2 = exerciseDao.insert(exercise2);

        Exercise exercise3 = new Exercise();
        exercise3.title = "Leg Press";
        exercise3.ownerId = userId;
        long exId3 = exerciseDao.insert(exercise3);

        List<ExerciseSummary> summaries = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getExerciseSummaries());

        Assert.assertEquals(3, summaries.size());

        // Check if the names exist in the list
        List<String> titles = new ArrayList<>();
        for (ExerciseSummary s : summaries) {
            titles.add(s.exercise.title);
        }

        Assert.assertTrue(titles.contains("Push Up"));
        Assert.assertTrue(titles.contains("Dips"));
        Assert.assertTrue(titles.contains("Leg Press"));
    }

    @Test
    public void getExerciseSummaries_updatesListAutoOnInsert() throws InterruptedException {
        Exercise exercise1 = new Exercise();
        exercise1.title = "Push Up";
        exercise1.ownerId = userId;
        long exId = exerciseDao.insert(exercise1);

        // get the initial list
        List<ExerciseSummary> initial = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getExerciseSummaries());
        Assert.assertEquals(1, initial.size());

        Exercise exercise2 = new Exercise();
        exercise2.title = "Dips";
        exercise2.ownerId = userId;
        long exId2 = exerciseDao.insert(exercise2);

        // Fetch again (LiveData should reflect the new count)
        List<ExerciseSummary> updated = LiveDataTestUtil.getOrAwaitValue(exerciseDao.getExerciseSummaries());
        Assert.assertEquals(2, updated.size());
    }

    @Test
    public void delete_removesCrossRefEntries() throws InterruptedException {
        Exercise exercise = new Exercise();
        exercise.title = "Push Up";
        exercise.ownerId = userId;
        long exId = exerciseDao.insert(exercise);
        exercise.exerciseId = exId; // assign id to object

        long catId = categoryDao.insert(new Category(null, "Strength", "TYPE"));

        // Link them
        ExerciseCategoryCrossRef ref = new ExerciseCategoryCrossRef(exId, catId);
        exerciseDao.insertExerciseCategoryCrossRef(ref);

        // Delete the Exercise
        exerciseDao.delete(exercise);

        List<ExerciseCategoryCrossRef> refs = exerciseDao.getCrossRefsForExercise(exId);
        Assert.assertTrue("CrossRef entries should be deleted automatically", refs.isEmpty());
    }

    @Test
    public void deleteStepsByExerciseId_onlyDeletesTargetSteps() {
        Exercise exercise1 = new Exercise();
        exercise1.title = "Push Up";
        exercise1.ownerId = userId;
        long exId1 = exerciseDao.insert(exercise1);

        Exercise exercise2 = new Exercise();
        exercise2.title = "Dips";
        exercise2.ownerId = userId;
        long exId2 = exerciseDao.insert(exercise2);

        stepDao.insert(new Step(exId1, 1, "Step A1"));
        stepDao.insert(new Step(exId2, 1, "Step B1"));

        exerciseDao.deleteStepsByExerciseId(exId1);

        List<Step> stepsA = stepDao.getStepsForExercise(exId1);
        List<Step> stepsB = stepDao.getStepsForExercise(exId2);

        Assert.assertTrue("Steps for A should be gone", stepsA.isEmpty());
        Assert.assertFalse("Steps for B should still exist", stepsB.isEmpty());
        Assert.assertEquals("Step B1", stepsB.get(0).description);
    }

    @Test
    public void deleteCategoryCrossRefs_clearsCategoriesLinks() {
        Exercise exercise = new Exercise();
        exercise.title = "Push Up";
        exercise.ownerId = userId;
        long exId = exerciseDao.insert(exercise);
        exercise.exerciseId = exId;

        long catId = categoryDao.insert(new Category(null, "Strength", "TYPE"));

        exerciseDao.insertExerciseCategoryCrossRef(new ExerciseCategoryCrossRef(exId, catId));

        exerciseDao.deleteCategoryCrossRefsByExerciseId(exId);

        List<ExerciseCategoryCrossRef> refs = exerciseDao.getCrossRefsForExercise(exId);
        Assert.assertTrue("Links should be cleared", refs.isEmpty());

        // The Category must still exist
        Category cat = categoryDao.getCategoryById(catId);
        Assert.assertNotNull("Category should NOT be deleted, only the link", cat);
    }

    @Test
    public void getExercisesBySearchOnly_filtersCorrectly() throws InterruptedException {
        Exercise exercise1 = new Exercise();
        exercise1.title = "Push Up";
        exercise1.ownerId = userId;
        long exId1 = exerciseDao.insert(exercise1);

        Exercise exercise2 = new Exercise();
        exercise2.title = "Deadlift";
        exercise2.ownerId = userId;
        long exId2 = exerciseDao.insert(exercise2);

        Exercise exercise3 = new Exercise();
        exercise3.title = "Leg Press";
        exercise3.ownerId = userId;
        long exId3 = exerciseDao.insert(exercise3);

        // Add a step to one to verify FullExerciseRecord mapping works during search
        stepDao.insert(new Step(exId1, 1, "Lie on bench"));

        // Test Case A - Partial Match (Middle of string)
        List<FullExerciseRecord> results = LiveDataTestUtil.getOrAwaitValue(
                exerciseDao.getExercisesBySearchOnly("Up")
        );
        Assert.assertEquals(1, results.size());
        Assert.assertEquals("Push Up", results.get(0).exercise.title);
        Assert.assertEquals(1, results.get(0).steps.size()); // Verify relation is intact

        // Test Case B - Case Insensitivity & Start of string
        List<FullExerciseRecord> results2 = LiveDataTestUtil.getOrAwaitValue(
                exerciseDao.getExercisesBySearchOnly("dead")
        );
        Assert.assertEquals(1, results2.size());
        Assert.assertEquals("Deadlift", results2.get(0).exercise.title);

        // Test Case C - Null Search
        List<FullExerciseRecord> allResults = LiveDataTestUtil.getOrAwaitValue(
                exerciseDao.getExercisesBySearchOnly(null)
        );
        Assert.assertEquals(3, allResults.size());

        // Test Case D - No Matches
        List<FullExerciseRecord> emptyResults = LiveDataTestUtil.getOrAwaitValue(
                exerciseDao.getExercisesBySearchOnly("Yoga")
        );
        Assert.assertTrue(emptyResults.isEmpty());
    }

}
