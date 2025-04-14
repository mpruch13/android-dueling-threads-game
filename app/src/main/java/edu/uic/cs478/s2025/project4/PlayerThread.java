/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

 Class for PlayerThreads in the Microgolf game. A player thread communicates with the UI thread and another player thread
 to coordinate an automated game of Microgolf. Each player thread is initialized with a strategy that it will use to determine the next
 hole to shoot into. PlayerThreads are also responsible for sending their shots to the UI thread to be processed, receiving
 outcomes from the UI thread to inform their strategies, and signaling the opponent thread to take their turn (with a 2 second
 delay so that the user can follow the game in the UI). PlayerThreads can also be paused/resumed with messages, and they will
 gracefully shut down after receiving a game over message.
 */

package edu.uic.cs478.s2025.project4;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import java.util.function.IntSupplier;


public class PlayerThread extends Thread {

    private final ArrayList<Integer> previousShots = new ArrayList<>();
    private Handler mHandler;
    private Handler mainHandler;
    private Handler opponentHandler;
    private PlayerThread opponent;
    private final Runnable myTurnRunnable;
    private int lastOutcome = GameConstants.OUTCOME_BIG_MISS; // start with random shot always
    private int lastGroup = -1;
    private final int iAm;
    private boolean paused = false;
    private boolean firstShot = true;
    private int aggressiveDirection = 1;

    /// Just setters and getters
    protected Runnable getTurnRunnable(){
        return myTurnRunnable;
    }
    protected Handler getHandler(){
        return mHandler;
    }
    protected void setOpponent(PlayerThread opponent){
        this.opponent = opponent;
    }
    protected void setMainHandler(Handler mainHandler){
        this.mainHandler = mainHandler;
    }

    /**
     * Constructor for a PlayerThread. Takes the main/UI handler
     * that the thread will communicate with, an integer representing
     * the player's id (player 1 or player 2), and an integer flag
     * representing the strategy the thread should use.
     */
    PlayerThread(Handler mainHandler, int iAm, int strategy){
        this.mainHandler = mainHandler;
        this.iAm = iAm;
        // Generate a strategy runnable based on the given strategy flag.
        switch (strategy){
            case GameConstants.STRAT_AGGRESSIVE:
                myTurnRunnable = buildTurnRunnable(this::aggressiveSearchStrat);
                Log.i("PlayerThread", "Player " + iAm + " initialized with Aggressive Search strategy");
                break;
            default:
                myTurnRunnable = buildTurnRunnable(this::dumbLuckStrat);
                Log.i("PlayerThread", "Player " + iAm + " initialized with Dumb Luck strategy");
                break;
        }
    }

    /**
     * Execute when the thread starts/runs. Starts the Looper and defines a handler. The handler
     * handles messages from other threads, which could be
     */
    public void run() {
        Log.i("PlayerThread", "Player " + iAm + " starting");
        Looper.prepare();

        mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
            public void handleMessage(@NonNull Message msg) {
                int what = msg.what ;
                switch (what) {
                    // Update outcome from UI thread
                    case GameConstants.OUTCOME:
                        Log.i("PlayerThread", "Player " + iAm + " received outcome " + msg.arg1);
                        lastOutcome = msg.arg1;
                        break;
                    // Remove any pending runnables/messages and quit the looper when the game ends
                    case GameConstants.GAME_OVER:
                        Log.i("PlayerThread", "Player " + iAm + " received game over message. Quitting thread.");
                        mHandler.removeCallbacksAndMessages(null);
                        getLooper().quit();
                        break;
                    // Cancel any pending turns and set paused status
                    case GameConstants.GAME_PAUSED:
                        paused = true;
                        mHandler.removeCallbacks(myTurnRunnable);
                        Log.i("PlayerThread", "Player " + iAm + " received pause message. Pausing thread.");
                        break;
                    // Remove paused status on receiving a resume message.
                    case GameConstants.GAME_RESUMED:
                        paused = false;
                        Log.i("PlayerThread", "Player " + iAm + " received resume message. Resuming game thread.");
                        break;
                    // Should ideally never reach this case. Print an error message.
                    default:
                        Log.e("PlayerThread", "Player " + iAm + " received unknown message.");
                        break;
                }
            }
        };

