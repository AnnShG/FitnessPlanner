package com.example.fitnesscalendar.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.fitnesscalendar.dao.AiDao;
import com.example.fitnesscalendar.dao.CalendarDayDao;
import com.example.fitnesscalendar.dao.WorkoutDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.AiMessage;
import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.relations.CalendarDayWorkoutCrossRef;
import com.example.fitnesscalendar.relations.DateColourResult;
import com.example.fitnesscalendar.relations.FullWorkoutRecord;
import com.example.fitnesscalendar.relations.PlannedWorkoutInfo;
import com.example.fitnesscalendar.relations.WorkoutExerciseCrossRef;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Combines multiple DAO calls - acts a DAO coordinator
// handles complex business logic like M:M relationship mapping and background execution
public class WorkoutRepository {

    private final WorkoutDao workoutDao;
    private final CalendarDayDao calendarDao;
    public AiDao aiDao;

    // A dedicated thread pool for database operations to prevent blocking the UI
    public static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(2);

    public WorkoutRepository(Application app) {
        AppDatabase db = AppDatabase.getDatabase(app);
        workoutDao = db.workoutDao();
        calendarDao = db.calendarDayDao();
        aiDao = db.aiDao();
    }

    // tessting
    public WorkoutRepository(WorkoutDao workoutDao, CalendarDayDao calendarDayDao, AiDao aiDao) {
        this.workoutDao = workoutDao;
        this.calendarDao = calendarDayDao;
        this.aiDao = aiDao;
    }

    /**
     * Inserts a workout and links it to a list of exercises.
     */
    public void insertFullWorkout(Workout workout, List<Long> exerciseIds) {
        databaseExecutor.execute(() -> { // all DB operations run on bg thread
            long newWorkoutId = workoutDao.insert(workout);

            if (exerciseIds != null) {
                for (Long exId : exerciseIds) {
                    WorkoutExerciseCrossRef crossRef = new WorkoutExerciseCrossRef();
                    crossRef.workoutId = newWorkoutId; // new id push to cross ref bridge table
                    crossRef.exerciseId = exId; // exId that we took from the list push to cross ref

                    workoutDao.insertWorkoutExerciseCrossRef(crossRef);
                }
            }
        });
    }

    public LiveData<List<FullWorkoutRecord>> getFullWorkoutRecords(long userId) {
        return workoutDao.getFullWorkoutRecords(userId);
    }

    public LiveData<FullWorkoutRecord> getFullWorkoutById(long id) {
        return workoutDao.getFullWorkoutById(id);
    }

    /**
     * Updates an existing workout and synchronizes its exercise list.
     */
    public void updateFullWorkout(Workout workout, List<Long> existingExercisesIds) {
        databaseExecutor.execute(() -> {
            // update the main workout entity (without exercises)
            workoutDao.update(workout);

            // clear out the old exercise associations in the bridge table
            workoutDao.deleteExercisesForWorkout(workout.getWorkoutId());

            // Re-insert the current list of exercises
            if (existingExercisesIds != null) {
                for (Long exId : existingExercisesIds) {
                    WorkoutExerciseCrossRef crossRef = new WorkoutExerciseCrossRef();
                    crossRef.workoutId = workout.getWorkoutId();
                    crossRef.exerciseId = exId;
                    workoutDao.insertWorkoutExerciseCrossRef(crossRef);
                }
            }
        });
    }

    public void deleteWorkout(Workout workout) {
        databaseExecutor.execute(() -> {
            workoutDao.delete(workout); // clean workout table
        });
    }

    /**
     * Attaches a workout to specific calendar dates.
     */
    public void attachWorkoutToDates(long userId, long workoutId, Set<LocalDate> dates) {
        databaseExecutor.execute(() -> {
            for (LocalDate date : dates) {
                // Get the ID for the date
                long dayId = calendarDao.getOrCreateDayId(userId, date.toEpochDay());
                // Check if day already has 3 workouts
                if (calendarDao.getWorkoutCountForDay(dayId) < 3) {
                    CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef();
                    ref.calendarDayId = dayId;
                    ref.workoutId = workoutId;
                    calendarDao.insertCalendarDayWorkoutCrossRef(ref);
                }
            }
        });
    }

