/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

 MainActivity for the Microgolf game. Displays a main menu with two buttons that allow players to either start a game or
 exit the application.

 Game Rules: There are 50 holes, each divided into groups of 5. Each row of 5 holes in the GameActivity display represents 1
 hole group. Holes are indexed left to right, starting from 0, and groups are indexed top to botoom, starting from 0. For example,
 group 0 is the very top row on the display, and it contains holes [0,1,2,3,4], group 1 is the next group down and contains
 holes [5,6,7,8,0], and so on. The game is played automatically by two PlayerThreads. Player 1 is represented by a blue ball, and
 player 2 is represented by a red ball. The first player to find the winning hole (represented by a white cup) wins the game. A game
 can also end in a catastrophe, which is when 1 PlayerThread shoots their ball into a hole already occupied the opposing player thread.
 Upon a catastrophe, the shooting player automatically loses and the player who occupied the hole first wins. Each player thread uses
 a different strategy. Strategy details can be found in the PlayerThread class.
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
     * Shows layout, gets the buttons, then sets a listener for each one.
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

        // Start the game
        startGameButton.setOnClickListener(v -> {
            // Create and send broadcast intent
            Intent gameIntent = new Intent(this, GameActivity.class);
            startActivity(gameIntent);
        });

        // Exit the application
        quitGameButton.setOnClickListener(v -> {
            Log.i("MainActivity", "User selected quit. Exiting application");
            finish();
        });
    }
}