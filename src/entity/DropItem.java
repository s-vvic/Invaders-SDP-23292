package entity;

import engine.DisplayableItem;
import engine.ItemRarity;
import engine.DrawManager.SpriteType;

import java.util.Random;

public class DropItem extends Entity {

    /**
     * Defines the types of items that can be dropped, including their properties for gameplay and display.
     */
    public enum ItemType implements DisplayableItem {
        Explode("폭탄", "화면의 모든 적에게 피해를 줍니다.", ItemRarity.RARE, SpriteType.Item_Explode, 2),
        Slow("둔화장", "적들의 이동 속도를 잠시동안 늦춥니다.", ItemRarity.COMMON, SpriteType.Item_Slow, 10),
        Stop("시간 정지", "적들을 잠시동안 완전히 멈춥니다.", ItemRarity.RARE, SpriteType.Item_Stop, 10),
        Push("반발 필드", "모든 적들을 뒤로 밀어냅니다.", ItemRarity.COMMON, SpriteType.Item_Push, 5),
        Shield("보호막", "일정 시간동안 적의 공격을 1회 막아줍니다.", ItemRarity.RARE, SpriteType.Item_Shield, 5),
        Heal("체력 회복", "플레이어의 체력을 1칸 회복합니다.", ItemRarity.COMMON, SpriteType.Item_Heal, 5);

        private final String displayName;
        private final String description;
        private final ItemRarity rarity;
        private final SpriteType spriteType;
        private final int weight;

        ItemType(String displayName, String description, ItemRarity rarity, SpriteType spriteType, final int weight) {
            this.displayName = displayName;
            this.description = description;
            this.rarity = rarity;
            this.spriteType = spriteType;
            this.weight = weight;
        }

        private static final ItemType[] VALUES = values();
        private static final Random RANDOM = new Random();
        private static final int TOTAL_WEIGHT;

        static {
            int sum = 0;
            for (ItemType type : VALUES) {
                sum += type.weight;
            }
            TOTAL_WEIGHT = sum;
        }

        /**
         * Selects a random ItemType based on its weight.
         * @return A randomly selected ItemType.
         */
        public static ItemType selectItemType() {
            int randomWeight = RANDOM.nextInt(TOTAL_WEIGHT);
            int cumulativeWeight = 0;

            for (ItemType type : VALUES) {
                cumulativeWeight += type.weight;
                if (randomWeight < cumulativeWeight) {
                    return type;
                }
            }
            return VALUES[0]; // Fallback
        }

        /**
         * Returns the item type corresponding to the given ID string.
         * @param id The string identifier for the item type.
         * @return The matching ItemType, or null if not found.
         */
        public static ItemType fromString(String id) {
            for (ItemType type : values()) {
                if (type.name().equalsIgnoreCase(id)) {
                    return type;
                }
            }
            return null;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public String getDescription() {
            return this.description;
        }

        @Override
        public SpriteType getSpriteType() {
            return this.spriteType;
        }

        @Override
        public ItemRarity getRarity() {
            return this.rarity;
        }
    }

    /** Speed of the item, positive is down. */
    private int speed;
    /** Type of the item. */
    private ItemType itemType;

    public DropItem(final int positionX, final int positionY, final int speed, final ItemType itemType) {
        super(positionX, positionY, 5 * 2, 5 * 2, itemType.rarity.getColor());
        this.speed = speed;
        this.itemType = itemType;
        this.spriteType = itemType.spriteType;
    }

    private static long freezeEndTime = 0;

    /**
     * Pushes all enemies on the screen upwards by a certain distance.
     * @param enemyShipFormation The formation of enemies to push.
     * @param distanceY The vertical distance to push the enemies.
     */
    public static void PushbackItem(EnemyShipFormation enemyShipFormation, int distanceY) {
        if (enemyShipFormation == null) {
            return;
        }

        // All enemyship push
        for (EnemyShip enemy : enemyShipFormation) {
            if (enemy != null && !enemy.isDestroyed()) {
                enemy.move(0, -distanceY);
            }
        }
    }

    /**
     * Freezes all enemy movement for a specified duration.
     * @param durationMillis Freeze duration in milliseconds.
     */
    public static void applyTimeFreezeItem(int durationMillis) {
        freezeEndTime = System.currentTimeMillis() + durationMillis;
    }

    /**
     * Checks if the time freeze effect is currently active.
     * @return True if enemies should be frozen, false otherwise.
     */
    public static boolean isTimeFreezeActive() {
        if (freezeEndTime > 0 && System.currentTimeMillis() < freezeEndTime) {
            return true;
        }
        if (freezeEndTime > 0 && System.currentTimeMillis() >= freezeEndTime) {
            freezeEndTime = 0;
        }
        return false;
    }

    /**
     * Updates the item's position.
     */
    public final void update() {
        this.positionY += this.speed;
    }

    public final void setSpeed(final int speed) {
        this.speed = speed;
    }

    public final int getSpeed() {
        return this.speed;
    }

    public final ItemType getItemType() {
        return this.itemType;
    }

    public final void setItemType(final ItemType itemType) {
        this.itemType = itemType;
        this.spriteType = itemType.spriteType;
        this.color = itemType.rarity.getColor();
    }

    /**
     * Returns a random item type based on a given probability.
     * @param proba The probability (0.0 to 1.0) of returning an item.
     * @return A random ItemType or null.
     */
    public static ItemType getRandomItemType(final double proba) {
        if (Math.random() < proba){
            return ItemType.selectItemType();
        }
        else {
            return null;
        }
    }
}