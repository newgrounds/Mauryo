package ch.idsia.ai.agents.ai;

import ch.idsia.ai.agents.Agent;
import ch.idsia.mario.environments.Environment;
import ch.idsia.mario.engine.sprites.Mario;
import ch.idsia.mario.engine.sprites.Mario.MODE;

/**
 * Created by adamgressen on 10/16/14.
 */
public class TuringAgent extends BasicAIAgent implements Agent
{
    // Tile types
    static class TILE {
        public static final int NONE = 0;
        public static final int ENEMY = 2;
        public static final int SPECIAL_ENEMY = 9;
        public static final int HARD_TERRAIN = -10;
        public static final int SOFT_TERRAIN = -11;
        public static final int FLOWER_ENEMY = 12;
        public static final int FIRE_FLOWER = 15;
        public static final int BRICK = 16;
        public static final int FLOWER_PIPE = 20;
        public static final int QBRICK = 21;
    }

    // count jump height
    private int trueJumpCounter = 0;

    // time since last left
    private int lastLeftTime = 0;
    // left interval
    private int leftInterval = 10;

    // time since last jump
    private int lastJumpTime = 0;
    // jump interval
    private int jumpInterval = 5;

    // Mario's current mode
    private MODE currentMode = MODE.MODE_FIRE;

    // level width and height
    private static final int SIZE = 22;
    // Mario's position in the level scene
    private static final int M_POS = 11;

    public TuringAgent()
    {
        super("TuringAgent");
        reset();
    }

    public void reset()
    {
    }

    public boolean[] getAction(Environment observation)
    {
        /*float[] marioPos = agentObservation.getMarioFloatPos();
        float[] enemiesPos = agentObservation.getEnemiesFloatPos();*/

        // set Mario's mode
        SetMode(observation);

        byte[][] levelScene = observation.getCompleteObservation();

        // default movement values
        action[Mario.KEY_RIGHT] = true;
        action[Mario.KEY_LEFT] = false;
        action[Mario.KEY_SPEED] = false;
        action[Mario.KEY_JUMP] = false;

        /*if (lastLeftTime) {

        }*/

        // if we're under soft terrain then try to jump through
        /*if (IsUnder(levelScene, TILE.SOFT_TERRAIN)) {
            action[Mario.KEY_LEFT] = true;
            action[Mario.KEY_RIGHT] = false;
            //action[Mario.KEY_JUMP] = true;
            PrintLevelScene(levelScene);
        }*/

        // if there's a flower enemy ahead
        if (IsBehind(levelScene, TILE.FLOWER_ENEMY)) {
            if (currentMode == MODE.MODE_FIRE) {
                action[Mario.KEY_SPEED] = false;
                action[Mario.KEY_SPEED] = true;
            }
            // if there's a dangerous flower about
            if (FlowerDanger(levelScene)) {
                System.out.println("mario will wait");
                action[Mario.KEY_RIGHT] = false;
                return action;
            } else {
                action[Mario.KEY_LEFT] = false;
                action[Mario.KEY_RIGHT] = true;
                //System.out.println("jump over flower");
            }
        }

        // if we're ahead of an enemy
        if (IsAhead(levelScene, TILE.ENEMY)) {
            action[Mario.KEY_LEFT] = true;
            action[Mario.KEY_RIGHT] = false;
            System.out.println("enemy behind us");
        }

        /*// if we're over a special brick
        if (IsOver(levelScene, TILE.QBRICK)) {
            action[Mario.KEY_LEFT] = true;
            action[Mario.KEY_RIGHT] = false;
            System.out.println("we think we're over it");
        }*/

        /*if (IsUnder(levelScene, TILE.QBRICK)) {
            action[Mario.KEY_JUMP] = true;
            System.out.println("UNDER IT");
            //PrintLevelScene(levelScene);
        }*/

        // jump if there's something to the right
        if (levelScene[M_POS][M_POS + 2] != TILE.NONE ||
                levelScene[M_POS][M_POS + 1] != TILE.NONE ||
                DangerOfGap(levelScene) ||
                IsUnder(levelScene, TILE.QBRICK)) {
            // check if we should still hold down jump
            if (observation.mayMarioJump() || !observation.isMarioOnGround()) {
                action[Mario.KEY_JUMP] = true;
            }
            ++trueJumpCounter;
        }

        // jump if there's something to the left
        else if (action[Mario.KEY_LEFT] == true &&
                (levelScene[M_POS][M_POS - 2] != TILE.NONE ||
                        levelScene[M_POS][M_POS - 1] != TILE.NONE ||
                        DangerOfGap(levelScene) ||
                        IsUnder(levelScene, TILE.QBRICK))) {
            // check if we should still hold down jump
            if (observation.mayMarioJump() || !observation.isMarioOnGround()) {
                action[Mario.KEY_JUMP] = true;
            }
            ++trueJumpCounter;
        }

        // don't jump
        else if (action[Mario.KEY_JUMP] == false) {
            trueJumpCounter = 0;
        }

        // are you done jumping?
        if (trueJumpCounter > 16) {
            action[Mario.KEY_JUMP] = false;
            trueJumpCounter = 0;
        }

        /*boolean leftCoin = false;
        int coinX = 0, coinY = 0;
        for (int x = 0; x < M_POS; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (levelScene[y][x] == TILE.SOFT_TERRAIN) {
                    coinX = x;
                    coinY = y;
                    leftCoin = true;
                }
            }
        }*/

        //if not dangerous or too scared run
        //action[Mario.KEY_SPEED] = AiUtils.DangerOfGap(levelScene) && (fearVal < 2.5);

        /*if(!action[Mario.KEY_SPEED] && AiUtils.isOnFlatSurface(levelScene)){
            action[Mario.KEY_SPEED] = (fearVal < 0.25);
        }*/
        return action;
    }

