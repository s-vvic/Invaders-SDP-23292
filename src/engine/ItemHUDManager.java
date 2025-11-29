package engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.Ship;
import entity.ShopItem;
import entity.DropItem;
import screen.GameScreen;
import screen.Screen;

/**
 * Manages the display of items in the HUD, including tooltips and visual effects.
 * It handles both permanent shop upgrades and temporary dropped items in a unified way.
 *
 * @author Team 8 - HUD Implementation (Refactored)
 */
public class ItemHUDManager {

    /** Singleton instance */
    private static ItemHUDManager instance;

    private static final int ITEM_SQUARE_SIZE = 20;
    private static final int SQUARE_SPACING = 3;
    private static final int PERMANENT_ITEMS_Y = 450;
    private static final int ACTIVE_ITEMS_Y = 420;
    private int startX;

    private final List<ActiveItemInfo> activeItems;
    private final List<DisplayableItem> permanentItems;
    private DisplayableItem hoveredItem = null;
    private final List<Rectangle> itemRects = new ArrayList<>();
    private final List<DisplayableItem> rectItems = new ArrayList<>();

    /** Timers for the item activation "flash" effect, in frames. */
    private final HashMap<DisplayableItem, Integer> flashTimers = new HashMap<>();
    private static final int FLASH_DURATION_FRAMES = 30; // Approx 0.5 seconds at 60fps

    private static final int MAX_DYNAMIC_ITEMS = 6;
    private static final long DROPPED_ITEM_DISPLAY_DURATION = 10000; // 10 seconds

    private static class ActiveItemInfo {
        public DisplayableItem item;
        public long displayStartTime;

        public ActiveItemInfo(DisplayableItem item) {
            this.item = item;
            this.displayStartTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - displayStartTime > DROPPED_ITEM_DISPLAY_DURATION;
        }
    }

    private ItemHUDManager() {
        this.activeItems = new ArrayList<>();
        this.permanentItems = new ArrayList<>();
        for (ShopItem.ShopUpgrade upgrade : ShopItem.getUpgrades()) {
            this.permanentItems.add(upgrade);
        }
    }

    public static ItemHUDManager getInstance() {
        if (instance == null) {
            instance = new ItemHUDManager();
        }
        return instance;
    }

    public void initialize(Screen screen) {
        int totalFixedWidth = permanentItems.size() * ITEM_SQUARE_SIZE + (permanentItems.size() - 1) * SQUARE_SPACING;
        this.startX = screen.getWidth() - totalFixedWidth - 20;
    }

    public void addActiveItem(DisplayableItem item) {
        cleanupExpiredItems();
        if (activeItems.size() < MAX_DYNAMIC_ITEMS) {
            activeItems.add(new ActiveItemInfo(item));
        } else {
            activeItems.remove(0);
            activeItems.add(new ActiveItemInfo(item));
        }
    }

    private void cleanupExpiredItems() {
        activeItems.removeIf(ActiveItemInfo::isExpired);
    }

    /**
     * Triggers a visual flash effect on a specific item in the HUD.
     * @param item The item to flash.
     */
    public void triggerFlash(DisplayableItem item) {
        flashTimers.put(item, FLASH_DURATION_FRAMES);
    }

    public void update(int mouseX, int mouseY) {
        // Update hover state
        this.hoveredItem = null;
        for (int i = 0; i < itemRects.size(); i++) {
            if (itemRects.get(i).contains(mouseX, mouseY)) {
                this.hoveredItem = rectItems.get(i);
                break;
            }
        }

        // Update flash timers
        if (!flashTimers.isEmpty()) {
            List<DisplayableItem> toRemove = new ArrayList<>();
            for (Map.Entry<DisplayableItem, Integer> entry : flashTimers.entrySet()) {
                int newTime = entry.getValue() - 1;
                if (newTime <= 0) {
                    toRemove.add(entry.getKey());
                } else {
                    flashTimers.put(entry.getKey(), newTime);
                }
            }
            toRemove.forEach(flashTimers::remove);
        }
    }

    public void drawItems(Screen screen, Graphics graphics) {
        cleanupExpiredItems();
        itemRects.clear();
        rectItems.clear();

        int x = startX;
        for (DisplayableItem item : permanentItems) {
            drawItemSquare(screen, graphics, x, PERMANENT_ITEMS_Y, item);
            Rectangle rect = new Rectangle(x, PERMANENT_ITEMS_Y, ITEM_SQUARE_SIZE, ITEM_SQUARE_SIZE);
            itemRects.add(rect);
            rectItems.add(item);
            x += ITEM_SQUARE_SIZE + SQUARE_SPACING;
        }

        x = startX;
        for (int i = 0; i < MAX_DYNAMIC_ITEMS; i++) {
            int currentX = x + i * (ITEM_SQUARE_SIZE + SQUARE_SPACING);
            if (i < activeItems.size()) {
                ActiveItemInfo itemInfo = activeItems.get(i);
                drawItemSquare(screen, graphics, currentX, ACTIVE_ITEMS_Y, itemInfo.item);
                Rectangle rect = new Rectangle(currentX, ACTIVE_ITEMS_Y, ITEM_SQUARE_SIZE, ITEM_SQUARE_SIZE);
                itemRects.add(rect);
                rectItems.add(itemInfo.item);
            } else {
                drawEmptySquare(graphics, currentX, ACTIVE_ITEMS_Y);
            }
        }

        if (this.hoveredItem != null) {
            drawTooltip(graphics, this.hoveredItem, InputManager.getMouseX(), InputManager.getMouseY());
        }
    }

