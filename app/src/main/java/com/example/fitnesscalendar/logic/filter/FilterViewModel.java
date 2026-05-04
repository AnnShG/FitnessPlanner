package com.example.fitnesscalendar.logic.filter;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

// Purpose - to hold and manage UI filter state with sync update
public class FilterViewModel extends ViewModel {
    private final MutableLiveData<List<Long>> exerciseFilters = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> workoutFilters = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Long>> getExerciseFilters() { return exerciseFilters; }
    public void setExerciseFilters(List<Long> ids) { exerciseFilters.setValue(ids); }

    public LiveData<List<Long>> getWorkoutFilters() { return workoutFilters; }
    public void setWorkoutFilters(List<Long> ids) { workoutFilters.setValue(ids); }
}
