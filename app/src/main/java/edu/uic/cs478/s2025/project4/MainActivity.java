package edu.uic.cs478.s2025.project4;

import android.os.Bundle;
import android.widget.GridView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Random;


public class MainActivity extends AppCompatActivity {


    protected ArrayList<Integer> holeList;
    private Integer winner;
    private final Integer numHoles = 50;
    private Integer p1Location = -1;
    private Integer p2Location = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initHoles();
        GridView gridview = findViewById(R.id.gridview);
        gridview.setAdapter(new GolfAdapter(this, holeList));
    }

    private void initHoles(){
        winner = new Random().nextInt(50);
        // Create holeList. Initialize holes to default image and winning hole to winning image
        holeList = new ArrayList<>(numHoles);
        for(int i = 0; i < numHoles; i++){
            if(i != winner){
                holeList.add(i, R.drawable.golf_hole);
            }
            else{
                holeList.add(i, R.drawable.winning_hole);
            }
        }
    }
}