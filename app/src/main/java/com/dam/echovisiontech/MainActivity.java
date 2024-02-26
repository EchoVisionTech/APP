package com.dam.echovisiontech;

import android.app.Dialog;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.dam.echovisiontech.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    DialogRegister dialog = new DialogRegister();
    boolean phoneValidated = false;
    boolean tokenValidated = false;
    boolean showDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_ullada, R.id.navigation_historial, R.id.navigation_compte)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        if (phoneValidated && tokenValidated) {
            navController.navigate(R.id.navigation_ullada);
        } else {
            navController.navigate(R.id.navigation_compte);
            showDialog = true;
            //DialogRegister.showNameDialog(this);
            DialogRegister.showAlertDialog(this, "EchoVisionTech", "Valida el teu telèfon per poder accedir a la resta de funcionalitats");
        }

    }
    @Override
    protected void onStart() {
        super.onStart();
    }
}