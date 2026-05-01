package com.example.fitnesscalendar.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Data;

@Data
@Entity(tableName = "exercises",foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "user_id",
        childColumns = "owner_id",
        onDelete = ForeignKey.CASCADE
),
        indices = {@Index("owner_id")}
)
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "exercise_id")
    public Long exerciseId;

    @ColumnInfo(name = "owner_id")
    public Long ownerId; // Null for pre-defined, UserID for custom

    @ColumnInfo(name = "media_uri")
    public String mediaUri;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "notes")
    public String note;

    @ColumnInfo(name = "user_created")
    public Boolean userCreated; // true for user-created, false for system-provided
}
