package engine;

import java.awt.Color;

/**
 * Defines the rarity levels for items, each with an associated color.
 */
public enum ItemRarity {
    COMMON(Color.WHITE),
    RARE(new Color(0, 150, 255)), // Blue
    EPIC(new Color(150, 0, 255)); // Purple

    private final Color color;

    ItemRarity(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }
}
