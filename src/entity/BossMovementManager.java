package entity;

import engine.Cooldown;
import engine.Core;

public class BossMovementManager {

    /**  Reference to the Final Boss entity */
    private FinalBoss boss;
    /**  Reference to the Player Ship entity */
    private Ship player;

    /** Horizontal speed for zigzag moving */
    private int zigzagSpeed;
    /** Speed for dash pattern */
    private int dashSpeed;
    /** Cool time for dash pattern */
    private int dashCooldownTime;
    /** Cool time for dash charging */
    private int dashChargeCooldowntime;
    /** The difficulty level of boss */
    private int difficulty;
    /** The Fixed Position X of player ship when start dash pattern*/
    private int fixedPositionX;
    /** The Fixed Position Y of player ship when start dash pattern*/
    private int fixedPositionY;
    /** for move pattern */
    private int zigDirection = 1;
    /** for move pattern */
    private boolean goingDown = true;
    /** Dash Velocity X */
    private double dashVelX = 0;
    /** Dash Velocity Y */
    private double dashVelY = 0;
    /** The Decimal Point of dashVelX */
    private double subDashVelX;
    /** The Decimal Point of dashVelY */
    private double subDashVelY;
    /** Degree of shaking X axis */
    private int shakeOffX = 0;
    /** Degree of shaking Y axis */
    private int shakeOffY = 0;
    /** Cooldown for charging of dash */
    private Cooldown dashChargeCooldown;
    /** Dashing's cool down */
    private Cooldown dashCooldown;

    /** Dash Pattern State */
    private enum DashPattern {
        NONE,
        FOLLOWING,
        CHARGING,
        DASHING,
    }

    /** Dash Pattern Current State */
    private DashPattern dashOption;

    /** basic attribute of boss movement manager */
    public BossMovementManager(FinalBoss boss, Ship player) {
        this.boss = boss;
        this.player = player;
        initializeMovement();
    }

    /** initialize movement parameters according to difficulty */
    private void initializeMovement() {
        this.difficulty = boss.getDifficulty();

        switch(this.difficulty) {
            case 1: // easy
                this.zigzagSpeed = 3;
                this.dashSpeed = 8;
                this.dashCooldownTime = 3000;
                this.dashChargeCooldowntime = 1200;
                break;
            case 2: // normal
                this.zigzagSpeed = 4;
                this.dashSpeed = 10;
                this.dashCooldownTime = 3000;
                this.dashChargeCooldowntime = 1000;
                break;
            case 3: // hard
                this.zigzagSpeed = 5;
                this.dashSpeed = 14;
                this.dashCooldownTime = 2000;
                this.dashChargeCooldowntime = 500;
                break;
            default:
                break;
        }

        dashOption = DashPattern.NONE;
        this.dashChargeCooldown = Core.getCooldown(this.dashChargeCooldowntime);
        this.dashCooldown = Core.getCooldown(this.dashCooldownTime);
    }

    /** update movement logic of final boss */
    public void updateMovement() {
        if(boss.getHealPoint() > boss.getMaxHp()*FinalBoss.PHASE_2_HP_THRESHOLD) {
            if (this.difficulty == 1) {
                // don't move in easy mode
            } else {
                this.moveZigzag(2,1);
            }
        } else if (boss.getHealPoint() > boss.getMaxHp()*FinalBoss.PHASE_3_HP_THRESHOLD) {
            this.moveZigzag(this.zigzagSpeed, 3);
        }
        else {
            this.activateDashPattern();
            dashPattern(this.dashSpeed);
        }    
    }

    /** move zigzag */
    public void moveZigzag(int zigSpeed, int vertSpeed){
        boss.setPositionX(boss.getPositionX() + (this.zigDirection * zigSpeed));
        if(boss.getPositionX() <= 0) {
            boss.setPositionX(0);
            this.zigDirection = 1;
        } else if (boss.getPositionX() >= boss.getScreenWidth() - boss.getWidth()) {
            boss.setPositionX(boss.getScreenWidth() - boss.getWidth());
            this.zigDirection = -1;
        }

        if(goingDown) {
            boss.setPositionY(boss.getPositionY() + vertSpeed);
            if (boss.getPositionY() >= boss.getScreenHeight()/2 - boss.getHeight()) goingDown = false;
        }
        else {
            boss.setPositionY(boss.getPositionY() - vertSpeed);
            if (boss.getPositionY() <= 0) goingDown = true;
        }
    }

