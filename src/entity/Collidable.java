package entity;

import screen.GameScreen;

/**
 * An interface for entities that can collide with the player's ship and
 * have a specific behavior for that collision.
 */
public interface Collidable {
    /**
     * Defines the behavior of the entity when it collides with the player's ship.
     * @param screen The GameScreen instance, providing context and access to game state.
     */
    void handleCollisionWithShip(GameScreen screen);
    boolean isDestroyed();
    boolean collidesWith(Entity other);
}
