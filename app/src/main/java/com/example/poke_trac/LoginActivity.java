package com.example.poke_trac;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private TextView textMessage;
    private Button buttonLogin, buttonRegister, buttonReset;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {   // still signed in from last time
            goHome();
            return;
        }

        setContentView(R.layout.activity_login);
        editEmail = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        textMessage = findViewById(R.id.textMessage);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonReset = findViewById(R.id.buttonReset);

        buttonLogin.setOnClickListener(v -> doLogin());
        buttonRegister.setOnClickListener(v -> doRegister());
        buttonReset.setOnClickListener(v -> doReset());
    }

    private boolean validEmail(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textMessage.setText("Enter a valid email address");
            return false;
        }
        return true;
    }

    private void doLogin() {
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString();
        if (!validEmail(email)) return;
        if (pass.isEmpty()) {
            textMessage.setText("Enter your password");
            return;
        }

        setBusy(true);
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            setBusy(false);
            if (task.isSuccessful()) goHome();
            else textMessage.setText(friendly(task.getException()));
        });
    }

    private void doRegister() {
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString();
        if (!validEmail(email)) return;
        if (pass.length() < 6) {
            textMessage.setText("Password must be at least 6 characters");
            return;
        }

        setBusy(true);
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            setBusy(false);
            if (task.isSuccessful()) goHome();   // new accounts are signed in automatically
            else textMessage.setText(friendly(task.getException()));
        });
    }

    private void doReset() {
        String email = editEmail.getText().toString().trim();
        if (!validEmail(email)) return;

        setBusy(true);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            setBusy(false);
            if (task.isSuccessful()) {
                textMessage.setText("If that email has an account, a reset link has been sent");
            } else {
                textMessage.setText(friendly(task.getException()));
            }
        });
    }

    private String friendly(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) return "That email already has an account";
        if (e instanceof FirebaseAuthWeakPasswordException) return "Password is too weak";
        if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) return "Wrong email or password";
        if (e instanceof FirebaseNetworkException) return "No internet connection";
        return e == null ? "Something went wrong" : e.getMessage();
    }

    private void setBusy(boolean busy) {
        buttonLogin.setEnabled(!busy);
        buttonRegister.setEnabled(!busy);
        buttonReset.setEnabled(!busy);
        if (busy) textMessage.setText("");
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}