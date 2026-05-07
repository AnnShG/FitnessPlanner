package com.example.fitnesscalendar.relations;

import androidx.room.ColumnInfo;

/**
 * A Projection POJO class used to represent a summary of a workout that has been scheduled on the calendar.
 * It is used by the DAO to return a specific subset of columns (ID, Title, Colour) from the 'workouts' table
 */
public class PlannedWorkoutInfo {
    @ColumnInfo(name = "workout_id")
    public long workoutId;
    public String title;
    public Integer colour;
}
