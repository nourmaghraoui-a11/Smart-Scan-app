package com.example.myapplication.UI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class Profileactivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    // Login views
    private LinearLayout layoutLogin;
    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoRegister;

    // Register views
    private LinearLayout layoutRegister;
    private EditText etRegEmail, etRegPassword, etRegConfirm;
    private Button btnRegister, btnGoLogin;

    // Profile views (logged in)
    private LinearLayout layoutProfile;
    private TextView tvUserEmail;
    private Button btnLogout;
    private ImageView ivProfilePic;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        // Persist permission for the URI
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    ivProfilePic.setImageURI(uri);
                    updateProfilePicture(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();

        // Login
        layoutLogin    = findViewById(R.id.layoutLogin);
        etEmail        = findViewById(R.id.etEmail);
        etPassword     = findViewById(R.id.etPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        btnGoRegister  = findViewById(R.id.btnGoRegister);

        // Register
        layoutRegister = findViewById(R.id.layoutRegister);
        etRegEmail     = findViewById(R.id.etRegEmail);
        etRegPassword  = findViewById(R.id.etRegPassword);
        etRegConfirm   = findViewById(R.id.etRegConfirm);
        btnRegister    = findViewById(R.id.btnRegister);
        btnGoLogin     = findViewById(R.id.btnGoLogin);

        // Profile
        layoutProfile  = findViewById(R.id.layoutProfile);
        tvUserEmail    = findViewById(R.id.tvUserEmail);
        btnLogout      = findViewById(R.id.btnLogout);
        ivProfilePic   = findViewById(R.id.ivProfilePic);

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoRegister.setOnClickListener(v -> showRegister());
        btnRegister.setOnClickListener(v -> registerUser());
        btnGoLogin.setOnClickListener(v -> showLogin());
        btnLogout.setOnClickListener(v -> logoutUser());
        
        ivProfilePic.setOnClickListener(v -> pickImage.launch("image/*"));

        refreshUI();
    }

    private void refreshUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            layoutLogin.setVisibility(View.GONE);
            layoutRegister.setVisibility(View.GONE);
            layoutProfile.setVisibility(View.VISIBLE);
            tvUserEmail.setText(user.getEmail());
            
            Uri photoUrl = user.getPhotoUrl();
            if (photoUrl != null) {
                // Check if we still have permission to access this URI
                try {
                    ivProfilePic.setImageURI(photoUrl);
                } catch (SecurityException e) {
                    // If permission is lost (e.g. after app restart with pick_get_content URIs)
                    ivProfilePic.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                ivProfilePic.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            showLogin();
        }
    }

    private void updateProfilePicture(Uri uri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(uri)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showLogin() {
        layoutLogin.setVisibility(View.VISIBLE);
        layoutRegister.setVisibility(View.GONE);
        layoutProfile.setVisibility(View.GONE);
    }

    private void showRegister() {
        layoutLogin.setVisibility(View.GONE);
        layoutRegister.setVisibility(View.VISIBLE);
        layoutProfile.setVisibility(View.GONE);
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void registerUser() {
        String email   = etRegEmail.getText().toString().trim();
        String pass    = etRegPassword.getText().toString().trim();
        String confirm = etRegConfirm.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Registration failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
        showLogin();
    }
}
