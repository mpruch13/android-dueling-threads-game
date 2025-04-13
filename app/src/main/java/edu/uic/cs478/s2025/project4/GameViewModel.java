package edu.uic.cs478.s2025.project4;

import android.os.Handler;
import android.os.Message;

import androidx.lifecycle.ViewModel;

public class GameViewModel extends ViewModel {

    private PlayerThread p1Thread;
    private PlayerThread p2Thread;

    public void setP1Thread(PlayerThread thread){
        p1Thread = thread;
    }
    public void setP2Thread(PlayerThread thread){
        p2Thread = thread;
    }
    public PlayerThread getP1Thread(){
        return p1Thread;
    }
    public PlayerThread getP2Thread(){
        return p2Thread;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Tell threads the game is over if they haven't been informed already (if the player ends the game before it ends on it's own)
        if (p1Thread.isAlive()) {
            Handler p1Handler = p1Thread.getHandler();
            Message msg1 = p1Handler.obtainMessage(GameConstants.GAME_OVER);
            p1Handler.sendMessage(msg1);
        }
        if (p2Thread.isAlive()) {
            Handler p1Handler = p2Thread.getHandler();
            Message msg1 = p1Handler.obtainMessage(GameConstants.GAME_OVER);
            p1Handler.sendMessage(msg1);
        }
    }
}
