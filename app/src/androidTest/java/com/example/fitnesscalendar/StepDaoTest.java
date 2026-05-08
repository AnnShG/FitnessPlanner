package com.example.fitnesscalendar;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.ExerciseDao;
import com.example.fitnesscalendar.dao.StepDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Step;
import com.example.fitnesscalendar.entities.User;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class StepDaoTest {
    private AppDatabase db;
    private StepDao stepDao;
    private ExerciseDao exerciseDao;
    private long exerciseId;
    private long userId;
    private UserDao userDao;



    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        stepDao = db.stepDao();
        exerciseDao = db.exerciseDao();
        userDao = db.userDao();

        User user = new User();
        user.name = "Test User";
        userId = userDao.insert(user);

        // create the parent Exercise
        Exercise exercise = new Exercise();
        exercise.title = "Pushup";
        exerciseId = exerciseDao.insert(exercise);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertStepsOutOfOrder_returnsSortedByStepNumber() {
        Step step3 = new Step(exerciseId, 3, "Stretch");
        Step step1 = new Step(exerciseId, 1, "Get on floor");
        Step step2 = new Step(exerciseId, 2, "Lower chest");

        // Insert goals in a random order
        stepDao.insert(step3);
        stepDao.insert(step1);
        stepDao.insert(step2);

        // Verify the list is sorted 1, 2, 3
        List<Step> results = stepDao.getStepsForExercise(exerciseId);

        Assert.assertEquals(3, results.size());
        Assert.assertEquals(1, results.get(0).getStepNumber());
        Assert.assertEquals(2, results.get(1).getStepNumber());
        Assert.assertEquals(3, results.get(2).getStepNumber());
        Assert.assertEquals("Get on floor", results.get(0).getDescription());
    }

    @Test
    public void getStepsForExercise_onlyReturnsStepsForTargetExercise() {
        long otherExerciseId = exerciseDao.insert(new Exercise("Squat"));

        stepDao.insert(new Step(exerciseId, 1, "Pushup Step"));
        stepDao.insert(new Step(otherExerciseId, 1, "Squat Step"));

        List<Step> pushupSteps = stepDao.getStepsForExercise(exerciseId);

        Assert.assertEquals(1, pushupSteps.size());
        Assert.assertEquals("Pushup Step", pushupSteps.get(0).getDescription());
    }

    @Test
    public void updateStepDescription() {
        Step step = new Step(exerciseId, 1, "Old Description");
        stepDao.insert(step);

        List<Step> list = stepDao.getStepsForExercise(exerciseId);
        Step savedStep = list.get(0);

        savedStep.setDescription("New Description");
        stepDao.update(savedStep);

        List<Step> updatedList = stepDao.getStepsForExercise(exerciseId);
        Assert.assertEquals("New Description", updatedList.get(0).getDescription());
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

        stepDao.deleteStepsByExerciseId(exId1);

        List<Step> stepsA = stepDao.getStepsForExercise(exId1);
        List<Step> stepsB = stepDao.getStepsForExercise(exId2);

        Assert.assertTrue("Steps for A should be gone", stepsA.isEmpty());
        Assert.assertFalse("Steps for B should still exist", stepsB.isEmpty());
        Assert.assertEquals("Step B1", stepsB.get(0).description);
    }


}
