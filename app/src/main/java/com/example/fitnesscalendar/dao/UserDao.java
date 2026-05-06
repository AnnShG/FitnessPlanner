package com.example.fitnesscalendar.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.relations.UserWithGoals;
//import com.example.fitnesscalendar.relations.UsersWithWorkouts;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("SELECT * FROM users WHERE user_id = :userId")
    User getUserById(int userId);

    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    LiveData<UserWithGoals> getUserWithGoals(long userId);

    @Transaction
    @Query("SELECT * FROM users LIMIT 1") // one user
    LiveData<UserWithGoals> getLatestUser();

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}
