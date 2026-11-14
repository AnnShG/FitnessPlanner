package com.example.fitnesscalendar.relations;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Workout;

import java.util.List;

// query the entities - combines two tables - used to read data
// fetch a workout with its exercises
// Room load many-to-many relations (rows)
public class FullWorkoutRecord {

    @Embedded
    public Workout workout;

    @Relation(
            parentColumn = "workout_id",
            entityColumn = "exercise_id", // FK column inside join table
            associateBy = @Junction(
            value = WorkoutExerciseCrossRef.class,
            parentColumn = "workout_id", // PK in workouts table
            entityColumn = "exercise_id"  // PK in exercises table
    )
    )
    public List<Exercise> exercises;
}
