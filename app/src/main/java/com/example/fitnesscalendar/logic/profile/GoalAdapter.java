package com.example.fitnesscalendar.logic.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesscalendar.R;
import com.example.fitnesscalendar.databinding.ItemGoalBinding;
import com.example.fitnesscalendar.entities.Goal;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter is a part of a Recycler system - dynamically creates that frames that may fit the screen
 */
public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {
    private List<Goal> goals = new ArrayList<>();

    public void setGoals(List<Goal> goals) {
        this.goals = goals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    // builds new rows (frames) for adding the data to the screen
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGoalBinding binding = ItemGoalBinding.inflate( // item_goal.xml
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GoalViewHolder(binding);
    }

    @Override
    // Fills the frames (rows) with the goal
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal currentGoal = goals.get(position);
        
        holder.binding.goalText.setText(currentGoal.getGoalTitle());
        
        if (currentGoal.getGoalSubtitle() != null) {
            holder.binding.goalSubtitle.setVisibility(View.VISIBLE);
            holder.binding.goalSubtitle.setText(currentGoal.getGoalSubtitle());
        }

        // extra top margin to custom goals to separate from predefined ones
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (currentGoal.isCustom()) {
            params.topMargin = (int) (20 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
        } else {
            params.topMargin = 0;
        }
        holder.itemView.setLayoutParams(params);

        int black = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_black_colour);
        holder.binding.goalText.setTextColor(black);
    }

    @Override
    // Calculates how many rows the RecyclerView needs to draw
    public int getItemCount() {
        return goals.size();
    }

    public static class GoalViewHolder extends RecyclerView.ViewHolder {
        public final ItemGoalBinding binding;

        public GoalViewHolder(ItemGoalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
