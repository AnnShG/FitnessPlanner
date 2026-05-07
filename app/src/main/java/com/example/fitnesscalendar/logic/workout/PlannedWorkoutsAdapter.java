package com.example.fitnesscalendar.logic.workout;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesscalendar.databinding.PlannedWorkoutItemBinding;
import com.example.fitnesscalendar.relations.PlannedWorkoutInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * PlannedWorkoutsAdapter manages the list of "Management Cards" displayed below the calendar.
 * Each card represents a workout that is currently scheduled on at least one day.
 * This adapter allows the user to trigger the 'Edit' mode or 'Delete' the entire schedule for a workout.
 */
public class PlannedWorkoutsAdapter extends RecyclerView.Adapter<PlannedWorkoutsAdapter.ViewHolder> {
    // The data source - a list of unique workouts scheduled on the calendar
    private List<PlannedWorkoutInfo> items = new ArrayList<>();
    private final OnPlanActionListener listener; // sends button clicks from the card back to the Fragment

    public interface OnPlanActionListener { // handles user interactions within the list
        void onEdit(PlannedWorkoutInfo info); // Triggered when the "Edit" text is clicked
        void onDelete(long workoutId); // Triggered when the "Bin" icon is clicked
    }

    public PlannedWorkoutsAdapter(OnPlanActionListener listener) { this.listener = listener; }

    public void setWorkouts(List<PlannedWorkoutInfo> newList) { // Updates the list of workouts and refreshes the UI
        this.items = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding to inflate the layout we created earlier
        PlannedWorkoutItemBinding binding = PlannedWorkoutItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    // Maps the data from a PlannedWorkoutInfo object to the UI elements of the card
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlannedWorkoutInfo item = items.get(position);

        holder.binding.plannedWorkoutTitle.setText(item.title);

        // Set the color dot
        if (item.colour != null) {
            holder.binding.workoutColorDot.setBackgroundTintList(ColorStateList.valueOf(item.colour));
        }

        // Listeners for the actions
        holder.binding.btnDeletePlan.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item.workoutId);
            }
        });

        holder.binding.btnEditPlan.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final PlannedWorkoutItemBinding binding;

        public ViewHolder(@NonNull PlannedWorkoutItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
