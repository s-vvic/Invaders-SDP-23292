package engine;

public class StarSpeedManager {
    private static final float MIN_GLOBAL_SPEED_MULTIPLIER = 1.0f; // Min speed boost
    private static final float MAX_GLOBAL_SPEED_MULTIPLIER = 6.0f; // Max speed boost
    private static final float SPEED_CHANGE_RATE = 0.00001f; // How fast speed changes per update
    private static final long SPEED_CYCLE_INTERVAL = 5000; // Time in ms for one speed phase (e.g., 10 seconds)

    private float currentGlobalSpeedMultiplier;
    private long lastSpeedChangeTime;
    private boolean speedingUp; // True if currently speeding up, false if slowing down

            public StarSpeedManager() {
                this.currentGlobalSpeedMultiplier = MIN_GLOBAL_SPEED_MULTIPLIER; // Start at slowest speed
                this.lastSpeedChangeTime = System.currentTimeMillis();
                this.speedingUp = true; // Start speeding up
            }
    public float updateAndGetGlobalSpeedMultiplier() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpeedChangeTime > SPEED_CYCLE_INTERVAL) {
            // Change direction of speed adjustment
            speedingUp = !speedingUp;
            lastSpeedChangeTime = currentTime;
        }

        if (speedingUp) {
            currentGlobalSpeedMultiplier += SPEED_CHANGE_RATE;
            if (currentGlobalSpeedMultiplier > MAX_GLOBAL_SPEED_MULTIPLIER) {
                currentGlobalSpeedMultiplier = MAX_GLOBAL_SPEED_MULTIPLIER;
                speedingUp = false; // Immediately start slowing down if max reached
                lastSpeedChangeTime = currentTime; // Reset timer for next phase
            }
        } else {
            currentGlobalSpeedMultiplier -= SPEED_CHANGE_RATE;
            if (currentGlobalSpeedMultiplier < MIN_GLOBAL_SPEED_MULTIPLIER) {
                currentGlobalSpeedMultiplier = MIN_GLOBAL_SPEED_MULTIPLIER;
                speedingUp = true; // Immediately start speeding up if min reached
                lastSpeedChangeTime = currentTime;
            }
        }
        return currentGlobalSpeedMultiplier;
    }
}
