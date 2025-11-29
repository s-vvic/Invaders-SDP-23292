package entity;

import java.awt.Color;

public abstract class BossAttack extends Entity {

    public BossAttack(int x, int y, int width, int height, Color color) {
        super(x, y, width, height, color);
    }

    public abstract void update();
}
