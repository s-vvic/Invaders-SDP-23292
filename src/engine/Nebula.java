package engine;

import java.awt.Color;

/**
 * Represents a single nebula cloud in the background.
 */
public class Nebula {

    public float x;
    public float y;
    public float z;
    public float size;
    public Color color;
    public float speed;

    public Nebula(float x, float y, float z, float size, Color color, float speed) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.color = color;
        this.speed = speed;
    }
}
