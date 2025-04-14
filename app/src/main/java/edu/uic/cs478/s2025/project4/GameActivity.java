/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

 Activity class for running a game of MicroGolf. Handles the UI, processes player shots, and determines the ultimate
 outcome of a game. Runs a looper and handler on the UI thread that reacts to messages and sends responses to PlayerThreads
 throughout a game. After receiving a shot message from a PlayerThread, it updates the UI accordingly, sends the outcome
 of the shot to the PlayerThread, and determines if the game is over. Games can end after a Jackpot, when a player threads
 shoots into the winning whole, or a Catastrophe, which is when a player threads shoots into a hole occupied by it's opponent.
 A catastrophe is an automatic loss for the player who shot into the opponent's hole. Once a game is over, the player threads
 are sent a message telling them to stop their execution, and a dialog appears that shows the outcome of the game and prompts
 users to either start a new game or return to the MainActivity's main menu.
 */


package edu.uic.cs478.s2025.project4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.GridView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Random;


public class GameActivity extends AppCompatActivity implements  EndGameDialogFragment.EndGameDialogListener {
    private PlayerThread p1Thread, p2Thread;
    private Handler p1Handler, p2Handler;
    private GameViewModel threadViewModel;

    // note: initHoleList is just the initial list given to the GolfAdapter.
    //       It does not get updated as the game progresses.
    protected ArrayList<Integer> initHoleList;
    private GridView gridView;
    private GolfAdapter golfAdapter;
    private int winningHole;
    private int winningGroup;
    private int endGameStatus = -1;
    private int p1Location = -1;
    private int p2Location = - 1;
    private int p1LastOutcome = GameConstants.OUTCOME_BIG_MISS;
    private int p2LastOutcome = GameConstants.OUTCOME_BIG_MISS;
    private int lastPlayer = 0;
    protected boolean gameOver = false;
    FragmentManager mFragmentManager;

    // Keys for saving/restoring state
    private final String WINNING_HOLE = "winning hole";
    private final String P1_LOC = "p1 loc";
    private final String P2_LOC = "p2 loc";
    private final String LAST_PLAYER = "last player";
    private final String GAME_OVER = "game over";
    private final String HOLE_LIST = "hole list";

