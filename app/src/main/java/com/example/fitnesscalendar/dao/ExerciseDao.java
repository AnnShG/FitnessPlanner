package com.example.fitnesscalendar.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.relations.ExerciseCategoryCrossRef;
import com.example.fitnesscalendar.relations.ExerciseSummary;
import com.example.fitnesscalendar.relations.FullExerciseRecord;

import java.util.List;

@Dao
public interface ExerciseDao {
    @Insert
    long insert(Exercise exercise);

    @Update
    void update(Exercise exercise);

    // method creates the "Link" between an Exercise and a Category.
    @Insert(onConflict = OnConflictStrategy.REPLACE) // prevents linking the same Ex to the same Cat
    void insertExerciseCategoryCrossRef(ExerciseCategoryCrossRef crossRef);

    @Transaction // required because Room runs several queries (exercises, steps, categories)
    @Query("SELECT * FROM exercises")
    LiveData<List<FullExerciseRecord>> getFullExerciseRecords();

    @Query("SELECT * FROM exercises")
    LiveData<List<ExerciseSummary>> getExerciseSummaries();

    @Query("SELECT * FROM exercises")
    LiveData<List<Exercise>> getAllExercisesOnly();

    @Query("SELECT * FROM exercise_category_cross_ref WHERE exercise_id = :exId")
    List<ExerciseCategoryCrossRef> getCrossRefsForExercise(long exId);

    @Transaction
    @Query("SELECT * FROM exercises WHERE exercise_id = :id")
    LiveData<FullExerciseRecord> getFullExerciseById(long id);

    @Delete
    void delete(Exercise exercise);

    // Delete existing steps before inserting new ones to avoid duplicates
    @Query("DELETE FROM steps WHERE exercise_id = :exerciseId")
    void deleteStepsByExerciseId(long exerciseId);

    // Delete existing categories before inserting new ones to avoid duplicates
    @Query("DELETE FROM exercise_category_cross_ref WHERE exercise_id = :exerciseId")
    void deleteCategoryCrossRefsByExerciseId(long exerciseId);

    // TEST
    @Transaction
    @Query("SELECT DISTINCT e.* FROM exercises e " +
            "INNER JOIN exercise_category_cross_ref ec ON e.exercise_id = ec.exercise_id " +
            "WHERE (:searchQuery IS NULL OR e.title LIKE '%' || :searchQuery || '%') " +
            "AND ec.category_id IN (:categoryIds)")
    LiveData<List<FullExerciseRecord>> getExercisesFilteredAndSearched(List<Long> categoryIds, String searchQuery);

    @Transaction
    @Query("SELECT * FROM exercises WHERE :searchQuery IS NULL OR title LIKE '%' || :searchQuery || '%'")
    LiveData<List<FullExerciseRecord>> getExercisesBySearchOnly(String searchQuery);

    // Get ALL exercises
//    @Transaction
//    @Query("SELECT * FROM exercises WHERE owner_id IS NULL or owner_id = :userId")
//    LiveData<List<FullExerciseRecord>> getAvailableExercises(long userId);

    // Get ONLY the exercises created by the user:
//    @Transaction
//    @Query("SELECT * FROM exercises WHERE owner_id = :userId")
//    LiveData<List<FullExerciseRecord>> getMyCustomExercises(long userId);

//    @Transaction
//    @Query("SELECT * FROM exercises WHERE owner_id IS NULL")
//    LiveData<List<FullExerciseRecord>> getPreDefinedExercises();
}
