package com.nikol.ciphernote;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nikol.ciphernote.Database.RoomDB;
import com.nikol.ciphernote.Model.Profiles;
import com.nikol.ciphernote.Cryptography.PasswordHasher;
import com.nikol.ciphernote.Cryptography.SessionManager;
import com.nikol.ciphernote.Network.Res.AuthResponse;
import com.nikol.ciphernote.Network.Req.LoginRequest;
import com.nikol.ciphernote.Network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText editText_username;
    private EditText editText_password;
    private RoomDB database;
    private Profiles profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editText_username = findViewById(R.id.editText_username);
        editText_password = findViewById(R.id.editText_password);
        Button loginButton = findViewById(R.id.loginButton);
        Button createAccountButton = findViewById(R.id.createAccountButton);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        database = RoomDB.getInstance(this);

        loginButton.setOnClickListener(v -> {
            String username = editText_username.getText().toString().trim();
            String password = editText_password.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            String passwordHash = PasswordHasher.generateDeterministicServerHash(password, username);
            LoginRequest loginRequest = new LoginRequest(username, passwordHash);

            RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        AuthResponse auth = response.body();
                        RetrofitClient.setAuthToken(auth.token);

                        new Thread(() -> {
                            profile = database.mainDAO().getProfile(username);
                            if (profile == null) {
                                profile = new Profiles();
                                profile.setUsername(username);
                            }
                            profile.token = auth.token;
                            profile.keySalt = auth.keySalt;
                            profile.password_hash = passwordHash;
                            database.mainDAO().insert_profile(profile);

                            SessionManager.getInstance().deriveMasterKeyWithSalt(password, auth.keySalt);

                            runOnUiThread(() -> {
                                Log.d("login", "Credentials accepted by server!");
                                Intent main = new Intent(LoginActivity.this, MainActivity.class);
                                main.putExtra("user", profile);
                                startActivity(main);
                                finish();
                            });
                        }).start();
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Login failed!", Toast.LENGTH_SHORT).show();
                            loginButton.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        loginButton.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    });
                }
            });
        });

        createAccountButton.setOnClickListener(v -> {
            Intent caa = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(caa);
        });
    }
}
