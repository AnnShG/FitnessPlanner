package com.example.fitnesscalendar.logic.workout;

import android.app.Application;

import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.fitnesscalendar.database.AppDatabase;
import com.example.fitnesscalendar.entities.AiMessage;
import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.relations.DateColourResult;
import com.example.fitnesscalendar.relations.FullWorkoutRecord;
import com.example.fitnesscalendar.relations.PlannedWorkoutInfo;
import com.example.fitnesscalendar.relations.UserWithGoals;
import com.example.fitnesscalendar.repository.AiRepository;
import com.example.fitnesscalendar.repository.UserRepository;
import com.example.fitnesscalendar.repository.WorkoutRepository;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.vertexai.type.GenerateContentResponse;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WorkoutViewModel serves as the bridge between the UI (Fragments) and the Data Layer (Repositories)
 * It manages the UI's data state and coordinates operations across both Workout and User repositories.
 */
public class WorkoutViewModel extends AndroidViewModel {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final AiRepository aiRepository;

    private final MutableLiveData<String> aiAdvice = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAiLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Long>> filterIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    
    // The main stream of workouts that responds to both search and category filters
    public final LiveData<List<FullWorkoutRecord>> filteredWorkouts;

    public WorkoutViewModel(@NotNull Application app) {
        super(app);
        // Initializing repositories to handle database operations
        workoutRepository = new WorkoutRepository(app);
        userRepository = new UserRepository(app);
        aiRepository = new AiRepository();

        // Combined source for filters and search queries
        LiveData<Pair<List<Long>, String>> combined = new CombinedLiveData<>(filterIds, searchQuery);

        // Reactively fetch workouts whenever the user, filters, or search query changes
        filteredWorkouts = Transformations.switchMap(getLoggedInUser(), user -> {
            if (user == null) return new MutableLiveData<>(new ArrayList<>());

            return Transformations.switchMap(combined, pair -> {
                List<Long> ids = (pair != null && pair.first != null) ? pair.first : new ArrayList<>();
                String query = (pair != null && pair.second != null) ? pair.second : "";

                return workoutRepository.getWorkoutsFilteredAndSearched(
                        user.user.id,
                        ids,
                        query
                );
            });
        });
    }

    // for testing purposes
    public WorkoutViewModel(@NotNull Application app, WorkoutRepository workoutRepo, UserRepository userRepo, AiRepository aiRepo) {
        super(app);
        this.workoutRepository = workoutRepo;
        this.userRepository = userRepo;
        this.aiRepository = aiRepo;

        LiveData<Pair<List<Long>, String>> combined = new CombinedLiveData<>(filterIds, searchQuery);

        filteredWorkouts = Transformations.switchMap(getLoggedInUser(), user -> {
            if (user == null) return new MutableLiveData<>(new ArrayList<>());

            return Transformations.switchMap(combined, pair -> {
                List<Long> ids = (pair != null && pair.first != null) ? pair.first : new ArrayList<>();
                String query = (pair != null && pair.second != null) ? pair.second : "";
                return workoutRepository.getWorkoutsFilteredAndSearched(
                        user.user.id,
                        ids,
                        query
                );
            });
        });
    }

    public LiveData<String> getAiAdvice() {
        return aiAdvice;
    }
    public LiveData<Boolean> getIsAiLoading() {
        return isAiLoading;
    }

    public LiveData<UserWithGoals> getLoggedInUser() {
        return userRepository.getLatestUser();
    }

    public void saveWorkout(Workout workout, List<Long> exercises) {
        workoutRepository.insertFullWorkout(workout, exercises);
    }

    // Fetches all workouts created by a user.
    public LiveData<List<FullWorkoutRecord>> getFullWorkoutRecords(long userId) {
        return workoutRepository.getFullWorkoutRecords(userId);
    }

    // Retrieves detailed information for a single workout by its ID.
    public LiveData<FullWorkoutRecord> getFullWorkoutById(long id) {
        return workoutRepository.getFullWorkoutById(id);
    }

    public void updateWorkout(Workout workout, List<Long> exerciseIds) {
        workoutRepository.updateFullWorkout(workout, exerciseIds);
    }

    public void deleteWorkout(Workout workout) {
        workoutRepository.deleteWorkout(workout);
    }

    // Links a workout to multiple dates on the calendar
    public void attachWorkoutToDates(long userId, long workoutId, Set<Long> epochDays) {
        // Converts UI provided Set<Long> into Set<LocalDate> before passing to repo using Java Streams
        Set<LocalDate> localDates = epochDays.stream()
                .map(LocalDate::ofEpochDay)
                .collect(Collectors.toSet());
        workoutRepository.attachWorkoutToDates(userId, workoutId, localDates);
    }

    // Provides the data needed to draw colored dots on the calendar grid
    public LiveData<List<DateColourResult>> getWorkoutDotsForUser(long userId) {
        return workoutRepository.getWorkoutColorsForUser(userId);
    }

