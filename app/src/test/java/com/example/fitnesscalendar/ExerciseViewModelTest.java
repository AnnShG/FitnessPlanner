package com.example.fitnesscalendar;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Step;
import com.example.fitnesscalendar.logic.exercise.ExerciseViewModel;
import com.example.fitnesscalendar.repository.ExerciseRepository;
import com.example.fitnesscalendar.repository.UserRepository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExerciseViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    private ExerciseViewModel viewModel;
    @Mock private Application application;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private UserRepository userRepository;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(exerciseRepository.getAllFullExercises())
                .thenReturn(new MutableLiveData<>(new ArrayList<>()));
        when(exerciseRepository.getExercisesFilteredAndSearched(anyList(), anyString()))
                .thenReturn(new MutableLiveData<>(new ArrayList<>()));

        viewModel = new ExerciseViewModel(application, exerciseRepository, userRepository);
    }

    @Test
    public void testLombokGettersSetters() {
        viewModel.setExerciseId(100L);
        assertEquals(Long.valueOf(100L), viewModel.getExerciseId());

        viewModel.setTitle("Jump");
        Assert.assertEquals("Jump", viewModel.getTitle());

        String desc = "Hold for 30 seconds";
        viewModel.setDescription(desc);
        assertEquals(desc, viewModel.getDescription());

        String uri = "content://media/exercise.mp4";
        viewModel.setMediaUri(uri);
        assertEquals(uri, viewModel.getMediaUri());

        viewModel.setNotes("Test notes");
        Assert.assertEquals("Test notes", viewModel.getNotes());

        viewModel.setUserCreated(true);
        assertTrue(viewModel.getUserCreated());

        viewModel.setUserCreated(false);
        assertFalse(viewModel.getUserCreated());
    }

    @Test
    public void testSetSearchQuery_UpdatesValue() {
        String query = "Pushups";
        viewModel.setSearchQuery(query);
        assertNotNull(viewModel.filteredExercises);
    }

    @Test
    public void testSetFilters_UpdatesInternalIds() {
        List<Long> ids = Arrays.asList(1L, 2L);
        viewModel.setFilters(ids);
        Assert.assertEquals(ids, viewModel.getFilterIds().getValue());
    }

    /**
     * if branch -> (ids.isEmpty() && query.isEmpty())
     */
    @Test
    public void testFilteredExercises_BranchBothEmpty_CallsGetAll() {
        // dummy observe to trigger the switchMap
        viewModel.filteredExercises.observeForever(list -> {});

        verify(exerciseRepository, atLeastOnce()).getAllFullExercises();
    }

    @Test
    public void testSaveExercise_CallsRepository() {
        Exercise ex = new Exercise();
        List<Step> steps = new ArrayList<>();
        List<Long> categories = Arrays.asList(1L);

        viewModel.saveExercise(ex, steps, categories);
        verify(exerciseRepository).insertFullExercise(ex, steps, categories);
    }

    @Test
    public void testUpdateExercise_CallsRepository() {
        Exercise ex = new Exercise();
        viewModel.updateExercise(ex, new ArrayList<>(), new ArrayList<>());
        verify(exerciseRepository).updateExercise(eq(ex), anyList(), anyList());
    }

    @Test
    public void testDeleteExercise_CallsRepository() {
        viewModel.deleteExercise(100L);
        verify(exerciseRepository).deleteFullExercise(100L);
    }

    @Test
    public void testCombinedLiveData_EmitsPair() {
        MutableLiveData<String> s1 = new MutableLiveData<>();
        MutableLiveData<Integer> s2 = new MutableLiveData<>();

        ExerciseViewModel.CombinedLiveData<String, Integer> combined =
                new ExerciseViewModel.CombinedLiveData<>(s1, s2);

        combined.observeForever(pair -> {
            if (pair.first != null && pair.second != null) {
                Assert.assertEquals("A", pair.first);
                Assert.assertEquals(Integer.valueOf(1), pair.second);
            }
        });

        s1.setValue("A");
        s2.setValue(1);
    }

    @Test
    public void testSimpleQueries_DelegateCorrectly() {
        viewModel.getFullExerciseById(1L);
        verify(exerciseRepository).getFullExerciseById(1L);

        viewModel.getAllCategories();
        verify(exerciseRepository).getAllCategories();

        viewModel.getLoggedInUser();
        verify(userRepository).getLatestUser();

        viewModel.getAllFullExerciseRecords();
        verify(exerciseRepository).getAllFullExerciseRecords();
    }

}