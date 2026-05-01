package com.example.fitnesscalendar.logic.calendar;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnesscalendar.R;
import com.example.fitnesscalendar.relations.DateColourResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CalendarAdapter acts as a Data Mapper or a Translator.
 * It takes a list of strings (day numbers) and maps them to a grid of TextViews - into the little boxes on the calendar
 * It is responsible for rendering the visual state of each day, including:
 * 1. The day number (1,2,3,4)
 * 2. Selection highlights (grey circles).
 * 3. Planned workout indicators (colored dots).
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private CalendarManager calendarManager;
    private final List<String> daysOfMonth;
    private final OnItemListener onItemListener;

    // State Management: Stores which dates are currently selected by the user (grey circles)
    private Set<Long> highlightedDates = new HashSet<>();

    // Data Management: Raw list of results from the database (Date + Colour + WorkoutID)
    private List<DateColourResult> plannedWorkouts = new ArrayList<>();

    // store the generated Drawables which are drawn once for each unique combination of colors
    private final Map<String, Drawable> circleCache = new HashMap<>();

    // UI Optimization: A Map that groups workout colors by date for instant lookup during drawing
    private final Map<Long, List<Integer>> dayWorkoutsMap = new HashMap<>(); // 20554 (April 10) -> [Red, Blue]
    private final Map<Long, List<Integer>> completedWorkoutsMap = new HashMap<>();
    public interface OnItemListener { // communicate click events back to the Fragment
        void onItemClick(int position, String dayText);
    }

    public CalendarAdapter(List<String> daysOfMonth, OnItemListener onItemListener) {
        this.daysOfMonth = daysOfMonth;
        this.onItemListener = onItemListener;
    }

    // Updates the highlighted state (grey circles) and stores the manager reference
    public void setHighlightedDates(Set<Long> newDates, CalendarManager manager) {
//        this.highlightedDates = newDates;
        this.calendarManager = manager;

        Set<Long> changedDates = new HashSet<>(this.highlightedDates);
        changedDates.addAll(newDates);

        Set<Long> oldDates = new HashSet<>(this.highlightedDates);// temp set of old dates to find what was removed/edit

        // Identify unchanged dates and remove them from 'changedDates'
        Set<Long> unchanged = new HashSet<>(oldDates);
        unchanged.retainAll(newDates);
        changedDates.removeAll(unchanged);

        // update the state
        this.highlightedDates = new HashSet<>(newDates);

        // loop through the visible grid ONCE
        if (calendarManager != null) {
            for (int i = 0; i < daysOfMonth.size(); i++) {
                String dayText = daysOfMonth.get(i);
                if (!dayText.isEmpty()) {
                    Long epochDay = calendarManager.getEpochDayForDay(dayText);
                    // only notify if this specific cell is one of the ones that changed
                    if (epochDay != null && changedDates.contains(epochDay)) {
                        notifyItemChanged(i);
                    }
                }
            }
        }
    }


    public void setDays(List<String> days) { // Updates the grid days (e.g., when moving from March to April)
        this.daysOfMonth.clear();
        this.daysOfMonth.addAll(days);
        notifyDataSetChanged();
    }

    /**
     * Receives new data from the DB and pre-calculates the Colour Map.
     */
    public void setPlannedWorkouts(List<DateColourResult> plans) {
        this.plannedWorkouts = plans;
        dayWorkoutsMap.clear();
        completedWorkoutsMap.clear();

        for (DateColourResult plan : plans) {
            if (plan.date == null) continue; // skip if date is null

            if (plan.isCompleted) {
                // add to Completion Circle Map
                completedWorkoutsMap.computeIfAbsent(plan.date, k -> new ArrayList<>()).add(plan.colour);
            } else {
                // add to Active Dots Map (Only if NOT completed)
                List<Integer> colours = dayWorkoutsMap.computeIfAbsent(plan.date, k -> new ArrayList<>());
                if (colours.size() < 3) colours.add(plan.colour);
            }
        }
        notifyDataSetChanged();
    }

    public List<DateColourResult> getPlannedWorkouts() {
        return plannedWorkouts != null ? plannedWorkouts : new ArrayList<>();
    }


    // create the grid cells for every number
    // TextViews are created programmatically to keep the layout lightweight
    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView dayText = new TextView(parent.getContext());

        dayText.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // Width fills the 1/7th of the grid
                160 // Fixed height of 160 pixels for the calendar row
        ));

        dayText.setGravity(Gravity.CENTER);
        dayText.setTextSize(20);
        dayText.setTextColor(Color.BLACK);

        return new CalendarViewHolder(dayText);
    }



    // runs for every cell in the calendar
    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String dayText = daysOfMonth.get(position);
        holder.dayOfMonth.setText(dayText);

        // Initial Reset - empty calendar at the beginning
        holder.dayOfMonth.setTypeface(null, android.graphics.Typeface.NORMAL);
        holder.dayOfMonth.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        holder.dayOfMonth.setBackground(null);
