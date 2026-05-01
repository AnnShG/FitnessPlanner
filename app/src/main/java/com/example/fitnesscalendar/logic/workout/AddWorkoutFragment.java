package com.example.fitnesscalendar.logic.workout;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.fitnesscalendar.R;
import com.example.fitnesscalendar.databinding.AddWorkoutScreenBinding;
import com.example.fitnesscalendar.entities.Workout;
import com.example.fitnesscalendar.logic.exercise.ExerciseViewModel;
import com.example.fitnesscalendar.relations.FullExerciseRecord;
import com.example.fitnesscalendar.relations.FullWorkoutRecord;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

public class AddWorkoutFragment extends Fragment {

    private AddWorkoutScreenBinding binding;
    private WorkoutViewModel workoutViewModel;
    private ExerciseViewModel exerciseViewModel;
    private Long currentUserId;
    private final List<Long> selectedExerciseIdList = new ArrayList<>();
    private Integer selectedWorkoutColour = null;
    private long existingWorkoutId = -1; // the workout exists for updating and deleting? -1 it is not


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = AddWorkoutScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        workoutViewModel = new ViewModelProvider(requireActivity()).get(WorkoutViewModel.class);
        exerciseViewModel = new ViewModelProvider(requireActivity()).get(ExerciseViewModel.class);

        // DB observation
        workoutViewModel.getLoggedInUser().observe(getViewLifecycleOwner(), userWithGoals -> { // calls lambda everytime the data changes (LiveData)
            if (userWithGoals != null) {
                this.currentUserId = userWithGoals.user.getId();
            }
        });

        setupColourSelection();

        if (selectedWorkoutColour != null) {
            View savedView = getColorViewByValue(selectedWorkoutColour);
            if (savedView != null) {
                selectColour(selectedWorkoutColour, savedView);
            }
        }

        // Fragment to fragment communication (exercise selection)
        getParentFragmentManager().setFragmentResultListener("exercise_selection", // registers a listener for a result with the key "exercise_selection"
                getViewLifecycleOwner(), // Ties the listener to the fragment’s view lifecycle
                (requestKey, bundle) -> {
                    long[] selectedIds = bundle.getLongArray("selected_ids");
                    if (selectedIds != null) {
                        selectedExerciseIdList.clear();
                        binding.exercisesContainer.removeAllViews();
                        loadSelectedExercisesIntoUI(selectedIds);
                    }
                });

        // edit mode screen opened?
        if (getArguments() != null) {
            existingWorkoutId = getArguments().getLong("workoutId", -1);
        }

        if (existingWorkoutId != -1) { // 1 means it exists
            binding.detailWorkoutTitle.setText("Edit Workout");

            binding.deleteWorkoutButton.setVisibility(View.VISIBLE);
            binding.deleteWorkoutButton.setOnClickListener(v -> {
                showDeleteConfirmationDialog();
            });

            workoutViewModel.getFullWorkoutById(existingWorkoutId)
                    .observe(getViewLifecycleOwner(), record -> {
                        if (record != null) {
                            prefillForm(record);
                        }
                    });
        }

