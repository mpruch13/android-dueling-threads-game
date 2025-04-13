package edu.uic.cs478.s2025.project4;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;


public class PlayerThread extends Thread {

    private final ArrayList<Integer> previousShots = new ArrayList<>();
    private Handler mHandler;
    private Handler mainHandler;
    private Handler opponentHandler;
    private PlayerThread opponent;
    private Runnable myTurnRunnable;
    private int lastOutcome = GameConstants.OUTCOME_BIG_MISS; // start with random shot always
    private int lastGroup = -1;
    private final int iAm;
    private boolean paused = false;
    private boolean firstShot = true;

    private int aggressiveDirection = 1;

    PlayerThread(Handler mainHandler, int iAm, int strategy){
        this.mainHandler = mainHandler;
        this.iAm = iAm;

        switch (strategy){
            case GameConstants.STRAT_AGGRESSIVE:
                myTurnRunnable = aggressiveStrategy;
                Log.i("PlayerThread", "Player " + iAm + " initialized with Aggressive Strategy");
                break;
            default:
                myTurnRunnable = basicStrategy;
                Log.i("PlayerThread", "Player " + iAm + " initialized with Basic Strategy");
                break;
        }
    }

    public void run() {
        Log.i("PlayerThread", "Player " + iAm + " starting");
        Looper.prepare();

        mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
            public void handleMessage(@NonNull Message msg) {
                int what = msg.what ;
                switch (what) {
                    case GameConstants.OUTCOME:
                        Log.i("PlayerThread", "Player " + iAm + " received outcome " + msg.arg1);
                        lastOutcome = msg.arg1;
                        break;
                    case GameConstants.GAME_OVER:
                        Log.i("PlayerThread", "Player " + iAm + " received game over message. Quitting thread.");
                        mHandler.removeCallbacksAndMessages(null);
                        getLooper().quit();
                        break;
                    case GameConstants.GAME_PAUSED:
                        paused = true;
                        mHandler.removeCallbacks(myTurnRunnable);
                        Log.i("PlayerThread", "Player " + iAm + " received pause message. Pausing thread.");
                        break;
                    case GameConstants.GAME_RESUMED:
                        paused = false;
                        Log.i("PlayerThread", "Player " + iAm + " received resume message. Resuming game thread.");
                        break;
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
            if(opponent != null){
                opponentHandler = opponent.getHandler();
            }

        }
        Log.i("PlayerThread", "Player " + iAm + " found opponent. Ready to play.");
        Looper.loop();
    }

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

    private int randomShot(){
        int nextShot;
        do{
            nextShot = new Random().nextInt(GameConstants.NUM_HOLES);
        }while(previousShots.contains(nextShot));
        //previousShots.add(nextShot);
        // lastGroup = nextShot / GameConstants.GROUP_SIZE;
        return nextShot;
    }

    private int sameGroupShot(){
        int nextShot;
        // The minimum value of the last group that was shot
        // (e.g,. group 2 is 10-14, and 2 * 5 = 10, so 10 is the min value)
        int groupMin = lastGroup * 5;
        do{
            // Next shot is determined by a random number for 0-4 plus the group minimum
            // This guarantees the next shot will be in the desired group
            // (e.g,. if we want group 2: the lower bound is 0 + 10 = 10, and upper bound is 4 + 10 = 14)
            nextShot = new Random().nextInt(GameConstants.GROUP_SIZE) + groupMin;
        }while(previousShots.contains(nextShot));
        // previousShots.add(nextShot);
        // lastGroup = nextShot / GameConstants.GROUP_SIZE;
        return nextShot;
    }


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
        do{
            // Next shot will be randomly from one of the adjacent groups
            int shotOffset = new Random().nextInt(GameConstants.GROUP_SIZE);
            Log.i("PlayerThread", "Player " + iAm + " calling getCloseGroupHole with:"
                        + "\noffset: " + shotOffset + "\nlowerGroupMin: " + lowerGroupMin
                        + "\nupperGroupMin: " + upperGroupMin);
            nextShot = getCloseGroupHole(shotOffset,lowerGroupMin,upperGroupMin);

        }while(previousShots.contains(nextShot));
        // previousShots.add(nextShot);
        // lastGroup = nextShot / GameConstants.GROUP_SIZE;
        return nextShot;
    }

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


