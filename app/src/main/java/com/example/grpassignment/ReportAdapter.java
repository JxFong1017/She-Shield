package com.example.grpassignment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private Context context;
    private List<Report> reportList;

    public ReportAdapter(Context context, List<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.txtTitle.setText(report.getType());
        
        // Format location and time for display
        String displayLoc = "Location: " + report.getLocation() + " | " + report.getTime();
        holder.txtLocationTime.setText(displayLoc);

        // Use Glide to load the image from the URL
        if (report.getMediaUri() != null) {
            Glide.with(context)
                 .load(report.getMediaUri())
                 .placeholder(R.drawable.screenshot_2025_11_18_130845_removebg_preview) // Image while loading
                 .error(R.drawable.screenshot_2025_11_18_130845_removebg_preview) // Image if loading fails
                 .into(holder.imgReport);
        } else {
            // Set a default image if no media URI exists
            holder.imgReport.setImageResource(R.drawable.screenshot_2025_11_18_130845_removebg_preview); 
        }

        // Handle "View Details" click
        holder.btnViewDetails.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putSerializable("report", report);
            androidx.navigation.Navigation.findNavController(v).navigate(R.id.action_nav_report_to_reportDetail, args);
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        ImageView imgReport;
        TextView txtTitle, txtLocationTime;
        Button btnViewDetails;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            imgReport = itemView.findViewById(R.id.img_report);
            txtTitle = itemView.findViewById(R.id.txt_report_title);
            txtLocationTime = itemView.findViewById(R.id.txt_report_location_time);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }
    }
}