    /*
        Mario is under something if it's within 1 of his x position
     */
    private boolean IsUnder(byte[][] levelScene, int type) {
        for (int y = 0; y < M_POS - 1; y++) {
            if (levelScene[y][M_POS] == type) {
                return true;
            }
        }
        return false;
    }

    /*
        Mario is over something if it's within 1 of his x position
     */
    private boolean IsOver(byte[][] levelScene, int type) {
        for (int y = M_POS + 1; y < SIZE; y++) {
            if (levelScene[y][M_POS] == type) {
                System.out.println(y);
                return true;
            }
        }
        return false;
    }

    /*
        Mario is behind something if it's ahead by <= 3
     */
    private boolean IsBehind(byte[][] levelScene, int type) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = M_POS + 1; x < M_POS + 3; x++) {
                if (levelScene[y][x] == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
        Mario is ahead of something if it's behind by <= 3
     */
    private boolean IsAhead(byte[][] levelScene, int type) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = M_POS - 3; x < M_POS; x++) {
                if (levelScene[y][x] == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
        Mario can jump over this thing if it's below the pipe
     */
    private boolean FlowerDanger(byte[][] levelScene) {
        int lowestY = 0;
        int flowerY = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = M_POS + 1; x < M_POS + 3; x++) {
                if (levelScene[y][x] == TILE.FLOWER_ENEMY) {
                    flowerY = flowerY == 0 ? y : Math.min(y, flowerY);
                }
                if (levelScene[y][x] == TILE.FLOWER_PIPE) {
                    lowestY = lowestY == 0 ? y : Math.min(y, lowestY);
                }
            }
        }
        //System.out.println("lowestY: " + lowestY + ", flowerY: " + flowerY);
        return lowestY > (flowerY - 1);
    }

    /*
        Mario can jump over this thing if it's not above him
     */
    private boolean IsJumpable(byte[][] levelScene, int type) {
        boolean above = false, below = false;
        for (int y = 0; y < SIZE; y++) {
            for (int x = M_POS + 1; x < M_POS + 3; x++) {
                if (levelScene[y][x] == type) {
                    if (y < M_POS) {
                        above = true;
                        System.out.print("y = " + y);
                    } else {
                        below = true;
                        System.out.print("y = " + y);
                    }
                }
            }
        }
        System.out.println("above: " + above + ", below: " + below);
        return !above && below;
    }

    private boolean DangerOfGap(byte[][] levelScene)
    {
        for (int x = M_POS - 2; x < M_POS + 2; ++x)
        {
            boolean f = true;
            for(int y = M_POS + 1; y < SIZE; ++y)
            {
                if  (levelScene[y][x] != 0)
                    f = false;
            }
            if (f && levelScene[M_POS + 1][M_POS] != 0)
                return true;
        }
        return false;
    }

    /*
        Set Mario's mode
     */
    private void SetMode(Environment observation) {
        // get Mario's mode
        /*if (MODE.values()[observation.getMarioMode()].compareTo(currentMode) < 0) {
            timesHit++;
        }*/
        currentMode = MODE.values()[observation.getMarioMode()];
    }

    private void PrintLevelScene(byte[][] levelScene) {
        System.out.println("");
        for (int ly = 0; ly < SIZE; ly++) {
            System.out.print(ly + "y == ");
            for (int lx = 0; lx < SIZE; lx++) {
                System.out.print(" " + lx + ":" + levelScene[ly][lx] + " ");
            }
            System.out.println("");
        }
    }
}

