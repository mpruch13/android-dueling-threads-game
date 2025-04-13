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
    private final Handler mainHandler;
    private Handler OpponentHandler;
    private PlayerThread opponent;
    private int lastOutcome = GameConstants.OUTCOME_BIG_MISS; // start with random shot always
    private int lastGroup = -1;
    private int lastShot = -1;
    private final int iAm;
    private boolean gameOver = false;

    PlayerThread(Handler mainHandler, int iAm){
        this.mainHandler = mainHandler;
        this.iAm = iAm;
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
                        gameOver = true;
                        getLooper().quitSafely();
                        break;
                    default:
                        // Do nothing
                        break;
                }
            }
        };
        // Wait until main thread gives us an opponent and until the opponent's handler is ready'
        Log.i("PlayerThread", "Player " + iAm + " waiting for opponent");
        while (opponent == null || OpponentHandler == null) {
            // wait for a bit
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(opponent != null){
                OpponentHandler = opponent.getHandler();
            }

        }
        Log.i("PlayerThread", "Player " + iAm + " found opponent. Ready to play.");
        Looper.loop();
    }

    private final Runnable myTurnRunnable = new Runnable() {
            public void run() {
                // Don't run if game is over
//                if(MainActivity.gameOver){
//                    return;
//                }
                Message msg;
                if(iAm == GameConstants.PLAYER_1){
                    msg = mainHandler.obtainMessage(GameConstants.P1_SHOT);
                }
                else{
                    msg = mainHandler.obtainMessage(GameConstants.P2_SHOT);
                }

                switch (lastOutcome) {
                    case GameConstants.OUTCOME_BIG_MISS:
                        msg.arg1 = randomShot();
                        Log.i("Player Thread","Player " + iAm +  " taking random shot."
                                + "\nLast group: " + lastGroup + "\nLast Outcome: " + lastOutcome);
                        break;
                    case GameConstants.OUTCOME_NEAR_GROUP:
                        msg.arg1 = closeGroupShot();
                        Log.i("Player Thread", "Player " + iAm + " taking near group shot."
                                + "\nLast group: " + lastGroup + "\nLast Outcome: " + lastOutcome);
                        break;
                    case GameConstants.OUTCOME_NEAR_MISS:
                        Log.i("Player Thread", "Player " + iAm + " taking same group shot."
                                + "\nLast group: " + lastGroup + "\nLast Outcome: " + lastOutcome);
                        msg.arg1 = sameGroupShot();
                        break;
                    default:
                        Log.e("Player Thread", "Player " + iAm
                                + " received an invalid outcome value: " + lastOutcome + "  Taking random shot");
                        msg.arg1 = randomShot();
                    }
                mainHandler.sendMessage(msg);
                // Sleep for 2 seconds before sending runnable to other thread
//                if(!MainActivity.gameOver){
                    OpponentHandler.postDelayed(opponent.getTurnRunnable(), 2000);
//                }
            }
        } ;

    protected Runnable getTurnRunnable(){
        return myTurnRunnable;
    }

    protected Handler getHandler(){
        return mHandler;
    }

    protected void setOpponent(PlayerThread opponent){
        this.opponent = opponent;
    }

//    private void takeTurn(){
//        // Sleep for 2 seconds
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        Message msg;
//        if(iAm == GameConstants.PLAYER_1){
//           msg = mainHandler.obtainMessage(GameConstants.P1_SHOT);
//        }
//        else{
//            msg = mainHandler.obtainMessage(GameConstants.P2_SHOT);
//        }
//        msg.arg1 = randomShot();
//        msg.obj = mHandler;
//        mainHandler.sendMessage(msg);
//    }

    private int randomShot(){
        int nextShot;
        do{
            nextShot = new Random().nextInt(GameConstants.NUM_HOLES);
        }while(previousShots.contains(nextShot));
        previousShots.add(nextShot);
        lastGroup = nextShot / GameConstants.GROUP_SIZE;
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
        previousShots.add(nextShot);
        lastGroup = nextShot / GameConstants.GROUP_SIZE;
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
        previousShots.add(nextShot);
        lastGroup = nextShot / GameConstants.GROUP_SIZE;
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
        if(previousShots.contains(target)){
            //TODO: decide what to do if
            return target;
        }
        else{
            return target;
        }
    }
}
