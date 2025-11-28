package entity;

import engine.DisplayableItem;
import engine.ItemRarity;
import engine.DrawManager.SpriteType;

/**
 * Manages the in-game item (enhancement) system.
 * This class holds the state (levels) of permanent upgrades purchased by the player.
 * It also defines the static properties of those upgrades via the nested ShopUpgrade enum.
 */
public class ShopItem {

    /**
     * Defines the static properties of all permanent shop upgrades.
     * Implements DisplayableItem to be compatible with the HUD and tooltips.
     */
    public enum ShopUpgrade implements DisplayableItem {
        MULTI_SHOT("멀티샷", "한 번에 여러 발의 총알을 발사합니다.", ItemRarity.EPIC, SpriteType.Item_Push),
        RAPID_FIRE("연사 속도", "총알 발사 간격이 짧아집니다.", ItemRarity.RARE, SpriteType.Item_Slow),
        PENETRATION("관통탄", "총알이 적을 관통하여 여러 적을 맞출 수 있습니다.", ItemRarity.EPIC, SpriteType.Item_Shield),
        SHIP_SPEED("기체 속도", "플레이어 기체의 좌우 이동 속도가 증가합니다.", ItemRarity.COMMON, SpriteType.Item_Heal),
        BULLET_SPEED("탄환 속도", "총알이 더 빠르게 날아갑니다.", ItemRarity.COMMON, SpriteType.Item_Stop);

        private final String displayName;
        private final String description;
        private final ItemRarity rarity;
        private final SpriteType spriteType;

        ShopUpgrade(String displayName, String description, ItemRarity rarity, SpriteType spriteType) {
            this.displayName = displayName;
            this.description = description;
            this.rarity = rarity;
            this.spriteType = spriteType;
        }

        @Override
        public String getDisplayName() { return displayName; }

        @Override
        public String getDescription() { return description; }

        @Override
        public SpriteType getSpriteType() { return spriteType; }

        @Override
        public ItemRarity getRarity() { return rarity; }
    }

    /**
     * @return An array of all available shop upgrades.
     */
    public static ShopUpgrade[] getUpgrades() {
        return ShopUpgrade.values();
    }


    // ==================== MultiShot DropItem ====================

    /** MultiShot level (0 = not purchased, 1-3 = enhancement levels) */
    private static int multiShotLevel = 0;

    /** Maximum MultiShot level */
    private static final int MAX_MULTI_SHOT_LEVEL = 3;

    /** Number of bullets fired per level */
    private static final int[] MULTI_SHOT_BULLETS = {1, 2, 3, 4};

    /** Spacing between bullets per level (in pixels) */
    private static final int[] MULTI_SHOT_SPACING = {0, 10, 8, 5};


    /**
     * Private constructor - this class should not be instantiated.
     * It is intended to be used only with static methods.
     */
    private ShopItem() {
    }
    //==================== Rapid Fire DropItem =======================

    /** Rapid Fire lever (0 = not purchased, 1~5 = enhancement levels)*/
    private static int rapidFireLevel = 0;

    /** maximum Rapid Fire level */
    private static final int MAX_RAPID_FIRE_LEVEL = 5;

    /** Base Shooting Interval */
    private static final int BASE_SHOOTING_INTERVAL = 750;

    /** Rapid Fire Reduction Per Level (%)*/
    private static final int[] RAPID_FIRE_REDUCTION ={0, 5, 10, 15, 20, 30};


    //===================== penetration DropItem =====================

    /** penetration level (0 = not purchased, 1~2 = enhancement levels) */
    private static int penetrationLevel = 0;

    /** maximum penetration level */
    private static final int MAX_PENETRATION_LEVEL = 2;

    /** penetration count */
    private static final int[] PENETRATION_COUNT = {0,1,2};

    //===================== ShipSpeed DropItem =====================

    private static final int MAX_SHIP_SPEED_LEVEL = 5;

    /** Ship speed per level */
    private static final int[] SHIP_SPEED = {0, 5, 10, 15, 20, 25};

    /** Ship Speed Increase Per Level (%)*/
    private static int SHIPSPEEDLEVEL = 0;
    // ==================== Bullet Speed DropItem ====================

    /** Bullet Speed level (0 = not purchased, 1-3 = enhancement levels) */
    private static int bulletSpeedLevel = 0;

    /** Maximum Bullet Speed level */
    private static final int MAX_BULLET_SPEED_LEVEL = 3;

    /** Bullet speed value per level */
    private static final int[] BULLET_SPEED_VALUES = {-6, -8, -10, -12};

    // ==================== MultiShot Methods ====================

    /**
     * Sets the MultiShot level (called upon purchase from a shop).
     *
     * @param level The level to set (0-3).
     * @return true if the level was set successfully, false otherwise.
     */
    public static boolean setMultiShotLevel(int level) {
        if (level < 0 || level > MAX_MULTI_SHOT_LEVEL) {
            return false;
        }
        multiShotLevel = level;
        return true;
    }

    /**
     * Returns the current MultiShot level.
     *
     * @return The current level (0-3).
     */
    public static int getMultiShotLevel() {
        return multiShotLevel;
    }

    /**
     * Returns the number of bullets to fire for the MultiShot.
     *
     * @return The number of bullets (1-4).
     */
    public static int getMultiShotBulletCount() {
        return MULTI_SHOT_BULLETS[multiShotLevel];
    }

