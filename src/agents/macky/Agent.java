package agents.macky;

import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import java.util.Random;

public class Agent implements MarioAgent {
    // Jump state for walls/gaps
    private enum JumpType {
        ENEMY, GAP, WALL, NONE
    }
    private JumpType jumpType = JumpType.NONE;
    private int jumpCount = 0, jumpSize = -1;
    private boolean[] action;

    private enum State {
        NEUTRAL, STOPPED, HANDLE_ENEMY, HANDLE_WALL, HANDLE_GAP
    }

    private State currentState;
    private Random random = new Random();

    private int enemyBehavior = -1;
    private long enemyStartTime;
    private boolean enemyJumped, enemyJumpTriggered = false;
    private int retreatDuration = 0;
    private long postJumpStartTime;

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        currentState = State.NEUTRAL;
        action = new boolean[5];
        action[1] = true; // right
        action[3] = true; // speed/run
        jumpType = JumpType.NONE;
        jumpCount = 0;
        jumpSize = -1;
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        switch (currentState) {
            case HANDLE_ENEMY:
                return handleEnemyState(model, timer);
            default:
                return handlePlatformerActions(model, timer);
        }
    }

    // Logic for walls/gaps, inspired by trondEllingsen.Agent
    private boolean[] handlePlatformerActions(MarioForwardModel model, MarioTimer timer) {
        // Enemy detection and state switch
        State obstacleState = this.detectObstacle(model);
        if (obstacleState == State.HANDLE_ENEMY) {
            currentState = obstacleState;
            return new boolean[5];
        }

        // Wall/gap/jump logic
        final float marioSpeed = model.getMarioFloatVelocity()[0];
        final boolean dangerOfGap = dangerOfGap(model);
        final int[] marioTile = model.getMarioScreenTilePos();
        final int wallHeight = getWallHeight(marioTile[0], marioTile[1], model.getScreenSceneObservation());
        final boolean onGround = model.isMarioOnGround();
        final boolean canJump = model.mayMarioJump();

        if ((onGround || canJump) && !jumpType.equals(JumpType.NONE)) {
            jumpType = JumpType.NONE;
            jumpSize = -1;
        } else if (canJump) {
            int jumpError = random.nextInt(4) - 1; // -1, 0, 1, or 2
            if (dangerOfGap && marioSpeed > 0) {
                jumpType = JumpType.GAP;
                jumpSize = (marioSpeed < 6 ? (int) (9 - marioSpeed) : 1) + jumpError;
                if (jumpSize < 1) jumpSize = 1;
                jumpCount = 0;
            } else if (marioSpeed <= 1 && wallHeight > 0) {
                jumpType = JumpType.WALL;
                jumpSize = (wallHeight >= 4 ? wallHeight + 3 : wallHeight) + jumpError;
                if (jumpSize < 1) jumpSize = 1;
                jumpCount = 0;
            }
        } else if (!jumpType.equals(JumpType.NONE)) {
            jumpCount++;
        }

        // Action array construction
        boolean[] act = new boolean[5];
        act[1] = true; // right
        act[3] = true; // speed/run
        act[4] = !jumpType.equals(JumpType.NONE) && jumpCount < jumpSize;
        return act;
    }

    // Helper: wall height calculation
    private int getWallHeight(int tileX, int tileY, int[][] levelScene) {
        int y = tileY + 1, wallHeight = 0;
        while (y-- > 0 && levelScene[tileX + 1][y] != 0) {
            wallHeight++;
        }
        return wallHeight;
    }

    // Helper: gap detection
    private boolean dangerOfGap(MarioForwardModel model) {
        int[] marioTile = model.getMarioScreenTilePos();
        int[][] levelScene = model.getScreenSceneObservation();
        for (int y = marioTile[1] + 1; y < levelScene[0].length; y++) {
            if (levelScene[marioTile[0] + 1][y] != 0) {
                return false;
            }
        }
        return true;
    }

    // Enemy handling logic with randomized strategies
    private boolean[] handleEnemyState(MarioForwardModel model, MarioTimer timer) {
        long now = System.currentTimeMillis();

        // Initialize enemy behavior if not set
        if (enemyBehavior == -1) {
            enemyBehavior = random.nextInt(3); // 0, 1, or 2
            enemyStartTime = now;
            enemyJumped = false;
        }

        switch (enemyBehavior) {
            case 0: // Walk + jump on Enemy
                long elapsedJump = System.currentTimeMillis() - enemyStartTime;

                if (!enemyJumped) {
                    enemyJumped = true;
                    enemyStartTime = System.currentTimeMillis();
                    return new boolean[]{true, false, false, true, true}; // jump left
                } else if (elapsedJump < 150) {
                    return new boolean[]{true, false, false, true, true}; // keep jumping left
                } else if (elapsedJump < 320) {
                    return new boolean[]{false, true, false, false, false}; // keep jumping right
                } else {
                    resetEnemyState();
                    return new boolean[]{false, true, false, false, false}; // resume walking right
                }

            case 1:
                long elapsedRetreat = System.currentTimeMillis() - enemyStartTime;

                if (!enemyJumped) {
                    enemyJumped = true;
                    enemyStartTime = System.currentTimeMillis();
                    retreatDuration = 300; // 300–370ms
                    return new boolean[]{true, false, false, true, false}; // start sprinting back
                } else if (elapsedRetreat < retreatDuration) {
                    return new boolean[]{true, false, false, true, false}; // keep sprinting back
                } else if ((isEnemyClose(model) || elapsedRetreat > retreatDuration + 1800) && !enemyJumpTriggered) {
                    enemyJumpTriggered = true;
                    postJumpStartTime = System.currentTimeMillis();
                    return new boolean[]{false, false, false, false, true}; // jump in place
                } else if (enemyJumpTriggered && System.currentTimeMillis() - postJumpStartTime > 1000) {
                    resetEnemyState();
                    return new boolean[]{false, true, false, true, false}; // resume running
                } else if (elapsedRetreat > retreatDuration + 1800) {
                    resetEnemyState();
                    return new boolean[]{false, true, false, true, false}; // resume running
                } else {
                    return new boolean[]{false, false, false, false, false}; // wait
                }

            case 2:
                long elapsedBackJump = System.currentTimeMillis() - enemyStartTime;

                // Jump left
                if (!enemyJumped) {
                    enemyJumped = true;
                    enemyStartTime = System.currentTimeMillis();
                    return new boolean[]{true, false, false, false, true}; // jump left
                } else if (elapsedBackJump < 750 + 100) {
                    return new boolean[]{true, false, false, false, true}; // keep jumping left
                }

                // Pause
                else if (elapsedBackJump < 1200 + 100) {
                    return new boolean[]{false, false, false, false, false}; // wait
                }

                // Begin forward jump
                else if (!enemyJumpTriggered) {
                    enemyJumpTriggered = true;
                    return new boolean[]{false, true, false, false, true}; // jump right
                } else if (elapsedBackJump < 1400) {
                    return new boolean[]{false, true, false, false, true}; // keep jumping right
                }

                // Resume neutral
                else {
                    resetEnemyState();
                    return new boolean[]{false, true, false, false, false}; // walk right
                }

            default:
                resetEnemyState();
                return new boolean[]{false, true, false, false, false}; // fallback
        }
    }

    // Helper: check if enemy is close
    private boolean isEnemyClose(MarioForwardModel model) {
        float[] marioPos = model.getMarioFloatPos();
        float marioX = marioPos[0];
        float marioY = marioPos[1];

        float[] enemies = model.getEnemiesFloatPos();
        for (int i = 0; i < enemies.length - 2; i += 3) {
            float type = enemies[i];
            float ex = enemies[i + 1];
            float ey = enemies[i + 2];

            float dx = ex - marioX;
            float dy = ey - marioY;

            if (type == 2.0f && dx > -18 && dx < 18 && Math.abs(dy) < 16) {
                return true;
            }
        }
        return false;
    }

    // Helper: reset enemy state
    private void resetEnemyState() {
        currentState = State.NEUTRAL;
        enemyBehavior = -1;
        enemyJumped = false;
        enemyJumpTriggered = false;
    }

    // Detect obstacles (walls, gaps, enemies) ahead of Mario
    private State detectObstacle(MarioForwardModel model) {
        int[][] scene = model.getMarioSceneObservation(1);
        int centerX = scene.length / 2;
        int centerY = scene[0].length / 2;
        float[] marioPos = model.getMarioFloatPos();
        float marioX = marioPos[0];
        float marioY = marioPos[1];

        // Check for solid tiles, pipes, bricks, or other blocks ahead (not enemies)
        boolean wallAhead = false;
        for (int dx = 1; dx <= 3; dx++) {
            int tile = scene[centerX + dx][centerY];
            if (tile == MarioForwardModel.OBS_PIPE ||
                tile == MarioForwardModel.OBS_SOLID ||
                tile == MarioForwardModel.OBS_BRICK ||
                tile == MarioForwardModel.OBS_QUESTION_BLOCK ||
                tile == MarioForwardModel.OBS_PLATFORM ||
                tile == MarioForwardModel.OBS_PLATFORM_SINGLE ||
                tile == MarioForwardModel.OBS_PLATFORM_LEFT ||
                tile == MarioForwardModel.OBS_PLATFORM_RIGHT ||
                tile == MarioForwardModel.OBS_PLATFORM_CENTER) {
                wallAhead = true;
            }
        }
        if (wallAhead) {
            return State.HANDLE_WALL;
        }

        /* We're ignoring question blocks */
        // // Check for ? block overhead
        // int overhead = scene[centerX + 1][centerY - 1];
        // if (overhead == MarioForwardModel.OBS_QUESTION_BLOCK) {
        //     return State.STOPPED;
        // }

        // Check for enemies within a danger zone
        float[] enemies = model.getEnemiesFloatPos();
        for (int i = 0; i < enemies.length - 2; i += 3) {
            float type = enemies[i];
            float ex = enemies[i + 1];
            float ey = enemies[i + 2];

            float dx = ex - marioX;
            float dy = ey - marioY;

            // Generalize: handle all enemies (Goomba, Koopa, Spiky, etc.)
            if (type >= 2.0f && type <= 11.0f && dx > 0 && dx < 70 && Math.abs(dy) < 16) {
                return State.HANDLE_ENEMY;
            }
        }

        return null; // No obstacle detected
    }

    @Override
    public String getAgentName() {
        return "mackyAgent";
    }
}