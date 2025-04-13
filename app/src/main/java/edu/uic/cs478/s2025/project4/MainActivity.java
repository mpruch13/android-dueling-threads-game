/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

 Main Activity for the Project 3 A1 app. This is a simple single-activity app that consists of two buttons
 that send implicit broadcast intents for app A2. The attractions button sends a broadcast telling A2 to open
 the Attractions POI Activity, and the restaurants button sends a broadcast telling it to open the Restaurants
 Activity. Each button also creates a toast message confirming that the broadcast intent was sent.

 */

package edu.uic.cs478.s2025.project4;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    /**
     * Shows layout, gets the buttons, then sets a listener for each one that
     * creates and sends a broadcast for app A2.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button startGameButton = findViewById(R.id.button);
        Button quitGameButton = findViewById(R.id.button2);

        // Create listener for the attractions button
        startGameButton.setOnClickListener(v -> {
            // Create and send broadcast intent
            Intent gameIntent = new Intent(this, GameActivity.class);
            startActivity(gameIntent);
        });

        // Create listener for restaurants button
        quitGameButton.setOnClickListener(v -> {
            Log.i("MainActivity", "User selected quit. Exiting application");
            finish();
        });
    }
}