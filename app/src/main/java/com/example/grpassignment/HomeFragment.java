package com.example.grpassignment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
    private Button sharePostButton;
    private CardView communityReportButton;
    private CardView safetyMapsButton;
    private CardView educationHubCard;
    private TextView viewAllText;
    private Handler handler = new Handler();
    private Runnable runnable;
    private boolean isSosActive = false;

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
        sharePostButton = view.findViewById(R.id.button6);
        communityReportButton = view.findViewById(R.id.btn_share_location);
        safetyMapsButton = view.findViewById(R.id.btn_safety_zones);
        educationHubCard = view.findViewById(R.id.btn_report);
        viewAllText = view.findViewById(R.id.tv_view_all);

        sosButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isSosActive) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        // Deactivate SOS
                        isSosActive = false;
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.sos_inactive_red)); // Original Red
                        sosText.setText("PRESS FOR HELP");
                        sosHoldText.setText("Hold for 3 seconds to activate");
                        handler.removeCallbacks(runnable); // Cancel any pending activation
                    }
                    return true; // Consume the event
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Start a timer for 3 seconds to activate
                        runnable = new Runnable() {
                            @Override
                            public void run() {
                                // Change color and text after 3 seconds
                                isSosActive = true;
                                sosButton.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.sos_active_red)); // Dark Red
                                sosText.setText("SOS ACTIVE");
                                sosHoldText.setText("Emergency alerts sent to contacts!" +
                                        "\nPress again to deactivate");
                            }
                        };
                        handler.postDelayed(runnable, 3000); // 3000 milliseconds = 3 seconds
                        return true;
                    case MotionEvent.ACTION_UP:
                        // Cancel the timer if the touch is released before 3 seconds
                        handler.removeCallbacks(runnable);
                        return true;
                }
                return false;
            }
        });

        sharePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PostReportActivity.class);
                startActivity(intent);
            }
        });

        communityReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_nav_home_to_nav_report);
            }
        });

        safetyMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_nav_home_to_nav_map);
            }
        });

        View.OnClickListener educationHubClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_nav_home_to_nav_resources);
            }
        };

        educationHubCard.setOnClickListener(educationHubClickListener);
        viewAllText.setOnClickListener(educationHubClickListener);
    }
}
