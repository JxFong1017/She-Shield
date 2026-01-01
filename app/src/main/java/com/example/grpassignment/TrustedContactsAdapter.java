package com.example.grpassignment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrustedContactsAdapter extends RecyclerView.Adapter<TrustedContactsAdapter.ContactViewHolder> {

    private List<TrustedContact> contactList;// Add these two variables to hold the location
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    public TrustedContactsAdapter(List<TrustedContact> contactList) {
        this.contactList = contactList;
    }

    // Helper function to update location from Fragment
    public void updateCurrentLocation(double lat, double lng) {
        this.currentLat = lat;
        this.currentLng = lng;
        // no need to notifyDataSetChanged here, just store the values
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_trusted_contact.xml layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trusted_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        TrustedContact contact = contactList.get(position);

        holder.name.setText(contact.getName());
        holder.phone.setText(contact.getPhone());

        // Set Initial (First letter of name)
        if (contact.getName() != null && !contact.getName().isEmpty()) {
            holder.initial.setText(contact.getName().substring(0, 1).toUpperCase());
        }
        holder.phone.setText(contact.getPhone());

        // Handle Share Button Click inside the item
        holder.shareBtn.setOnClickListener(v -> {
            String phoneNumber = contact.getPhone();

            // Check if we actually have a location yet
            if (currentLat == 0.0 && currentLng == 0.0) {
                Toast.makeText(v.getContext(), "Waiting for GPS signal...", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create the FREE Google Maps link
            // Format: https://www.google.com/maps/search/?api=1&query=LAT,LNG
            String mapLink = "https://www.google.com/maps/search/?api=1&query=" + currentLat + "," + currentLng;

            String message = "Help! I am using SheShield. My current location is: " + mapLink;

            // Send via WhatsApp
            String formattedNumber = phoneNumber.replace("+", "");
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                String url = "https://api.whatsapp.com/send?phone=" + formattedNumber + "&text=" + java.net.URLEncoder.encode(message, "UTF-8");
                intent.setPackage("com.whatsapp");
                intent.setData(android.net.Uri.parse(url));
                v.getContext().startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });

        // CALL BUTTON LOGIC
        holder.callBtn.setOnClickListener(v -> {
            String phone = contact.getPhone();
            if (phone != null && !phone.isEmpty()) {
                // ACTION_DIAL opens the keypad. ACTION_CALL calls immediately (needs permission)
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                intent.setData(android.net.Uri.parse("tel:" + phone));
                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(v.getContext(), "No phone number available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    // ViewHolder class maps the UI elements
    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView name, phone, initial;
        TextView shareBtn, callBtn;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contact_name);
            phone = itemView.findViewById(R.id.contact_phone);
            initial = itemView.findViewById(R.id.contact_initial);
            shareBtn = itemView.findViewById(R.id.btn_item_share);
            callBtn = itemView.findViewById(R.id.btn_item_call);
        }
    }
}