    // Player 1 Strategy: Take a random first shot, then close shots until
    private final Runnable basicStrategy = new Runnable() {
        public void run() {
            // Don't run if paused
            if(paused){
                return;
            }
            Message msg;
            if(iAm == GameConstants.PLAYER_1){
                msg = mainHandler.obtainMessage(GameConstants.P1_SHOT);
            }
            else{
                msg = mainHandler.obtainMessage(GameConstants.P2_SHOT);
            }

            if(firstShot){
                Log.i("Player Thread","Player " + iAm +  " taking random first shot.");
                msg.arg1 = randomShot();
                firstShot = false;
            }
            // Shoot into a close group until getting a near miss
            else if(lastOutcome != sameGroupShot()){
                msg.arg1 = closeGroupShot();
                String logMsg = "Player " + iAm + " taking near group shot.\nLast group: " + lastGroup;
                if(lastOutcome == GameConstants.OUTCOME_NEAR_GROUP){
                    logMsg += "\nLast Outcome: Near group";
                }
                Log.i("PlayerThread", logMsg);
            }
            // Always take a near-group shot if the last shot was a near miss
            else{
                msg.arg1 = sameGroupShot();
                Log.i("Player Thread", "Player " + iAm + " taking same group shot."
                        + "\nLast group: " + lastGroup + "\nLast Outcome: Near miss");
            }

//            switch (lastOutcome) {
//                case GameConstants.OUTCOME_BIG_MISS:
//                    msg.arg1 = randomShot();
//                    if(!firstShot){
//                        Log.i("Player Thread","Player " + iAm +  " taking random shot."
//                                + "\nLast group: " + lastGroup + "\nLast Outcome: Big miss");
//                    }
//                    else{
//                        Log.i("Player Thread","Player " + iAm +  " taking random first shot.");
//                    }
//                    break;
//                case GameConstants.OUTCOME_NEAR_GROUP:
//                    msg.arg1 = closeGroupShot();
//
//                    break;
//                case GameConstants.OUTCOME_NEAR_MISS:
//                    Log.i("Player Thread", "Player " + iAm + " taking same group shot."
//                            + "\nLast group: " + lastGroup + "\nLast Outcome: Near miss");
//                    msg.arg1 = sameGroupShot();
//                    break;
//                default:
//                    Log.e("Player Thread", "Player " + iAm
//                            + " received an invalid outcome value: " + lastOutcome + "  Taking random shot");
//                    msg.arg1 = randomShot();
//            }
            // Send shot to UI thread and post opponent's runnable with a 2 second delay
            previousShots.add(msg.arg1);
            lastGroup = msg.arg1 / GameConstants.GROUP_SIZE;
            Log.i("Player Thread","Player " + iAm +  " sending shot for hole: " + msg.arg1 + " set last group to: " + lastGroup);
            mainHandler.sendMessage(msg);
            opponentHandler.postDelayed(opponent.getTurnRunnable(), 2000);
        }
    };

    /**
     * Runnable that implements the "aggressive" stategy behavior. IN
     */
    private final Runnable aggressiveStrategy = new Runnable() {
        public void run() {
            // Don't run if paused
            if(paused){
                return;
            }

            Message msg;
            if(iAm == GameConstants.PLAYER_1){
                msg = mainHandler.obtainMessage(GameConstants.P1_SHOT);
            }
            else{
                msg = mainHandler.obtainMessage(GameConstants.P2_SHOT);
            }

            if(firstShot){
                Log.i("Player Thread","Player " + iAm +  " taking random first shot.");
                msg.arg1 = randomShot();
                firstShot = false;
            }
            // If last shot was a big miss, skip the next adjacent group and go higher
            else if(lastOutcome == GameConstants.OUTCOME_BIG_MISS){
                Log.i("Player Thread", "Player " + iAm + " attempting skip group shot."
                        + "\nLast group: " + lastGroup + "\nLast Outcome: Big miss");
               int target;

               // WHen moving in positive direction: If there is a group 2 groups ahead, shoot into it.
               if(aggressiveDirection == 1 && lastGroup + 2 < (GameConstants.NUM_HOLES / GameConstants.GROUP_SIZE)){
                   target = (lastGroup + 2) * 5;
               }
               // When moving in positive direction: If no group exists 2 ahead, then start moving in negative direction and shoot 1 group behind
               else if (aggressiveDirection == 1){
                   target = (lastGroup - 1) * 5;
                   aggressiveDirection = 0;
               }
               // When moving in negative direction: If there is a group 2 groups behind, shoot into it.
               else if (aggressiveDirection == 0 && lastGroup + 2 < (GameConstants.NUM_HOLES / GameConstants.GROUP_SIZE)){
                    target = (lastGroup - 2) * 5;
               }
               // Moving in negative direction: If no group exists 2 behind, start moving in positive direction and shoot 1 group ahead
               else{
                    target = (lastGroup + 1) * 5;
                    aggressiveDirection = 1;
               }
               // Now, make sure target has not already been shot previously
                int possibleTargets = 5;
                while(previousShots.contains(target) && possibleTargets > 0){
                    target+=1;
                    possibleTargets-=1;
                }
                // If there are no possible targets in (probably will never happen), then take a close group shot instead.
                if(possibleTargets == 0){
                    target = closeGroupShot();
                }
                msg.arg1 = targetHoleShot(target);
            }
            // If close group, take a close group shot
            else if(lastOutcome == GameConstants.OUTCOME_NEAR_GROUP){
                Log.i("Player Thread", "Player " + iAm + " taking close group shot."
                        + "\nLast group: " + lastGroup + "\nLast Outcome: Near miss");
                msg.arg1 = closeGroupShot();
            }
            // Always take a near-group shot if the last shot was a near miss
            else{
                msg.arg1 = sameGroupShot();
                Log.i("Player Thread", "Player " + iAm + " taking same group shot."
                        + "\nLast group: " + lastGroup + "\nLast Outcome: Near miss");
            }

            // Send shot to UI thread, update last group, and post opponent's runnable with a 2 second delay
            previousShots.add(msg.arg1);
            lastGroup = msg.arg1 / GameConstants.GROUP_SIZE;
            Log.i("Player Thread","Player " + iAm +  " sending shot for hole: " + msg.arg1 + " set last group to: " + lastGroup);
            mainHandler.sendMessage(msg);
            opponentHandler.postDelayed(opponent.getTurnRunnable(), 2000);

        }
    };
}
