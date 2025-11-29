package entity;

import java.awt.Color;

import engine.DrawManager;
import engine.Cooldown;
import engine.Core;

/**
 * Implements a boss laser entity.
 * 
 */
public class BossLaser extends BossAttack {
    /** X_OFFSET to change position */
    private final static int X_OFFSET = 30;
    /** Y_OFFSET to change position */
    private final static int Y_OFFSET = 140;
    /** The shooter of this BossLaser */
    private FinalBoss shooter;
    /** The index of n-th created laser */
    private int laserIndex;
    /**Cooldown for duration */
    private Cooldown duration;
    /** Cooldown for animation */
    private Cooldown animationCooldown;

    /**
     * Constructor, establishes boss laser properties.
     *
     * @param x
     *            current x-coordinate
     * @param y
     *            current y-coordinate
     * @param width
     *            laser's width
     * @param height
     *            laser's height
     */

    public BossLaser(int x, int y, int width, int height, FinalBoss shooter, int laserIndex, Cooldown duration) {
        super(x, y, width, height, Color.YELLOW);

        this.shooter = shooter;
        this.laserIndex = laserIndex;
        this.duration = duration;
        this.animationCooldown = Core.getCooldown(300);


        this.spriteType = DrawManager.SpriteType.BossLaser1;
    }

    public final void update() {
        if (this.animationCooldown.checkFinished()) {
            this.animationCooldown.reset();

            switch (this.spriteType) {
                case BossLaser1:
                    this.spriteType = DrawManager.SpriteType.BossLaser2;
                    break;
                case BossLaser2:
                    this.spriteType = DrawManager.SpriteType.BossLaser3;
                    break;
                case BossLaser3:
                    this.spriteType = DrawManager.SpriteType.BossLaser1;
                    break;
                default:
                    break;
            }
        }
        this.setPositionX(this.shooter.getPositionX() + X_OFFSET);
        this.setPositionY(this.shooter.getPositionY() + Y_OFFSET + this.laserIndex*this.getHeight());
    }

    public boolean isRemoved() {
        return this.duration.checkFinished();
    }
}