    /**
     * Returns the spacing for MultiShot bullets.
     *
     * @return The spacing between bullets in pixels.
     */
    public static int getMultiShotSpacing() {
        return MULTI_SHOT_SPACING[multiShotLevel];
    }

    /**
     * Checks if the MultiShot is active.
     *
     * @return true if the level is 1 or higher, false otherwise.
     */
    public static boolean isMultiShotActive() {
        return multiShotLevel > 0;
    }


    //==================== Rapid Fire Methods ====================

    /**
     * Sets rapid fire level.
     *
     * @param level The level to set (0-5).
     * @return True if the level was set successfully, false otherwise.
     */
    public static boolean setRapidFireLevel(int level) {
        if (level < 0 || level > MAX_RAPID_FIRE_LEVEL) {
            return false;
        }
        rapidFireLevel = level;
        return true;
    }

    /**
     * Returns the current rapid fire level.
     *
     * @return The current level (0-5).
     */
    public static int getRapidFireLevel() {
        return rapidFireLevel;
    }

    /**
     * Returns the current shooting interval.
     *
     * @return The shooting interval.
     */
    public static int getShootingInterval() {
        int reduction = RAPID_FIRE_REDUCTION[rapidFireLevel];
        return BASE_SHOOTING_INTERVAL * (100 - reduction) / 100;
    }


    //===================== Penetration Methods ================

    /**
     * Set Penetration Level
     *
     * @param level The level to set (0-2).
     * @return True if the level was set successfully, false otherwise.
     */
    public static boolean setPenetrationLevel(int level) {
        if (level < 0 || level > MAX_PENETRATION_LEVEL) {
            return false;
        }
        penetrationLevel = level;
        return true;
    }


    /**
     * Returns the current rapid fire level.
     *
     * @return The current level (0-2).
     */
    public static int getPenetrationLevel() {
        return penetrationLevel;
    }

    /**
     * return Penetration count
     *
     * @return Penetration count (0 = cannot penetrate, 1~2 = can penetrate)
     */
    public static int getPenetrationCount() {
        return PENETRATION_COUNT[penetrationLevel];
    }

    /**
     * Checks if penetration is enabled.
     *
     * @return true if the level is 1 or higher, false otherwise.
     */
    public static boolean isPenetrationActive() {
        return penetrationLevel > 0;
    }

    // ==================== Bullet Speed Methods ====================

    /**
     * Sets the Bullet Speed level (called upon purchase from a shop).
     *
     * @param level The level to set (0-3).
     * @return true if the level was set successfully, false otherwise.
     */
    public static boolean setBulletSpeedLevel(int level) {
        if (level < 0 || level > MAX_BULLET_SPEED_LEVEL) {
            return false;
        }
        bulletSpeedLevel = level;
        return true;
    }

    /**
     * Returns the current Bullet Speed level.
     *
     * @return The current level (0-3).
     */
    //===================== ShipSpeed Methods ================
    public static boolean setSHIPSPEED(int level){
        if (level < 0 || level > MAX_SHIP_SPEED_LEVEL) {
            return false;
        }
        SHIPSPEEDLEVEL = level;
        return true;
    }

    public static int getSHIPSpeedCOUNT() {
        return SHIP_SPEED[SHIPSPEEDLEVEL];
    }

    public static int getBulletSpeedLevel() {
        return bulletSpeedLevel;
    }

    /**
     * Returns the bullet speed for the current enhancement level.
     *
     * @return The bullet speed.
     */
    public static int getBulletSpeed() {
        return BULLET_SPEED_VALUES[bulletSpeedLevel];
    }


    // ==================== Utility Methods ====================

    /**
     * Resets all items (for testing or game reset).
     */
    public static void resetAllItems() {
        multiShotLevel = 0;
        rapidFireLevel = 0;
        penetrationLevel = 0;
        bulletSpeedLevel = 0;
        SHIPSPEEDLEVEL = 0;
    }

    /**
     * Returns the current status of items (for debugging purposes).
     *
     * @return A string representing the item status.
     */
    public static String getItemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== DropItem Status ===\n");
        status.append("MultiShot Level: ").append(multiShotLevel)
                .append(" (Bullets: ").append(getMultiShotBulletCount())
                .append(", Spacing: ").append(getMultiShotSpacing())
                .append(")\n");
        status.append("Rapid Fire Level: ").append(rapidFireLevel)
                .append(" (Interval: ").append(getShootingInterval())
                .append(")\n");
        status.append("Penetration Level: ").append(penetrationLevel)
                .append(" (Max Penetration Count: ").append(getPenetrationCount())
                .append(")\n");
        status.append("Bullet Speed Level: ").append(bulletSpeedLevel)
                .append(" (Speed: ").append(getBulletSpeed())
                .append(")\n");
        status.append("Ship Speed Level: ").append(SHIPSPEEDLEVEL)
                .append(" (Speed: ").append(getSHIPSpeedCOUNT());
        return status.toString();
    }

    /**
     * For testing - sets the Spread Shot to its maximum level.
     */
    public static void setMaxLevelForTesting() {
        multiShotLevel = MAX_MULTI_SHOT_LEVEL;
        rapidFireLevel = MAX_RAPID_FIRE_LEVEL;
        penetrationLevel = MAX_RAPID_FIRE_LEVEL;
        bulletSpeedLevel = MAX_BULLET_SPEED_LEVEL;
        SHIPSPEEDLEVEL = MAX_SHIP_SPEED_LEVEL;
    }
}