    /** Manage the Dash Pattern */
    public void dashPattern(int dashSpeed) {
        switch (this.dashOption) {
            case FOLLOWING:
                this.follow(dashSpeed);
                this.dashChargeCooldown.reset();
                break;
            case CHARGING:
                this.charge();
                break;
            case DASHING:
                dash();
                break;
            default:
                break;
        }
    }

    /** Fix the Position of Player Ship */
    public void follow(int dashSpeed) {
        this.dashCooldown.reset();
        boss.changeToPowerUpMode();


        this.fixedPositionX = this.player.getPositionX() - boss.getWidth()/2 + this.player.getWidth()/2; 
        this.fixedPositionY = this.player.getPositionY() - boss.getHeight()/2 + this.player.getHeight()/2;

        double angle = Math.atan2(fixedPositionY - boss.getPositionY(), fixedPositionX - boss.getPositionX());

        this.dashVelX = dashSpeed * Math.cos(angle);
        this.dashVelY = dashSpeed * Math.sin(angle);

        this.dashOption = DashPattern.CHARGING;

        this.subDashVelX = 0;
        this.subDashVelY = 0;
    }

    /** Charge for a While Before Starting Dash Pattern */
    public void charge() {
        if (this.dashChargeCooldown.checkFinished()) {
            this.dashOption = DashPattern.DASHING;

            boss.move(-shakeOffX, -shakeOffY);
            this.shakeOffX = 0;
            this.shakeOffY = 0;

        } else {
            boss.move(-shakeOffX, -shakeOffY);

            int shakeAmount = 2;
            this.shakeOffX = java.util.concurrent.ThreadLocalRandom.current().nextInt(-1, 2) * shakeAmount;
            this.shakeOffY = java.util.concurrent.ThreadLocalRandom.current().nextInt(-1, 2) * shakeAmount;

            boss.move(this.shakeOffX, this.shakeOffY);
        }
    }

    /** Dash Pattern */
    public void dash() {
        double dx = fixedPositionX - boss.getPositionX();
        double dy = fixedPositionY - boss.getPositionY();
        double distanceSq = dx * dx + dy * dy;
        double speedSq = dashVelX * dashVelX + dashVelY * dashVelY;
        
		if (distanceSq <= speedSq || !boss.getIsPowerUp()) {

            if (boss.getIsPowerUp()) {
                boss.setPositionX(this.fixedPositionX);
                boss.setPositionY(this.fixedPositionY);
                boss.changeToPowerUpMode();
            }

            if (this.dashCooldown.checkFinished()) {
                this.dashCooldown.reset();

                this.dashOption = DashPattern.FOLLOWING;
                this.dashVelX = 0;
                this.dashVelY = 0;
            }
        } else {

            this.subDashVelX += this.dashVelX;
            this.subDashVelY += this.dashVelY;

            int moveX = (int) this.subDashVelX;
            int moveY = (int) this.subDashVelY;

            if (moveX != 0 || moveY !=0) {
                boss.move(moveX, moveY);

                this.subDashVelX -= moveX;
                this.subDashVelY -= moveY;
            }
        }
    }


    /** preprocess before dash Pattern */
    public void activateDashPattern() {
        if (this.dashOption == DashPattern.NONE && !boss.isLaserActivate()) {
                this.dashOption = DashPattern.FOLLOWING;
                this.dashCooldown.reset();
        }
    }

    /** Set after finishing the dash pattern */
    public void deactivateDashPattern() {
        if (this.dashOption != DashPattern.NONE) {
                this.dashOption = DashPattern.NONE;
                boss.setClear();
        }
    }
}
