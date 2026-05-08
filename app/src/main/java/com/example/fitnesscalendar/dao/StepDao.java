package com.example.fitnesscalendar.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fitnesscalendar.entities.Step;

import java.util.List;

@Dao
public interface StepDao {

    @Insert
    void insert(Step step);

    @Update
    void update(Step step);

    @Query("SELECT * FROM steps WHERE exercise_id = :exerciseId ORDER BY step_number ASC")
    List<Step> getStepsForExercise(long exerciseId);

    // Delete existing steps before inserting new ones to avoid duplicates
    @Query("DELETE FROM steps WHERE exercise_id = :exerciseId")
    void deleteStepsByExerciseId(long exerciseId);
}

