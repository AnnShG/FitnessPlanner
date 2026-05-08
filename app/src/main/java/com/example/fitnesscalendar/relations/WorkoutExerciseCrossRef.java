package com.example.fitnesscalendar.relations;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Workout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(
        tableName = "workout_exercise_cross_ref",
        primaryKeys = {"workout_id", "exercise_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Workout.class,
                        parentColumns = "workout_id",
                        childColumns = "workout_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Exercise.class,
                        parentColumns = "exercise_id",
                        childColumns = "exercise_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("exercise_id")} )// improves query performance

public class WorkoutExerciseCrossRef {
    @ColumnInfo(name = "workout_id")
    public long workoutId; // FK

    @ColumnInfo(name = "exercise_id")
    public long exerciseId; // FK
}