    private void drawItemSquare(Screen screen, Graphics graphics, int x, int y, DisplayableItem item) {
        int level = 0;
        boolean isActive = false;

        if (item instanceof ShopItem.ShopUpgrade) {
            ShopItem.ShopUpgrade upgrade = (ShopItem.ShopUpgrade) item;
            switch (upgrade) {
                case MULTI_SHOT: level = ShopItem.getMultiShotLevel(); break;
                case RAPID_FIRE: level = ShopItem.getRapidFireLevel(); break;
                case PENETRATION: level = ShopItem.getPenetrationLevel(); break;
                case BULLET_SPEED: level = ShopItem.getBulletSpeedLevel(); break;
                case SHIP_SPEED: level = ShopItem.getSHIPSpeedCOUNT(); break;
            }
            isActive = level > 0;
        } else if (item instanceof DropItem.ItemType) {
            isActive = true;
        }

        graphics.setColor(isActive ? new Color(0, 0, 0, 150) : new Color(0, 0, 0, 200));
        graphics.fillRect(x, y, ITEM_SQUARE_SIZE, ITEM_SQUARE_SIZE);

        // Draw item icon
        DrawManager.SpriteType spriteType = item.getSpriteType();
        if (spriteType != null && (isActive || item instanceof ShopItem.ShopUpgrade)) {
            // The sprite itself is 5x5, but drawn with 2x pixels, so it's 10x10.
            // Center it in the 20x20 square.
            DrawManager.getInstance().drawSprite(spriteType, x + 5, y + 5, Color.WHITE);
        }

        // Cooldown VFX
        if (item == DropItem.ItemType.Shield && screen instanceof GameScreen) {
            Ship ship = ((GameScreen) screen).getShip();
            if (ship != null && ship.isInvincible()) {
                Cooldown shieldCooldown = ship.getShieldCooldown();
                int totalDuration = shieldCooldown.getDuration();
                long remaining = shieldCooldown.getRemainingMilliseconds();
                double percent = (totalDuration > 0) ? (double)remaining / (double)totalDuration : 0;

                Graphics2D g2d = (Graphics2D) graphics.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(100, 100, 255, 100)); // Semi-transparent blue
                g2d.fillArc(x, y, ITEM_SQUARE_SIZE, ITEM_SQUARE_SIZE, 90, (int)(360.0 * (1.0 - percent)));
                g2d.dispose();
            }
        }

        // Border and Flash VFX
        if (flashTimers.containsKey(item)) {
            graphics.setColor(Color.WHITE); // Bright white flash
        } else {
            graphics.setColor(isActive ? item.getRarity().getColor() : Color.DARK_GRAY);
        }
        graphics.drawRect(x, y, ITEM_SQUARE_SIZE, ITEM_SQUARE_SIZE);

        if (level > 0 && item instanceof ShopItem.ShopUpgrade) {
            graphics.setColor(Color.YELLOW);
            graphics.drawString(String.valueOf(level), x + ITEM_SQUARE_SIZE - 6, y + ITEM_SQUARE_SIZE - 3);
        }
    }

    private void drawEmptySquare(Graphics graphics, int x, int y) {
        graphics.setColor(new Color(0, 0, 0, 100));
        graphics.fillRect(x, y, ITEM_SQUARE_SIZE, ITEM_SQUARE_SIZE);
        graphics.setColor(Color.GRAY);
        graphics.drawRect(x, y, ITEM_SQUARE_SIZE, ITEM_SQUARE_SIZE);
    }

    private void drawTooltip(Graphics g, DisplayableItem item, int mouseX, int mouseY) {
        Font nameFont = new Font("SansSerif", Font.BOLD, 14);
        Font descFont = new Font("SansSerif", Font.PLAIN, 12);
        FontMetrics nameMetrics = g.getFontMetrics(nameFont);
        FontMetrics descMetrics = g.getFontMetrics(descFont);

        String name = item.getDisplayName();
        String desc = item.getDescription();

        int nameWidth = nameMetrics.stringWidth(name);
        int descWidth = descMetrics.stringWidth(desc);
        int width = Math.max(nameWidth, descWidth) + 20;
        int height = nameMetrics.getHeight() + descMetrics.getHeight() + 20;

        int x = mouseX + 15;
        int y = mouseY;

        if (x + width > 448) {
            x = mouseX - width - 15;
        }
        if (y + height > 512) {
            y = mouseY - height;
        }

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(x, y, width, height);

        g.setColor(item.getRarity().getColor());
        g.drawRect(x, y, width, height);

        g.setFont(nameFont);
        g.setColor(item.getRarity().getColor());
        g.drawString(name, x + 10, y + 10 + nameMetrics.getAscent());

        g.setFont(descFont);
        g.setColor(Color.WHITE);
        g.drawString(desc, x + 10, y + 20 + nameMetrics.getHeight() + descMetrics.getAscent());
    }
}
