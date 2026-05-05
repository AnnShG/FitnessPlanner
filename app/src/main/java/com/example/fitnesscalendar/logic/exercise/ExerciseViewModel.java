package com.example.fitnesscalendar.logic.exercise;

import android.app.Application;
import android.util.Pair;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Step;
import com.example.fitnesscalendar.relations.FullExerciseRecord;
import com.example.fitnesscalendar.relations.UserWithGoals;
import com.example.fitnesscalendar.repository.ExerciseRepository;
import com.example.fitnesscalendar.repository.UserRepository;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class ExerciseViewModel extends AndroidViewModel {
    @Setter
    @Getter
    private Long exerciseId;
    @Setter
    @Getter
    private String title;
    @Setter
    @Getter
    private String description;
    @Setter
    @Getter
    private String mediaUri;
    @Setter
    @Getter
    public String notes;
    @Setter
    @Getter
    public Boolean userCreated;

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<List<Long>> filterIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    public final LiveData<List<FullExerciseRecord>> filteredExercises;

    // constructor
    public ExerciseViewModel(@NotNull Application app) {
        this(app, new ExerciseRepository(app), new UserRepository(app));
    }

    // testing
    public ExerciseViewModel(@NotNull Application app, ExerciseRepository exerciseRepository, UserRepository userRepository) {
        super(app);
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;

        LiveData<Pair<List<Long>, String>> combined = new CombinedLiveData<>(filterIds, searchQuery);

        this.filteredExercises = Transformations.switchMap(combined, pair -> {
            List<Long> ids = (pair != null && pair.first != null) ? pair.first : new ArrayList<>();
            String query = (pair != null && pair.second != null) ? pair.second : "";

            if (ids.isEmpty() && query.isEmpty()) {
                return exerciseRepository.getAllFullExercises();
            }
            return exerciseRepository.getExercisesFilteredAndSearched(ids, query);
        });
    }

    public LiveData<UserWithGoals> getLoggedInUser() {
        return userRepository.getLatestUser();
    }

    public LiveData<List<Category>> getAllCategories() {
        return exerciseRepository.getAllCategories();
    }

    public void saveExercise(Exercise exercise, List<Step> steps, List<Long> categoryIds) {
        exerciseRepository.insertFullExercise(exercise, steps, categoryIds);
    }

    public LiveData<FullExerciseRecord> getFullExerciseById(long id) {
        return exerciseRepository.getFullExerciseById(id);
    }

    public LiveData<List<FullExerciseRecord>> getAllFullExerciseRecords() {
        return exerciseRepository.getAllFullExerciseRecords();
    }

    public void updateExercise(Exercise exercise, List<Step> steps, List<Long> categoryIds) {
        exerciseRepository.updateExercise(exercise, steps, categoryIds);
    }

    public void deleteExercise(long id) {
        exerciseRepository.deleteFullExercise(id);
    }

    public void setFilters(List<Long> ids) {
        filterIds.setValue(ids);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public static class CombinedLiveData<F, S> extends MediatorLiveData<Pair<F, S>> {
        public CombinedLiveData(LiveData<F> first, LiveData<S> second) {
            addSource(first, value -> setValue(new Pair<>(value, second.getValue())));
            addSource(second, value -> setValue(new Pair<>(first.getValue(), value)));
        }
    }

    public LiveData<List<Long>> getFilterIds() {
        return filterIds;
    }

}
