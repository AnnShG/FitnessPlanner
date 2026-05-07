package com.example.fitnesscalendar.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.fitnesscalendar.entities.CalendarDay;
import com.example.fitnesscalendar.relations.CalendarDayWorkoutCrossRef;
import com.example.fitnesscalendar.relations.DateColourResult;
import com.example.fitnesscalendar.relations.PlannedWorkoutInfo;

import java.util.List;

/**
 * Data Access Object (DAO) for managing calendar-related database operations.
 * Handles the mapping between users, specific dates, and the workouts scheduled on those dates.
 */
@Dao
public interface CalendarDayDao {
    @Insert
    long insert(CalendarDay days); // insert a new day and return its auto-generated Primary Key

    /**
     * Ensures a CalendarDay record exists for a specific date before try to attach a workout to it.
     * @param userId The ID of the current user.
     * @param epochDay The date represented as a Long
     * @return The ID of the existing or newly created calendar day.
     */
    @Transaction
    default long getOrCreateDayId(long userId, long epochDay) {
        Long id = getDayIdByDate(userId, epochDay);
        if (id != null) {
            return id;
        } else {
            CalendarDay newDay = new CalendarDay();
            newDay.userId = userId;
            newDay.date = epochDay;
            return  insert(newDay);
        }
    }

    /**
     * Helper method to find if a specific date already exists in the table for a specific user.
     */
    @Query("SELECT calendar_day_id FROM calendar_days WHERE user_id = :userId AND date = :date")
    Long getDayIdByDate(long userId, long date);

    @Query("SELECT COUNT(*) FROM calendar_day_workout_cross_ref")
    int getCrossRefCount();

    // --- Many-to-Many Relationship Logic (Linking Workouts to Days) ---

    /**
     * Creates a link between a workout and a calendar day.
     * Strategy is set to IGNORE because the UI already validates and prevents
     * the user from selecting a date that already contains this specific workout.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCalendarDayWorkoutCrossRef(CalendarDayWorkoutCrossRef crossRef);

    /**
     * Retrieves a list of all planned workouts for a user, including their dates and colors.
     * Used to draw the colored dots on the calendar grid.
     */
    @Transaction
    @Query("SELECT cd.date, w.colour, w.workout_id, w.title, ref.is_completed FROM calendar_days cd " +
            "INNER JOIN calendar_day_workout_cross_ref ref ON cd.calendar_day_id = ref.calendar_day_id " +
            "INNER JOIN workouts w ON ref.workout_id = w.workout_id " +
            "WHERE cd.user_id = :userId")
    LiveData<List<DateColourResult>> getCalendarWorkoutDots(long userId);

    /**
     * Returns the number of workouts attached to a specific day.
     * Used to enforce the UI limit of 3 workouts per day.
     */
    @Query("SELECT COUNT(*) FROM calendar_day_workout_cross_ref WHERE calendar_day_id = :dayId")
    int getWorkoutCountForDay(long dayId);

    /**
     * Retrieves unique workouts that have been added to the calendar.
     * Used to populate the management cards (with Edit/Bin buttons) below the calendar.
     */
    @Query("SELECT DISTINCT w.workout_id, w.title, w.colour FROM workouts w " +
            "INNER JOIN calendar_day_workout_cross_ref ref ON w.workout_id = ref.workout_id " +
            "INNER JOIN calendar_days cd ON ref.calendar_day_id = cd.calendar_day_id " +
            "WHERE cd.user_id = :userId")
    LiveData<List<PlannedWorkoutInfo>> getUniquePlannedWorkouts(long userId);

    /**
     * Clears all links for a specific workout before re-inserting new ones during Edit Mode.
     * This ensures that unselected dates are properly removed from the database.
     */
    @Query("DELETE FROM calendar_day_workout_cross_ref " +
            "WHERE workout_id = :workoutId " +
            "AND calendar_day_id IN (SELECT calendar_day_id FROM calendar_days WHERE user_id = :userId)")
    void deleteWorkoutPlanLinks(long userId, long workoutId);

    /**
     * Retrieves unique workouts that are assigned to a specific day.
     * Used to populate the daily workout item cards
     */
    @Transaction
    @Query("SELECT cd.date, w.colour, w.workout_id, w.title, ref.is_completed FROM calendar_days cd " +
            "INNER JOIN calendar_day_workout_cross_ref ref ON cd.calendar_day_id = ref.calendar_day_id " +
            "INNER JOIN workouts w ON ref.workout_id = w.workout_id " +
            "WHERE cd.user_id = :userId AND cd.date = :epochDay")
    LiveData<List<DateColourResult>> getWorkoutsForSpecificDay(long userId, long epochDay);

    @Query("DELETE FROM calendar_day_workout_cross_ref " +
            "WHERE workout_id = :workoutId " +
            "AND calendar_day_id = (SELECT calendar_day_id FROM calendar_days WHERE user_id = :userId AND date = :epochDay)")
    void deleteSpecificWorkoutPlan(long userId, long workoutId, long epochDay);

    @Query("UPDATE calendar_day_workout_cross_ref SET is_completed = :completed " +
            "WHERE workout_id = :workoutId AND calendar_day_id = " +
            "(SELECT calendar_day_id FROM calendar_days WHERE user_id = :userId AND date = :epochDay)")
    void updateWorkoutCompletion(long userId, long workoutId, long epochDay, boolean completed);



    /**
     * Completely removes a workout's plan from the calendar for a specific user.
     * Triggered when the user clicks the 'Bin' icon on a workout card.
     */
//    @Query("DELETE FROM calendar_day_workout_cross_ref " +
//            "WHERE workout_id = :workoutId " +
//            "AND calendar_day_id IN (SELECT calendar_day_id FROM calendar_days WHERE user_id = :userId)")
//    void deleteWorkoutFromCalendar(long userId, long workoutId);

}
