package com.example.fitnesscalendar.logic.survey;

import android.app.DatePickerDialog;import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fitnesscalendar.R;
import com.example.fitnesscalendar.databinding.SurveyPage2Binding;
import com.example.fitnesscalendar.entities.User;
import com.example.fitnesscalendar.viewModel.UserViewModel;
import com.example.fitnesscalendar.viewModel.SurveyViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SurveyPage2Fragment extends Fragment {

    private SurveyPage2Binding binding;
    private SurveyViewModel surveyViewModel;
    private UserViewModel userViewModel;
    private boolean isEditMode = false;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment using ViewBinding
        binding = SurveyPage2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Access the shared ViewModels
        surveyViewModel = new ViewModelProvider(requireActivity()).get(SurveyViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean("isEditMode", false);
        }

        if (isEditMode) {
            // Apply edit mode changes
            binding.survey2Root.setBackgroundColor(Color.parseColor("#FFFAFA"));
            binding.textView.setText("Edit my data");
            binding.continueButton.setText("Save");
            binding.continueButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_black_colour));
            binding.backButton.setVisibility(View.INVISIBLE);
        }

        // --- GENDER SELECTION LOGIC ---
        binding.buttonMale.setOnClickListener(v -> handleGenderSelection(binding.buttonMale, "Male"));
        binding.buttonFemale.setOnClickListener(v -> handleGenderSelection(binding.buttonFemale, "Female"));
        binding.buttonNoAnswer.setOnClickListener(v -> handleGenderSelection(binding.buttonNoAnswer, "Prefer not to say"));

        // Setup Date Picker
        binding.userInputBirthDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            if (surveyViewModel.getBirthDate() != null) {
                c.setTime(surveyViewModel.getBirthDate());
            }
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    R.style.Widget_App_DatePickerSpinner,
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        String dateString = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                        binding.userInputBirthDate.setText(dateString);

                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(selectedYear, selectedMonth, selectedDay);
                        surveyViewModel.setBirthDate(selectedCal.getTime());
                    },
                    year, month, day
            );

            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            if (datePickerDialog.getWindow() != null) {
                datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            datePickerDialog.show();
        });

        // --- STATE RESTORATION / PRE-FILLING ---
        // if the ViewModel has no data yet, load it from the actual User Data
        userViewModel.getLoggedInUser().observe(getViewLifecycleOwner(), userWithGoals -> {
            if (userWithGoals != null && userWithGoals.user != null) {
                this.currentUser = userWithGoals.user;

                if (isEditMode) {
                    // Pre-fill the UI and ViewModel from DB if they haven't been modified yet
                    if (surveyViewModel.getName() == null) {
                        surveyViewModel.setName(currentUser.getName());
                        binding.userInputName.setText(currentUser.getName());
                    } else {
                        binding.userInputName.setText(surveyViewModel.getName());
                    }

                    if (surveyViewModel.getBirthDate() == null) {
                        surveyViewModel.setBirthDate(currentUser.getBirthDate());
                    }

                    if (surveyViewModel.getGender() == null) {
                        surveyViewModel.setGender(currentUser.getGender());
                    }

                    // Update UI for date and gender
                    if (surveyViewModel.getBirthDate() != null) {
                        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                        binding.userInputBirthDate.setText(format.format(surveyViewModel.getBirthDate()));
                    }

                    if (surveyViewModel.getGender() != null) {
                        restoreGenderUI(surveyViewModel.getGender());
                    }
                }
            }
        });

        // Regular onboarding pre-filling (if not in Edit Mode or if user already typed something)
        if (!isEditMode) {
            if (surveyViewModel.getName() != null) {
                binding.userInputName.setText(surveyViewModel.getName());
            }
            if (surveyViewModel.getBirthDate() != null) {
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                binding.userInputBirthDate.setText(format.format(surveyViewModel.getBirthDate()));
            }
            if (surveyViewModel.getGender() != null) {
                restoreGenderUI(surveyViewModel.getGender());
            }
        }

        // --- NAVIGATION / SAVING ---
        binding.continueButton.setOnClickListener(v -> {
            boolean isValid = true;

            String name = "";
            if (binding.userInputName.getText() != null) {
                name = binding.userInputName.getText().toString().trim();
            }

            if (name.isEmpty()) {
                binding.userInputName.setError("Please enter your name");
                isValid = false;
            } else {
                surveyViewModel.setName(name);
            }

            if (surveyViewModel.getBirthDate() == null) {
                binding.userInputBirthDate.setError("Please select your birth date");
                isValid = false;
            }

            if (surveyViewModel.getGender() == null) {
                Toast.makeText(requireContext(), "Please select a gender", Toast.LENGTH_SHORT).show();
                isValid = false;
            }

            if (isValid) {
                if (isEditMode) {
                    saveUserDataToDatabase();
                } else {
                    // regular flow
                    NavHostFragment.findNavController(SurveyPage2Fragment.this)
                            .navigate(R.id.action_SurveyPage2_to_SurveyPage3);
                }
            }
        });

        binding.backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(SurveyPage2Fragment.this)
                        .navigateUp()
        );
    }

    // Helper Method
    private void handleGenderSelection(MaterialButton selectedButton, String genderValue) {
        // save to ViewModel
        surveyViewModel.setGender(genderValue);

        int orangeStroke = ContextCompat.getColor(requireContext(), R.color.chip_selected_orange);
        int grayStroke = ContextCompat.getColor(requireContext(), R.color.button_stroke_colour);

        MaterialButton[] buttons = {binding.buttonMale, binding.buttonFemale, binding.buttonNoAnswer};

        for (MaterialButton btn : buttons) {
            // force background to stay white
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));

            if (btn.getId() == selectedButton.getId()) {
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(orangeStroke));
                btn.setStrokeWidth(6);
            } else {
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(grayStroke));
                btn.setStrokeWidth(2);
            }
        }
    }

    private void restoreGenderUI(String gender) {
        switch (gender) {
            case "Male": handleGenderSelection(binding.buttonMale, "Male"); break;
            case "Female": handleGenderSelection(binding.buttonFemale, "Female"); break;
            case "Prefer not to say": handleGenderSelection(binding.buttonNoAnswer, "Prefer not to say"); break;
        }
    }

    private void saveUserDataToDatabase() {
        if (currentUser != null) {
            currentUser.setName(surveyViewModel.getName());
            currentUser.setBirthDate(surveyViewModel.getBirthDate());
            currentUser.setGender(surveyViewModel.getGender());

            userViewModel.updateUser(currentUser);

            // clear the shared ViewModel state after saving
            surveyViewModel.setName(null);
            surveyViewModel.setBirthDate(null);
            surveyViewModel.setGender(null);

            Toast.makeText(getContext(), "Data updated!", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigateUp();
        } else {
            Toast.makeText(getContext(), "Error: User session lost", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}