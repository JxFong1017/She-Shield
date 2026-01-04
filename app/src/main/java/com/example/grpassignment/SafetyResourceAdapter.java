package com.example.grpassignment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SafetyResourceAdapter extends RecyclerView.Adapter<SafetyResourceAdapter.ViewHolder> {

    private final List<SafetyResource> resources;
    private final List<SafetyResource> filteredResources;
    private final Context context;

    public SafetyResourceAdapter(Context context, List<SafetyResource> resources) {
        this.context = context;
        this.resources = new ArrayList<>();
        this.filteredResources = new ArrayList<>();
        if (resources != null) {
            this.resources.addAll(resources);
            this.filteredResources.addAll(resources);
        }
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

        // Load image using Glide
        if (resource.getImageUrl() != null && !resource.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(resource.getImageUrl())
                    .placeholder(R.drawable.image_removebg_preview__24_)
                    .error(R.drawable.image_removebg_preview__24_)
                    .centerCrop()
                    .into(holder.iconImageView);
        } else {
            // Fallback to type-based icons if no imageUrl
            setIconByType(holder.iconImageView, resource.getType());
        }

        setCategoryColor(holder.categoryBadge, holder.categoryTextView, resource.getCategory());

        // Navigate to detail fragment on click
        holder.cardView.setOnClickListener(v -> openResourceDetail(resource));
    }

    @Override
    public int getItemCount() {
        return filteredResources.size();
    }

    private void setIconByType(ImageView imageView, String type) {
        switch (type) {
            case "Video":
                imageView.setImageResource(R.drawable.image_removebg_preview__26_);
                break;
            case "Article":
                imageView.setImageResource(R.drawable.image_removebg_preview__24_);
                break;
            case "Workshop":
                imageView.setImageResource(R.drawable.workshop);
                break;
            case "Legal":
                imageView.setImageResource(R.drawable.legal);
                break;
            default:
                imageView.setImageResource(R.drawable.image_removebg_preview__24_);
                break;
        }
    }

    private void setCategoryColor(CardView badgeCard, TextView badgeText, String category) {
        badgeCard.setCardBackgroundColor(Color.parseColor("#D1FAE5"));
        badgeText.setTextColor(Color.parseColor("#047857"));
        badgeText.setText(category);
    }

    private void openResourceDetail(SafetyResource resource) {
        try {
            // Create bundle with resource data
            Bundle bundle = new Bundle();
            bundle.putParcelable("safetyResource", resource);

            // Create and show detail fragment
            SafetyResourceDetailFragment detailFragment = new SafetyResourceDetailFragment();
            detailFragment.setArguments(bundle);

            // Navigate to detail fragment
            if (context instanceof FragmentActivity) {
                ((FragmentActivity) context).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            } else if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            }
        } catch (Exception e) {
            Log.e("SafetyResourceAdapter", "Error opening detail: " + e.getMessage());
        }
    }

    public void updateList(List<SafetyResource> newList) {
        this.resources.clear();
        this.filteredResources.clear();
        if (newList != null) {
            this.resources.addAll(newList);
            this.filteredResources.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public void filterByType(String type) {
        filteredResources.clear();
        if (type.equalsIgnoreCase("All")) {
            filteredResources.addAll(resources);
        } else {
            for (SafetyResource resource : resources) {
                if (resource.getType() != null && resource.getType().equalsIgnoreCase(type)) {
                    filteredResources.add(resource);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView, categoryBadge;
        ImageView iconImageView;
        TextView titleTextView, categoryTextView, durationTextView;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            iconImageView = itemView.findViewById(R.id.iconImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            durationTextView = itemView.findViewById(R.id.durationTextView);
        }
    }
}
