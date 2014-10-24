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
        public static final int HARD_ENEMY = 9;

        public static final int FLOWER_ENEMY = 12;
        public static final int FLOWER_PIPE = 20;

        public static final int HARD_TERRAIN = -10;
        public static final int SOFT_TERRAIN = -11;

        public static final int BRICK = 16;
        public static final int QBRICK = 21;

        public static final int FIRE_FLOWER = 15;
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

        // TODO: add firing, account for spiky enemies (9), go left sometimes, break more bricks
        // TODO: STRETCH GOALS: fix gap detection

        // check if it's time to go right
        TryRight(observation);

        // check if it's time to go left
        TryLeft(observation);

        // check if it's time to jump at something
        TryJump(observation);

        return action;
    }

    /*
        Check if it's time to go right
     */
    private void TryRight(Environment observation) {
        byte[][] levelScene = observation.getCompleteObservation();
        //byte[][] enemies = observation.getEnemiesObservation();

        boolean shouldRight = action[Mario.KEY_RIGHT];

        // if there's a flower enemy ahead
        if (IsBehind(levelScene, TILE.FLOWER_ENEMY)) {
            /*if (currentMode == MODE.MODE_FIRE) {
                action[Mario.KEY_SPEED] = false;
                action[Mario.KEY_SPEED] = true;
            }*/
            // if there's a dangerous flower about
            if (FlowerDanger(levelScene)) {
                System.out.println("mario will wait");
                shouldRight = false;
            }
        }

        action[Mario.KEY_RIGHT] = shouldRight;
    }

    /*
        Check if it's time to go left
     */
    private void TryLeft(Environment observation) {
        byte[][] levelScene = observation.getCompleteObservation();

        boolean shouldLeft = action[Mario.KEY_LEFT];

        // if we're ahead of an enemy
        if (IsAhead(levelScene, TILE.ENEMY)) {
            shouldLeft = true;
            System.out.println("enemy behind us");
        }

        // don't head towards a greater number of enemies
        if (EnemiesAhead(observation) <= EnemiesBehind(observation)) {
            shouldLeft = false;
        }

        // if it's dangerous above and to the right
        if (DangerAbove(observation) && DangerRight(observation)) {
            shouldLeft = true;
        }

        // actually send Mario left
        if (shouldLeft) {
            action[Mario.KEY_RIGHT] = false;
            action[Mario.KEY_LEFT] = shouldLeft;
        }
    }

    /*
        Check if it's time to jump
     */
    private void TryJump(Environment observation) {
        byte[][] levelScene = observation.getCompleteObservation();
        boolean right = action[Mario.KEY_RIGHT];
        boolean left = action[Mario.KEY_LEFT];

        boolean shouldJump = action[Mario.KEY_JUMP];

        // if there's something in the way to the right
        if (right && (DangerRight(observation) || levelScene[M_POS][M_POS + 2] != TILE.NONE || levelScene[M_POS][M_POS + 1] != TILE.NONE)) {
            System.out.print(levelScene[M_POS][M_POS + 2] + " and ");
            System.out.println(levelScene[M_POS][M_POS + 1]);
            shouldJump = true;
        }

        // if there's something in the way to the left
        if (left && (levelScene[M_POS][M_POS - 2] != TILE.NONE || levelScene[M_POS][M_POS - 1] != TILE.NONE)) {
            shouldJump = true;
        }

        // if there's a gap
        if ((left || right) && DangerOfGap(levelScene)) {
            shouldJump = true;
        }

        // if we're under qbrick
        if (IsDirectlyUnder(levelScene, TILE.QBRICK)) {
            shouldJump = true;
        }

        // if we're under brick, or under soft terrain
        // based on chance

        // if there's danger above
        if (DangerAbove(observation)) {
            shouldJump = false;
        }

        // not jumping, reset jump counter
        if (!shouldJump || trueJumpCounter > 16) {
            shouldJump = false;
            trueJumpCounter = 0;
        }

        // actually jump finally
        if (shouldJump) {
            if (observation.mayMarioJump() || !observation.isMarioOnGround()) {
                action[Mario.KEY_JUMP] = true;
            }
            ++trueJumpCounter;
        }
    }

    /*
        Number of enemies behind
     */
    private int EnemiesBehind(Environment observation) {
        byte[][] enemies = observation.getEnemiesObservationZ(2);

        int numEnemies = 0;
        for (int y = 0; y < SIZE; ++y) {
            for (int x = 0; x < M_POS; ++x) {
                if (enemies[y][x] == 1) {
                    numEnemies++;
                }
            }
        }

        return numEnemies;
    }

    /*
        Number of enemies ahead
     */
    private int EnemiesAhead(Environment observation) {
        byte[][] enemies = observation.getEnemiesObservationZ(2);

        int numEnemies = 0;

        for (int y = 0; y < SIZE; ++y) {
            for (int x = M_POS; x < SIZE; ++x) {
                if (enemies[y][x] == 1) {
                    numEnemies++;
                }
            }
        }

        return numEnemies;
    }

    /*
        Mario is under something if it's within 1 of his x position
     */
    private boolean IsUnder(byte[][] levelScene, int type) {
        for (int y = 0; y < M_POS - 1; y++) {
            if (levelScene[y][M_POS] == type || levelScene[y][M_POS - 1] == type || levelScene[y][M_POS + 1] == type) {
                return true;
            }
        }
        return false;
    }

    /*
        Mario is directly under something if it shares his x position
     */
    private boolean IsDirectlyUnder(byte[][] levelScene, int type) {
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
        Mario should watch the skies
     */
    private boolean DangerAbove(Environment observation) {
        byte[][] enemies = observation.getEnemiesObservationZ(2);
        return IsUnder(enemies, 1);
    }

    /*
        Mario should keep his eyes front
     */
    private boolean DangerRight(Environment observation) {
        byte[][] enemies = observation.getEnemiesObservationZ(2);
        for (int y = M_POS - 3; y < M_POS + 3; y++) {
            for (int x = M_POS + 1; x < M_POS + 3; x++) {
                if (enemies[y][x] == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
        Mario should watch his back
     */
    private boolean DangerLeft(Environment observation) {
        byte[][] enemies = observation.getEnemiesObservationZ(2);
        for (int y = M_POS - 3; y < M_POS + 3; y++) {
            for (int x = M_POS - 3; x < M_POS; x++) {
                if (enemies[y][x] == 1) {
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

    private boolean DangerOfGap(byte[][] levelScene) {
        for (int x = M_POS - 2; x <= M_POS + 2; ++x) {
            boolean f = true;
            for (int y = M_POS + 1; y < SIZE; ++y) {
                if  (levelScene[y][x] != TILE.NONE) {
                    f = false;
                }
            }
            if (f && levelScene[M_POS + 1][M_POS] != TILE.NONE) {
                return true;
            }
        }
        return false;
    }

    /*
        Set Mario's mode
     */
    private void SetMode(Environment observation) {
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

