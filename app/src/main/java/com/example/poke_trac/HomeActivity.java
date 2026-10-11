package com.example.poke_trac;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView textWelcome, textCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!UserManager.isLoggedIn(this)) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_home);
        textWelcome = findViewById(R.id.textWelcome);
        textCount = findViewById(R.id.textCount);

        textWelcome.setText("Poke-Trac");
        Collection_Manager.pull(this, this::updateCount);
        findViewById(R.id.buttonAdd).setOnClickListener(v ->
                startActivity(new Intent(this, display_cards.class)));

        findViewById(R.id.buttonView).setOnClickListener(v ->
                startActivity(new Intent(this, CollectionActivity.class)));

        findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            UserManager.logout(this);
            goToLogin();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCount();
    }

    private void updateCount() {
        if (textCount == null) return;
        List<Card> cards = Collection_Manager.getAll(this);
        int total = 0;
        for (Card c : cards) total += c.quantity;
        textCount.setText(total + " cards in your collection");
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}