//        holder.dayOfMonth.setTextColor(ContextCompat.getColor(
//                holder.itemView.getContext(),
//                R.color.text_black_colour
//        ));

        // handle empty cells
        if (dayText.isEmpty()) {
            holder.itemView.setOnClickListener(null);
            return;
        }

        // Touch listener - one item clicked
        holder.itemView.setOnClickListener(v -> {
            if (onItemListener != null) {
                onItemListener.onItemClick(position, dayText);
            }
        });

        //  Convert the number on the calendar to a unique numeric EpochDay
        Long epochDay = (calendarManager != null) ? calendarManager.getEpochDayForDay(dayText) : null;

        if (epochDay != null) {
            // today highlighting
            long todayEpoch = LocalDate.now().toEpochDay();
            if (epochDay == todayEpoch) {
                holder.dayOfMonth.setTypeface(null, android.graphics.Typeface.BOLD);
                holder.dayOfMonth.setTextColor(androidx.core.content.ContextCompat.getColor(
                        holder.itemView.getContext(),
                        R.color.text_black_colour
                ));
            }

            // multiple dots - max 3
            List<Integer> colours = dayWorkoutsMap.get(epochDay);
            if (colours != null && !colours.isEmpty()) {
                // a custom horizontal bitmap of dots
                Drawable dotsDrawable = drawDots(holder.itemView.getContext(), colours);
                holder.dayOfMonth.setCompoundDrawablesWithIntrinsicBounds(null, null, null, dotsDrawable);
                holder.dayOfMonth.setCompoundDrawablePadding(6);
            }

            // Selection and Completion backgrounds
            List<Integer> doneColours = completedWorkoutsMap.get(epochDay);
            boolean isSelected = highlightedDates != null && highlightedDates.contains(epochDay);

            Drawable completionDrawable = null;
            if (doneColours != null && !doneColours.isEmpty()) {
                completionDrawable = getCachedCompletionCircle(holder.itemView.getContext(), doneColours);
            }

            if (isSelected && completionDrawable != null) {
                // COMBO: The user has this day selected AND there are completed workouts
                // Create a LayerDrawable to stack the grey selection circle under the completion segments
                Drawable selectionCircle = ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.plan_selected_day_circle);
                LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{selectionCircle, completionDrawable});
                holder.dayOfMonth.setBackground(layerDrawable);
            } else if (isSelected) {
                // Only selected, no completions yet
                holder.dayOfMonth.setBackgroundResource(R.drawable.plan_selected_day_circle);
            } else if (completionDrawable != null) {
                // Not selected, but has completions
                holder.dayOfMonth.setBackground(completionDrawable);
            } else {
                // Default state: no background
                holder.dayOfMonth.setBackground(null);
            }
        }
    }

    // using Android Canvas API
