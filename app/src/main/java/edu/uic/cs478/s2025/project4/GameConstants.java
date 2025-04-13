package edu.uic.cs478.s2025.project4;

public class GameConstants {
    protected static final int GAME_OVER = 0;
    protected static final int PLAYER_1 = 1;
    protected static final int PLAYER_2 = 2;
    protected static final int P1_SHOT = 10;
    protected static final int P2_SHOT = 20;
    protected static final int TAKE_TURN = 50;
    protected static final int OUTCOME = 60;
    protected static final int OUTCOME_BIG_MISS = 61;
    protected static final int OUTCOME_NEAR_GROUP = 62;
    protected static final int OUTCOME_NEAR_MISS = 63;

    // Number of holes and group size
    // Game should work with any number of holes as long as
    // it can be evenly divided by group size (i.e., every group must have the same number of holes)
    protected static final Integer NUM_HOLES = 50;
    protected static final int GROUP_SIZE = 5;
}
