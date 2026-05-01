package com.example.fitnesscalendar.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.fitnesscalendar.entities.AiMessage;

import java.util.List;

@Dao
public interface AiDao {
    @Insert
    void insert(AiMessage message);

    @Query("SELECT * FROM ai_messages WHERE user_id = :userId ORDER BY timestamp DESC LIMIT 20")
    List<AiMessage> getChatHistoryForUser(long userId);
}
