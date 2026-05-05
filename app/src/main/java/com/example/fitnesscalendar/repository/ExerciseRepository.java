package com.example.fitnesscalendar.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.fitnesscalendar.dao.CategoryDao;
import com.example.fitnesscalendar.dao.ExerciseDao;
import com.example.fitnesscalendar.dao.StepDao;
import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Step;
import com.example.fitnesscalendar.relations.ExerciseCategoryCrossRef;
import com.example.fitnesscalendar.relations.ExerciseSummary;
import com.example.fitnesscalendar.relations.FullExerciseRecord;

import java.util.List;
import java.util.concurrent.Executors;

import lombok.Getter;

public class ExerciseRepository {
    private final ExerciseDao exerciseDao;
    private final CategoryDao categoryDao;
    private final StepDao stepDao;

    // full record path for the single exercise detail screen
    @Getter // get  allFullExerciseRecords
    private final LiveData<List<FullExerciseRecord>> allFullExerciseRecords;
    private final LiveData<List<ExerciseSummary>> allExerciseSummaries;

    // runs DB on background thread, because Android doesn't allow it to run on main thread
    private static final java.util.concurrent.ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(2);

    public ExerciseRepository(Application app) {
        AppDatabase db = AppDatabase.getDatabase(app); // Give me the database instance

        // retrieving DAOs for exercises, steps and categories, now the repo can talk to DB
        exerciseDao = db.exerciseDao();
        stepDao = db.stepDao();
        categoryDao = db.categoryDao();

        allFullExerciseRecords = exerciseDao.getFullExerciseRecords();
        allExerciseSummaries = exerciseDao.getExerciseSummaries();
    }

    // for testing
    public ExerciseRepository(ExerciseDao exerciseDao, StepDao stepDao, CategoryDao categoryDao) {
        this.exerciseDao = exerciseDao;
        this.stepDao = stepDao;
        this.categoryDao = categoryDao;

        this.allFullExerciseRecords = exerciseDao.getFullExerciseRecords();
        this.allExerciseSummaries = exerciseDao.getExerciseSummaries();
    }

    // this method inserts steps and categories inside the exercise (WRITING into DB) in the bg to not freeze the UI
    public void insertFullExercise(Exercise exercise, List<Step> steps, List<Long> categoryIds) {
        // Validation: Title and at least one category are required for exercise creation
        if (exercise.title == null || exercise.title.trim().isEmpty() || categoryIds == null || categoryIds.isEmpty()) {
            return; // Exercise is not saved to database
        }

        // use the fixed thread pool defined at the top
        databaseExecutor.execute(() -> {

            // 1. Insert the parent Exercise entity.
            long newExerciseId = exerciseDao.insert(exercise); //auto-generated id (15) by Room

            // 2. Insert child 'Step' entity (1:M Relationship).
            if (steps != null) {
                // For each element inside the collection steps, take one element and call it step (var)
                // (int i = 0; i < steps.size(); i++)     Step step = steps.get(i);
                for (Step step : steps) {
                    // assign the parent's generated ID to the child's FK field to link them in the database.
                    step.setExerciseId(newExerciseId);
                    // Persist the step to the 'steps' table via its specific DAO.
                    stepDao.insert(step);
                }
            }

            // 3. Process Category associations (M:M Relationship).
            if (categoryIds != null) {
                // For each category ID inside the list categoryIds ([3, 5, 6]), call it catId.
                for (Long catId : categoryIds) {
                    // Create an instance (row) of the Join Table (Bridge) entity to save there rows
                    ExerciseCategoryCrossRef crossRef = new ExerciseCategoryCrossRef();
                    // Link the parent Exercise ID with the existing Category ID
                    // Every row is saved inside crossRef (bridge) table with each next loop
                    crossRef.exerciseId = newExerciseId; // newId (15) assign to the id from bridge table
                    crossRef.categoryId = catId; // 3 (from the list) is assigned to categoryId, then 4

                    // Persist the relationship to the cross-reference table.
                    exerciseDao.insertCategoryCrossRef(crossRef);
                }
            }
        });
    }

    public LiveData<FullExerciseRecord> getFullExerciseById(long id) {
        return exerciseDao.getFullExerciseById(id);
    }

    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    public void updateExercise(Exercise exercise, List<Step> steps, List<Long> categoryIds) {
        databaseExecutor.execute(() -> {
            exerciseDao.update(exercise);

            exerciseDao.deleteStepsByExerciseId(exercise.getExerciseId());
            if (steps != null) {
                for (Step step : steps) {
                    step.setExerciseId(exercise.getExerciseId());
                    stepDao.insert(step);
                }
            }

            exerciseDao.deleteCategoryCrossRefsByExerciseId(exercise.getExerciseId());
            if (categoryIds != null) {
                for (Long catId : categoryIds) {
                    ExerciseCategoryCrossRef ref = new ExerciseCategoryCrossRef();
                    ref.exerciseId = exercise.getExerciseId();
                    ref.categoryId = catId;
                    exerciseDao.insertCategoryCrossRef(ref);
                }
            }
        });
    }

    public void deleteFullExercise(long id) {
        databaseExecutor.execute(() -> {
            exerciseDao.deleteStepsByExerciseId(id);
            exerciseDao.deleteCategoryCrossRefsByExerciseId(id);

            Exercise exercise = new Exercise();
            exercise.setExerciseId(id);
            exerciseDao.delete(exercise);
        });
    }

    public LiveData<List<FullExerciseRecord>> getExercisesFilteredAndSearched(List<Long> categoryIds, String query) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return exerciseDao.getExercisesBySearchOnly(query);
        } else {
            return exerciseDao.getExercisesFilteredAndSearched(categoryIds, query);
        }
    }

    public LiveData<List<FullExerciseRecord>> getAllFullExercises() {
        return exerciseDao.getFullExerciseRecords();
    }

}