    /**
     * Retrieves the data needed for drawing calendar event dots.
     */
    public LiveData<List<DateColourResult>> getWorkoutColorsForUser(long userId) {
        return calendarDao.getCalendarWorkoutDots(userId);
    }

    /**
     * Retrieves unique workouts currently found on the user's calendar.
     */
    public LiveData<List<PlannedWorkoutInfo>> getUniquePlannedWorkouts(long userId) {
        return calendarDao.getUniquePlannedWorkouts(userId);
    }

    public void deleteOnlyPlannedWorkoutsFromCalendar(long userId, long workoutId) {
        databaseExecutor.execute(() -> {
            workoutDao.deleteOnlyPlannedWorkoutsFromCalendar(userId, workoutId);
        });
    }

    /**
     * Syncs a workout schedule during Edit Mode - select/unselect operations.
     * Respects the completion status of existing workouts.
     */
    public void updateWorkoutPlan(long userId, long workoutId, Set<Long> newEpochDays, Set<Long> completedDays) {
        // create copies of the sets on the calling thread (UI thread).
        final Set<Long> daysToSave = new HashSet<>(newEpochDays);
        final Set<Long> completedSnapshot = (completedDays != null)
                ? new HashSet<>(completedDays)
                : new HashSet<>();

        databaseExecutor.execute(() -> {
            // remove all existing schedule links for this specific workout
            calendarDao.deleteWorkoutPlanLinks(userId, workoutId);

            //  re-insert the new set of dates using the stable copies
            for (Long day : daysToSave) {
                CalendarDayWorkoutCrossRef ref = new CalendarDayWorkoutCrossRef();
                ref.workoutId = workoutId;
                ref.calendarDayId = calendarDao.getOrCreateDayId(userId, day);

                // If this date was in  completed snapshot, keep 'is_completed' true
                ref.isCompleted = completedSnapshot.contains(day);

                calendarDao.insertCalendarDayWorkoutCrossRef(ref);
            }
        });
    }


    public LiveData<List<DateColourResult>> getWorkoutsForSpecificDay(long userId, long epochDay) {
        return calendarDao.getWorkoutsForSpecificDay(userId, epochDay);
    }

    public void deleteSpecificWorkoutPlan(long userId, long workoutId, long epochDay) {
        databaseExecutor.execute(() -> {
            calendarDao.deleteSpecificWorkoutPlan(userId, workoutId, epochDay);
        });
    }

    public void updateWorkoutCompletion(long userId, long workoutId, long epochDay, boolean completed) {
        databaseExecutor.execute(() -> {
            calendarDao.updateWorkoutCompletion(userId, workoutId, epochDay, completed);
        });
    }

    // receives the chat history from the DB for a specific user
    public List<AiMessage> getAiChatHistoryForUser(long userId) {
        return aiDao.getChatHistoryForUser(userId);
    }

    public void saveAiMessage(AiMessage message) {
        AppDatabase.databaseWriteExecutor.execute(() -> aiDao.insert(message));
    }

    public LiveData<List<FullWorkoutRecord>> getWorkoutsFilteredAndSearched(long userId, List<Long> categoryIds, String query) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return workoutDao.getWorkoutsBySearchOnly(userId, query);
        } else {
            return workoutDao.getWorkoutsFiltered(userId, categoryIds, query);
        }
    }

    public LiveData<List<Category>> getAllCategories() {
        return workoutDao.getAllCategories();
    }

    public LiveData<Integer> getTotalWorkoutsInMonth(long start, long end) {
        return workoutDao.getTotalWorkoutsInMonth(start, end);
    }
    public LiveData<Integer> getCompletedWorkoutsInMonth(long start, long end) {
        return workoutDao.getCompletedWorkoutsInMonth(start, end);
    }

}
