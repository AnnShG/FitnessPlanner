package com.example.fitnesscalendar.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    public Long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "birth_date")
    public Date birthDate;

    @ColumnInfo(name = "gender")
    public String gender;

    public Date createdAt = new Date();


    public User() {}

    // initialised data
    public User(String name) {
        this.name = name;
        this.createdAt = new Date();
    }

    public User(String name, String other) {
        this.name = name;
        this.birthDate = new Date();
        this.gender = other;
    }
}
