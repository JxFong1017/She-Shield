package com.example.grpassignment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SafetyZoneAdapter extends RecyclerView.Adapter<SafetyZoneAdapter.ViewHolder> {

    private List<SafetyZone> list = new ArrayList<>();
    private org.osmdroid.util.GeoPoint userLocation;
    private OnItemClickListener listener; // Listener for item clicks

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(SafetyZone zone);
    }

    // Constructor to accept the listener
    public SafetyZoneAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<SafetyZone> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public void setUserLocation(org.osmdroid.util.GeoPoint location) {
        this.userLocation = location;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_safety_zone, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SafetyZone zone = list.get(position);
        holder.bind(zone, listener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvDistance, tvIs24hr;
        ImageView iconType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvIs24hr = itemView.findViewById(R.id.tvIs24hr);
            iconType = itemView.findViewById(R.id.iconType);
        }

        public void bind(final SafetyZone zone, final OnItemClickListener listener) {
            tvName.setText(zone.name);

            if (userLocation != null && zone.geolocation != null) {
                org.osmdroid.util.GeoPoint zonePoint = new org.osmdroid.util.GeoPoint(
                        zone.geolocation.getLatitude(),
                        zone.geolocation.getLongitude()
                );
                double distanceInMeters = userLocation.distanceToAsDouble(zonePoint);
                String distanceText = distanceInMeters < 1000 ?
                        String.format(Locale.US, "📍 %.0f m", distanceInMeters) :
                        String.format(Locale.US, "📍 %.1f km", distanceInMeters / 1000.0);
                tvDistance.setText(distanceText);
            } else {
                tvDistance.setText("📍 Calculating…");
            }

            tvIs24hr.setText(zone.is24hour ? "24/7" : "Limited");

            if (zone.imageUrl != null && !zone.imageUrl.isEmpty()) {
                Glide.with(itemView.getContext()).load(zone.imageUrl).into(iconType);
            }

            // Set the click listener on the entire item view
            itemView.setOnClickListener(v -> listener.onItemClick(zone));
        }
    }
}