    /// Handler that acts as the game server. Receives shot messages from PlayerThreads and reacts to
    /// them by: updating the UI, sending outcomes back to PlayerThreads, and determining if the game
    /// is over.
    public  final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case GameConstants.P1_SHOT: {
                    Log.i("MainActivity", "Received shot from player 1");
                    processShot(GameConstants.PLAYER_1, msg.arg1);
                    if(!gameOver){
                        sendShotOutcome(GameConstants.PLAYER_1);
                    }
                    lastPlayer = GameConstants.PLAYER_1;
                    break;
                }
                case GameConstants.P2_SHOT:
                    Log.i("MainActivity", "Received shot from player 2");
                    processShot(GameConstants.PLAYER_2, msg.arg1);
                    if(!gameOver){
                        sendShotOutcome(GameConstants.PLAYER_2);
                    }
                    lastPlayer = GameConstants.PLAYER_2;
                    break;
                default:
                    Log.e("MainActivity", "Received unknown/invalid message: "
                            + "\"" + what + "\"");
                    break;
            }
            // Check if game is over and notify threads if so
            if(gameOver){
                endGame();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.game_layout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        gridView = findViewById(R.id.gridview);
        gridView.setSelector(android.R.color.transparent);
        threadViewModel = new ViewModelProvider(this).get(GameViewModel.class);
        mFragmentManager = getSupportFragmentManager();
        // If no savedInstanceState, start a new game
        if (savedInstanceState == null){
            startNewGame();
        }
        // If there is a savedInstanceState, restore game variables and resume game if needed
        else{
            // Only get variables needed to restore UI and check game state
            gameOver = savedInstanceState.getBoolean(GAME_OVER);
            initHoleList = savedInstanceState.getIntegerArrayList(HOLE_LIST);
            restoreGameUI();
            // If game is not over, restore other variables and resume
            if(!gameOver){
                winningHole = savedInstanceState.getInt(WINNING_HOLE);
                // calculate winning group from winning hole
                winningGroup = winningHole / GameConstants.GROUP_SIZE;
                p1Location = savedInstanceState.getInt(P1_LOC);
                p2Location = savedInstanceState.getInt(P2_LOC);
                lastPlayer = savedInstanceState.getInt(LAST_PLAYER);
                resumeGame();
            }
        }
    }


    @Override
    protected void onSaveInstanceState (@NonNull Bundle outState){
        Log.i("GameActivity", "In onSaveInstanceState!");
        super.onSaveInstanceState(outState);
        // Pause game if needed (game could be over, in which case we just want to restore UI)
        if(!gameOver){
            pauseGame();
        }
        outState.putInt(WINNING_HOLE, winningHole);
        outState.putInt(P1_LOC, p1Location);
        outState.putInt(P2_LOC, p2Location);
        outState.putInt(LAST_PLAYER, lastPlayer);
        outState.putBoolean(GAME_OVER, gameOver);
        outState.putIntegerArrayList(HOLE_LIST, golfAdapter.getResourceList());
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.i("GameActivity", "In onDestroy");
    }

    private void restoreGameUI(){
        golfAdapter = new GolfAdapter(this, initHoleList);
        gridView.setAdapter(golfAdapter);
    }

    private void pauseGame(){
        Message msg1 = p1Handler.obtainMessage(GameConstants.GAME_PAUSED);
        p1Handler.sendMessageAtFrontOfQueue(msg1);
        msg1.obj = mHandler;
        Message msg2 = p2Handler.obtainMessage(GameConstants.GAME_PAUSED);
        msg2.obj = mHandler;
        // Make sure game over message takes precedent
        p2Handler.sendMessageAtFrontOfQueue(msg2);
    }
    private void resumeGame(){
        // Restore threads/handlers from View Model
        p1Thread = threadViewModel.getP1Thread();
        p2Thread = threadViewModel.getP2Thread();
        p1Handler = p1Thread.getHandler();
        p2Handler = p2Thread.getHandler();

        // Tell threads it's time to resume
        p1Thread.setMainHandler(mHandler);
        p2Thread.setMainHandler(mHandler);
        sendResumeMessage();

        // Signal the thread whose turn it is to go
        if(lastPlayer == GameConstants.PLAYER_1){
            p1Handler.postDelayed(p2Thread.getTurnRunnable(), 3000);
        }
        else{
            p2Handler.postDelayed(p1Thread.getTurnRunnable(), 3000);
        }
    }

    private void sendResumeMessage(){
        Message msg1 = p1Handler.obtainMessage(GameConstants.GAME_RESUMED);
        p1Handler.sendMessageAtFrontOfQueue(msg1);
        msg1.obj = mHandler;
        Message msg2 = p2Handler.obtainMessage(GameConstants.GAME_RESUMED);
        msg2.obj = mHandler;
        p2Handler.sendMessageAtFrontOfQueue(msg2);
    }

    private void resetGameVariables(){
        // UI and hole variables are already reset in init_holes()
        p1Thread = null;
        p2Thread = null;
        p1Handler = null;
        p2Handler = null;
        p1Location = -1;
        p2Location = - 1;
        p1LastOutcome = GameConstants.OUTCOME_BIG_MISS;
        p2LastOutcome = GameConstants.OUTCOME_BIG_MISS;
        lastPlayer = 0;
        gameOver = false;
        endGameStatus = -1;
    }
    private void startNewGame(){
        Log.i("GameActivity", "in startNewGame()!");
        resetGameVariables();
        initHoles();
        golfAdapter = new GolfAdapter(this, initHoleList);
        gridView.setAdapter(golfAdapter);

        // Create and start player threads
        p1Thread = new PlayerThread(mHandler, GameConstants.PLAYER_1, GameConstants.STRAT_BASIC);
        p2Thread = new PlayerThread(mHandler, GameConstants.PLAYER_2, GameConstants.STRAT_AGGRESSIVE);
        p1Thread.start();
        p2Thread.start();

        // Assign each thread their opponent so they can communicate
        p1Thread.setOpponent(p2Thread);
        p2Thread.setOpponent(p1Thread);

        threadViewModel.setP1Thread(p1Thread);
        threadViewModel.setP2Thread(p2Thread);

        // Probably not the ideal way to do this, but it shouldn't take more than a few milliseconds
        long p1StartTime = System.currentTimeMillis();
        while (p1Handler == null) {
            p1Handler = p1Thread.getHandler();
        }
        long p1EndTime = System.currentTimeMillis();

        long p2StartTime = System.currentTimeMillis();
        while (p2Handler == null) {
            p2Handler = p2Thread.getHandler();
        }
        long p2EndTime = System.currentTimeMillis();

        Log.i("MainActivity", "Waiting for p1 Handler took " + (p1EndTime - p1StartTime) + " ms");
        Log.i("MainActivity", "Waiting for p2 Handler took " + (p2EndTime - p2StartTime) + " ms");

        // Tell p1Thread to start with a 2 second delay
        p1Handler.postDelayed(p1Thread.getTurnRunnable(), 2000);
    }

    private void endGame(){
        notifyGameOver();

        int dialogMsg;
        switch (endGameStatus){
            case GameConstants.THREAD_1_VICTORY:
                dialogMsg = R.string.thread1_win;
                break;
            case GameConstants.THREAD_1_CAT:
                dialogMsg = R.string.thread1_catastrophe;
                break;
            case GameConstants.THREAD_2_VICTORY:
                dialogMsg = R.string.thread2_win;
                break;
            case GameConstants.THREAD_2_CAT:
                dialogMsg = R.string.thread2_catastrophe;
                break;
            default:
                Log.e("GameActivity", "Game ended without endGameStatus set");
                dialogMsg = R.string.default_dialog_message;
        }

        EndGameDialogFragment endGameFragment = EndGameDialogFragment.newInstance(dialogMsg);
        endGameFragment.show(mFragmentManager, "END_GAME_DIALOG");
    }

    private void sendShotOutcome(int player){
        Handler ph;
        int lastOutcome;
        if(player == GameConstants.PLAYER_1){
            ph = p1Handler;
            lastOutcome = p1LastOutcome;
        }
        else{
            ph = p2Handler;
            lastOutcome = p2LastOutcome;
        }
        Log.i("MainActivity", "sendShotOutcome: Sending player " + player + " outcome " + lastOutcome);
        Message msg = ph.obtainMessage(GameConstants.OUTCOME);
        msg.arg1 = lastOutcome;
        ph.sendMessageAtFrontOfQueue(msg);
    }

    private void sendTakeTurnMsg(int nextPlayer){
        Handler nextPlayerHandler;
        if(nextPlayer == GameConstants.PLAYER_1){
            nextPlayerHandler = p1Handler;
        }
        else{
            nextPlayerHandler = p2Handler;
        }
        Message msg = nextPlayerHandler.obtainMessage(GameConstants.TAKE_TURN);
        msg.obj = mHandler;
        nextPlayerHandler.sendMessage(msg);
    }

    private int getShotOutcome(int shotLoc){
        int shotGroup = shotLoc / GameConstants.GROUP_SIZE;
        Log.i("GameActivity", "getShotOutcome: Shot fell into group " + shotGroup);
        // Check if shot fell within winning group
        if (shotGroup == winningGroup){
            return GameConstants.OUTCOME_NEAR_MISS;
        }
        // check if shot fell within adjacent group
        else if(shotGroup == winningGroup - 1 || shotGroup == winningGroup + 1){
            return GameConstants.OUTCOME_NEAR_GROUP;
        }
        // Any other outcome is big miss
        else{
            return GameConstants.OUTCOME_BIG_MISS;
        }
    }

    private void processShot(int player, int shotLoc){
        // Move player 1 shot location in UI, set outcome, and check for win/catastrophe
        if(player == GameConstants.PLAYER_1){
            p1LastOutcome = getShotOutcome(shotLoc);
            if(p1Location != -1) {
                golfAdapter.setImage(p1Location, R.drawable.golf_hole);
            }
            if (shotLoc == p2Location){
                golfAdapter.setImage(shotLoc, R.drawable.blue_catastrophe);
                Log.i("processShot", "Player 1 catastrophe");
                gameOver = true;
                endGameStatus = GameConstants.THREAD_1_CAT;
            }
            else if(shotLoc == winningHole){
                golfAdapter.setImage(shotLoc, R.drawable.blue_win);
                Log.i("processShot", "Player 1 win");
                gameOver = true;
                endGameStatus = GameConstants.THREAD_1_VICTORY;
            }
            else{
                golfAdapter.setImage(shotLoc, R.drawable.blue_hole);
                Log.i("processShot", "Player 1 shot " + shotLoc);
                p1Location = shotLoc;
            }
        }
        // Move player 2 shot location in UI, set outcome, and check for win/catastrophe
        else if (player == GameConstants.PLAYER_2){
            p2LastOutcome = getShotOutcome(shotLoc);
            if(p2Location != -1){
                golfAdapter.setImage(p2Location, R.drawable.golf_hole);
            }
            if (shotLoc == p1Location){
                golfAdapter.setImage(shotLoc, R.drawable.red_catastrophe);
                Log.i("processShot", "Player 2 catastrophe");
                gameOver = true;
                endGameStatus = GameConstants.THREAD_2_CAT;
            }
            else if(shotLoc == winningHole){
                golfAdapter.setImage(shotLoc, R.drawable.red_win);
                Log.i("processShot", "Player 2 win");
                gameOver = true;
                endGameStatus = GameConstants.THREAD_2_VICTORY;
            }
            else{
                golfAdapter.setImage(shotLoc, R.drawable.red_hole);
                Log.i("processShot", "Player 2 shot " + shotLoc);
                p2Location = shotLoc;
            }
        }
        golfAdapter.notifyDataSetChanged();
    }

    private void notifyGameOver(){
        Message msg1 = p1Handler.obtainMessage(GameConstants.GAME_OVER);
        p1Handler.sendMessageAtFrontOfQueue(msg1);
        msg1.obj = mHandler;
        Message msg2 = p2Handler.obtainMessage(GameConstants.GAME_OVER);
        msg2.obj = mHandler;
        // Make sure game over message takes precedent
        p2Handler.sendMessageAtFrontOfQueue(msg2);
    }

    private void initHoles(){
        // Calculate the winning hole and the winning hole group
        winningHole = new Random().nextInt(GameConstants.NUM_HOLES);
        winningGroup = winningHole / GameConstants.GROUP_SIZE;
        // Create holeList. Initialize holes to default image and winning hole to winning image
        initHoleList = new ArrayList<>(GameConstants.NUM_HOLES);
        for(int i = 0; i < GameConstants.NUM_HOLES; i++){
            if(i != winningHole){
                initHoleList.add(i, R.drawable.golf_hole);
            }
            else{
                initHoleList.add(i, R.drawable.winning_hole);
            }
        }
    }

    @Override
    public void onStartNewGame() {
        startNewGame();
    }

    @Override
    public void onQuit() {
        finish();
    }
}