        // Wait until main thread gives us an opponent and then wait until the opponent's handler is ready'
        Log.i("PlayerThread", "Player " + iAm + " waiting for opponent");
        while (opponent == null || opponentHandler == null) {
            // wait for a short amount of time (should hopefully let the OS switch to a different thread right away instead of wasting cycles with useless looping)
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Try to get handler once we have an opponent.
            if(opponent != null){
                opponentHandler = opponent.getHandler();
            }

        }
        // Output message and start looper when ready to play.
        Log.i("PlayerThread", "Player " + iAm + " found opponent. Ready to play.");
        Looper.loop();
    }

    /**
     * Returns a shot into a random hole. The random hole can be
     * any hole in the entire hole array that has not already been
     * shot into by the PlayerThread.
     *
     * @return an int representing a random shot
     */
    private int randomShot(){
        int nextShot;
        // Get a random shot, keep looping until shot is not found
        // in the previous shots array.
        do{
            nextShot = new Random().nextInt(GameConstants.NUM_HOLES);
        }while(previousShots.contains(nextShot));
        return nextShot;
    }

    /**
     * Returns a random shot that falls into the same group as the
     * PlayerThread's previous shot that has not already been shot
     * into by the PlayerThread.
     *
     * @return an int representing a random shot into the same group.
     */
    private int sameGroupShot(){
        int nextShot;
        // The minimum value of the last group that was shot
        // (e.g,. group 2 is 10-14, and 2 * 5 = 10, so 10 is the min value)
        int groupMin = lastGroup * 5;
        // Get a same-group shot, keep looping until we get one that does not
        // fall into the previous shots list.
        do{
            // Next shot is determined by a random number for 0-4 plus the group minimum
            // This guarantees the next shot will be in the desired group
            // (e.g,. if we want group 2: the lower bound is 0 + 10 = 10, and upper bound is 4 + 10 = 14)
            nextShot = new Random().nextInt(GameConstants.GROUP_SIZE) + groupMin;
        }while(previousShots.contains(nextShot));
        return nextShot;
    }

    /**
     * Returns a shot that falls randomly into a group that is adjacent to
     * the group of the thread's previous shot, and also has not yet
     * been shot into by the PlayerThread.
     *
     * @return an int representing a random shot into a close group.
     */
    private int closeGroupShot(){
        int nextShot;
        int lowerGroupMin = -1;
        int upperGroupMin = -1;

        // Make sure lower group exists
        if(lastGroup - 1 >= 0){
            // if lastGroup was 2 (holes 10-14), then minimum of the lower adjacent group,
            // group 1 (5-9) should be (2 - 1) * 5 = 5
            lowerGroupMin = (lastGroup - 1) * 5;
        }
        // Make sure upper group exists
        if(lastGroup + 1 < (GameConstants.NUM_HOLES / GameConstants.GROUP_SIZE)){
            // if lastGroup was 2 (holes 10-14), then minimum of the upper adjacent group,
            // group 3 (15-19) should be (2 + 1) * 5 = 15
            upperGroupMin = (lastGroup + 1) * 5;
        }
        // Get a close group shot. If the hole was previously shot into, keep
        // looping until finding a hole not in the previous hole list.
        do{
            // Next shot will be randomly from one of the adjacent groups
            int shotOffset = new Random().nextInt(GameConstants.GROUP_SIZE);
//            Log.i("PlayerThread", "Player " + iAm + " calling getCloseGroupHole with:"
//                        + "\noffset: " + shotOffset + "\nlowerGroupMin: " + lowerGroupMin
//                        + "\nupperGroupMin: " + upperGroupMin);
            nextShot = getCloseGroupHole(shotOffset,lowerGroupMin,upperGroupMin);

        }while(previousShots.contains(nextShot));
        return nextShot;
    }

    /**
     * Given an offset (from 0-group size), minimum hole number of the lower adjacent group,
     * and minimum hole-number of the upper adjacent group, returns a shot location that could
     * fall into either the lower or upper group.
     *
     * @param  shotOffset An integer representing the 0-indexed location that the returned shot
     *                    will fall into in the chosen group (e.g., offset 4 will put the
     *                    shot in the 5th hole of whatever group ends up being chosen).
     * @param lowerMin An int representing the minimum of the lower group the shot could
     *                 be placed into (e.g,. for groups of 5, a minimum of 0 means that
     *                 the shot could be placed in holes 0-4).
     * @param upperMin An int representing the minimum of the upper group the shot could
     *                 be placed into (e.g,. for groups of 5, an upper minimum of 10 means that
     *                 the shot could be placed in holes 10-14).
     *
     * @return Returns a Runnable object that can be posted to the PlayerThread's job que.
     */
    private int getCloseGroupHole(int shotOffset, int lowerMin, int upperMin){
        int upperOrLower;
        int nextShot = shotOffset;

        // If both lowerMin and upperMin are valid, then randomly choose between them
        if(lowerMin!= -1 && upperMin != -1){
            upperOrLower = new Random().nextInt(2);
            // Put next shot in the lower group if random number was 0
            if(upperOrLower == 0){
                nextShot += lowerMin;
            }
            // Put next shot in upper group if random was 1
            else{
                nextShot += upperMin;
            }
        }
        // If only the lower minimum is valid (last shot was in the highest group),
        else if (lowerMin != -1){
            nextShot += lowerMin;
        }
        // Only other possibility is that only the upper group is valid
        else{
            nextShot += upperMin;
        }
        return nextShot;
    }

    /**
     * Essentially just a wrapper that makes sure a target hole chosen by a thread never
     * ends up in a hole that was previously chosen by the thread. Ideally should always
     * just return the target, but if the target was already used, prints an error message
     * to the log and returns a random shot instead.
     *
     * @param  target An integer representing the hole a PlayerThread has chosen to shoot
     *                into.
     *
     * @return Returns the target hole if valid, or a random hole if not.
     */
    private int targetHoleShot(int target){
        // For debugging. Ideally should never see this message since player threads should never try to
        // shoot in a hole they already previously shot in
        if(previousShots.contains(target)){
            Log.e("PlayerThread", "Player " + iAm + " tried a targeted shot on a hole they already shot in!"
                        + "Taking random shot instead!");
            return randomShot();
        }
        else{
            return target;
        }
    }

    /**
     * Creates and returns a runnable that executes everything a PlayerThread does during
     * a single turn. Each runnable returned by this method follows the same basic format:
     * It determines the next shot location, sends this location as a message to the UI thread,
     * updates shot tracking variables (LastGroup and previousShots, and finally, posts the
     * opponent's runnable to the opponent's job queue with a 2 second delay.
     * The only difference between runnables returned by this method is strategy used to
     * determine the next shot location, which is set using the playerStrategy argument.
     *
     * @param  playerStrategy A method with no arguments that returns an integer. Used as
     *                        the PlayerThreads strategy for determining subsequent shots.
     * @return Returns a Runnable object that can be posted to the PlayerThread's job que.
     */
    private Runnable buildTurnRunnable(IntSupplier playerStrategy){
       return () -> {
           // Don't run if paused
           if(paused){
               return;
           }

           // Create Message
           Message msg;
           if(iAm == GameConstants.PLAYER_1){
               msg = mainHandler.obtainMessage(GameConstants.P1_SHOT);
           }
           else{
               msg = mainHandler.obtainMessage(GameConstants.P2_SHOT);
           }

           // Determine shot location
           if(firstShot){
               Log.i("Player Thread","Player " + iAm +  " taking random first shot.");
               msg.arg1 = randomShot();
               firstShot = false;
           }
           else{
               msg.arg1 = playerStrategy.getAsInt();
           }

           // Send shot to UI thread and post opponent's runnable with a 2 second delay
           previousShots.add(msg.arg1);
           lastGroup = msg.arg1 / GameConstants.GROUP_SIZE;
           //Log.i("Player Thread","Player " + iAm +  " sending shot for hole: " + msg.arg1 + " set last group to: " + lastGroup);
           mainHandler.sendMessage(msg);
           opponentHandler.postDelayed(opponent.getTurnRunnable(), 2000);
       };
    }

    /**
     * Method that defines the "dumb luck" shot strategy. Keeps taking close shots to jump randomly
     * up or down one row every shot until it stumbles into a near miss, then it takes near group
     * shots until finding the jackpot.
     *
     * Used as an IntSupplier for the buildStrategyRunnable() method.
     * Takes no arguments and returns an integer representing the hole chosen for the next shot.
     */
    private int dumbLuckStrat(){
        int shot;
        // Shoot into a close group until getting a near miss
        if(lastOutcome != GameConstants.OUTCOME_NEAR_MISS){
            shot = closeGroupShot();
            String logMsg = "Player " + iAm + " taking near group shot.\nLast group: " + lastGroup;
            if(lastOutcome == GameConstants.OUTCOME_NEAR_GROUP){
                logMsg += "\nLast Outcome: Near group";
            }
            else{
                logMsg += "\nLast Outcome: Big miss";
            }
            Log.i("PlayerThread", logMsg);
        }
        // Always take a near-group shot if the last shot was a near miss
        else{
            shot = sameGroupShot();
            Log.i("Player Thread", "Player " + iAm + " taking same group shot."
                    + "\nLast group: " + lastGroup + "\nLast Outcome: Near miss");
        }
        return shot;
    }

    /**
     * Method that defines the "aggressive search" strategy. Searches every other row until receiving a close or same group
     * outcome, starting from the initial shot group and moving down towards the bottom of the screen (positive direction
     * of hole/group numbers), then switches directions and moves back up towards the top. Takes a close shot after a close
     * group outcome, and takes a same group shot after a near group outcome. "Searching" shots after a big miss are guaranteed
     * to be in the first hole of a row, unless that hole was previously shot into.
     *
     * Used as an IntSupplier for the buildStrategyRunnable() method.
     * Takes no arguments and returns an integer representing the hole chosen for the next shot.
     */
    private int aggressiveSearchStrat(){
        int shot;
        // If last shot was a big miss, shoot one past the next adjacent group, or if that isn't possible, switch directions.
        if(lastOutcome == GameConstants.OUTCOME_BIG_MISS){
            Log.i("Player Thread", "Player " + iAm + " attempting skip shot."
                    + "\nLast group: " + lastGroup + "\nLast Outcome: Big miss");
            int target;
            // When moving in positive direction: If there is a group 2 groups ahead, shoot into it.
            if(aggressiveDirection == 1 && lastGroup + 2 < (GameConstants.NUM_HOLES / GameConstants.GROUP_SIZE)){
                target = (lastGroup + 2) * 5;
            }
            // When moving in positive direction: If no group exists 2 groups ahead, then start moving in negative direction.
            // Shoot 1 group behind to avoid going over the same groups
            else if (aggressiveDirection == 1){
                target = (lastGroup - 1) * 5;
                aggressiveDirection = 0;
            }
            // When moving in negative direction: If there is a group 2 groups behind, shoot into it.
            else if (aggressiveDirection == 0 && lastGroup - 2 < (GameConstants.NUM_HOLES / GameConstants.GROUP_SIZE)){
                target = (lastGroup - 2) * 5;
            }
            // Moving in negative direction: If no group exists 2 behind, start moving in positive direction.
            // Shoot 1 group ahead to avoid going over the same groups
            else{
                target = (lastGroup + 1) * 5;
                aggressiveDirection = 1;
            }
            // After finding a group, make sure the first hole wasn't previously shot
            int possibleTargets = 5;
            // Check all holes in the target group
            while(previousShots.contains(target) && possibleTargets > 0){
                target+=1;
                possibleTargets-=1;
            }
            // If there are no possible targets in (probably will never happen), then just take a close group shot instead.
            if(possibleTargets == 0){
                target = closeGroupShot();
            }
            // Run target through targetHoleShot to be 100% sure it hasn't been shot already
            shot = targetHoleShot(target);
        }
        // If the previous shot was near group, just take a close group shot.
        else if(lastOutcome == GameConstants.OUTCOME_NEAR_GROUP){
            Log.i("Player Thread", "Player " + iAm + " taking close group shot."
                    + "\nLast group: " + lastGroup + "\nLast Outcome: Near miss");
            shot = closeGroupShot();
        }
        // Always take a near-group shot if the last shot was a near miss
        else{
            shot = sameGroupShot();
            Log.i("Player Thread", "Player " + iAm + " taking same group shot."
                    + "\nLast group: " + lastGroup + "\nLast Outcome: Near miss");
        }
        return shot;
    }
}
