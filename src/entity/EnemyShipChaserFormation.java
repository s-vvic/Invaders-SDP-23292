package entity;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import engine.Core;
import engine.DrawManager;
import engine.level.Level;
import screen.Screen;
/**
 * Manages the spawning, updating, and drawing of independent Chaser enemies.
 */
public class EnemyShipChaserFormation implements Iterable<Chaser> {

    /** Logger for debugging. */
    private Logger logger;
    /** DrawManager for drawing entities. */
    private DrawManager drawManager;
    /** Screen reference (currently unused, but good practice like other formations). */

    /** List of active Chaser enemies. */
    private List<Chaser> chasers;

    /** Y-position offset from the top separation line. */
    private static final int SPAWN_Y_OFFSET = 30;

    /**
     * Constructor. Creates Chasers based on the current level.
     *
     * @param level      The current level definition.
     * @param screenWidth The width of the game screen for positioning.
     * @param p1Ship       The Player 1 ship, passed to the Chaser constructor.
     */
    public EnemyShipChaserFormation(Level level, int screenWidth, Ship p1Ship) {
        this.logger = Core.getLogger();
        this.drawManager = Core.getDrawManager();
        this.chasers = new ArrayList<>();

        int currentLevel = level.getLevel();
        
        // Y-position for spawning (Separation Line + Offset)
        int spawnY = 68 + SPAWN_Y_OFFSET; 

        if (currentLevel >= 2 && currentLevel < 5) {
            // Level 1: Spawn 1 Chaser in the middle.
            this.chasers.add(new Chaser(screenWidth / 2, spawnY, p1Ship, currentLevel));
            this.logger.info("Spawned 1 Chaser for Level 2~4.");
            
        } else if (currentLevel >=5) {
            // Level 4: Spawn 2 Chasers at 1/3 and 2/3 positions.
            this.chasers.add(new Chaser(screenWidth / 3, spawnY, p1Ship, currentLevel));
            this.chasers.add(new Chaser((screenWidth / 3) * 2, spawnY, p1Ship, currentLevel));
            this.logger.info("Spawned 2 Chasers for Level 5~.");
        }
        // Other levels will spawn 0 Chasers.
    }

    /**
     * Attaches the formation to the screen (standard practice).
     * @param screen The game screen.
     */
    public final void attach(final Screen screen) {
    }

    /**
     * Updates all active Chasers in the list.
     * @param targetShip The ship for the Chasers to track (Player 1).
     */
    public final void update(final Ship targetShip) {
        Iterator<Chaser> it = this.chasers.iterator();
        while (it.hasNext()) {
            Chaser chaser = it.next();

            if (chaser.isDestroyed() && chaser.isExplosionFinished()) {
                // If explosion is finished, remove from game
                it.remove();
            } else if (!chaser.isDestroyed()) {
                // If alive, update tracking logic
                chaser.update(targetShip);
            }
        }
    }

    /**
     * Draws all active Chasers.
     */
    public final void draw() {
        for (Chaser chaser : this.chasers) {
            drawManager.drawEntity(chaser, chaser.getPositionX(), chaser.getPositionY());
        }
    }

    /**
     * Returns an iterator over the active Chasers.
     * This is essential for GameScreen's collision detection.
     *
     * @return An iterator for the list of Chasers.
     */
    @Override
    public final Iterator<Chaser> iterator() {
        return this.chasers.iterator();
    }

    /**
     * Checks if all Chasers in this formation are destroyed or removed.
     * @return true if the list of chasers is empty.
     */
    public final boolean isEmpty() {
        return this.chasers.isEmpty();
    }
}