package edu.uic.cs478.s2025.project4;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.Objects;

public static final int MY_TURN = 1;
public static final int GAME_OVER = 0;

public class PlayerThread extends Thread {

        public Handler pHandler;

        public void run() {
            Looper.prepare();

            pHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
                public void handleMessage(@NonNull Message msg) {
                    int what = msg.what ;
                    switch (what) {
                        case MY_TURN:
                            takeTurn();
                            break;
                        case GAME_OVER:
                            // TODO: Handle Player 2 shot
                            break;
                        default:
                            // Do nothing
                            break;
                    }
                }
            };

            Looper.loop();
        }

        private void takeTurn(){


        }
}