        binding.backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(AddWorkoutFragment.this)
                        .navigateUp()
        );

        binding.addExerciseButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            // Convert List<Long> to long[] to send in bundle
            long[] existing = selectedExerciseIdList.stream().mapToLong(l -> l).toArray();
            bundle.putLongArray("existing_ids", existing);

            Navigation.findNavController(view)
                    .navigate(R.id.action_AddWorkoutScreen_to_ExerciseSelectScreen, bundle);
        });

        binding.saveWorkoutButton.setOnClickListener(v -> {
            onSaveButtonClicked();
        });

        binding.cancelWorkoutButton.setOnClickListener(v -> {
                    NavHostFragment.findNavController(this)
                            .navigateUp();
                }
        );
    }

    private void setupColourSelection() {
        binding.colorGreen.setOnClickListener(v -> selectColour(0xFF4CAF50, v));
        binding.colorBlue.setOnClickListener(v -> selectColour(0xFF2196F3, v));
        binding.colorPurple.setOnClickListener(v -> selectColour(0xFF9C27B0, v));
        binding.colorRed.setOnClickListener(v -> selectColour(0xFFF44336, v));
        binding.colorDarkBlue.setOnClickListener(v -> selectColour(0xFF3F51B5, v));
        binding.colorGrey.setOnClickListener(v -> selectColour(0xFF888588, v));
        binding.colorYellow.setOnClickListener(v -> selectColour(0xFFF2D607, v));
    }

    private View getColorViewByValue(int color) {
        if (color == 0xFF4CAF50) return binding.colorGreen;
        if (color == 0xFF2196F3) return binding.colorBlue;
        if (color == 0xFF9C27B0) return binding.colorPurple;
        if (color == 0xFFF44336) return binding.colorRed;
        if (color == 0xFF3F51B5) return binding.colorDarkBlue;
        if (color == 0xFF888588) return binding.colorGrey;
        if (color == 0xFFF2D607) return binding.colorYellow;
        return null;
    }

    private void selectColour(int color, View view) {
        this.selectedWorkoutColour = color; // this will be stored in the Workout table

        View[] colors = {binding.colorGreen, binding.colorBlue, binding.colorPurple,
                binding.colorRed, binding.colorDarkBlue, binding.colorGrey, binding.colorYellow}; // array of clickable views

        // setting transparency to not chosen circles
        for (View v : colors) {
            v.setAlpha(0.5f);
            if (v instanceof ShapeableImageView) {
                ((ShapeableImageView) v).setStrokeWidth(0f);
            }
        }

        view.setAlpha(1.0f); // the one view (v) the user clicked
        if (view instanceof ShapeableImageView) {
            ShapeableImageView selectedView = (ShapeableImageView) view;

            selectedView.setStrokeWidth(6f);

            selectedView.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.workout_circle_indicator_choice_colour_stroke)
            ));
        }
    }

    private void loadSelectedExercisesIntoUI(long[] selectedIds) {
        for (int i = 0; i < selectedIds.length; i++) {
            long id = selectedIds[i];
            selectedExerciseIdList.add(id);

            //  async database call
            final int position = i + 1;

            exerciseViewModel.getFullExerciseById(id).observe(getViewLifecycleOwner(), record -> {
                if (record != null) {
                    inflateExerciseRow(record, position);
                }
            });
        }
    }

    private void inflateExerciseRow(FullExerciseRecord record, int position) {
        View rowView = getLayoutInflater().inflate(R.layout.workout_exercise_item_row, binding.exercisesContainer, false);

        TextView indexTv = rowView.findViewById(R.id.leftSideControlIndex);
        TextView titleLabel = rowView.findViewById(R.id.titleLabel);
        TextView catsList = rowView.findViewById(R.id.categoriesList);
        ImageView img = rowView.findViewById(R.id.exerciseImage);
        ImageView deleteBtn = rowView.findViewById(R.id.leftSideControlDelete);

        indexTv.setText(String.valueOf(position));
        titleLabel.setText(record.exercise.getTitle());

        if (record.categories != null && !record.categories.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < record.categories.size(); i++) {
                sb.append(record.categories.get(i).getName());
                if (i < record.categories.size() - 1) {
                    sb.append(", ");
                }
            }
            catsList.setText(sb.toString());
        } else {
            catsList.setText("");
        }

        if (record.exercise.getMediaUri() != null) {
            Glide.with(this)
                    .load(record.exercise.getMediaUri())
                    .fitCenter()
                    .into(img);
        }

