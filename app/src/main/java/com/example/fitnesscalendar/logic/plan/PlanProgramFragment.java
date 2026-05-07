package com.example.fitnesscalendar.logic.plan;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesscalendar.R;
import com.example.fitnesscalendar.databinding.PlanProgramScreenBinding;
import com.example.fitnesscalendar.logic.calendar.CalendarAdapter;
import com.example.fitnesscalendar.logic.calendar.CalendarManager;
import com.example.fitnesscalendar.logic.workout.PlannedWorkoutsAdapter;
import com.example.fitnesscalendar.logic.workout.WorkoutViewModel;
import com.example.fitnesscalendar.relations.DateColourResult;
import com.example.fitnesscalendar.relations.PlannedWorkoutInfo;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.NonNull;

/**
 * PlanProgramFragment allows users to schedule specific workouts onto the calendar.
 * It manages two main visual states:
 * 1. "Attach Mode": Selecting a workout and picking new days.
 * 2. "Edit Mode": Modifying the existing schedule of a previously planned workout.
 */
public class PlanProgramFragment extends Fragment {
    private PlanProgramScreenBinding binding;
    private CalendarAdapter adapter;
    private PlannedWorkoutsAdapter plannedAdapter;
    private WorkoutViewModel workoutViewModel;
    private final CalendarManager manager = new CalendarManager();
    private final Set<Long> highlightedDates = new HashSet<>(); // Temporarily stores selected epochDay IDs (Grey circles)
    private final Set<Long> completedDatesForThisWorkout = new HashSet<>();
    private final List<String> daysList = new ArrayList<>(); // takes a list of numbers/dates 1,2,3,4
    private long currentSelectedWorkoutId = -1;
    private long currentUserId = -1;
    private boolean isWorkoutSelected = false; // Prevents calendar clicks if no workout is selected
    private boolean isEditMode = false; // Determines if is used 'Insert' or 'Update' logic on Apply btn

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = PlanProgramScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //  Handles the selection/unselection of dates on the grid
        adapter = new CalendarAdapter(daysList, (position, dayText) -> {
            if (!isWorkoutSelected) {
                Toast.makeText(getContext(), "Please select a workout first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!dayText.isEmpty()) {
                Long epochDay = manager.getEpochDayForDay(dayText);
                if (epochDay != null) {

                    // Duplicate check
                    // If not in Edit Mode, block selecting a day that already has this workout
                    if (!isEditMode && adapter.isWorkoutAlreadyPlanned(epochDay, currentSelectedWorkoutId)) {
                        Toast.makeText(getContext(), "This workout is already planned for this day", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Max limit check (3 Workouts)
                    // During the selection process, the workout cannot be attached if the limit reached
                    if (!highlightedDates.contains(epochDay)) {
                        int totalWorkouts = adapter.getWorkoutsCountForDay(epochDay);

                        if (totalWorkouts >= 3) {
                            Toast.makeText(getContext(), "Maximum of 3 workouts per day reached", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    if (isEditMode && highlightedDates.contains(epochDay)) {
                        if (adapter.isWorkoutCompleted(epochDay, currentSelectedWorkoutId)) {
                            Toast.makeText(getContext(), "Completed workouts cannot be removed from the plan", Toast.LENGTH_SHORT).show();
                            return; //do not allow unselecting this date
                        }
                    }

                    // This part handles the selecting and unselecting (toggling)
                    if (highlightedDates.contains(epochDay)) { // temporary highlighted dates
                        highlightedDates.remove(epochDay);
                    } else {
                        highlightedDates.add(epochDay);
                    }

                    adapter.setHighlightedDates(highlightedDates, manager);
                    updateApplyButtonVisibility();
                }
            }
        });
        binding.calendarRecyclerView.setAdapter(adapter);

        // Initialize Adapter for the List of planned workouts Cards below the calendar
        plannedAdapter = new PlannedWorkoutsAdapter(new PlannedWorkoutsAdapter.OnPlanActionListener() {
            @Override
            public void onEdit(PlannedWorkoutInfo info) {
                enterEditMode(info);
            }

            @Override
            public void onDelete(long workoutId) {
                workoutViewModel.deleteWorkoutFromCalendar(currentUserId, workoutId);
            }
        });

        // Set LayoutManager for the cards' workout list
        binding.plannedWorkoutsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.plannedWorkoutsRecyclerView.setAdapter(plannedAdapter);

        workoutViewModel = new ViewModelProvider(requireActivity()).get(WorkoutViewModel.class);

        // Once the user is logged in, starts observing two data streams:
        workoutViewModel.getLoggedInUser().observe(getViewLifecycleOwner(), userWithGoals -> {
            if (userWithGoals != null) {
                this.currentUserId = userWithGoals.user.id;

                // Observe Calendar Dots - draws colored indicators on the grid
                workoutViewModel.getWorkoutDotsForUser(currentUserId).observe(getViewLifecycleOwner(), plans -> {
                    if (plans != null) {
                        adapter.setPlannedWorkouts(plans);
                        adapter.setHighlightedDates(highlightedDates, manager);
                    }
                });

                // Observe unique Workout Cards - populates the management list
                workoutViewModel.getUniquePlannedWorkouts(currentUserId).observe(getViewLifecycleOwner(), list -> {
                    if (list != null) {
                        plannedAdapter.setWorkouts(list);
                    }
                });
            }
        });

        setupClickListeners();
        updateUI();

        // Receives the selected workout from the WorkoutSelectScreen and activates Attach Mode.
        getParentFragmentManager().setFragmentResultListener("workout_selection", getViewLifecycleOwner(), (requestKey, bundle) -> {
            this.isEditMode = false;
            this.isWorkoutSelected = true;

            long workoutId = bundle.getLong("workoutId");
            String workoutTitle = bundle.getString("workoutTitle");
            int workoutColor = bundle.getInt("workoutColor");

//           store the received workoutId from 'WorkoutSelectScreen' locally so it can be used when "Apply" is clicked
            this.currentSelectedWorkoutId = workoutId;

//             Show the Selected Workout Card
            binding.selectedWorkoutCard.setVisibility(View.VISIBLE);
            binding.selectedWorkoutTitle.setText("Attaching: " + workoutTitle);
            binding.workoutCircle.setBackgroundTintList(ColorStateList.valueOf(workoutColor));

            // Update button appearance to "Replace Mode"
            applyReplaceButtonStyle();
            updateApplyButtonVisibility();
        });
    }

    private void setupClickListeners() {
        binding.calendarPrevButton.setOnClickListener(v -> { manager.goToPrevMonth(); updateUI(); });
        binding.calendarNextButton.setOnClickListener(v -> { manager.goToNextMonth(); updateUI(); });

        binding.btnAttachWorkout.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_PlanProgramScreen_to_WorkoutSelectScreen);
        });

        // Commits the temporary 'highlightedDates' to the permanent database
        binding.btnApply.setOnClickListener(v -> {
            if (currentUserId != -1 && currentSelectedWorkoutId != -1 && !highlightedDates.isEmpty()) {

                if (isEditMode) {
                    // Update logic: Deletes old links for this workout and inserts the new set
                    workoutViewModel.updateWorkoutPlan(currentUserId, currentSelectedWorkoutId, highlightedDates, completedDatesForThisWorkout);
                    Toast.makeText(getContext(), "Schedule updated!", Toast.LENGTH_SHORT).show();
                } else {
                    // Attach logic: simple insert for a new plan
                    workoutViewModel.attachWorkoutToDates(currentUserId, currentSelectedWorkoutId, highlightedDates);
                    Toast.makeText(getContext(), "Workout successfully attached!", Toast.LENGTH_SHORT).show();
                }

                resetPlanningState();
            }
        });
    }

    // is used when the process of planning/editing workout was finished
    // cleans up the Fragment state, restores the original "Attach Workout" button and clears grey circles.
    private void resetPlanningState() {
        // clear internal memory
        highlightedDates.clear();
        isWorkoutSelected = false;
        isEditMode = false;
        currentSelectedWorkoutId = -1;

        adapter.setHighlightedDates(highlightedDates, manager);
        binding.btnApply.setVisibility(View.GONE);
        binding.selectedWorkoutCard.setVisibility(View.GONE);
        binding.btnAttachWorkout.setVisibility(View.VISIBLE);

        binding.btnAttachWorkout.setText("Attach Workout to Calendar");
        binding.btnAttachWorkout.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.chip_selected_orange, null)));
        binding.btnAttachWorkout.setTextColor(Color.BLACK);

