package com.shadowfox.smartclassroom.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.shadowfox.smartclassroom.databinding.ActivityLoginBinding;
import com.shadowfox.smartclassroom.repository.AuthRepository;
import com.shadowfox.smartclassroom.repository.Callback;
import com.shadowfox.smartclassroom.ui.home.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private final AuthRepository auth = AuthRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.loginButton.setOnClickListener(v -> attemptLogin());
        b.registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        // Already signed in from a previous session: skip straight to the app.
        if (auth.isLoggedIn()) {
            setLoading(true);
            auth.loadCurrentProfile(Callback.of(u -> openMain(), msg -> {
                auth.logout();
                setLoading(false);
            }));
        }
    }

    private void attemptLogin() {
        String email = String.valueOf(b.emailInput.getText()).trim();
        String password = String.valueOf(b.passwordInput.getText());
        b.emailLayout.setError(null);
        b.passwordLayout.setError(null);

        if (email.isEmpty()) { b.emailLayout.setError("Enter your email"); return; }
        if (password.isEmpty()) { b.passwordLayout.setError("Enter your password"); return; }

        setLoading(true);
        auth.login(email, password, Callback.of(u -> openMain(), msg -> {
            setLoading(false);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }));
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        b.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        b.loginButton.setEnabled(!loading);
    }
}
