/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

 A ViewModel class for storing PlayerThreads during a config change. During a config change, the GameActivity sends
 a message to both threads letting them know to pause the game. When the GameActivity is rebuilt, it uses this
 ViewModel class to retrieve the threads and then tells them to resume the game where they left off.
 */

package edu.uic.cs478.s2025.project4;

import android.os.Handler;
import android.os.Message;

import androidx.lifecycle.ViewModel;

public class GameViewModel extends ViewModel {

    private PlayerThread p1Thread;
    private PlayerThread p2Thread;

    /// Just setters and getters
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

    /**
     Tell the player threads that the game is over (and that they should end their execution) if the GameActivity
     is fully destroyed before a game ends (when the user presses the back button).
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        // Tell threads the game is over if they haven't been informed already
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
