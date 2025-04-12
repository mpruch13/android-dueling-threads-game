package edu.uic.cs478.s2025.project4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
    /// TODO: MAJOR ERROR - Don't initialize player positions to a valid location, this causes a catastrophe if a player's first shot happens to land on the other player's initial spot
    private int gameState = -1;
    private static final int RESUME = 1;
    protected static final int GAME_OVER = 0;
    protected static final int PLAYER_1 = 1;
    protected static final int PLAYER_2 = 2;
    protected static final int P1_SHOT = 10;
    protected static final int P2_SHOT = 20;
    protected static final int TAKE_TURN = 50;
    protected static final int OUTCOME = 60;
    protected static final int BIG_MISS = 11;
    protected static final int NEAR_GROUP = 12;
    protected static final int NEAR_MISS = 12;
    protected static final int THREAD_READY = 100;
    private static PlayerThread p1Thread, p2Thread;
    private static Handler p1Handler, p2Handler;
    private boolean p1Ready = false;
    private  boolean p2Ready = true;
    protected ArrayList<Integer> holeList;

    private GridView gridView;
    private GolfAdapter golfAdapter;
    private int winner;
    protected static final Integer numHoles = 40;
    private int p1Location = -1;
    private int p1LastOutcome = BIG_MISS;
    private int p2LastOutcome = BIG_MISS;
    private int p2Location = - 1;
    private boolean gameOver = false;

    // This handler, running on the UI thread, will be our server
    public  final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            int what = msg.what;
//            Handler sender = (Handler) msg.obj;
//            if(sender == p1Handler){
//                Log.i("MainActivity", "Received shot from player 1");
//                processShot(PLAYER_1, msg.arg1);
//                if(!gameOver){
//                    sendTakeTurnMsg(PLAYER_2);
//                }
//            }
//            else{
//                Log.i("MainActivity", "Received shot from player 2");
//                processShot(PLAYER_2, msg.arg1);
//                if(!gameOver){
//                    sendTakeTurnMsg(PLAYER_1);
//                }
//            }
//            if(gameOver){
//                notifyGameOver();
//            }
            switch (what) {
                case P1_SHOT: {
                    Log.i("MainActivity", "Received shot from player 1");
                    processShot(PLAYER_1, msg.arg1);
                    if(!gameOver){
                        sendShotOutcome(PLAYER_1);
                        sendTakeTurnMsg(PLAYER_2);
                    }
                    break;
                }
                case P2_SHOT:
                    Log.i("MainActivity", "Received shot from player 2");
                    processShot(PLAYER_2, msg.arg1);
                    if(!gameOver){
                        sendShotOutcome(PLAYER_2);
                        sendTakeTurnMsg(PLAYER_1);
                    }
                    break;
                default:
                    // Do nothing
                    break;
            }
            // Check if game is over and notify threads if so
            if(gameOver){
                notifyGameOver();
            }
        }
    };

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
        gridView = findViewById(R.id.gridview);
        startNewGame();
    }

    private void startNewGame(){
        initHoles();
        golfAdapter = new GolfAdapter(this, holeList);
        gridView.setAdapter(golfAdapter);

        // Start player threads
        p1Thread = new PlayerThread(mHandler, PLAYER_1);
        p2Thread = new PlayerThread(mHandler, PLAYER_2);

        p1Thread.start();
        p2Thread.start();

        // Send first message to player1
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        p1Handler = p1Thread.getHandler();
        p2Handler = p2Thread.getHandler();
        Message msg = p1Handler.obtainMessage(TAKE_TURN);
        msg.obj = mHandler;
        p1Handler.sendMessage(msg);
    }

    private void sendShotOutcome(int player){
        Handler playerHandler;
        int lastOutcome;
        if(player == PLAYER_1){
            playerHandler = p1Handler;
            lastOutcome = p1LastOutcome;
        }
        else{
            playerHandler = p2Handler;
            lastOutcome = p2LastOutcome;
        }
        Message msg = playerHandler.obtainMessage(OUTCOME);
        msg.arg1 = lastOutcome;
        playerHandler.sendMessage(msg);
    }

    private void sendTakeTurnMsg(int nextPlayer){
        Handler nextPlayerHandler;
        if(nextPlayer == PLAYER_1){
            nextPlayerHandler = p1Handler;
        }
        else{
            nextPlayerHandler = p2Handler;
        }
        Message msg = nextPlayerHandler.obtainMessage(TAKE_TURN);
        msg.obj = mHandler;
        nextPlayerHandler.sendMessage(msg);
    }

    private void processShot(int player, int shotLoc){
        if(player == PLAYER_1){
            golfAdapter.setImage(p1Location, R.drawable.golf_hole);
            if (shotLoc == p2Location){
                golfAdapter.setImage(shotLoc, R.drawable.blue_catastrophe);
                Log.i("processShot", "Blue catastrophe");
                gameOver = true;
            }
            else if(shotLoc == winner){
                golfAdapter.setImage(shotLoc, R.drawable.blue_win);
                Log.i("processShot", "Blue win");
                gameOver = true;
            }
            else{
                golfAdapter.setImage(shotLoc, R.drawable.blue_hole);
                Log.i("processShot", "Blue shot " + shotLoc);
                p1Location = shotLoc;
            }
        }
        else if (player == PLAYER_2){
            golfAdapter.setImage(p2Location, R.drawable.golf_hole);
            if (shotLoc == p1Location){
                golfAdapter.setImage(shotLoc, R.drawable.red_catastrophe);
                Log.i("processShot", "Red catastrophe");
                gameOver = true;
            }
            else if(shotLoc == winner){
                golfAdapter.setImage(shotLoc, R.drawable.red_win);
                Log.i("processShot", "Red win");
                gameOver = true;
            }
            else{
                golfAdapter.setImage(shotLoc, R.drawable.red_hole);
                Log.i("processShot", "Red shot " + shotLoc);
                p2Location = shotLoc;
            }
        }
        golfAdapter.notifyDataSetChanged();
    }

    private void notifyGameOver(){
        Message msg1 = p1Handler.obtainMessage(GAME_OVER);
        p1Handler.sendMessage(msg1);
        msg1.obj = mHandler;
        Message msg2 = p2Handler.obtainMessage(GAME_OVER);
        msg2.obj = mHandler;
        p2Handler.sendMessage(msg2);
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

//    private void testImageChange() throws InterruptedException {
//
//        Runnable aRunnable = new Runnable() {
//            public void run() {
//                if(!gameOver){
//                    int nextShot = new Random().nextInt(numHoles);
//                    take_shot(nextShot, playerTurn);
//                    if(playerTurn == 1){
//                        playerTurn = 2;
//                    }
//                    else{
//                        playerTurn = 1;
//                    }
//                    Log.i("Thread t1", "Run iteration complete");
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                    run();
//                }
//                else{
//                    Log.i("Thread t1","Game Over!");
//                }
//            }
//        } ;
//        Thread t1 = new Thread(aRunnable);
//        t1.start();
//    }

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