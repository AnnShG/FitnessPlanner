package com.example.fitnesscalendar.relations;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Exercise;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

// bridge - join table

// unique row in this table is the combination of an exercise and category
@NoArgsConstructor // required by Room
@AllArgsConstructor
@Entity(
        tableName = "exercise_category_cross_ref",
        primaryKeys = {"exercise_id", "category_id"},
        indices = {@Index("category_id")},
        foreignKeys = {
                @ForeignKey(
                        entity = Exercise.class,
                        parentColumns = "exercise_id",
                        childColumns = "exercise_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Category.class,
                        parentColumns = "category_id",
                        childColumns = "category_id",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class ExerciseCategoryCrossRef {
    @ColumnInfo(name = "exercise_id")
    public long exerciseId;
    @ColumnInfo(name = "category_id")
    public long categoryId;
}

