package engine;

import java.awt.image.BufferedImage;

/**
 * An interface for any item that can be displayed on the HUD.
 * Ensures that all displayable items provide essential information for tooltips and UI rendering.
 */
public interface DisplayableItem {
    /**
     * Gets the name of the item to be displayed in the tooltip title.
     * @return The display name of the item.
     */
    String getDisplayName();

    /**
     * Gets the description of the item's effect for the tooltip body.
     * @return The description of the item.
     */
    String getDescription();

    /**
     * Gets the icon image of the item for the HUD slot.
     * @return The SpriteType of the item's icon.
     */
    DrawManager.SpriteType getSpriteType();

    /**
     * Gets the rarity of the item, used for tooltip borders and other visual cues.
     * @return The rarity of the item.
     */
    ItemRarity getRarity();
}
