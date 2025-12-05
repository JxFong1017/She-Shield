package com.example.grpassignment;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Find the toolbar from your activity's layout
        Toolbar toolbar = findViewById(R.id.toolbar); // Make sure your Toolbar has this ID
        setSupportActionBar(toolbar);

        // 2. Get the NavController from the NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment); // Use the ID of your FragmentContainerView
        NavController navController = navHostFragment.getNavController();

//        // 3. Find the DrawerLayout
//        DrawerLayout drawerLayout = findViewById(R.id.DLMain);
//
//        // 4. Create AppBarConfiguration and link it with the DrawerLayout
//        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph())
//                .setOpenableLayout(drawerLayout)   // add this to handle drawer correctly
//                .build();

        // 3. Connect the BottomNavigationView to the NavController
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // 4. Connect the Toolbar to the NavController
        // This will automatically update the toolbar title and handle the Up button
        NavigationUI.setupActionBarWithNavController(this, navController);


        // 4. Link the NavController to the ActionBar (the Toolbar)
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);




//        // 1. Find the BottomNavigationView by its ID
//        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
//
//        // 2. Connect the NavController to the BottomNavigationView
//        NavigationUI.setupWithNavController(bottomNav, navController);


        // NEW
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        drawerLayout.addDrawerListener(toggle);
//        toggle.syncState();
//
//        setupNavMenu(navController);
    }

    //NEW
//    private void setupNavMenu(NavController navController){
//        NavigationView sideNav = findViewById(R.id.sideNav);
//        NavigationUI.setupWithNavController(sideNav, navController);
//    }
//
//
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_bottom, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        try {
//            Navigation.findNavController(this, R.id.NHFMain).navigate(item.getItemId());
//            return true;
//        }
//        catch (Exception ex)
//        {
//            return super.onOptionsItemSelected(item);
//        }
//    }
    //NEW END


    // 5. Handle the "Up" button press
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    };
}
