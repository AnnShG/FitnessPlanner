package com.example.fitnesscalendar.logic.exercise;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesscalendar.R;
import com.example.fitnesscalendar.databinding.ExercisesListScreenBinding;
import com.example.fitnesscalendar.entities.Category;
import com.example.fitnesscalendar.logic.filter.FilterViewModel;
import com.example.fitnesscalendar.logic.utils.CategoryStyleHelper;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

public class ExercisesListFragment extends Fragment {

    protected ExercisesListScreenBinding binding;
    protected ExerciseAdapter adapter;
    protected ExerciseViewModel exerciseViewModel;
    protected FilterViewModel filterViewModel;
    protected View root;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Only inflate the default parent binding if the child hasn't set 'root' yet
        if (root == null) {
            // inflated binding is assigned to the class field 'binding'
            binding = ExercisesListScreenBinding.inflate(inflater, container, false);
            root = binding.getRoot();
        } else {
            // If root was already set (e.g., by a child fragment),
            // re-bind it so 'binding' isn't null
            binding = ExercisesListScreenBinding.bind(root);
        }
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        filterViewModel = new ViewModelProvider(requireActivity()).get(FilterViewModel.class);
        exerciseViewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);

        setupRecyclerView(view);

        // connection FilterViewModel -> ExerciseViewModel
        filterViewModel.getExerciseFilters().observe(getViewLifecycleOwner(), ids -> {
            if (ids != null) {
                exerciseViewModel.setFilters(ids);
            }
        });

        // Observe the Data - when it changes - the block runs
        exerciseViewModel.filteredExercises.observe(getViewLifecycleOwner(), exercises -> {
            if (exercises != null  && binding != null) {
                adapter.setAllExercises(exercises); //redrawing the screen to refresh the list of the exes on the screen

                String countText = exercises.size() + " Exercises Found";
                binding.filteredCountText.setText(countText);
            }  else {
                binding.filteredCountText.setText("0 Exercises Found");
            }
        });

        binding.searchExerciseField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                exerciseViewModel.setSearchQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) { // real-time filtering
                exerciseViewModel.setSearchQuery(newText);
                binding.filteredCountText.setText(adapter.getItemCount() + " Exercises Found");
                return false;
            }
        });

        // categories chips
        exerciseViewModel.getAllCategories().observe(getViewLifecycleOwner(), allCategories -> {
            if (allCategories != null) {
                exerciseViewModel.getFilterIds().observe(getViewLifecycleOwner(), selectedIds -> {
                    if (selectedIds != null) {
                        renderFilterChips(selectedIds, allCategories);
                    }
                });
            }
        });

//        View backBtn = view.findViewById(R.id.backButton);
//        if (backBtn != null) {
//            backBtn.setOnClickListener(v ->
//                    NavHostFragment.findNavController(this).navigateUp()
//            );
//        }
        binding.backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.filterExerciseBtn.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("filter_type", "exercise");
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_ExercisesList_to_FilterScreen, bundle);
        });
    }

    private void setupRecyclerView(View view) {
        if (adapter == null) adapter = new ExerciseAdapter();
        RecyclerView recyclerView = view.findViewById(R.id.exercisesRecyclerView);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            recyclerView.setAdapter(adapter);
            recyclerView.setNestedScrollingEnabled(false);
        }

        // navigation to details logic
        adapter.setOnInfoClickListener(id -> { // lambda defines what happens when the users taps the item
            Bundle bundle = new Bundle();
            bundle.putLong("exerciseId", id);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_ExercisesList_to_ExerciseDetail, bundle); // take bundle (envelope) with ex id
        });
    }

    private void renderFilterChips(List<Long> selectedIds, List<Category> allCategories) {
        binding.selectedFilterChips.removeAllViews();

        for (Long id : selectedIds) {
            for (Category cat : allCategories) {
                if (cat.getId().equals(id)) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(cat.getName());
                    chip.setCloseIconVisible(true);

                    // utils helper method
                    CategoryStyleHelper.CategoryStyle style = CategoryStyleHelper.getStyleForGroup(cat.getCategoryGroup());
                    chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), style.backgroundColor)));
                    chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), style.strokeColor)));
                    chip.setChipStrokeWidth(2f);

                    chip.setOnCloseIconClickListener(v -> {
                        List<Long> newList = new ArrayList<>(selectedIds);
                        newList.remove(id);
                        filterViewModel.setExerciseFilters(newList);
                    });

                    binding.selectedFilterChips.addView(chip);
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
