package com.example.fitnesscalendar;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.fitnesscalendar.entities.AiMessage;
import com.example.fitnesscalendar.entities.Goal;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.relations.DateColourResult;
import com.example.fitnesscalendar.relations.UserWithGoals;
import com.example.fitnesscalendar.repository.AiRepository;
import com.google.firebase.vertexai.java.GenerativeModelFutures;
import com.google.firebase.vertexai.type.Content;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class AiRepositoryTest {
    private AiRepository repository;
    @Mock
    private GenerativeModelFutures mockModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new AiRepository(mockModel);
    }

    @Test
    public void buildPrompt_withValidData_generatesCorrectString() {
        User user = new User();
        user.gender = "Male";
        
        // Set birth date to 25 years ago
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -25);
        user.birthDate = cal.getTime();

        Goal goal = new Goal();
        goal.goalTitle = "Lose Weight";
        goal.isCustom = false;

        // 1:M
        UserWithGoals userWithGoals = new UserWithGoals();
        userWithGoals.user = user;
        userWithGoals.goals = Arrays.asList(goal);

        // 2 workouts status - completed and not
        List<DateColourResult> history = new ArrayList<>();
        DateColourResult item1 = new DateColourResult();
        item1.isCompleted = true;
        DateColourResult item2 = new DateColourResult();
        item2.isCompleted = false;
        history.addAll(Arrays.asList(item1, item2));

        String prompt = repository.buildPrompt(userWithGoals, history, "Eat more protein");

        assertTrue(prompt.contains("25-year-old Male"));
        assertTrue(prompt.contains("Lose Weight"));
        assertTrue(prompt.contains("2 workouts scheduled, 1 completed"));
        assertTrue(prompt.contains("Eat more protein"));
    }

    @Test
    public void buildPrompt_withNoPredefinedGoals_usesDefaultGoal() {
        User user = new User();
        user.gender = "Female";
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -30);
        user.birthDate = cal.getTime();

        UserWithGoals userWithGoals = new UserWithGoals();
        userWithGoals.user = user;
        userWithGoals.goals = new ArrayList<>(); // No goals

        String prompt = repository.buildPrompt(userWithGoals, new ArrayList<>(), "");

        assertTrue(prompt.contains("General Fitness Improvement"));
    }

    @Test
    public void getAdvice_callsGenerativeModel() {
        List<AiMessage> history = new ArrayList<>();
        history.add(new AiMessage(1L, "user", "Old prompt"));
        history.add(new AiMessage(1L, "model", "Old response"));

        repository.getAdvice("New prompt", history);

        // Verify that generateContent was called with the history context
        verify(mockModel).generateContent(any(Content[].class));
    }
}
