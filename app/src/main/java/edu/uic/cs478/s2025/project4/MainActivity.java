package edu.uic.cs478.s2025.project4;

import android.os.Bundle;
import android.util.Log;
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
    private GolfAdapter golfAdapter;
    private int winner;
    private final Integer numHoles = 40;
    private int p1Location = -1;
    private int p2Location = - 1;
    private boolean gameOver = false;

    private int playerTurn = 1;

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
        golfAdapter = new GolfAdapter(this, holeList);
        gridview.setAdapter(golfAdapter);
        try {
            testImageChange();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        try {
//            testImageChange();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

    private void testImageChange() throws InterruptedException {

        Runnable aRunnable = new Runnable() {
            public void run() {
                if(!gameOver){
                    int nextShot = new Random().nextInt(numHoles);
                    take_shot(nextShot, playerTurn);
                    if(playerTurn == 1){
                        playerTurn = 2;
                    }
                    else{
                        playerTurn = 1;
                    }
                    Log.i("Thread t1", "Run iteration complete");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    run();
                }
                else{
                    Log.i("Thread t1","Game Over!");
                }
            }
        } ;
        Thread t1 = new Thread(aRunnable);
        t1.start();
    }

    public void take_shot(int loc, int player) {

        //  The display of the picture must be executed on the UI thread
        //  One can use runOnUiThread() to make this happen
        runOnUiThread(() -> {
            // No data race with the UI thread on mImageView, but careful
            // with mBitmap!
            int old_image = (int) golfAdapter.getItemId(loc);
            if(player == 1){
                golfAdapter.setImage(p1Location, R.drawable.golf_hole);
                if (old_image == R.drawable.red_hole){
                    golfAdapter.setImage(loc, R.drawable.blue_catastrophe);
                    Log.i("take_shot", "Blue catastrophe");
                    gameOver = true;
                }
                else if(old_image == R.drawable.winning_hole){
                    golfAdapter.setImage(loc, R.drawable.blue_win);
                    Log.i("take_shot", "Blue win");
                    gameOver = true;
                }
                else{
                    golfAdapter.setImage(loc, R.drawable.blue_hole);
                    p1Location = loc;
                }
            }
            else{
                golfAdapter.setImage(p2Location, R.drawable.golf_hole);
                if (old_image == R.drawable.blue_hole){
                    golfAdapter.setImage(loc, R.drawable.red_catastrophe);
                    Log.i("take_shot", "Red catastrophe");
                    gameOver = true;
                }
                else if(old_image == R.drawable.winning_hole){
                    golfAdapter.setImage(loc, R.drawable.red_win);
                    Log.i("take_shot", "Red win");
                    gameOver = true;
                }
                else{
                    golfAdapter.setImage(loc, R.drawable.red_hole);
                    p2Location = loc;
                }
            }
            golfAdapter.notifyDataSetChanged();
        });
    }

    private void initHoles(){
        winner = new Random().nextInt(numHoles);
        // Create holeList. Initialize holes to default image and winning hole to winning image
        holeList = new ArrayList<>(numHoles);
        boolean set_start = false;
        for(int i = 0; i < numHoles; i++){
            if(i != winner){
                holeList.add(i, R.drawable.golf_hole);
                if(!set_start){
                    p1Location = i;
                    p2Location = i;
                    set_start = true;
                }
            }
            else{
                holeList.add(i, R.drawable.winning_hole);
            }
        }
    }


}