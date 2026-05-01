package com.example.fitnesscalendar.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Data;

@Data
@Entity(
        tableName = "workouts",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "user_id",
                childColumns = "owner_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("owner_id")}
)
public class  Workout {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "workout_id")
    public Long workoutId;

    @ColumnInfo(name = "owner_id")
    public Long ownerId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "notes")
    public String note;

    @ColumnInfo(name = "colour")
    public Integer colour;

    @ColumnInfo(name = "user_created")
    public Boolean userCreated;
}