//        deleteBtn.setOnClickListener(v -> {
//            binding.exercisesContainer.removeView(rowView);
//
//            Long idToRemove = record.exercise.getExerciseId(); // remove by value
//            selectedExerciseIdList.remove(idToRemove);
//
//            recalculateIndices(); //  update 1, 2, 3... labels
//        });

        deleteBtn.setOnClickListener(v -> {
            // 1. Start the animation
            rowView.animate()
                    .translationX(rowView.getWidth()) // Slide out to the right
                    .alpha(0f)                        // Fade out
                    .setDuration(300)                 // 300 milliseconds
                    .withEndAction(() -> {            // Run this when animation finishes
                        // 2. Actually remove from UI
                        binding.exercisesContainer.removeView(rowView);

                        // 3. Remove from data list
                        Long idToRemove = record.exercise.getExerciseId();
                        selectedExerciseIdList.remove(idToRemove);

                        // 4. Update the numbers (1, 2, 3...)
                        recalculateIndices();
                    })
                    .start();
        });

        binding.exercisesContainer.addView(rowView);
    }

    private void recalculateIndices() {
        for (int i = 0; i < binding.exercisesContainer.getChildCount(); i++) {
            View child = binding.exercisesContainer.getChildAt(i);
            TextView exerciseIndex = child.findViewById(R.id.leftSideControlIndex);
            if (exerciseIndex != null) exerciseIndex.setText(String.valueOf(i + 1));
        }
    }

    private void onSaveButtonClicked() {
        String title = String.valueOf(binding.workoutTitleInput.getText()).trim();
        if (title.isEmpty()) {
            binding.workoutTitleInput.setError("Title is required");
            return;
        }

        String Description = String.valueOf(binding.workoutDescriptionInput.getText());
        String note = String.valueOf(binding.workoutNotesInput.getText());

        Workout workout = new Workout();
        workout.setTitle(title);
        workout.setColour(selectedWorkoutColour);
        workout.setDescription(Description);
        workout.setNote(note);

        if (selectedWorkoutColour == null) {
            Toast.makeText(getContext(), "Please select a workout colour", Toast.LENGTH_SHORT).show();
            return; // stop execution if no color is picked
        }

        if (selectedExerciseIdList.isEmpty()) {
            Toast.makeText(getContext(), "Please add at least one exercise to this workout", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId != null) {
            workout.setOwnerId(currentUserId);
            workout.setUserCreated(true);
        } else {
            Toast.makeText(getContext(), "Error: User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (existingWorkoutId != -1) {
            workout.setWorkoutId(existingWorkoutId); // attach the ID to the object
            workoutViewModel.updateWorkout(workout, selectedExerciseIdList);
            Toast.makeText(getContext(), "Workout Updated!", Toast.LENGTH_SHORT).show();
        } else {
            workoutViewModel.saveWorkout(workout, selectedExerciseIdList);
            Toast.makeText(getContext(), "Workout Saved!", Toast.LENGTH_SHORT).show();
        }

        NavHostFragment.findNavController(this).navigateUp();
    }


    private void prefillForm(FullWorkoutRecord record) {
        binding.workoutTitleInput.setText(record.workout.getTitle());
        binding.workoutDescriptionInput.setText(record.workout.getDescription());
        binding.workoutNotesInput.setText(record.workout.getNote());

        if (record.workout.getColour() != null) {
            this.selectedWorkoutColour = record.workout.getColour();
            View colorView = getColorViewByValue(selectedWorkoutColour);
            if (colorView != null) {
                selectColour(selectedWorkoutColour, colorView);
            }
        }

        if (record.exercises != null && !record.exercises.isEmpty()) {
            if (selectedExerciseIdList.isEmpty()) {
                // Extract IDs from the list of exercises
                long[] exerciseIds = new long[record.exercises.size()];
                for (int i = 0; i < record.exercises.size(); i++) {
                    exerciseIds[i] = record.exercises.get(i).getExerciseId();
                }
                loadSelectedExercisesIntoUI(exerciseIds);
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Workout")
                .setMessage("Are you sure you want to delete this workout? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCurrentWorkout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCurrentWorkout() {
        if (existingWorkoutId != -1) {
            Workout workoutToDelete = new Workout();
            workoutToDelete.setWorkoutId(existingWorkoutId);

            workoutViewModel.deleteWorkout(workoutToDelete);

            Toast.makeText(getContext(), "Workout deleted", Toast.LENGTH_SHORT).show();

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_AddWorkoutScreen_to_WorkoutsList);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}