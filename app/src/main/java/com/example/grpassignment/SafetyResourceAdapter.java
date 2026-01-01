package com.example.grpassignment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SafetyResourceAdapter extends RecyclerView.Adapter<SafetyResourceAdapter.ViewHolder> {

    private final List<SafetyResource> resources;
    private final List<SafetyResource> filteredResources;
    private final Context context;

    public SafetyResourceAdapter(Context context, List<SafetyResource> resources) {
        this.context = context;
        this.resources = resources;
        this.filteredResources = new ArrayList<>(resources);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_safety_resource, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SafetyResource resource = filteredResources.get(position);

        holder.titleTextView.setText(resource.getTitle());
        holder.categoryTextView.setText(resource.getCategory());
        holder.durationTextView.setText(resource.getDuration());

        // Set icon based on type
        setIconAndColor(holder, resource.getType());

        // Set category badge color
        setCategoryColor(holder.categoryBadge, holder.categoryTextView, resource.getCategory());

        // Click listener to open resource
        holder.cardView.setOnClickListener(v -> openResource(resource));
    }

    @Override
    public int getItemCount() {
        return filteredResources.size();
    }

    private void setIconAndColor(ViewHolder holder, String type) {
        switch (type) {
            case "Videos":
                holder.iconCard.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                holder.iconImageView.setImageResource(R.drawable.screenshot_2025_11_18_160336);
                break;
            case "Articles":
                holder.iconCard.setCardBackgroundColor(Color.parseColor("#DBEAFE"));
                holder.iconImageView.setImageResource(R.drawable.screenshot_2025_11_18_160352);
                break;
            case "Workshops":
                holder.iconCard.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                holder.iconImageView.setImageResource(R.drawable.screenshot_2025_11_18_160406);
                break;
            case "Legal":
                holder.iconCard.setCardBackgroundColor(Color.parseColor("#DBEAFE"));
                holder.iconImageView.setImageResource(R.drawable.screenshot_2025_11_18_160422);
                break;
            default:
                holder.iconCard.setCardBackgroundColor(Color.parseColor("#F3E8FF"));
                holder.iconImageView.setImageResource(R.drawable.screenshot_2025_11_18_160439);
                break;
        }
    }

    private void setCategoryColor(CardView badgeCard, TextView badgeText, String category) {
        // Set colors based on category
        badgeCard.setCardBackgroundColor(Color.parseColor("#D1FAE5"));
        badgeText.setTextColor(Color.parseColor("#047857"));
        badgeText.setText(category);
    }

    public void filterByType(String type) {
        filteredResources.clear();
        if (type.equals("All")) {
            filteredResources.addAll(resources);
        } else {
            for (SafetyResource resource : resources) {
                if (resource.getType().equals(type)) {
                    filteredResources.add(resource);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void openResource(SafetyResource resource) {
        if (resource.getFile() != null && !resource.getFile().isEmpty()) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(resource.getFile()));
            context.startActivity(browserIntent);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView, iconCard, categoryBadge;
        ImageView iconImageView;
        TextView titleTextView, categoryTextView, durationTextView;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            iconCard = itemView.findViewById(R.id.iconCard);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            durationTextView = itemView.findViewById(R.id.durationTextView);
        }
    }
}