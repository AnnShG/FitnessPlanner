package com.example.fitnesscalendar;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.CalendarDayDao;
import com.example.fitnesscalendar.dao.CategoryDao;
import com.example.fitnesscalendar.dao.ExerciseDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.dao.WorkoutDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.relations.CalendarDayWorkoutCrossRef;
import com.example.fitnesscalendar.relations.ExerciseCategoryCrossRef;
import com.example.fitnesscalendar.relations.FullWorkoutRecord;
import com.example.fitnesscalendar.relations.WorkoutExerciseCrossRef;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WorkoutDaoTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private WorkoutDao workoutDao;
    private ExerciseDao exerciseDao;
    private CalendarDayDao calendarDayDao;
    private CategoryDao categoryDao;

    private long userId;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        workoutDao = db.workoutDao();
        exerciseDao = db.exerciseDao();
        calendarDayDao = db.calendarDayDao();
        categoryDao = db.categoryDao();

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

    @Test
    public void getWorkoutById() {
        Workout workout = new Workout();
        workout.title = "Test Workout";
        workout.ownerId = userId;
        long id = workoutDao.insert(workout);

        Workout retrieved = workoutDao.getAllWorkouts().get(0);
        Assert.assertEquals("Test Workout", retrieved.title);
    }

    @Test
    public void getAllWorkouts() {
        Workout workout1 = new Workout();
        workout1.title = "Workout 1";
        workout1.ownerId = userId;
        workoutDao.insert(workout1);

        Workout workout2 = new Workout();
        workout2.title = "Workout 2";
        workout2.ownerId = userId;
        workoutDao.insert(workout2);

        Workout workout3 = new Workout();
        workout3.title = "Workout 3";
        workout3.ownerId = userId;
        workoutDao.insert(workout3);

        List<Workout> allWorkouts = workoutDao.getAllWorkouts();
        Assert.assertEquals(3, allWorkouts.size());
    }

    @Test
    public void getTotalWorkoutsInMonth() throws InterruptedException {
        Workout workout1 = new Workout();
        workout1.title = "Workout 1";
        workout1.ownerId = userId;
        long id1 = workoutDao.insert(workout1);

        Workout workout2 = new Workout();
        workout2.title = "Workout 2";
        workout2.ownerId = userId;
        long id2 = workoutDao.insert(workout2);

        // Insert some calendar days into the database
        long epochDay1 = 1000L;
        long dayId1 = calendarDayDao.getOrCreateDayId(userId, epochDay1);

        long epochDay2 = 2000L;
        long dayId2 = calendarDayDao.getOrCreateDayId(userId, epochDay1);

        // Link the workouts to the calendar days
        CalendarDayWorkoutCrossRef ref1 = new CalendarDayWorkoutCrossRef();
        ref1.calendarDayId = dayId1;
        ref1.workoutId = id1;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref1);

        CalendarDayWorkoutCrossRef ref2 = new CalendarDayWorkoutCrossRef();
        ref2.calendarDayId = dayId2;
        ref2.workoutId = id2;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref2);

        long start = 500L;
        long end = 2500L;
        int totalWorkouts = LiveDataTestUtil.getOrAwaitValue(workoutDao.getTotalWorkoutsInMonth(start, end));
        Assert.assertNotNull(totalWorkouts);
        Assert.assertEquals(2, totalWorkouts);
    }

    @Test
    public void getCompletedWorkoutsInMonth() throws InterruptedException {
        Workout workout1 = new Workout();
        workout1.title = "Workout 1";
        workout1.ownerId = userId;
        long id1 = workoutDao.insert(workout1);

        Workout workout2 = new Workout();
        workout2.title = "Workout 2";
        workout2.ownerId = userId;
        long id2 = workoutDao.insert(workout2);

        // Insert some calendar days into the database
        long epochDay1 = 1000L;
        long dayId1 = calendarDayDao.getOrCreateDayId(userId, epochDay1);

        long epochDay2 = 2000L;
        long dayId2 = calendarDayDao.getOrCreateDayId(userId, epochDay2);

        // Link the workouts to the calendar days
        CalendarDayWorkoutCrossRef ref1 = new CalendarDayWorkoutCrossRef();
        ref1.calendarDayId = dayId1;
        ref1.workoutId = id1;
        ref1.isCompleted = true;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref1);

        CalendarDayWorkoutCrossRef ref2 = new CalendarDayWorkoutCrossRef();
        ref2.calendarDayId = dayId2;
        ref2.workoutId = id2;
        ref2.isCompleted = false;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref2);

        long start = 500L;
        long end = 2500L;

        int completedWorkouts = LiveDataTestUtil.getOrAwaitValue(workoutDao.getCompletedWorkoutsInMonth(start, end));
        Assert.assertNotNull(completedWorkouts);
        Assert.assertEquals(1, completedWorkouts);
    }

    @Test
    public void deleteOnlyPlannedWorkoutsFromCalendar() throws InterruptedException {
        Workout workout1 = new Workout();
        workout1.title = "Workout 1";
        workout1.ownerId = userId;
        long id1 = workoutDao.insert(workout1);

        Workout workout2 = new Workout();
        workout2.title = "Workout 2";
        workout2.ownerId = userId;
        long id2 = workoutDao.insert(workout2);

        // Insert some calendar days into the database
        long epochDay1 = 1000L;
        long dayId1 = calendarDayDao.getOrCreateDayId(userId, epochDay1);

        long epochDay2 = 2000L;
        long dayId2 = calendarDayDao.getOrCreateDayId(userId, epochDay2);

        // Link the workouts to the calendar days
        CalendarDayWorkoutCrossRef ref1 = new CalendarDayWorkoutCrossRef();
        ref1.calendarDayId = dayId1;
        ref1.workoutId = id1;
        ref1.isCompleted = true;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref1);

        CalendarDayWorkoutCrossRef ref2 = new CalendarDayWorkoutCrossRef();
        ref2.calendarDayId = dayId2;
        ref2.workoutId = id2;
        ref2.isCompleted = false;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref2);

        workoutDao.deleteOnlyPlannedWorkoutsFromCalendar(userId, id1);

        int count = calendarDayDao.getWorkoutCountForDay(dayId1);
        Assert.assertEquals(1, count);
    }

    @Test
    public void deleteExercisesForWorkout() throws InterruptedException {
        Workout workout = new Workout();
        workout.title = "Leg Day";
        workout.ownerId = userId;
        long wId = workoutDao.insert(workout);

        Exercise exercise = new Exercise();
        exercise.title = "Squats";
        workout.ownerId = userId;
        long exId = exerciseDao.insert(exercise);

        WorkoutExerciseCrossRef ref = new WorkoutExerciseCrossRef();
        ref.workoutId = wId;
        ref.exerciseId = exId;
        workoutDao.insertWorkoutExerciseCrossRef(ref);

        workoutDao.deleteExercisesForWorkout(wId); // delete the links in a join table

        List<Exercise> workoutExercises = LiveDataTestUtil.getOrAwaitValue(
                workoutDao.getExercisesForWorkout(wId)
        );

        Assert.assertTrue("The link should be deleted", workoutExercises.isEmpty());

        // Verify the workout and exercise still exist in the DB
        Assert.assertNotNull(workoutDao.getFullWorkoutById(wId));
        Assert.assertNotNull(exerciseDao.getFullExerciseById(exId));
    }

    @Test
    public void getExercisesForWorkout() throws InterruptedException {
        Workout workout = new Workout();
        workout.title = "Leg Day";
        workout.ownerId = userId;
        long wId = workoutDao.insert(workout);

        Exercise exercise = new Exercise();
        exercise.title = "Squats";
        workout.ownerId = userId;
        long exId = exerciseDao.insert(exercise);

        WorkoutExerciseCrossRef ref = new WorkoutExerciseCrossRef();
        ref.workoutId = wId;
        ref.exerciseId = exId;
        workoutDao.insertWorkoutExerciseCrossRef(ref);

        List<Exercise> workoutExercises = LiveDataTestUtil.getOrAwaitValue(
                workoutDao.getExercisesForWorkout(wId)
        );

        Assert.assertEquals(1, workoutExercises.size());
        Assert.assertEquals("Squats", workoutExercises.get(0).title);
    }

    @Test
    public void insertWorkoutExerciseCrossRef() throws InterruptedException {
        Workout workout = new Workout();
        workout.title = "Leg Day";
        workout.ownerId = userId;
        long wId = workoutDao.insert(workout);

        Exercise exercise = new Exercise();
        exercise.title = "Squats";
        workout.ownerId = userId;
        long exId = exerciseDao.insert(exercise);

        WorkoutExerciseCrossRef ref = new WorkoutExerciseCrossRef();
        ref.workoutId = wId;
        ref.exerciseId = exId;
        workoutDao.insertWorkoutExerciseCrossRef(ref);

        List<Exercise> workoutExercises = LiveDataTestUtil.getOrAwaitValue(
                workoutDao.getExercisesForWorkout(wId)
        );

        Assert.assertEquals(1, workoutExercises.size());
        Assert.assertEquals("Squats", workoutExercises.get(0).title);
    }


    @Test
    public void getAllCategories() throws InterruptedException {
        Category cat1 = new Category(null, "Strength", "TYPE");
        Category cat2 = new Category(null, "Cardio", "TYPE");

        categoryDao.insert(cat1);
        categoryDao.insert(cat2);

        List<Category> categories = LiveDataTestUtil.getOrAwaitValue(categoryDao.getAllCategories());
        Assert.assertEquals(2, categories.size());
    }
}