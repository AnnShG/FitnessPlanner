package com.example.fitnesscalendar.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.fitnesscalendar.entities.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Category categories);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Category> categories);

    @Update
    void update(Category categories);

    @Delete
    void delete(Category categories);

    @Query("SELECT * FROM categories")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();
}
