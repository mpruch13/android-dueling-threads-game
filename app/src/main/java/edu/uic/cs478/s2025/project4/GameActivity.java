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
    protected boolean gamePaused = false;
    FragmentManager mFragmentManager;

    // Keys for saving/restoring state
    private final String WINNING_HOLE = "winning hole";
    private final String P1_LOC = "p1 loc";
    private final String P2_LOC = "p2 loc";
    private final String LAST_PLAYER = "last player";
    private final String GAME_OVER = "game over";
    private final String HOLE_LIST = "hole list";

    private final String P1_SHOTS = "p1Shots";
    private final String P2_SHOTS = "p2Shots";
    private ArrayList<Integer> p1Shots;
    private ArrayList<Integer> p2Shots;

    /**
     * Handler that acts as the game server. Receives and responds to messages from PlayerThreads.
     * Upon receiving a shot message, it updates the UI, sends an outcome response back to the player,
     * and determines whether the game is over. If the game is over, it sends a game over message to
     * player threads and creates a dialog informing the user of the game outcome and giving them
     * the option to quit or start a new game.
     */
    public  final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case GameConstants.P1_SHOT: {
                    Log.i("GameActivity", "Received shot from player 1");
                    processShot(GameConstants.PLAYER_1, msg.arg1);
                    if(!gameOver){
                        sendShotOutcome(GameConstants.PLAYER_1);
                    }
                    lastPlayer = GameConstants.PLAYER_1;
                    break;
                }
                case GameConstants.P2_SHOT:
                    Log.i("GameActivity", "Received shot from player 2");
                    processShot(GameConstants.PLAYER_2, msg.arg1);
                    if(!gameOver){
                        sendShotOutcome(GameConstants.PLAYER_2);
                    }
                    lastPlayer = GameConstants.PLAYER_2;
                    break;
                default:
                    Log.e("GameActivity", "Received unknown/invalid message: "
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
        // Initialize gridview and setSelector so no animation plays if the user clicks on a grid cell
        gridView = findViewById(R.id.gridview);
        gridView.setSelector(android.R.color.transparent);

        // Get ViewModel (used for storing threads on config change)
        // and fragment manager (used for displaying the end game dialog option)
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
                p1Shots = savedInstanceState.getIntegerArrayList(P1_SHOTS);
                p2Shots = savedInstanceState.getIntegerArrayList(P2_SHOTS);
                resumeGameFromConfigChange();
            }
        }
    }

    /**
     * Saves necessary member variables so the game can be restored
     * after a config change.
     */
    @Override
    protected void onSaveInstanceState (@NonNull Bundle outState){
        Log.i("GameActivity", "In onSaveInstanceState!");
        super.onSaveInstanceState(outState);
        outState.putInt(WINNING_HOLE, winningHole);
        outState.putInt(P1_LOC, p1Location);
        outState.putInt(P2_LOC, p2Location);
        outState.putInt(LAST_PLAYER, lastPlayer);
        outState.putBoolean(GAME_OVER, gameOver);
        outState.putIntegerArrayList(HOLE_LIST, golfAdapter.getResourceList());
        outState.putIntegerArrayList(P1_SHOTS, p1Shots);
        outState.putIntegerArrayList(P2_SHOTS, p2Shots);
    }

    /// Overrode onPause to pause the game if it is still running whenever onPause is called
    @Override
    protected void onPause(){
        super.onPause();
        // Only pause if game isn't over and threads are alive
        if(!gameOver && (p1Thread != null && p1Thread.isAlive() && p2Thread.isAlive())){
                pauseGame();
        }
    }

    /// Overrode onPause to resume the game if onResume is called while the game is paused
    @Override
    protected void onResume(){
        super.onResume();
        if(gamePaused){
            softResumeGame();
        }
    }

    /**
     * Restores the display to the state it was in
     * before the last config change.
     */
    private void restoreGameUI(){
        golfAdapter = new GolfAdapter(this, initHoleList);
        gridView.setAdapter(golfAdapter);
    }

    /**
     * Sends messages to the PlayerThreads telling them to pause the
     * execution of the current game without stopping their execution.
     */
    private void pauseGame(){
        Message msg1 = p1Handler.obtainMessage(GameConstants.GAME_PAUSED);
        p1Handler.sendMessageAtFrontOfQueue(msg1);
        Message msg2 = p2Handler.obtainMessage(GameConstants.GAME_PAUSED);
        // Make sure game over message takes precedent
        p2Handler.sendMessageAtFrontOfQueue(msg2);
        gamePaused = true;
    }

    /**
     * For Resuming the game outside of config changes (e.g., if it is minimized or something pops up)
     * Just sends resume message and
     */
    private void softResumeGame(){
        // Make sure game is not over and threads are alive
        if(!gameOver && (p1Thread.isAlive() && p2Thread.isAlive())){
            sendResumeMessage();
            // Signal the thread whose turn it is to go
            if(lastPlayer == GameConstants.PLAYER_1){
                p1Handler.postDelayed(p2Thread.getTurnRunnable(), 2000);
            }
            else{
                p2Handler.postDelayed(p1Thread.getTurnRunnable(), 2000);
            }
        }
    }

    /**
     * Restores the PlayerThreads from the view model, sends each a
     * resume message, then posts the turn runnable to the thread
     * whose turn it was when the game was paused.
     */
    private void resumeGameFromConfigChange(){
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
            p2Handler.postDelayed(p2Thread.getTurnRunnable(), 2000);
        }
        else{
            p1Handler.postDelayed(p1Thread.getTurnRunnable(), 2000);
        }
    }

    /**
     * Sends a message to each thread letting them know it
     * is time to resume the game.
     */
    private void sendResumeMessage(){
        Message msg1 = p1Handler.obtainMessage(GameConstants.GAME_RESUMED);
        p1Handler.sendMessageAtFrontOfQueue(msg1);
        Message msg2 = p2Handler.obtainMessage(GameConstants.GAME_RESUMED);
        p2Handler.sendMessageAtFrontOfQueue(msg2);
    }

    /**
     * Resets game variables ot their original values for a new game.
     */
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
        gamePaused = false;
        endGameStatus = -1;
        p1Shots = new ArrayList<>();
        p2Shots = new ArrayList<>();
    }

    /**
     * Starts a new game. Resets all game variables, initializes the
     * hole list and display, then initializes both PlayerThreads,
     * adds them to the ViewModel, gets their handlers, and sends
     * Player 1 it's turn runnable to get the game going.
     */
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

        // See how long it took to get the handlers
        Log.i("GameActivity", "Waiting for p1 Handler took " + (p1EndTime - p1StartTime) + " ms");
        Log.i("GameActivity", "Waiting for p2 Handler took " + (p2EndTime - p2StartTime) + " ms");

        // Tell p1Thread to start with a 2 second delay
        p1Handler.postDelayed(p1Thread.getTurnRunnable(), 2000);
    }

    /**
     * Ends the game. Notifies both PlayerThreads that the game is over
     * and creates a dialog informing the player of the game outcome with
     * options to start a new game or return to the main menu (finish this activity)
     */
    private void endGame(){
        notifyGameOver();
        int dialogMsg;
        switch (endGameStatus){
            case GameConstants.THREAD_1_JACKPOT:
                dialogMsg = R.string.thread1_win;
                break;
            case GameConstants.THREAD_1_CAT:
                dialogMsg = R.string.thread1_catastrophe;
                break;
            case GameConstants.THREAD_2_JACKPOT:
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

    /**
     * Sends the latest shot outcome to a PlayerThread
     * (lastOutcome is updated in processShot())
     *
     * @param player An int representing the player to send the outcome to.
     */
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
        Log.i("GameActivity", "sendShotOutcome: Sending player " + player + " outcome " + lastOutcome);
        Message msg = ph.obtainMessage(GameConstants.OUTCOME);
        msg.arg1 = lastOutcome;
        ph.sendMessageAtFrontOfQueue(msg);
    }

    /**
     * Calculates the group the given shot falls into and returns
     * the outcome. Possible outcomes are: Near miss (shot is in same
     * group as jackpot), Near Group (shot is in a group adjacent to
     * jackpot's group), and Big Miss (shot is neither of the previous 2)
     *
     * @param shotLoc An int representing a player's shot
     *
     * @return An integer flag representing one of the three possible
     * outcomes
     */
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

    /**
     * Updates the UI and game variables based on a player's shot.
     * Determines if the game is over, and if so, sets the gameOver
     * flag and determines a game outcome (jackpot vs. catastrophe and winner)
     *
     * @param player An int representing a player whose shot is being processed
     * @param shotLoc An int representing the location of the player's latest shot
     */
    private void processShot(int player, int shotLoc){
        // Move player 1 shot location in UI, set outcome, and check for win/catastrophe
        if(player == GameConstants.PLAYER_1){
            p1LastOutcome = getShotOutcome(shotLoc);
            if (p2Shots.contains(shotLoc)){
                golfAdapter.setImage(shotLoc, R.drawable.blue_catastrophe);
                Log.i("processShot", "Player 1 catastrophe");
                gameOver = true;
                endGameStatus = GameConstants.THREAD_1_CAT;
            }
            else if(shotLoc == winningHole){
                golfAdapter.setImage(shotLoc, R.drawable.blue_win);
                Log.i("processShot", "Player 1 win");
                gameOver = true;
                endGameStatus = GameConstants.THREAD_1_JACKPOT;
            }
            else{
                golfAdapter.setImage(shotLoc, R.drawable.blue_hole);
                Log.i("processShot", "Player 1 shot " + shotLoc);
                p1Location = shotLoc;
                p1Shots.add(shotLoc);
            }
        }
        // Move player 2 shot location in UI, set outcome, and check for win/catastrophe
        else if (player == GameConstants.PLAYER_2){
            p2LastOutcome = getShotOutcome(shotLoc);
            if (p1Shots.contains(shotLoc)){
                golfAdapter.setImage(shotLoc, R.drawable.red_catastrophe);
                Log.i("processShot", "Player 2 catastrophe");
                gameOver = true;
                endGameStatus = GameConstants.THREAD_2_CAT;
            }
            else if(shotLoc == winningHole){
                golfAdapter.setImage(shotLoc, R.drawable.red_win);
                Log.i("processShot", "Player 2 win");
                gameOver = true;
                endGameStatus = GameConstants.THREAD_2_JACKPOT;
            }
            else{
                golfAdapter.setImage(shotLoc, R.drawable.red_hole);
                Log.i("processShot", "Player 2 shot " + shotLoc);
                p2Location = shotLoc;
                p2Shots.add(shotLoc);
            }
        }
        golfAdapter.notifyDataSetChanged();
    }

    /**
     * Sends messages to both PlayerThreads letting them know that the game
     * is over (and that they should end their execution)
     */
    private void notifyGameOver(){
        Message msg1 = p1Handler.obtainMessage(GameConstants.GAME_OVER);
        p1Handler.sendMessageAtFrontOfQueue(msg1);
        msg1.obj = mHandler;
        Message msg2 = p2Handler.obtainMessage(GameConstants.GAME_OVER);
        msg2.obj = mHandler;
        // Make sure game over message takes precedent
        p2Handler.sendMessageAtFrontOfQueue(msg2);
    }

    /**
     * Sends messages to both PlayerThreads letting them know that the game
     * is over (and that they should end their execution)
     */
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

    /** Implementations of the EndGameDialogFragment's listener interface.
    * Allows methods from this activity to be called when the user selects an
    * option from the dialog fragment.
    */
    @Override
    public void onStartNewGame() {
        startNewGame();
    }
    @Override
    public void onQuit() {
        finish();
    }
}