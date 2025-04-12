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

    private int lastOutcome = -1;
    private int lastGroup = -1;
    private int lastShot = -1;
    private final int iAm;

    PlayerThread(Handler mainHandler, int iAm){
        this.mainHandler = mainHandler;
        this.iAm = iAm;
    }

    public void run() {
        Looper.prepare();

        mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
            public void handleMessage(@NonNull Message msg) {
                int what = msg.what ;
                switch (what) {
                    case MainActivity.TAKE_TURN:
                        takeTurn();
                        break;
                    case MainActivity.OUTCOME:
                        Log.i("PlayerThread", "Player " + iAm + " received outcome " + msg.arg1);
                        lastOutcome = msg.arg1;
                        lastGroup = msg.arg2;
                        break;
                    case MainActivity.GAME_OVER:
                        getLooper().quitSafely();
                        break;
                    default:
                        // Do nothing
                        break;
                }
            }
        };
        Looper.loop();
    }
    public Handler getHandler(){
        return mHandler;
    }

    private void takeTurn(){
        // Sleep for 2 seconds
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Message msg;
        if(iAm == MainActivity.PLAYER_1){
           msg = mainHandler.obtainMessage(MainActivity.P1_SHOT);
        }
        else{
            msg = mainHandler.obtainMessage(MainActivity.P2_SHOT);
        }
        msg.arg1 = randomShot();
        msg.obj = mHandler;
        mainHandler.sendMessage(msg);
    }

    private int randomShot(){
        int nextShot;
        do{
            nextShot = new Random().nextInt(MainActivity.numHoles);
        }while(previousShots.contains(nextShot));
        previousShots.add(nextShot);
        return nextShot;
    }

    private int sameGroupShot(int group){
        int nextShot;
        do{
            nextShot = new Random().nextInt(MainActivity.numHoles);
        }while(previousShots.contains(nextShot));
        previousShots.add(nextShot);
        return nextShot;
    }

    private int closeGroupShot(int group){
        int nextShot;
        do{
            nextShot = new Random().nextInt(MainActivity.numHoles);
        }while(previousShots.contains(nextShot));
        previousShots.add(nextShot);
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
