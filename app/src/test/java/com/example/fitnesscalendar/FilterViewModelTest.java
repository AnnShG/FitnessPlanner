package com.example.fitnesscalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.fitnesscalendar.logic.filter.FilterViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class FilterViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule(); // Sync

    private FilterViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new FilterViewModel();
    }

    @Test
    public void initialValues_areEmptyLists() {
        assertNotNull(viewModel.getExerciseFilters().getValue());
        assertTrue(viewModel.getExerciseFilters().getValue().isEmpty());

        assertNotNull(viewModel.getWorkoutFilters().getValue());
        assertTrue(viewModel.getWorkoutFilters().getValue().isEmpty());
    }

    @Test
    public void setExerciseFilters_updatesLiveData() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        viewModel.setExerciseFilters(ids);

        assertEquals(ids, viewModel.getExerciseFilters().getValue());
    }

    @Test
    public void setWorkoutFilters_updatesLiveData() {
        List<Long> ids = Arrays.asList(10L, 20L);
        viewModel.setWorkoutFilters(ids);

        assertEquals(ids, viewModel.getWorkoutFilters().getValue());
    }
}
