package entity;

import audio.SoundManager; 
import java.awt.Color;
import engine.Cooldown;
import engine.Core; 
import engine.DrawManager.SpriteType; 
import engine.GameState;

/**
 * Implements a Chaser enemy ship, which tracks the player.
 * This class extends Entity and implements its own logic.
 */
public class Chaser extends Entity {

    /** Point value of a Chaser enemy. */
    private static final int CHASER_POINTS = 50; 
    
    /** Speed of the chaser. */
    private static final int CHASE_SPEED = 1; 
    private int healPoint;

    private Cooldown explosionCooldown;
    /** Checks if the ship has been hit by a bullet. */
    private boolean isDestroyed;
    /** Values of the ship, in points, when destroyed. */
    private int pointValue;

    /**
     * Constructor, establishes the ship's properties.
     *
     * @param positionX Initial position of the ship in the X axis.
     * @param positionY Initial position of the ship in the Y axis.
     * @param ship      (Required by constructor, but not used for initial tracking)
     */
    public Chaser(final int positionX, final int positionY, Ship ship) {
        

        super(positionX, positionY, 20, 20, Color.RED);
        
        this.spriteType = SpriteType.Chaser; 
        this.pointValue = CHASER_POINTS; 

        if (GameState.isDecreaseEnemyPower()) {
            this.healPoint = 1;
        } else {
            this.healPoint = 30; 
        }

    
        this.isDestroyed = false;
        this.explosionCooldown = Core.getCooldown(500); 
    }

    /**
     * Updates movement logic to track the player.
     * @param playerShip The ship to track.
     */
    public void update(final Ship playerShip) {
        // 이 코드는 파괴되었을 때는 실행되지 않아야 합니다.
        if (this.isDestroyed) {
            return;
        }

        if (playerShip != null && !playerShip.isDestroyed()) {
            int targetX = playerShip.getPositionX();
            int targetY = playerShip.getPositionY();
            
            if (this.positionX < targetX) {
                this.positionX += CHASE_SPEED;
            } else if (this.positionX > targetX) {
                this.positionX -= CHASE_SPEED;
            }

            if (this.positionY < targetY) {
                this.positionY += CHASE_SPEED;
            } else if (this.positionY > targetY) {
                this.positionY -= CHASE_SPEED;
            }
        }
    }
    
    public void update() {
    }

    public final void takeDamage(final int damage) {
        this.healPoint -= damage;
        if(GameState.isDecreaseEnemyPower()){
				SoundManager.stop("sfx/meow.wav");
            	SoundManager.play("sfx/meow.wav");
			}
		else{
				SoundManager.stop("sfx/disappearance.wav");
            	SoundManager.play("sfx/disappearance.wav");
			}
        if (this.healPoint <= 0 && !this.isDestroyed) {
            this.destroy();
        }
    }

    
    public final void destroy() {
        if (!this.isDestroyed) {
            this.isDestroyed = true;
            this.spriteType = SpriteType.Explosion; 
            this.explosionCooldown.reset();
        }
    }


    public final int getPointValue() {
        return this.pointValue;
    }

  
    public final boolean isDestroyed() {
        return this.isDestroyed;
    }

    public final boolean isExplosionFinished() {
        return this.isDestroyed && this.explosionCooldown.checkFinished();
    }
}