    // Retrieves a unique list of workouts that have been scheduled
    // Used to populate the management cards below the calendar
    public LiveData<List<PlannedWorkoutInfo>> getUniquePlannedWorkouts(long userId) {
        return workoutRepository.getUniquePlannedWorkouts(userId);
    }

    public void deleteWorkoutFromCalendar(long userId, long workoutId) {
        workoutRepository.deleteOnlyPlannedWorkoutsFromCalendar(userId, workoutId);
    }

    public void updateWorkoutPlan(long userId, long workoutId, Set<Long> newEpochDays, Set<Long> completedDays) {
        workoutRepository.updateWorkoutPlan(userId, workoutId, newEpochDays, completedDays);
    }

    public LiveData<List<DateColourResult>> getWorkoutsForSpecificDay(long userId, long epochDay) {
        return workoutRepository.getWorkoutsForSpecificDay(userId, epochDay);
    }

    public void deleteSpecificWorkoutPlan(long userId, long workoutId, long epochDay) {
        workoutRepository.deleteSpecificWorkoutPlan(userId, workoutId, epochDay);
    }

    public void updateWorkoutCompletion(long userId, long workoutId, long epochDay, boolean completed) {
        workoutRepository.updateWorkoutCompletion(userId, workoutId, epochDay, completed);
    }

    // requests DB data, sends to repo, and returns the response
    public void refreshAiInsight(UserWithGoals userWithGoals, List<DateColourResult> history) {
        long currentUserId = userWithGoals.user.id;
        
        // new user?
        long registrationDate = userWithGoals.user.createdAt.getTime();
        long sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000;

        if ((System.currentTimeMillis() - registrationDate) < sevenDaysInMillis) {
            aiAdvice.postValue("Learning your fitness habits. Personalized insights will appear here after 7 days of activity logging.");
            return;
        }

        isAiLoading.setValue(true);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // getting chat history from DB for this user
            List<AiMessage> chatHistory = workoutRepository.getAiChatHistoryForUser(currentUserId);

            String lastAdvice = "";
            for (int i = 0; i < chatHistory.size(); i++) {
                if ("model".equals(chatHistory.get(i).getRole())) {
                    lastAdvice = chatHistory.get(i).getContent();
                    break;
                }
            }

            String prompt = aiRepository.buildPrompt(userWithGoals, history, lastAdvice);

            // save user request in local DB with currentUserId
            workoutRepository.saveAiMessage(new AiMessage(currentUserId, "user", prompt));

            // Gemini request
            ListenableFuture<GenerateContentResponse> future = aiRepository.getAdvice(prompt, chatHistory);

            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    isAiLoading.postValue(false);
                    String text = result.getText();
                    aiAdvice.postValue(text);

                    // saving the AI response to DB with currentUserId
                    workoutRepository.saveAiMessage(new AiMessage(currentUserId, "model", text));
                }

                @Override
                public void onFailure(@NotNull Throwable t) {
                    isAiLoading.postValue(false);
                    aiAdvice.postValue("I'm having trouble connecting right now. Let's try again tomorrow!");
                }
            }, ContextCompat.getMainExecutor(getApplication()));
        });
    }

    // Sets the category filter IDs and triggers a data refresh
    public void setFilters(List<Long> ids) {
        filterIds.setValue(ids);
    }

    // Sets the text search query and triggers a data refresh
    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    /**
     * Helper class to combine two LiveData sources into a single pair.
     */
    public static class CombinedLiveData<F, S> extends MediatorLiveData<Pair<F, S>> {
        private F lastFirst;
        private S lastSecond;

        public CombinedLiveData(LiveData<F> first, LiveData<S> second) {
            addSource(first, value -> {
                lastFirst = value;
                setValue(new Pair<>(lastFirst, lastSecond));
            });
            addSource(second, value -> {
                lastSecond = value;
                setValue(new Pair<>(lastFirst, lastSecond));
            });
        }
    }

    public LiveData<List<Long>> getFilterIds() {
        return filterIds;
    }

    public LiveData<List<Category>> getAllCategories() {
        return workoutRepository.getAllCategories();
    }

    public LiveData<Pair<Integer, Integer>> getMonthlyStats(long start, long end) {
        MediatorLiveData<Pair<Integer, Integer>> result = new MediatorLiveData<>();

        LiveData<Integer> totalSource = workoutRepository.getTotalWorkoutsInMonth(start, end);
        LiveData<Integer> completedSource = workoutRepository.getCompletedWorkoutsInMonth(start, end);

        result.addSource(totalSource, total ->
                result.setValue(new Pair<>(total, completedSource.getValue() != null ? completedSource.getValue() : 0)));

        result.addSource(completedSource, completed ->
                result.setValue(new Pair<>(totalSource.getValue() != null ? totalSource.getValue() : 0, completed)));

        return result;
    }
}
