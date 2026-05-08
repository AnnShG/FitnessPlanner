package com.example.fitnesscalendar.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fitnesscalendar.entities.Goal;

import java.util.List;

@Dao
public interface GoalDao {
    @Insert
    long insert(Goal goal);

    @Update
    void update(Goal goal);

    //
    // get all goals for a specific user
    @Query("SELECT * FROM goals WHERE user_id = :userId")
    List<Goal> getGoalsForUser(long userId);

    @Query("SELECT * FROM goals WHERE goal_id = :goalId")
    Goal getGoalById(long goalId);

    /**
     * Deletes all goals associated with a specific user.
     * used when updating the goals to ensure the old set is completely replaced.
     */
    @Query("DELETE FROM goals WHERE user_id = :userId")
    void deleteGoalsByUserId(long userId);
}
