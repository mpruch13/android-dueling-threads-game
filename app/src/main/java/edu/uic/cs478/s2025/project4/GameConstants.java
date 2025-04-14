/**
 Michael Ruch
 University of Illinois Chicago
 CS 478, Spring 2025

This class just stores game-related constants that are shared by the GameActivity and PlayerThreads.
The number of holes can also be dynamically adjusted by changing the NUM_HOLES and GROUP_SIZE constants.

    Warning: The game will break if NUM_HOLES is not evenly divisible by GROUP_SIZE (i.e. each group must
              contain the same number of holes).
 */

package edu.uic.cs478.s2025.project4;

public class GameConstants {

    // Game status
    protected static final int GAME_RESUMED = 0;
    protected static final int GAME_PAUSED = 3;
    protected static final int GAME_OVER = 4;

    // Game results
    protected static final int THREAD_1_JACKPOT = 5;
    protected static final int THREAD_1_CAT = 6;
    protected static final int THREAD_2_JACKPOT = 7;
    protected static final int THREAD_2_CAT = 8;

    // Players and shot outcomes
    protected static final int PLAYER_1 = 1;
    protected static final int PLAYER_2 = 2;
    protected static final int P1_SHOT = 10;
    protected static final int P2_SHOT = 20;
    protected static final int TAKE_TURN = 50;
    protected static final int OUTCOME = 60;
    protected static final int OUTCOME_BIG_MISS = 61;
    protected static final int OUTCOME_NEAR_GROUP = 62;
    protected static final int OUTCOME_NEAR_MISS = 63;

    // Player Thread Strategies
    protected static final int STRAT_BASIC = 70;
    protected static final int STRAT_AGGRESSIVE = 71;

    // Number of holes and group size
    // Game should work with any number of holes as long as
    // it can be evenly divided by group size (i.e., every group must have the same number of holes)
    protected static final Integer NUM_HOLES = 50;
    protected static final int GROUP_SIZE = 5;
}
