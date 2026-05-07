package com.example.fitnesscalendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fitnesscalendar.dao.CategoryDao;
import com.example.fitnesscalendar.dao.ExerciseDao;
import com.example.fitnesscalendar.dao.StepDao;
import com.example.fitnesscalendar.entities.Exercise;
import com.example.fitnesscalendar.entities.Step;
import com.example.fitnesscalendar.repository.ExerciseRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

public class ExerciseRepositoryTest {
    private ExerciseRepository repository;
    @Mock
    private ExerciseDao exerciseDao;
    @Mock
    private StepDao stepDao;
    @Mock
    private CategoryDao categoryDao;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new ExerciseRepository(exerciseDao, stepDao, categoryDao);
    }

    @Test
    public void insertFullExercise_validData_callsInserts() {
        Exercise exercise = new Exercise();
        exercise.title = "Pushup";

        List<Step> steps = Arrays.asList(new Step(), new Step()); // 2 steps
        List<Long> categoryIds = Arrays.asList(1L, 2L, 3L); // 3 categories

        long mockExerciseId = 15L;
        when(exerciseDao.insert(exercise)).thenReturn(mockExerciseId); // mocking -- assign the ex id 15 in the "DB"

        repository.insertFullExercise(exercise, steps, categoryIds);

        verify(exerciseDao, timeout(1000)).insert(exercise);

        verify(stepDao, timeout(1000).times(2)).insert(any(Step.class));

        verify(exerciseDao, timeout(1000).times(3)).insertExerciseCategoryCrossRef(any());
    }

    @Test
    public void updateExercise_updatesAndResyncsData() {
        Exercise exercise = new Exercise();
        exercise.setExerciseId(15L);
        exercise.title = "Updated Pushup";

        List<Step> steps = Arrays.asList(new Step());
        List<Long> categoryIds = Arrays.asList(5L);

        repository.updateExercise(exercise, steps, categoryIds);

        verify(exerciseDao, timeout(1000)).update(exercise);

        // Check resync logic for steps and categories
        verify(exerciseDao, timeout(1000)).deleteStepsByExerciseId(15L);
        verify(stepDao, timeout(1000)).insert(any());

        verify(exerciseDao, timeout(1000)).deleteCategoryCrossRefsByExerciseId(15L);
        verify(exerciseDao, timeout(1000).times(1)).insertExerciseCategoryCrossRef(any());
    }

    @Test
    public void deleteFullExercise_callsDeletesInOrder() {
        long idToDelete = 15L;

        repository.deleteFullExercise(idToDelete);

        verify(exerciseDao, timeout(1000)).deleteStepsByExerciseId(idToDelete);
        verify(exerciseDao, timeout(1000)).deleteCategoryCrossRefsByExerciseId(idToDelete);
        verify(exerciseDao, timeout(1000)).delete(argThat(ex -> ex.getExerciseId() == idToDelete));
    }

    @Test
    public void getExercisesFilteredAndSearched_delegatesToDao() {
        repository.getExercisesFilteredAndSearched(null, "squat");
        verify(exerciseDao).getExercisesBySearchOnly("squat");

        List<Long> cats = Arrays.asList(1L);
        repository.getExercisesFilteredAndSearched(cats, "squat");
        verify(exerciseDao).getExercisesFilteredAndSearched(cats, "squat");
    }

    @Test
    public void simpleQueries_delegatesToDao() {
        repository.getFullExerciseById(1L);
        verify(exerciseDao).getFullExerciseById(1L);

        repository.getAllCategories();
        verify(categoryDao).getAllCategories();

        repository.getAllFullExercises();
        verify(exerciseDao, times(2)).getFullExerciseRecords();
    }
}
