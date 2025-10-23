package engine;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the data for a body moving in the starfield.
 * Contains all data related to position, trajectory, and trails.
 */
public class CelestialBody {

    public float z; // Depth (0 = close, MAX_Z = far)
    public float initial_screen_x_offset; // Initial X offset from screen center when star is far
    public float initial_screen_y_offset; // Initial Y offset from screen center when star is far
    public float speed; // Base speed for z movement

    // These are calculated and updated by CelestialManager
    public float current_screen_x;
    public float current_screen_y;
    
    public List<java.awt.geom.Point2D.Float> trail;
    public int trail_length;

    public CelestialBody(float z, float initial_screen_x_offset, float initial_screen_y_offset, float speed) {
        this.z = z;
        this.initial_screen_x_offset = initial_screen_x_offset;
        this.initial_screen_y_offset = initial_screen_y_offset;
        this.speed = speed;

        this.current_screen_x = 0;
        this.current_screen_y = 0;
        this.trail = new ArrayList<>();
        this.trail_length = 1;
    }
}
