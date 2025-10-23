package engine;

import screen.TitleScreen;

/**
 * Manages the position and trajectory updates for CelestialBody objects.
 * This is a stateless manager, the logic is centralized here.
 */
public class CelestialManager {

    public CelestialManager() {
        // Constructor is empty as this is a stateless manager.
    }

    public void update(CelestialBody body, 
                       StarSpeedManager speedManager, 
                       StarOriginManager originManager, 
                       int screenWidth, 
                       int screenHeight, 
                       float globalSpeedMultiplier) {
        
        float currentOriginX = originManager.getCurrentOriginX();
        float currentOriginY = originManager.getCurrentOriginY();
        final int screenCenterX = screenWidth / 2;
        final int screenCenterY = screenHeight / 2;

        // Calculate dynamic approach speed using the pre-calculated globalSpeedMultiplier
        float current_approach_speed = body.speed * (1.0f - body.z / TitleScreen.MAX_STAR_Z) * 2.0f * globalSpeedMultiplier;
        if (current_approach_speed < 0.1f) current_approach_speed = 0.1f;

        // Dynamically update trail length
        // Note: This will require MAX_TRAIL_LENGTH in TitleScreen to be public.
        body.trail_length = (int) (current_approach_speed * 1.0f);
        if (body.trail_length < 1) body.trail_length = 1;
        if (body.trail_length > 10) body.trail_length = 10; // Hardcoded for now.

        // Move body closer
        body.z -= current_approach_speed;

        // If body passes the viewer, reset it.
        // Note: This will require MIN_STAR_Z in TitleScreen to be public.
        if (body.z <= 0.1f) { // Hardcoded for now.
            body.z = TitleScreen.MAX_STAR_Z - (0.1f - body.z);
        }

        // Calculate scale factor
        float scale_factor = (1.0f - body.z / TitleScreen.MAX_STAR_Z);

        // Calculate screen position
        body.current_screen_x = screenCenterX + body.initial_screen_x_offset * scale_factor;
        body.current_screen_y = screenCenterY + body.initial_screen_y_offset * scale_factor;

        // Apply bending offset
        body.current_screen_x += (currentOriginX - screenCenterX) * (1.0f - scale_factor);
        body.current_screen_y += (currentOriginY - screenCenterY) * (1.0f - scale_factor);

        // Update trail history
        body.trail.add(new java.awt.geom.Point2D.Float(body.current_screen_x, body.current_screen_y));
        if (body.trail.size() > body.trail_length) {
            body.trail.remove(0);
        }
    }
}
