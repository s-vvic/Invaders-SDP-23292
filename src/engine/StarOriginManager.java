package engine;

public class StarOriginManager {
    private static final float OSCILLATION_RADIUS = 150.0f; // How far the origin moves from the center
    private static final float OSCILLATION_SPEED = 0.001f; // How fast the origin oscillates

    private float currentOriginX;
    private float currentOriginY;
    private long startTime;
    private int screenCenterX;
    private int screenCenterY;

    public StarOriginManager(int screenWidth, int screenHeight) {
        this.screenCenterX = screenWidth / 2;
        this.screenCenterY = screenHeight / 2;
        this.currentOriginX = screenCenterX;
        this.currentOriginY = screenCenterY;
        this.startTime = System.currentTimeMillis();
    }

    public void updateOrigin() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        // Oscillate origin around the screen center
        currentOriginX = screenCenterX + (float) (Math.sin(elapsedTime * OSCILLATION_SPEED) * OSCILLATION_RADIUS);
        currentOriginY = screenCenterY + (float) (Math.cos(elapsedTime * OSCILLATION_SPEED * 0.7f) * OSCILLATION_RADIUS);
    }

    public float getCurrentOriginX() {
        return currentOriginX;
    }

    public float getCurrentOriginY() {
        return currentOriginY;
    }
}