//     generates a BitmapDrawable containing 1 to 3 colored dots side-by-side.
    private Drawable drawDots(Context context, List<Integer> colors) {
        int dotRadius = 8;
        int spacing = 6;
        int maxDots = Math.min(colors.size(), 3);

        // Calculate total width needed for the dots
        int width = (dotRadius * 2 * maxDots) + (spacing * (maxDots - 1));
        int height = dotRadius * 2;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        for (int i = 0; i < maxDots; i++) {
            paint.setColor(colors.get(i));
            float x = dotRadius + (i * (dotRadius * 2 + spacing));
            canvas.drawCircle(x, dotRadius, dotRadius, paint);
        }

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private Drawable drawCompletionCircle(android.content.Context context, List<Integer> colours) {
        int bitmapSize = 120; // smaller than cell height
        Bitmap bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        float padding = 25f; // enlarge and diminish the circle
        float startAngle = -90f; // Start at the top
        float baseSweep = 360f / colours.size(); // baseSweep is the total space (slice) allocated for one colour

        float gap = colours.size() > 1 ? 25f : 0f; // // Gap size in degrees between colours
        float sweepAngle = baseSweep - gap;

        android.graphics.RectF rect = new RectF(padding, padding, bitmapSize - padding, bitmapSize - padding);

        for (int color : colours) {
            paint.setColor(color);

            // Adjust startAngle to center the segment within its allocated space
            float adjustedStart = startAngle + (gap / 2f);

            canvas.drawArc(rect, adjustedStart, sweepAngle, false, paint);
            startAngle += baseSweep; // Increment by baseSweep (full slice)
        }

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private Drawable getCachedCompletionCircle(Context context, List<Integer> colors) {
        // creates a unique key based on the colors
        StringBuilder keyBuilder = new StringBuilder();
        for (int col : colors) keyBuilder.append(col).append("_");
        String key = keyBuilder.toString();

        if (circleCache.containsKey(key)) {
            return circleCache.get(key);
        }

        // if not in cache, draw it
        Drawable drawable = drawCompletionCircle(context, colors);
        circleCache.put(key, drawable);
        return drawable;
    }

    @Override
    public int getItemCount() {
        return daysOfMonth.size();
    }

    /**
     * Checks if a specific workout is already on a specific date
     * Prevents the user from planning the same workout twice on the same day
     */
    public boolean isWorkoutAlreadyPlanned(Long epochDay, long workoutId) {
        if (plannedWorkouts == null || epochDay == null) return false;

        for (DateColourResult plan : plannedWorkouts) {
            // Compare the date and the specific workout ID
            if (epochDay.equals(plan.date)) {
                if (plan.workoutId != null && plan.workoutId == workoutId) {
                    return true;
                }
            }
        }
        return false;
    }

    // Returns how many dots&completed workouts are currently on this day, to prevent of allowing adding the forth one
    public int getWorkoutsCountForDay(Long epochDay) {
        if (epochDay == null) return 0;

        List<Integer> activeDots = dayWorkoutsMap.get(epochDay);
        int activeCount = (activeDots != null) ? activeDots.size() : 0;

        List<Integer> completed = completedWorkoutsMap.get(epochDay);
        int completedCount = (completed != null) ? completed.size() : 0;

        return activeCount + completedCount;
    }

    /**
     * Checks if a specific workout is marked as completed on a specific day.
     */
    public boolean isWorkoutCompleted(Long epochDay, long workoutId) {
        if (plannedWorkouts == null || epochDay == null) return false;

        for (DateColourResult plan : plannedWorkouts) {
            if (epochDay.equals(plan.date) && plan.workoutId != null && plan.workoutId == workoutId) {
                return plan.isCompleted; // Returns true if the database column ref.is_completed is true
            }
        }
        return false;
    }

    /**
     * ViewHolder class to hold the views for each calendar day item.
     */
    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        public final TextView dayOfMonth;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);

            dayOfMonth = (TextView) itemView;
        }
    }
}