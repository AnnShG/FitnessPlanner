package com.example.fitnesscalendar.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity(
        tableName = "ai_messages",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "user_id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("user_id")}
)
public class AiMessage {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "message_id")
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "role")
    public String role; // "user" or "model"

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "timestamp")
    public Date timestamp;

    public AiMessage(long userId, String role, String content) {
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.timestamp = new Date();
    }
}
