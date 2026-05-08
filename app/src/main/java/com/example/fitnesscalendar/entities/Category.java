package com.example.fitnesscalendar.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // required by Room
@AllArgsConstructor
@Entity(
        tableName = "categories",
        indices = {@Index(value = {"name"}, unique = true)}
)
public class Category {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    public Long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "category_group")
    public String categoryGroup;

    public Category(String name) {
        this.name = name;
    }
}
