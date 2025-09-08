package agents.macky;

import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import java.util.Random;

public class Agent implements MarioAgent {

    private enum State {
        NEUTRAL,
        STOPPED,
        HANDLE_GOOMBA,
        HANDLE_GAP,
        HANDLE_WALL
    }

    private State currentState = State.NEUTRAL;
    private long neutralStartTime;
    private boolean isRunning = false;
    private Random random = new Random();
    
    private int goombaBehavior = -1;
    private long goombaStartTime;
    private boolean goombaJumped = false;
    private int retreatDuration = 0;
    private boolean goombaJumpTriggered = false;
    private long postJumpStartTime;

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        currentState = State.NEUTRAL;
        neutralStartTime = System.currentTimeMillis();
        isRunning = false;
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        switch (currentState) {
            case NEUTRAL:
                return handleNeutralState(model, timer);
            case STOPPED:
                return new boolean[]{true, false, false, true, true};
            case HANDLE_GOOMBA:
                return handleGoombaState(model, timer);
            default:
                return new boolean[5];
        }
    }

    private boolean[] handleNeutralState(MarioForwardModel model, MarioTimer timer) {
        long elapsed = System.currentTimeMillis() - neutralStartTime;

        if (!isRunning && elapsed > 400 + random.nextInt(600)) {
            isRunning = true;
        }

        State obstacleState = detectObstacle(model);
        if (obstacleState == State.HANDLE_GOOMBA) { // Change to null
            currentState = obstacleState;
            return new boolean[5];
        }

        return new boolean[]{
            false, // left
            true,  // right
            false, // down
            isRunning, // run
            false  // jump
        };
    } 

    private boolean[] handleGoombaState(MarioForwardModel model, MarioTimer timer) {
        long now = System.currentTimeMillis();

        // Initialize behavior if not set
        if (goombaBehavior == -1) {
            goombaBehavior = random.nextInt(3); // 0, 1, or 2
            goombaStartTime = now;
            goombaJumped = false;
        }

        long elapsed = now - goombaStartTime;
        switch (goombaBehavior) {
            case 0: // Walk + jump on Goomba
                long elapsedJump = System.currentTimeMillis() - goombaStartTime;

                if (!goombaJumped) {
                    goombaJumped = true;
                    goombaStartTime = System.currentTimeMillis();
                    return new boolean[]{true, false, false, true, true}; // jump left
                } else if (elapsedJump < 150) {
                    return new boolean[]{true, false, false, true, true}; // keep jumping left
                } else if (elapsedJump < 320) {
                    return new boolean[]{false, true, false, false, false}; // keep jumping right
                } else {
                    resetGoombaState();
                    return new boolean[]{false, true, false, false, false}; // resume walking right
                }

            case 1:
                long elapsedRetreat = System.currentTimeMillis() - goombaStartTime;

                if (!goombaJumped) {
                    goombaJumped = true;
                    goombaStartTime = System.currentTimeMillis();
                    retreatDuration = 300; // 300–370ms
                    return new boolean[]{true, false, false, true, false}; // start sprinting back
                } else if (elapsedRetreat < retreatDuration) {
                    return new boolean[]{true, false, false, true, false}; // keep sprinting back
                } else if ((isGoombaClose(model) || elapsedRetreat > retreatDuration + 1800) && !goombaJumpTriggered) {
                    goombaJumpTriggered = true;
                    postJumpStartTime = System.currentTimeMillis();
                    return new boolean[]{false, false, false, false, true}; // jump in place
                } else if (goombaJumpTriggered && System.currentTimeMillis() - postJumpStartTime > 1000) {
                    resetGoombaState();
                    return new boolean[]{false, true, false, true, false}; // resume running
                } else if (elapsedRetreat > retreatDuration + 1800) {
                    resetGoombaState();
                    return new boolean[]{false, true, false, true, false}; // resume running
                } else {
                    return new boolean[]{false, false, false, false, false}; // wait
                }

            case 2:
                long elapsedBackJump = System.currentTimeMillis() - goombaStartTime;

                // Jump left
                if (!goombaJumped) {
                    goombaJumped = true;
                    goombaStartTime = System.currentTimeMillis();
                    return new boolean[]{true, false, false, false, true}; // jump left
                } else if (elapsedBackJump < 750 + 100) {
                    return new boolean[]{true, false, false, false, true}; // keep jumping left
                }

                // Pause
                else if (elapsedBackJump < 1200 + 100) {
                    return new boolean[]{false, false, false, false, false}; // wait
                }

                // Begin forward jump
                else if (!goombaJumpTriggered) {
                    goombaJumpTriggered = true;
                    return new boolean[]{false, true, false, false, true}; // jump right
                } else if (elapsedBackJump < 1400) {
                    return new boolean[]{false, true, false, false, true}; // keep jumping right
                }

                // Resume neutral
                else {
                    resetGoombaState();
                    return new boolean[]{false, true, false, false, false}; // walk right
                }

            default:
                resetGoombaState();
                return new boolean[]{false, true, false, false, false}; // fallback
        }
    }

    private boolean isGoombaClose(MarioForwardModel model) {
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

    private void resetGoombaState() {
        currentState = State.NEUTRAL;
        neutralStartTime = System.currentTimeMillis();
        isRunning = false;
        goombaBehavior = -1;
        goombaJumped = false;
    }

    private State detectObstacle(MarioForwardModel model) {
        int[][] scene = model.getMarioSceneObservation(1);
        int centerX = scene.length / 2;
        int centerY = scene[0].length / 2;
        float[] marioPos = model.getMarioFloatPos();
        float marioX = marioPos[0];
        float marioY = marioPos[1];

        // Check for solid tiles, pipes, or ? blocks ahead
        for (int dx = 1; dx <= 3; dx++) {
            int tile = scene[centerX + dx][centerY];
            if (tile == MarioForwardModel.OBS_PIPE ||
                tile == MarioForwardModel.OBS_SOLID) { 
                return State.STOPPED; // change this <---
            }
        }

        // Check for ? block overhead
        int overhead = scene[centerX + 1][centerY - 1];
        if (overhead == MarioForwardModel.OBS_QUESTION_BLOCK) {
            return State.STOPPED;
        }

        // Check for enemies within a danger zone
        float[] enemies = model.getEnemiesFloatPos();
        for (int i = 0; i < enemies.length - 2; i += 3) {
            float type = enemies[i];
            float ex = enemies[i + 1];
            float ey = enemies[i + 2];

            float dx = ex - marioX;
            float dy = ey - marioY;

            if (dx > 0 && dx < 70 && Math.abs(dy) < 16) {
                if (type == 2.0f) return State.HANDLE_GOOMBA;
                if (type == 3.0f) return State.STOPPED; // placeholder for Koopas
            }
        }

        return null; // No obstacle detected
    }

    @Override
    public String getAgentName() {
        return "mackyAgent";
    }
}