        if (binding.btnAttachWorkout instanceof MaterialButton) {
            ((MaterialButton) binding.btnAttachWorkout).setStrokeWidth(0);
        }
    }

    /**
     * Activates the Editing mode for a specific workout
     * Hides the 'Attach' button and highlights all dates where this workout is currently scheduled.
     */
    private void enterEditMode(PlannedWorkoutInfo info) {
        this.isEditMode = true;
        this.currentSelectedWorkoutId = info.workoutId;
        this.isWorkoutSelected = true;
        completedDatesForThisWorkout.clear();
        highlightedDates.clear();

        binding.selectedWorkoutCard.setVisibility(View.VISIBLE);
        binding.selectedWorkoutTitle.setText("Editing: " + info.title);
        binding.workoutCircle.setBackgroundTintList(ColorStateList.valueOf(info.colour));
        binding.btnAttachWorkout.setVisibility(View.GONE);

        // adapter's current plans is checked to find where the workout is

        for (DateColourResult plan : adapter.getPlannedWorkouts()) {
            if (plan.workoutId != null && plan.workoutId == info.workoutId) {
                highlightedDates.add(plan.date);
                if (plan.isCompleted) {
                    completedDatesForThisWorkout.add(plan.date);
                }
            }
        }
        adapter.setHighlightedDates(highlightedDates, manager);
        updateApplyButtonVisibility();
    }

    private void updateUI() {
        binding.monthAndYear.setText(manager.getHeaderString(true));
        List<String> days = manager.getDaysOfMonthList();
        adapter.setDays(days);
    }

    private void updateApplyButtonVisibility() {
        if (isWorkoutSelected && !highlightedDates.isEmpty()) {
            binding.btnApply.setVisibility(View.VISIBLE);
        } else {
            binding.btnApply.setVisibility(View.GONE);
        }
    }

    private void applyReplaceButtonStyle() {
        binding.btnAttachWorkout.setText("Replace Workout");
        binding.btnAttachWorkout.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.home_page_background_colour, null)));
        binding.btnAttachWorkout.setTextColor(Color.BLACK);

        if (binding.btnAttachWorkout instanceof MaterialButton) {
            MaterialButton mBtn = (MaterialButton) binding.btnAttachWorkout;
            mBtn.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.workout_replace_btn_stroke_colour, null)));
            mBtn.setStrokeWidth(2);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}