package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication.UI.CaptureActivity;
import com.example.myapplication.UI.NotesListActivity;
import com.example.myapplication.UI.Uploadactivity;
import com.example.myapplication.UI.Profileactivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ImageView ivProfile;
    private TextView tvPlaceholder;
    private ImageButton btnToggleTheme;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before super.onCreate
        sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        ivProfile = findViewById(R.id.ivMainProfilePic);
        tvPlaceholder = findViewById(R.id.tvProfilePlaceholder);
        btnToggleTheme = findViewById(R.id.btnToggleTheme);

        // If not logged in, redirect to log screen
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Profileactivity.class));
            finish();
            return;
        }

        updateProfileUI(currentUser);

        btnToggleTheme.setOnClickListener(v -> toggleTheme());

        // Scan with camera
        findViewById(R.id.btnScan).setOnClickListener(v ->
                startActivity(new Intent(this, CaptureActivity.class)));

        // Upload from gallery / file picker
        findViewById(R.id.btnUpload).setOnClickListener(v ->
                startActivity(new Intent(this, Uploadactivity.class)));

        // View saved notes
        findViewById(R.id.btnNotes).setOnClickListener(v ->
                startActivity(new Intent(this,NotesListActivity.class)));

        // Profile / logout
        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, Profileactivity.class)));
    }

    private void toggleTheme() {
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putBoolean("isDarkMode", false);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putBoolean("isDarkMode", true);
        }
        editor.apply();
        // The activity will recreate automatically to apply the new theme
    }

    private void updateProfileUI(FirebaseUser user) {
        if (user != null && user.getPhotoUrl() != null) {
            try {
                ivProfile.setImageURI(user.getPhotoUrl());
                ivProfile.setVisibility(View.VISIBLE);
                tvPlaceholder.setVisibility(View.GONE);
            } catch (SecurityException e) {
                ivProfile.setVisibility(View.GONE);
                tvPlaceholder.setVisibility(View.VISIBLE);
            }
        } else {
            ivProfile.setVisibility(View.GONE);
            tvPlaceholder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, Profileactivity.class));
            finish();
        } else {
            updateProfileUI(user);
        }
    }
}