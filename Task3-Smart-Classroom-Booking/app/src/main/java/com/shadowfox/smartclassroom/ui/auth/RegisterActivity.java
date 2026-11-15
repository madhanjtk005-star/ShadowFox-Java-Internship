package com.shadowfox.smartclassroom.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.shadowfox.smartclassroom.R;
import com.shadowfox.smartclassroom.databinding.ActivityRegisterBinding;
import com.shadowfox.smartclassroom.model.User;
import com.shadowfox.smartclassroom.repository.AuthRepository;
import com.shadowfox.smartclassroom.repository.Callback;
import com.shadowfox.smartclassroom.ui.home.MainActivity;

public class RegisterActivity extends AppCompatActivity {

    /** Demo-only gate for staff sign-up. In production assign roles with a backend / custom claims. */
    private static final String STAFF_CODE = "STAFF2026";

    private ActivityRegisterBinding b;
    private final AuthRepository auth = AuthRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.roleToggle.addOnButtonCheckedListener((group, id, checked) -> {
            if (checked) {
                b.staffCodeLayout.setVisibility(id == R.id.adminButton ? View.VISIBLE : View.GONE);
            }
        });
        b.registerButton.setOnClickListener(v -> attemptRegister());
        b.loginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = String.valueOf(b.nameInput.getText()).trim();
        String email = String.valueOf(b.emailInput.getText()).trim();
        String password = String.valueOf(b.passwordInput.getText());
        String confirm = String.valueOf(b.confirmInput.getText());
        boolean asAdmin = b.roleToggle.getCheckedButtonId() == R.id.adminButton;

        b.nameLayout.setError(null);
        b.emailLayout.setError(null);
        b.passwordLayout.setError(null);
        b.confirmLayout.setError(null);
        b.staffCodeLayout.setError(null);

        if (name.isEmpty()) { b.nameLayout.setError("Enter your name"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.emailLayout.setError("Enter a valid email"); return; }
        if (password.length() < 6) { b.passwordLayout.setError("At least 6 characters"); return; }
        if (!password.equals(confirm)) { b.confirmLayout.setError("Passwords do not match"); return; }
        if (asAdmin && !STAFF_CODE.equals(String.valueOf(b.staffCodeInput.getText()).trim())) {
            b.staffCodeLayout.setError("Invalid staff access code");
            return;
        }

        setLoading(true);
        String role = asAdmin ? User.ADMIN : User.STUDENT;
        auth.register(name, email, password, role, Callback.of(u -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }, msg -> {
            setLoading(false);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }));
    }

    private void setLoading(boolean loading) {
        b.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        b.registerButton.setEnabled(!loading);
    }
}
