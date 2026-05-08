package com.example.fitnesscalendar.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// step is a child of Exercise
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(
        tableName = "steps",
        foreignKeys = @ForeignKey(
                entity = Exercise.class, // parent entity (step is dependent on exercise)
                parentColumns = "exercise_id", // Exercise PK, should match id in entity
                childColumns = "exercise_id", // FK in Steps - the link to Exercise entity
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("exercise_id")} // for each set of steps its own exercise
)
public class Step {
    @PrimaryKey(autoGenerate = true)
    public long stepId;

    @ColumnInfo(name = "exercise_id")
    public long exerciseId; // foreign key to Exercise

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "step_number")
    public int stepNumber;

    public Step(long exerciseId, int stepNumber, String description) {
        this.exerciseId = exerciseId;
        this.description = description;
        this.stepNumber = stepNumber;
    }
}
