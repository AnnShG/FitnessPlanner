package com.example.fitnesscalendar.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.fitnesscalendar.dao.*;
import com.example.fitnesscalendar.entities.*;
import com.example.fitnesscalendar.relations.*;

/**
 * The Main Database Class for the application.
 * This class serves as the central hub for Room persistence, registering all
 * tables (Entities) and providing access points (DAOs) to interact with the data.
 */
@Database(
        entities= {
                User.class, CalendarDay.class, Quote.class, Exercise.class, Workout.class,
                Category.class, Step.class, Goal.class, AiMessage.class,
                ExerciseCategoryCrossRef.class, WorkoutExerciseCrossRef.class, CalendarDayWorkoutCrossRef.class,
},
        version = 30,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final int NUMBER_OF_THREADS = 4; // number of bg threads to use for database operations
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Singleton instance to prevent multiple database objects being opened simultaneously
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract GoalDao goalDao();
    public abstract CalendarDayDao calendarDayDao();
    public abstract ExerciseDao exerciseDao();
    public abstract CategoryDao categoryDao();
    public abstract StepDao stepDao();
    public abstract WorkoutDao workoutDao();
    public abstract AiDao aiDao();

    /**
     * Allows setting a test instance of the database
     * Used in instrumentation tests to provide an in-memory database
     */
    @VisibleForTesting
    public static void setTestInstance(AppDatabase db) {
        INSTANCE = db;
    }

    /**
     * Database Lifecycle Callback.
     */
    static final Migration MIGRATION_28_29 = new Migration(28, 29) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS user_workout_cross_ref");
        }
    };

    static final Migration MIGRATION_29_30 = new Migration(29, 30) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS ai_messages");
            database.execSQL("CREATE TABLE IF NOT EXISTS `ai_messages` (" +
                    "`message_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`user_id` INTEGER NOT NULL, " +
                    "`role` TEXT, " +
                    "`content` TEXT, " +
                    "`timestamp` INTEGER, " +
                    "FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE CASCADE )");

            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_user_id` ON `ai_messages` (`user_id`) ");
        }
    };

    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            fillCategories();
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            fillCategories();
        }
    };

    /**
     * Pre-populates the 'categories' table with default values
     */
    private static void fillCategories() {
        databaseWriteExecutor.execute(() -> {
            CategoryDao dao = INSTANCE.categoryDao();
            List<Category> predefined = getPredefinedCategories();
            for (Category cat : predefined) {
                dao.insert(cat); // Strategy.IGNORE handles the rest
            }
        });
    }

    /**
     * Singleton getter for the database.
     */
    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class, "fitness_calendar_db")
                                .addCallback(roomCallback)
                                .addMigrations(MIGRATION_28_29, MIGRATION_29_30)
                                .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Hardcoded list of default exercise categories.
     */
    private static List<Category> getPredefinedCategories() {
        List<Category> categories = new ArrayList<>();
        String[] types = {"Strength", "Cardio", "Bodyweight", "Stretching"};
        for (String s : types) categories.add(new Category(null, s, "TYPE"));

        String[] primaryGroups = {"Neck", "Shoulders", "Arms", "Chest", "Back", "Abs", "Glutes", "Legs", "Full Body"};
        for (String s : primaryGroups) categories.add(new Category(null, s, "BASIC"));

        String[] advancedGroups = {
                "Biceps", "Triceps", "Forearms", "Side Delts", "Front Delts", "Rear Delts", "Upper Chest",
                "Middle Chest", "Lower Chest", "Upper Back", "Lower Back", "Upper Abs", "Lower Abs", "Obliques",
                "Quadriceps", "Hamstrings", "Adductors", "Abductors", "Calves"
        };
        for (String s : advancedGroups) categories.add(new Category(null, s, "ADVANCED"));
        return categories;
    }
}
