package com.example.fitnesscalendar.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 1:M
@Data
@AllArgsConstructor
@Entity(
        tableName = "goals",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "user_id", // points to user's PK
                childColumns = "user_id", // points to goal's FK
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("user_id")}
)
public class Goal {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "goal_id")
    public long goalId;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "goal_title")
    public String goalTitle;

    @ColumnInfo(name = "goal_subtitle")
    public String goalSubtitle;

    @ColumnInfo(name = "is_custom")
    public boolean isCustom;


    public Goal() {}

    // 2. Add this constructor for your tests to use
    public Goal(long userId, String goalTitle, boolean isCustom) {
        this.userId = userId;
        this.goalTitle = goalTitle;
        this.isCustom = isCustom;
    }
}
