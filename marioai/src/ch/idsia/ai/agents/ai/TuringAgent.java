package ch.idsia.ai.agents.ai;

import ch.idsia.ai.agents.Agent;
import ch.idsia.mario.engine.sprites.Sprite;
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
        public static final int FLOWER_PIPE = 20;

        public static final int HARD_TERRAIN = -10;
        public static final int SOFT_TERRAIN = -11;

        public static final int BRICK = 16;
        public static final int QBRICK = 21;
    }

    // count jump height
    private int trueJumpCounter = 0;

    // should Mario get the qbrick
    private boolean targetBrick = false;
    // position of the qbrick
    private int[] brickPos = new int[] {0, 0};

    // should Mario get the flower
    private boolean targetFlower = false;
    // position of the flower
    private int[] flowerPos = new int[] {0, 0};
    private int flowerJumpCounter = 0;

    // Mario's current mode
    private MODE currentMode = MODE.MODE_FIRE;
    private boolean fired = false;

    private static final int SHOT_COOL_DOWN = 20;
    private int coolDown = SHOT_COOL_DOWN;

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
        // set Mario's mode
        SetMode(observation);

        // default movement values
        action[Mario.KEY_RIGHT] = true;
        action[Mario.KEY_LEFT] = false;
        action[Mario.KEY_SPEED] = false;
        action[Mario.KEY_JUMP] = false;

        // TODO: account for spiky enemies (9), fix gap detection, fix fire flower targeting

        // check if it's time to go right
        TryRight(observation);

        // check if it's time to go left
        TryLeft(observation);

        // check if we should target the qbricks
        TryQBrick(observation);

        // check if it's time to jump at something
        TryJump(observation);

        // check if we should target the fire flower
        /*if (currentMode != MODE.MODE_FIRE) {
            TryFireFlower(observation);
        }*/

        // check if we should run or shoot
        TryFire(observation);

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
        if (IsBehind(levelScene, Sprite.KIND_ENEMY_FLOWER)) {
            // if there's a dangerous flower about
            if (FlowerDanger(levelScene)) {
                //System.out.println("mario will wait");
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
        if (IsAhead(levelScene, Sprite.KIND_GOOMBA)) {
            shouldLeft = true;
            //System.out.println("enemy behind us");
        }

        // don't head towards a greater number of enemies
        int ahead = EnemiesAhead(observation);
        int behind = EnemiesBehind(observation);
        if (ahead <= behind && behind >= 3) {
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
        Check if it's time to speed or fire
     */
    private void TryFire(Environment observation) {
        byte[][] enemies = observation.getEnemiesObservationZ(1);
        int[] easyEnemyPos = GetPos(enemies, Sprite.KIND_GOOMBA);
        int[] hardEnemyPos = GetPos(enemies, Sprite.KIND_SPIKY);
        boolean enemy = false;

        if (easyEnemyPos[0] != Sprite.KIND_NONE || easyEnemyPos[1] != Sprite.KIND_NONE
                || hardEnemyPos[0] != Sprite.KIND_NONE || hardEnemyPos[1] != Sprite.KIND_NONE) {
            enemy = true;
        }

        boolean trigger = Math.floor(Math.random() * 5) == 0;

        if (trigger && enemy && !fired && currentMode == MODE.MODE_FIRE) {
            action[Mario.KEY_SPEED] = true;
            fired = true;
            //coolDown--;
        }
        else if (trigger && !enemy && currentMode == MODE.MODE_LARGE) {
            action[Mario.KEY_SPEED] = true;
            //coolDown = SHOT_COOL_DOWN;
        } else {
            fired = false;
            //coolDown = SHOT_COOL_DOWN;
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
        if (right && (DangerRight(observation) || levelScene[M_POS][M_POS + 2] != Sprite.KIND_NONE || levelScene[M_POS][M_POS + 1] != Sprite.KIND_NONE)) {
            //System.out.print(levelScene[M_POS][M_POS + 2] + " and ");
            //System.out.println(levelScene[M_POS][M_POS + 1]);
            shouldJump = true;
        }

        // if there's something in the way to the left
        if (left && (levelScene[M_POS][M_POS - 2] != Sprite.KIND_NONE || levelScene[M_POS][M_POS - 1] != Sprite.KIND_NONE)) {
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

        // if we're under soft terrain
        // 33% chance
        if (IsUnder(levelScene, TILE.SOFT_TERRAIN)
                && Math.floor(Math.random() * 3) == 0) {
            shouldJump = true;
        }

        // try to destroy all bricks
        if (IsUnder(levelScene, TILE.BRICK)) {
            shouldJump = true;
        }

        // random jumps
        // 3% chance
        if (IsUnder(levelScene, Sprite.KIND_NONE) && Math.floor(Math.random() * 33) == 0) {
            shouldJump = true;
        }

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
        Try to get qbrick
     */
    private void TryQBrick(Environment observation) {
        // handle the appearance of the qbrick
        int[] newBrickPos = GetPos(observation.getLevelSceneObservation(), TILE.QBRICK);

        // if there's no qbrick
        if (newBrickPos[0] == Sprite.KIND_NONE && newBrickPos[1] == Sprite.KIND_NONE) {
            brickPos = newBrickPos;
            targetBrick = false;
            return;
        }

        // 80% chance of targeting qbrick when it first appears
        if (brickPos[0] == Sprite.KIND_NONE && brickPos[1] == Sprite.KIND_NONE) {
            //targetBrick = Math.floor(Math.random() * 1.2) == 0;
            targetBrick = true;
            brickPos = newBrickPos;
        }
        // otherwise, update stored position of qbrick
        else {
            brickPos = newBrickPos;
        }

        // target the qbrick
        if (targetBrick) {
            // if it's under Mario
            if (brickPos[0] > M_POS) {
                action[Mario.KEY_RIGHT] = true;
                action[Mario.KEY_LEFT] = false;
                System.out.println("qbrick under Mario");
            }
            // if it's over Mario
            else if (brickPos[0] < M_POS && brickPos[1] == M_POS) {
                if (observation.mayMarioJump()) {
                    action[Mario.KEY_JUMP] = true;
                }
                System.out.println("qbrick over Mario");
            }
            // if it's on the same level
            else {
                // if it's to the left
                if (brickPos[1] < M_POS){
                    action[Mario.KEY_LEFT] = true;
                    action[Mario.KEY_RIGHT] = false;
                    action[Mario.KEY_JUMP] = false;
                    System.out.println("qbrick to the left");
                }
                // if it's to the right
                else if (brickPos[1] > M_POS) {
                    action[Mario.KEY_LEFT] = false;
                    action[Mario.KEY_RIGHT] = true;
                    action[Mario.KEY_JUMP] = false;
                    System.out.println("qbrick to the right");
                }
            }
        }
    }

    /*
        Try to get fire flower
     */
    private void TryFireFlower(Environment observation) {
        // handle the appearance of the fire flower
        int[] newFlowerPos = GetPos(observation.getEnemiesObservationZ(0), Sprite.KIND_FIRE_FLOWER);
        int[] oldFlowerPos = flowerPos;

        // if there's no fire flower
        if (newFlowerPos[0] == Sprite.KIND_NONE && newFlowerPos[1] == Sprite.KIND_NONE) {
            flowerPos = newFlowerPos;
            targetFlower = false;
            return;
        }

        // 80% chance of targeting fire flower when it first appears
        if (flowerPos[0] == Sprite.KIND_NONE && flowerPos[1] == Sprite.KIND_NONE) {
            //targetFlower = Math.floor(Math.random() * 1.2) == 0;
            targetFlower = true;
            flowerPos = newFlowerPos;
        }
        // otherwise, update stored position of fire flower
        else {
            flowerPos = newFlowerPos;
        }

        // target the fire flower
        if (targetFlower) {
            System.out.println("Fire flower y: " + flowerPos[0] + ", x: " + flowerPos[1]);

            boolean xCloser = Math.abs(M_POS - flowerPos[1]) < Math.abs(M_POS - oldFlowerPos[1]);

            byte[][] levelScene = observation.getLevelSceneObservation();

            boolean left = flowerPos[1] < M_POS;
            boolean right = flowerPos[1] > M_POS;
            boolean jump = false;

            // if Mario is jumping & can't make it onto something
            if (!observation.isMarioOnGround() && !xCloser && Math.abs(oldFlowerPos[1] - newFlowerPos[1]) < 2) {
                jump = true;
                action[Mario.KEY_LEFT] = left;
                action[Mario.KEY_RIGHT] = right;
                System.out.println("haven't gotten closer");
            }
            // if Mario is under a brick or the flower, go right
            if ((IsUnder(levelScene, TILE.BRICK) || IsUnder(levelScene, TILE.QBRICK)
                    || IsUnder(levelScene, Sprite.KIND_FIRE_FLOWER) || IsCloseUnder(levelScene, TILE.HARD_TERRAIN))
                    && observation.isMarioOnGround()) {
                action[Mario.KEY_RIGHT] = true;
                action[Mario.KEY_LEFT] = false;
                action[Mario.KEY_SPEED] = false;
                //jump = true;
                System.out.println("brick or flower above mario");
            }
            // if it's not on the same y
            else if (flowerPos[0] < M_POS) {
                jump = true;
                action[Mario.KEY_LEFT] = left;
                action[Mario.KEY_RIGHT] = right;
                System.out.println("fire flower is not on mario's level, x is " + flowerPos[1]);
            } else {
                action[Mario.KEY_LEFT] = left;
                action[Mario.KEY_RIGHT] = right;
                action[Mario.KEY_SPEED] = true;
                jump = false;
                System.out.println("else case");
            }

            // handle jumping for flower
            if (jump && (observation.mayMarioJump() || flowerJumpCounter < 21)) {
                action[Mario.KEY_JUMP] = true;
                flowerJumpCounter++;
            } else {
                action[Mario.KEY_JUMP] = false;
                flowerJumpCounter = 0;
            }
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
        He's close if it's within 3 of his y
     */
    private boolean IsCloseUnder(byte[][] levelScene, int type) {
        for (int y = M_POS - 3; y < M_POS; y++) {
            if (levelScene[y][M_POS] == type || levelScene[y][M_POS - 1] == type || levelScene[y][M_POS + 1] == type) {
                return true;
            }
        }
        return false;
    }

    /*
        Mario is under something if it's within 1 of his x position
     */
    private boolean IsUnder(byte[][] levelScene, int type) {
        for (int y = 0; y < M_POS; y++) {
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
            if (levelScene[y][M_POS] == type || levelScene[y][M_POS - 1] == type || levelScene[y][M_POS + 1] == type) {
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
                if (levelScene[y][x] == Sprite.KIND_ENEMY_FLOWER) {
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
        Get the rightmost position of a given tile, or 0 x 0
     */
    private int[] GetPos(byte[][] levelScene, int type) {
        int[] pos = new int[] {0, 0};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (levelScene[y][x] == type) {
                    if ((pos[1] == 0 && pos[0] == 0) || x > pos[1]) {
                        pos[0] = y;
                        pos[1] = x;
                        //System.out.println("y: " + y + ", x: " + x);
                        //return pos;
                    }
                }
            }
        }
        return pos;
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
                        //System.out.print("y = " + y);
                    } else {
                        below = true;
                        //System.out.print("y = " + y);
                    }
                }
            }
        }
        //System.out.println("above: " + above + ", below: " + below);
        return !above && below;
    }

    /*
        Checks if Mario is in danger of falling into a gap
    */
    private boolean DangerOfGap(byte[][] levelScene) {
        for (int x = M_POS - 2; x <= M_POS + 2; ++x) {
            boolean f = true;
            for (int y = M_POS + 1; y < SIZE; ++y) {
                if  (levelScene[y][x] != Sprite.KIND_NONE) {
                    f = false;
                }
            }
            if (f && levelScene[M_POS + 1][M_POS] != Sprite.KIND_NONE) {
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

    /*
        Prints level scene for debugging purposes
     */
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

