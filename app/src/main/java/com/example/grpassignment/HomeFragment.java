package com.example.grpassignment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class HomeFragment extends Fragment {

    private CardView sosButton;
    private TextView sosText;
    private TextView sosHoldText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean isSosActive = false;

    private Runnable blinkRunnable;
    private boolean isBlinking = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // This fragment inflates the layout that was formerly main_page.xml
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sosButton = view.findViewById(R.id.sos_button);
        sosText = view.findViewById(R.id.sos_text);
        sosHoldText = view.findViewById(R.id.sos_hold_text);
        Button sharePostButton = view.findViewById(R.id.button6);
        CardView communityReportButton = view.findViewById(R.id.btn_share_location);
        CardView safetyMapsButton = view.findViewById(R.id.btn_safety_zones);
        CardView educationHubCard = view.findViewById(R.id.btn_report);
        TextView viewAllText = view.findViewById(R.id.tv_view_all);

        sosButton.setOnClickListener(v -> {
            if (isSosActive) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.sos_deactivate_title)
                        .setMessage(R.string.sos_deactivate_message)
                        .setPositiveButton(R.string.sos_deactivate_yes, (dialog, which) -> {
                            // Deactivate SOS
                            isSosActive = false;
                            stopBlinking();
                            sosButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_inactive_red)); // Original Red
                            sosText.setText(R.string.sos_press_for_help);
                            sosHoldText.setText(R.string.sos_hold_to_activate);
                            handler.removeCallbacks(runnable); // Cancel any pending activation
                        })
                        .setNegativeButton(R.string.sos_deactivate_no, null)
                        .show();
            }
        });

        sosButton.setOnTouchListener((v, event) -> {
            if (isSosActive) {
                // Let the OnClickListener handle it by returning false
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start a timer for 3 seconds to activate
                    runnable = () -> {
                        // Change color and text after 3 seconds
                        isSosActive = true;
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_active_red)); // Dark Red
                        sosText.setText(R.string.sos_active);
                        sosHoldText.setText(R.string.sos_active_message);
                        startBlinking();
                    };
                    handler.postDelayed(runnable, 3000); // 3000 milliseconds = 3 seconds
                    return true;
                case MotionEvent.ACTION_UP:
                    // Cancel the timer if the touch is released before 3 seconds
                    handler.removeCallbacks(runnable);
                    v.performClick();
                    return true;
            }
            return false;
        });

        sharePostButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PostReportActivity.class);
            startActivity(intent);
        });

        communityReportButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_nav_home_to_nav_report);
        });

        safetyMapsButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_nav_home_to_nav_map);
        });

        View.OnClickListener educationHubClickListener = v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_nav_home_to_nav_resources);
        };

        educationHubCard.setOnClickListener(educationHubClickListener);
        viewAllText.setOnClickListener(educationHubClickListener);
    }

    private void startBlinking() {
        isBlinking = true;
        blinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBlinking) {
                    if (sosButton.getCardBackgroundColor().getDefaultColor() == ContextCompat.getColor(requireContext(), R.color.sos_active_red)) {
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_inactive_red));
                    } else {
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_active_red));
                    }
                    handler.postDelayed(this, 500); // 500ms interval
                }
            }
        };
        handler.post(blinkRunnable);
    }

    private void stopBlinking() {
        isBlinking = false;
        handler.removeCallbacks(blinkRunnable);
    }
}
