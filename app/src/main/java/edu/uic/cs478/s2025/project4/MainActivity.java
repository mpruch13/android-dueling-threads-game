package edu.uic.cs478.s2025.project4;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.GridView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private int gameState = -1;
    private static final int RESUME = 1;
    private static final int GAME_OVER = 0;
    private static final int PLAYER_1 = 1;
    private static final int PLAYER_2 = 2;
    private static final int P1_SHOT = 10;
    private static final int P2_SHOT = 20;

    // This handler, running on the UI thread, will be our server
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            int what = msg.what ;
            switch (what) {
                case P1_SHOT:
                    // TODO: Handle Player 1 shot
                    break;
                case P2_SHOT:
                    // TODO: Handle Player 2 shot
                    break;
                default:
                    // Do nothing
                    break;
            }

        }
    }	; // Handler is associated with UI Thread
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

        // This handler, running on the UI thread, will be our server
//        private final Handler mHandler = new Handler(Looper.getMainLooper()) {
//            public void handleMessage(Message msg) {
//                int what = msg.what ;
//                switch (what) {
//                    case MainActivity.PLAYER_1:
//                        // Take Turn
//                        break;
//                    case GAME_OVER:
//                        // TODO: stop thread
//                        break;
//                    default:
//                        // Do nothing
//                        break;
//                }
//
//            }
//        }	; // Handler is associated with UI Thread

//        public void run() {
//
//            // Get a message instance with target set to UI thread's message queue
//            Message msg = mHandler.obtainMessage(HandleMessageActivity.SET_PROGRESS_VISIBLE) ;
//            mHandler.sendMessage(msg) ;
//
//            // again, arg1 shows current progress
//            msg = mHandler.obtainMessage(POST_PROGRESS) ;
//            msg.arg1 = 0 ;
//            mHandler.sendMessage(msg) ;
//
//            try { Thread.sleep(2000); }
//            catch (InterruptedException e) { System.out.println("Thread interrupted!") ; }
//
//            // and again, arg1 shows current progress
//            msg = mHandler.obtainMessage(POST_PROGRESS) ;
//            msg.arg1 = 25 ;
//            mHandler.sendMessage(msg) ;
//
//            try { Thread.sleep(2000); }
//            catch (InterruptedException e) { System.out.println("Thread interrupted!") ; }
//
//            // and again, arg1 shows current progress
//            msg = mHandler.obtainMessage(POST_PROGRESS) ;
//            msg.arg1 = 50 ;
//            // UB 3/17/2021 -- Try sendToTarget() this time
//            // mHandler.sendMessage(msg) ;
//            msg.sendToTarget() ;
//
//            try { Thread.sleep(2000); }
//            catch (InterruptedException e) { System.out.println("Thread interrupted!") ; }
//
//            // and again, arg1 shows current progress
//            msg = mHandler.obtainMessage(POST_PROGRESS) ;
//            msg.arg1 = 75 ;
//            // UB 3/17/2021 -- Try sendToTarget() again
//
//            // mHandler.sendMessage(msg) ;
//            msg.sendToTarget() ;
//
//            // download bitmap
//            Bitmap b = null ;
//            try {
//                String urlString = "https://pictures.topspeed.com/IMG/crop/200512/2003-ferrari-enzo-40_600x0w.jpg";
//                URL aUrl = new URL(urlString) ;   // This could raise malformed URL exception
//                b = BitmapFactory.decodeStream((InputStream) aUrl.getContent()) ;
//            }
//            catch (Exception e) {System.out.println("Could not read image from web!") ; }
//
//            // Get message to UI's queue, send bitmap along with message
//            msg = mHandler.obtainMessage(UPDATE_IMAGE_VIEW) ;
//            msg.obj = b;
//            mHandler.sendMessage(msg) ;
//
//            // This message will be queued after previous message
//            msg = mHandler.obtainMessage(SET_PROGRESS_INVISIBLE) ;
//            mHandler.sendMessage(msg) ;
//        }


}