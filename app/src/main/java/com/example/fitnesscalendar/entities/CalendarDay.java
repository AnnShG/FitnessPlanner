package com.example.fitnesscalendar.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "calendar_days",
        indices = {@Index(value = {"user_id", "date"}, unique = true)},
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "user_id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE))
public class CalendarDay {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "calendar_day_id")
    public long calendarDayId;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "date")
    public Long date;


}
