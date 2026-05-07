package com.example.fitnesscalendar.relations;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.example.fitnesscalendar.entities.CalendarDay;
import com.example.fitnesscalendar.entities.Workout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "calendar_day_workout_cross_ref",
        primaryKeys = {"calendar_day_id", "workout_id"},
        indices = {@Index("workout_id")},
        foreignKeys = {
                @ForeignKey(entity = CalendarDay.class,
                        parentColumns = "calendar_day_id",
                        childColumns = "calendar_day_id",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Workout.class,
                        parentColumns = "workout_id",
                        childColumns = "workout_id",
                        onDelete = ForeignKey.CASCADE)
        })
public class CalendarDayWorkoutCrossRef {
    @ColumnInfo(name = "calendar_day_id")
    public long calendarDayId;

    @ColumnInfo(name = "workout_id")
    public long workoutId;

    @ColumnInfo(name = "is_completed")
    public boolean isCompleted = false;
}
