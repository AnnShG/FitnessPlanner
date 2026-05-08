package com.example.fitnesscalendar.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.relations.FullWorkoutRecord;
import com.example.fitnesscalendar.relations.WorkoutExerciseCrossRef;

import java.util.List;

@Dao
public interface WorkoutDao {
    @Insert
    long insert(Workout workout);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWorkoutExerciseCrossRef(WorkoutExerciseCrossRef crossRef);

    @Transaction
    @Query("SELECT * FROM workouts WHERE owner_id IS NULL OR owner_id = :userId")
    LiveData<List<FullWorkoutRecord>> getFullWorkoutRecords(long userId);

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id = :workoutId")
    LiveData<FullWorkoutRecord> getFullWorkoutById(long workoutId);

    @Transaction
    @Query("SELECT exercises.* FROM exercises " +
            "INNER JOIN workout_exercise_cross_ref ON exercises.exercise_id = workout_exercise_cross_ref.exercise_id " +
            " WHERE workout_exercise_cross_ref.workout_id = :workoutId ")
    LiveData<List<Exercise>> getExercisesForWorkout(long workoutId);

    @Update
    void update(Workout workout);

    @Delete
    void delete(Workout workout);

    @Query("SELECT * FROM workouts")
    List<Workout> getAllWorkouts();

    // Deletes the links between a workout and an exercise (the row in a join table)
    @Query("DELETE FROM workout_exercise_cross_ref WHERE workout_id = :workoutId")
    void deleteExercisesForWorkout(long workoutId);

    @Query("DELETE FROM calendar_day_workout_cross_ref " +
            "WHERE workout_id = :workoutId " +
            "AND calendar_day_id IN (SELECT calendar_day_id FROM calendar_days WHERE user_id = :userId) " +
            "AND is_completed = 0")
    void deleteOnlyPlannedWorkoutsFromCalendar(long userId, long workoutId);

    @Transaction
    @Query("SELECT DISTINCT w.* FROM workouts w " +
            "INNER JOIN workout_exercise_cross_ref we ON w.workout_id = we.workout_id " +
            "INNER JOIN exercise_category_cross_ref ec ON we.exercise_id = ec.exercise_id " +
            "WHERE w.owner_id = :userId " +
            "AND (:searchQuery IS NULL OR w.title LIKE '%' || :searchQuery || '%') " +
            "AND ec.category_id IN (:categoryIds)")
    LiveData<List<FullWorkoutRecord>> getWorkoutsFilteredAndSearched(long userId, List<Long> categoryIds, String searchQuery);

    @Transaction
    @Query("SELECT * FROM workouts WHERE owner_id = :userId " +
            "AND (:searchQuery IS NULL OR title LIKE '%' || :searchQuery || '%')")
    LiveData<List<FullWorkoutRecord>> getWorkoutsBySearchOnly(long userId, String searchQuery);

    /**
     * Counts all workouts scheduled within a specific date range for a monthly view
     * Joins with calendar_days to access the actual epoch date
     */
    @Query("SELECT COUNT(*) FROM calendar_day_workout_cross_ref ref " +
            "INNER JOIN calendar_days d ON ref.calendar_day_id = d.calendar_day_id " +
            "WHERE d.date >= :start AND d.date <= :end")
    LiveData<Integer> getTotalWorkoutsInMonth(long start, long end);

    /**
     * Counts only completed workouts within a specific date range
     */
    @Query("SELECT COUNT(*) FROM calendar_day_workout_cross_ref ref " +
            "INNER JOIN calendar_days d ON ref.calendar_day_id = d.calendar_day_id " +
            "WHERE ref.is_completed = 1 AND d.date >= :start AND d.date <= :end")
    LiveData<Integer> getCompletedWorkoutsInMonth(long start, long end);
}
