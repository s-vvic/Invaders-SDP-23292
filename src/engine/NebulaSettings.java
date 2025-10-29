package engine;

import java.awt.Color;

public class NebulaSettings {

    // Properties for TitleScreen
    public static final int NUM_NEBULAS = 20;
    public static final float MIN_NEBULA_SIZE = 20;
    public static final float MAX_NEBULA_SIZE = 100;
    public static final float MIN_NEBULA_SPEED = 0.1f;
    public static final float MAX_NEBULA_SPEED = 0.2f;
    public static final Color[] NEBULA_COLORS = {
        new Color(80, 0, 80), // Purple
        new Color(0, 0, 100), // Deep Blue
        new Color(139, 0, 0)  // Dark Red
    };

    // Properties for DrawManager
    public static final int NUM_PUFFS = 50;
    public static final float PUFF_SIZE_MIN_FACTOR = 0.1f;
    public static final float PUFF_SIZE_MAX_FACTOR = 0.5f;
    public static final float PUFF_OFFSET_FACTOR = 1.2f;
    public static final int PUFF_ALPHA_BASE = 100;
    public static final int PUFF_ALPHA_RANGE = 50;
    public static final double PULSATION_SPEED = 20000000000000000000000000000.0;
}
