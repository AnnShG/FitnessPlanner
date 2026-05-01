package com.example.fitnesscalendar.repository;

import android.annotation.SuppressLint;

import com.example.fitnesscalendar.entities.AiMessage;
import com.example.fitnesscalendar.relations.DateColourResult;
import com.example.fitnesscalendar.relations.UserWithGoals;
import com.google.firebase.vertexai.FirebaseVertexAI;
import com.google.firebase.vertexai.GenerativeModel;
import com.google.firebase.vertexai.java.GenerativeModelFutures;
import com.google.firebase.vertexai.type.Content;
import com.google.firebase.vertexai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

// Translator between the app and Google Gemini.
// Generates prompt, create a package of the DB history to AI format
public class AiRepository {
    private final GenerativeModelFutures model;

    public AiRepository() {
        // initialize Gemini 2.5 Flash Lite
        GenerativeModel gm = FirebaseVertexAI.getInstance()
                .generativeModel("gemini-2.5-flash-lite");
        this.model = GenerativeModelFutures.from(gm);
    }

    @SuppressLint("DefaultLocale")
    public String buildPrompt(UserWithGoals user, List<DateColourResult> history, String lastAdvice) {
        int age = Period.between(
                user.user.birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                LocalDate.now()
        ).getYears();

        String goals = user.goals.stream()
                .filter(g -> !g.isCustom) // keep only predefined goals
                .map(g -> g.goalTitle)
                .collect(Collectors.joining(", "));

        // in case the user has only custom goals
        if (goals.isEmpty()) {
            goals = "General Fitness Improvement";
        }

        long completed = history.stream().filter(h -> h.isCompleted).count();
        long total = history.size();

        return String.format(
                "Act as a professional AI Fitness Coach. Analyze user data and provide actionable advice.\n\n" +
                        "USER PROFILE:\n" +
                        "- Age/Gender: %d-year-old %s\n" +
                        "- Primary Goal: %s\n" +
                        "- Recent Activity: %d workouts scheduled, %d completed.\n\n" +
                        "CONTEXT:\n" +
                        "- Previous advice given to user: \"%s\"\n\n" +
                        "INSTRUCTIONS:\n" +
                        "1. Compare current activity with the previous advice.\n" +
                        "2. If progress is evident, provide positive reinforcement.\n" +
                        "3. If consistency is low (missed workouts), focus on motivation and small, manageable changes.\n" +
                        "4. If consistency is high, suggest a safe progression (e.g., adding 5%% intensity or a new exercise).\n" +
                        "5. Avoid repeating the previous advice.\n\n" +
                        "RESPONSE REQUIREMENTS:\n" +
                        "- Tone: Professional, motivating, concise.\n" +
                        "- Format: Use bullet points.\n" +
                        "- No 'fluff' or unnecessary introductions.\n" +
                        "- Max 3-4 short sentences.",
                age, user.user.gender, goals, total, (int)completed, (lastAdvice.isEmpty() ? "None" : lastAdvice)
        );
    }

    public ListenableFuture<GenerateContentResponse> getAdvice(String currentPrompt, List<AiMessage> dbHistory) {
        // build the history context for Gemini
        Content[] contents = new Content[dbHistory.size() + 1];

        for (int i = 0; i < dbHistory.size(); i++) {
            AiMessage msg = dbHistory.get(i);
            Content.Builder b = new Content.Builder();
            b.setRole(msg.role);
            b.addText(msg.content);
            contents[i] = b.build();
        }

        // prepare the last user message
        Content.Builder lastMsgBuilder = new Content.Builder();
        lastMsgBuilder.setRole("user");
        lastMsgBuilder.addText(currentPrompt);
        contents[contents.length - 1] = lastMsgBuilder.build();

        // send both history and the new prompt
        return model.generateContent(contents);
    }


}
