package com.example.fitnesscalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fitnesscalendar.dao.CalendarDayDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.dao.WorkoutDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.CalendarDay;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.relations.CalendarDayWorkoutCrossRef;
import com.example.fitnesscalendar.relations.DateColourResult;
import com.example.fitnesscalendar.relations.PlannedWorkoutInfo;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CalendarDayDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private CalendarDayDao calendarDayDao;
    private WorkoutDao workoutDao;
    private UserDao userDao;
    private long userId;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        calendarDayDao = db.calendarDayDao();
        workoutDao = db.workoutDao();
        userDao = db.userDao();

        // user is required for having the calendar
        User user = new User();
        user.name = "Test User";
        userId = userDao.insert(user);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void getOrCreateDayId_CreatesNewDayIfMissing() {
        long epochDay = 12345L;
        long dayId = calendarDayDao.getOrCreateDayId(userId, epochDay);
        
        assertTrue(dayId > 0);
        
        Long retrievedId = calendarDayDao.getDayIdByDate(userId, epochDay);
        assertEquals(Long.valueOf(dayId), retrievedId);
    }

    @Test
    public void getOrCreateDayId_ReturnsExistingDay() {
        long epochDay = 12345L;
        long firstId = calendarDayDao.getOrCreateDayId(userId, epochDay);
        long secondId = calendarDayDao.getOrCreateDayId(userId, epochDay);
        
        assertEquals(firstId, secondId);
    }

    @Test
    public void insertAndGetWorkoutCount_WorksCorrectly() {
        long epochDay = 20000L;
        long dayId = calendarDayDao.getOrCreateDayId(userId, epochDay);
        
        Workout workout = new Workout();
        workout.title = "Test Workout";
        workout.ownerId = userId;
        long workoutId = workoutDao.insert(workout);

        CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef();
        ref.calendarDayId = dayId;
        ref.workoutId = workoutId;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref);

        int count = calendarDayDao.getWorkoutCountForDay(dayId);
        assertEquals(1, count);
    }

    @Test
    public void updateWorkoutCompletion_UpdatesFlag() throws InterruptedException {
        long epochDay = 30000L;
        long dayId = calendarDayDao.getOrCreateDayId(userId, epochDay);
        
        Workout workout = new Workout();
        workout.title = "Cardio";
        workout.ownerId = userId;
        long workoutId = workoutDao.insert(workout);

        CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef();
        ref.calendarDayId = dayId;
        ref.workoutId = workoutId;
        ref.isCompleted = false;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref);

        calendarDayDao.updateWorkoutCompletion(userId, workoutId, epochDay, true);

        // Сheck via the query that returns completion status
        // Use LiveDataTestUtil to observe the value from the LiveData object
        List<DateColourResult> results = LiveDataTestUtil.getOrAwaitValue(calendarDayDao.getWorkoutsForSpecificDay(userId, epochDay));
        assertEquals(1, results.size());
        assertTrue(results.get(0).isCompleted);
    }

    @Test
    public void deleteSpecificWorkoutPlan_RemovesOnlyThatLink() throws InterruptedException {
        long epochDay = 40000L;
        long dayId = calendarDayDao.getOrCreateDayId(userId, epochDay);
        
        Workout workout = new Workout();
        workout.title = "Yoga";
        workout.ownerId = userId;
        long workoutId = workoutDao.insert(workout);

        CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef();
        ref.calendarDayId = dayId;
        ref.workoutId = workoutId;
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref);

        calendarDayDao.deleteSpecificWorkoutPlan(userId, workoutId, epochDay);
        
        int count = calendarDayDao.getWorkoutCountForDay(dayId);
        assertEquals(0, count);
    }

    @Test
    public void getCalendarWorkoutDots_returnsMappedDataCorrectly() throws InterruptedException {
        long epochDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24);

        Workout workout = new Workout();
        workout.title = "Yoga";
        workout.ownerId = userId;
        workout.colour = 0xFF0000;
        long workoutId = workoutDao.insert(workout);

        long dayId = calendarDayDao.insert(new CalendarDay(0, userId, epochDay));

        CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef(dayId, workoutId, true);
        calendarDayDao.insertCalendarDayWorkoutCrossRef(ref);

        List<DateColourResult> dots = LiveDataTestUtil.getOrAwaitValue(
                calendarDayDao.getCalendarWorkoutDots(userId)
        );

        Assert.assertFalse("List should not be empty", dots.isEmpty());
        DateColourResult result = dots.get(0);

        Assert.assertEquals((Long) epochDay, result.date);
        Assert.assertEquals((Integer) 0xFF0000, result.colour);
        Assert.assertTrue(result.isCompleted);
    }

    @Test
    public void getUniquePlannedWorkouts_returnsOnlyScheduledWorkoutsWithoutDuplicates() throws InterruptedException {
        long idA = workoutDao.insert(new Workout("Push Day", 0xFF0000, userId));
        long idB = workoutDao.insert(new Workout("Pull Day", 0x00FF00, userId));
        long idC = workoutDao.insert(new Workout("Leg Day", 0x0000FF, userId));

        long day1 = calendarDayDao.insert(new CalendarDay(0, userId, 19448L));
        long day2 = calendarDayDao.insert(new CalendarDay(0, userId, 19449L));

        // Schedule Workout A on BOTH days (Duplicate links)
        calendarDayDao.insertCalendarDayWorkoutCrossRef(new CalendarDayWorkoutCrossRef(day1, idA, false));
        calendarDayDao.insertCalendarDayWorkoutCrossRef(new CalendarDayWorkoutCrossRef(day2, idA, false));

        // Schedule Workout B on only one day
        calendarDayDao.insertCalendarDayWorkoutCrossRef(new CalendarDayWorkoutCrossRef(day1, idB, false));

        // Query unique planned workouts
        List<PlannedWorkoutInfo> results = LiveDataTestUtil.getOrAwaitValue(
                calendarDayDao.getUniquePlannedWorkouts(userId)
        );

        // Push Day and Pull Day
        Assert.assertEquals(2, results.size());

        // Check that Push Day only appears once despite being on 2 days
        int pushDayCount = 0;
        for (PlannedWorkoutInfo info : results) {
            if (info.workoutId == idA) pushDayCount++;
        }
        Assert.assertEquals("Workout A should only appear once due to DISTINCT", 1, pushDayCount);
    }

    @Test
    public void deleteWorkoutPlanLinks_deletesOnlyTargetedWorkout() throws InterruptedException {
        long userA = userId;
        long userB = userDao.insert(new User("User B"));

        long workoutId = workoutDao.insert(new Workout("Push Day", 0xFF0000, userA));

        long dayA = calendarDayDao.insert(new CalendarDay(0, userA, 19448L));
        long dayB = calendarDayDao.insert(new CalendarDay(0, userB, 19448L));

        calendarDayDao.insertCalendarDayWorkoutCrossRef(new CalendarDayWorkoutCrossRef(dayA, workoutId, false));
        calendarDayDao.insertCalendarDayWorkoutCrossRef(new CalendarDayWorkoutCrossRef(dayB, workoutId, false));

        long workoutId2 = workoutDao.insert(new Workout("Yoga", 0x00FF00, userA));
        calendarDayDao.insertCalendarDayWorkoutCrossRef(new CalendarDayWorkoutCrossRef(dayA, workoutId2, false));

        Assert.assertEquals("There should be 3 links", 3, calendarDayDao.getCrossRefCount());


        calendarDayDao.deleteWorkoutPlanLinks(userA, workoutId);

        List<DateColourResult> userADots = LiveDataTestUtil.getOrAwaitValue(
                calendarDayDao.getCalendarWorkoutDots(userA)
        );
        List<DateColourResult> userBDots = LiveDataTestUtil.getOrAwaitValue(
                calendarDayDao.getCalendarWorkoutDots(userB)
        );

        // User A should still have the "Yoga" workout, but "Push Day" is gone
        Assert.assertEquals(1, userADots.size());
        Assert.assertEquals("Yoga", userADots.get(0).title);

        // User B's "Push Day" should still be there
        Assert.assertEquals(1, userBDots.size());
        Assert.assertEquals("Push Day", userBDots.get(0).title);
    }

}
