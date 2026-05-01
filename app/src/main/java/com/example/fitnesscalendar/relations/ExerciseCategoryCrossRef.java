package com.example.fitnesscalendar.relations;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

// bridge - join table

// unique row in this table is the combination of an exercise and category
@Entity(
        tableName = "exercise_category_cross_ref",
        primaryKeys = {"exercise_id", "category_id"},
        indices = {@Index("category_id")}
)
public class ExerciseCategoryCrossRef {
    @ColumnInfo(name = "exercise_id")
    public long exerciseId;
    @ColumnInfo(name = "category_id")
    public long categoryId;
}

