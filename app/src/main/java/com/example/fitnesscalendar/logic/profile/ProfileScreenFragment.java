package com.example.fitnesscalendar.logic.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitnesscalendar.R;
import com.example.fitnesscalendar.databinding.ProfileScreenBinding;
import com.example.fitnesscalendar.viewModel.UserViewModel;

import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.Nullable;

import lombok.NonNull;

public class ProfileScreenFragment extends Fragment {

    private ProfileScreenBinding binding;
    private GoalAdapter goalAdapter;
    private UserViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = ProfileScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goalAdapter = new GoalAdapter();

        // setup the RecyclerView
        binding.goalsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.goalsRecyclerView.setAdapter(goalAdapter);

        viewModel = new ViewModelProvider(this).get(UserViewModel.class);

        viewModel.getProfileData().observe(getViewLifecycleOwner(), userWithGoals -> {
            if (userWithGoals != null && userWithGoals.user != null) {
                binding.userNameTitle.setText(userWithGoals.user.getName());

                if (userWithGoals.goals != null) {
                    goalAdapter.setGoals(userWithGoals.goals);
                }
            }
        });

        binding.editGoalButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("isEditMode", true);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_ProfileFragment_to_SurveyPage3, bundle);
        });

        binding.allExercisesRow.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_NavigationProfile_to_ExercisesList);
        });

        binding.allWorkoutsRow.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_NavigationProfile_to_WorkoutsList);
        });

        binding.userData.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("isEditMode", true);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_ProfileFragment_to_SurveyPage2, bundle);
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
