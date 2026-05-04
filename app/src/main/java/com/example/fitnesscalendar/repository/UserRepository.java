package com.example.fitnesscalendar.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.fitnesscalendar.dao.GoalDao;
import com.example.fitnesscalendar.dao.UserDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Goal;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.relations.UserWithGoals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private final UserDao userDao;
    private final GoalDao goalDao;

    // Mapping goal titles + subtitles
    public static final String GOAL_LOSE_WEIGHT = "Lose Weight";
    public static final String GOAL_BUILD_MUSCLE = "Build Muscle";
    public static final String GOAL_GET_STRONGER = "Get Stronger";
    public static final String GOAL_STAY_FIT = "Stay Fit";
    public static final String GOAL_RECOVER_INJURY = "Recover after injury";
    public static final String GOAL_STAY_ACTIVE = "Stay active";

    private static final Map<String, String> GOAL_SUBTITLES = new HashMap<>();

    static {
        GOAL_SUBTITLES.put(GOAL_LOSE_WEIGHT, "And see what the discipline can create");
        GOAL_SUBTITLES.put(GOAL_BUILD_MUSCLE, "Increase strength and body mass");
        GOAL_SUBTITLES.put(GOAL_GET_STRONGER, "Focus on power and endurance");
        GOAL_SUBTITLES.put(GOAL_STAY_FIT, "Maintain a healthy lifestyle");
        GOAL_SUBTITLES.put(GOAL_RECOVER_INJURY, "Gentle progression to full health");
        GOAL_SUBTITLES.put(GOAL_STAY_ACTIVE, "Daily movement for better mood");
    }

    // runs DB on background thread, because Android doesn't allow it to run on main thread
    // a single executor for the whole app
    private static final ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(2);

    public UserRepository(Application app) {
        AppDatabase db = AppDatabase.getDatabase(app);
        userDao = db.userDao();
        goalDao = db.goalDao();
    }

    // for testing purposes
    public UserRepository(UserDao userDao, GoalDao goalDao) {
        this.userDao = userDao;
        this.goalDao = goalDao;
    }

    public void insertUserWithGoals(User user, List<Goal> goals) {
        databaseExecutor.execute(() -> {
            long newUserId = userDao.insert(user);

            for (Goal goal : goals) {
                goal.setUserId(newUserId);
                if (!goal.isCustom() && GOAL_SUBTITLES.containsKey(goal.getGoalTitle())) {
                    goal.setGoalSubtitle(GOAL_SUBTITLES.get(goal.getGoalTitle()));
                }
                goalDao.insert(goal);
            }
        });
    }

    public LiveData<UserWithGoals> getLatestUser() {
        return userDao.getLatestUser();
    }

    public void updateGoal(Goal goal) {
        databaseExecutor.execute(() -> {
            goalDao.update(goal);
        });
    }

    public void updateUser(User user) {
        databaseExecutor.execute(() -> {
            userDao.update(user);
        });
    }

    public boolean hasUser() {
        return userDao.getUserCount() > 0;
    }

    public void updateUserGoals(Long userId, List<String> goalTitles, String customGoalText) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // dlete existing goals for this user
            goalDao.deleteGoalsByUserId(userId);

            // insert predefined goals
            for (String title : goalTitles) {
                Goal goal = new Goal();
                goal.setUserId(userId);
                goal.setGoalTitle(title);
                goal.setCustom(false);
                if (GOAL_SUBTITLES.containsKey(title)) {
                    goal.setGoalSubtitle(GOAL_SUBTITLES.get(title));
                }
                goalDao.insert(goal);
            }

            // insert custom goal if text is not empty
            if (customGoalText != null && !customGoalText.isEmpty()) {
                Goal custom = new Goal();
                custom.setUserId(userId);
                custom.setGoalTitle(customGoalText);
                custom.setCustom(true);
                custom.setGoalSubtitle("My personal goal");
                goalDao.insert(custom);
            }
        });
    }
}
