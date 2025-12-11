package com.example.grpassignment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SafetyZoneAdapter extends RecyclerView.Adapter<SafetyZoneAdapter.ViewHolder> {

    private List<SafetyZone> list = new ArrayList<>();

    public void setData(List<SafetyZone> newList) {
        this.list = newList;
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

        holder.tvName.setText(zone.name);
        holder.tvDistance.setText("📍 Calculating…");

        if (zone.is24hour) {
            holder.tvIs24hr.setText("24/7");
        } else {
            holder.tvIs24hr.setText("Limited");
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvDistance, tvIs24hr;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvName);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvIs24hr = itemView.findViewById(R.id.tvIs24hr);
        }
    }
}
