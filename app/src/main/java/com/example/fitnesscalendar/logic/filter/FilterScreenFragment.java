package com.example.fitnesscalendar.logic.filter;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fitnesscalendar.R;
import com.example.fitnesscalendar.databinding.FilterScreenBinding;
import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.logic.exercise.ExerciseViewModel;
import com.example.fitnesscalendar.logic.utils.CategoryStyleHelper;
import com.example.fitnesscalendar.logic.workout.WorkoutViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class FilterScreenFragment extends Fragment {

    private FilterScreenBinding binding;
    protected ExerciseViewModel exerciseViewModel;
    protected WorkoutViewModel workoutViewModel;
    private FilterViewModel filterViewModel;
    private String filterType;
    private List<Long> alreadySelectedIds = new ArrayList<>();
    private FilterListener filterListener;

    public interface FilterListener {
        void onFiltersSelected(List<Long> selectedCategoryIds);
    }

    public FilterScreenFragment() {
    }

    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }

    public void setAlreadySelectedIds(List<Long> ids) {
        if (ids != null) {
            this.alreadySelectedIds = new ArrayList<>(ids);
        } else {
            this.alreadySelectedIds = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FilterScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get filter type from arguments
        if (getArguments() != null) {
            filterType = getArguments().getString("filter_type", "exercise");
        }

        filterViewModel = new ViewModelProvider(requireActivity()).get(FilterViewModel.class);
        exerciseViewModel = new ViewModelProvider(requireActivity()).get(ExerciseViewModel.class);
        workoutViewModel = new ViewModelProvider(requireActivity()).get(WorkoutViewModel.class);

        // Initialize alreadySelectedIds based on type
        if ("workout".equals(filterType)) {
            List<Long> currentFilters = filterViewModel.getWorkoutFilters().getValue();
            this.alreadySelectedIds = (currentFilters != null) ? currentFilters : new ArrayList<>();
        } else {
            List<Long> currentFilters = filterViewModel.getExerciseFilters().getValue();
            this.alreadySelectedIds = (currentFilters != null) ? currentFilters : new ArrayList<>();
        }

        exerciseViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            renderAllCategories(categories);
        });

        binding.applyFilterButton.setOnClickListener(v -> {
            List<Long> selected = getFinalSelection();

            if ("workout".equals(filterType)) {
                filterViewModel.setWorkoutFilters(selected);
            } else {
                filterViewModel.setExerciseFilters(selected);
            }

            if (filterListener != null) {
                filterListener.onFiltersSelected(selected);
            }

            NavHostFragment.findNavController(this).navigateUp();
        });

        binding.cancelFilterText.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });
    }

    private Chip createFilterChip(Category category) {
//        Chip chip = new Chip(requireContext());
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(requireContext(), R.style.CustomChipStyle);
        Chip chip = new Chip(contextWrapper);
        chip.setText(category.getName());
        chip.setTag(category.getId());
        chip.setCheckable(true);

        CategoryStyleHelper.CategoryStyle style = CategoryStyleHelper.getStyleForGroup(category.getCategoryGroup());

        int bgColor = ContextCompat.getColor(requireContext(), style.backgroundColor);
        int defaultStroke = ContextCompat.getColor(requireContext(), style.strokeColor);
        int activeStroke = ContextCompat.getColor(requireContext(), R.color.exercise_selected_category_stroke_colour);

        boolean isInitiallySelected = alreadySelectedIds.contains(category.getId());

        // initial appearance
        chip.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
        chip.setChecked(isInitiallySelected);
        chip.setChipStrokeColor(ColorStateList.valueOf(isInitiallySelected ? activeStroke : defaultStroke));
        chip.setChipStrokeWidth(isInitiallySelected ? 6f : 4f);

        // orange stroke when selected
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chip.setChipStrokeColor(ColorStateList.valueOf(isChecked ? activeStroke : defaultStroke));
            chip.setChipStrokeWidth(isChecked ? 6f : 4f);
        });

        return chip;
    }

    public void renderAllCategories(List<Category> categories) {
        if (binding == null || categories == null) return;

        binding.filterTypeGroup.removeAllViews();
        binding.filterBasicGroup.removeAllViews();
        binding.filterAdvancedGroup.removeAllViews();

        for (Category cat : categories) {
            Chip chip = createFilterChip(cat);

            switch (cat.getCategoryGroup()) {
                case "TYPE":
                    binding.filterTypeGroup.addView(chip);
                    break;
                case "BASIC":
                    binding.filterBasicGroup.addView(chip);
                    break;
                case "ADVANCED":
                    binding.filterAdvancedGroup.addView(chip);
                    break;
            }
        }
    }

    private List<Long> getFinalSelection() {
        List<Long> selected = new ArrayList<>();

        selected.addAll(getSelectedIdsFromGroup(binding.filterTypeGroup));
        selected.addAll(getSelectedIdsFromGroup(binding.filterBasicGroup));
        selected.addAll(getSelectedIdsFromGroup(binding.filterAdvancedGroup));

        return selected;
    }

    private List<Long> getSelectedIdsFromGroup(ChipGroup group) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            if (chip.isChecked()) {
                ids.add((Long) chip.getTag());
            }
        }
        return ids;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
