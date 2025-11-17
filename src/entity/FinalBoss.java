package entity;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.GameState;

public class FinalBoss extends Entity implements BossEntity{

    private int healPoint;
    private int maxHp;
    private final int pointValue;
    private boolean isDestroyed;
    /** The State of Dash Power Up */
    private boolean isPowerUp = false;
    /** The Ship of player */
    private Ship player;
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
    /** Animation cool down */
    private Cooldown animationCooldown;
    /** PowerUp animation cool down */
    private Cooldown animationPowerUpCooldown;
    /** Shoot1's cool down */
    private Cooldown shootCooldown1;
    /** Shoot2's cool down */
    private Cooldown shootCooldown2;
    /** Shoot3's cool down */
    private Cooldown shootCooldown3;
    /** Cooldown for charging of dash */
    private Cooldown chargeCooldown;
    /** Cooldown for charging of laser */
    private Cooldown chargeLaserCooldown;
    /** Cooldown for laser pattern */
    private Cooldown laserCooldown;
    /** Dashing's cool down */
    private Cooldown dashCooldown;
    /** Duration of Laser cool down */
    private Cooldown laserDuration;

    /** Dash Pattern State */
    private enum DashPattern {
        NONE,
        FOLLOWING,
        CHARGING,
        DASHING,
    }

    /** Laser Pattern State */
    private enum LaserPattern {
        NONE,
        IDLE,
        CHARGING,
        FIRING,
    }

    /** Dash Pattern Current State */
    private DashPattern dashOption;
    /** Laser Pattern Current State */
    private LaserPattern laserOption;

    private int screenWidth;
    private int screenHeight;
    /** basic attribute of final boss */

    public FinalBoss(int positionX, int positionY, int screenWidth, int screenHeight, Ship ship){

        super(positionX,positionY,100,80, Color.RED);
        if (GameState.isDecreaseEnemyPower()) {
            this.healPoint = 1;
        } else {
            this.healPoint = 80;
        }
        this.maxHp = healPoint;
        this.pointValue = 1000;
        this.spriteType = DrawManager.SpriteType.FinalBoss1;
        this.isDestroyed = false;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.player = ship;
        this.dashOption = DashPattern.NONE;
        this.laserOption = LaserPattern.NONE;

        this.animationCooldown = Core.getCooldown(500);
        this.animationPowerUpCooldown = Core.getCooldown(250);
        this.shootCooldown1 = Core.getCooldown(5000);
        this.shootCooldown2 = Core.getCooldown(400);
        this.shootCooldown3 = Core.getCooldown(300);
        this.chargeCooldown = Core.getCooldown(2000);
        this.dashCooldown = Core.getCooldown(6000);
        this.chargeLaserCooldown = Core.getCooldown(1500);
        this.laserCooldown = Core.getCooldown(3000);
        this.laserDuration = Core.getCooldown(1600);
    }

    /** for vibrant moving with final boss
     * final boss spritetype is the same with special enemy and enemyshipA, because final boss spritetype have not yet implemented
     * becasue final boss is single object, moving and shooting pattern are included in update methods
     */
    @Override
    public void update(){
        if (this.isPowerUp) {
            if(this.animationPowerUpCooldown.checkFinished()){
                this.animationPowerUpCooldown.reset();

                switch (this.spriteType) {
                    case FinalBossPowerUp1:
                        this.spriteType = DrawManager.SpriteType.FinalBossPowerUp2;
                        break;
                    case FinalBossPowerUp2:
                        this.spriteType = DrawManager.SpriteType.FinalBossPowerUp3;
                        break;
                    case FinalBossPowerUp3:
                        this.spriteType = DrawManager.SpriteType.FinalBossPowerUp4;
                        break;
                    case FinalBossPowerUp4:
                        this.spriteType = DrawManager.SpriteType.FinalBossPowerUp1;
                        break;
                    default:
                        break;
                }
            }
        } else {
            if(this.animationCooldown.checkFinished()){
                this.animationCooldown.reset();

                switch (this.spriteType) {
                    case FinalBoss1:
                        this.spriteType = DrawManager.SpriteType.FinalBoss2;
                        break;
                    case FinalBoss2:
                        this.spriteType = DrawManager.SpriteType.FinalBoss1;
                        break;
                    default:
                        break;
                }
            }
        }
        movePattern();
    }

    /** decrease boss' healpoint */
    @Override
    public void takeDamage(int damage){
        this.healPoint -= damage;
        if(GameState.isDecreaseEnemyPower()){
            SoundManager.stop("sfx/meow.wav");
            SoundManager.play("sfx/meow.wav");
		} else{
            SoundManager.stop("sfx/pikachu.wav");
            SoundManager.play("sfx/pikachu.wav");
		}	
        if(this.healPoint <= 0){
            this.destroy();
        }
    }

    @Override
    public int getHealPoint(){
        return this.healPoint;
    }

    public int getMaxHp(){
        return  this.maxHp;
    }

    @Override
    public int getPointValue(){
        return this.pointValue;
    }

    /** move simple */
    @Override
    public void move(int distanceX, int distanceY){
        this.positionX += distanceX;
        this.positionY += distanceY;
    }
    
    /** movement pattern of final boss */
    public void movePattern(){
        if(this.healPoint > this.maxHp*3/4){
            this.move(0,0);
        }
        else if (this.healPoint > this.maxHp/2){
            if (this.laserOption == LaserPattern.NONE) {
                this.laserOption = LaserPattern.IDLE;
                this.laserCooldown.reset();
            }

            this.moveZigzag(4,3);
        }
        else if (this.healPoint > this.maxHp/4) {
            if (this.laserOption != LaserPattern.NONE) {
                this.laserOption = LaserPattern.NONE;
                this.setClear();
            }
            if (this.dashOption == DashPattern.NONE) {
                this.dashOption = DashPattern.FOLLOWING;
                this.dashCooldown.reset();
            }

            dashPattern(10);
        }
        else {
            if (this.dashOption != DashPattern.NONE) {
                this.dashOption = DashPattern.NONE;
                this.setClear();
            }

            this.moveZigzag(2,1);
        }
    }

    /** move zigzag */
    public void moveZigzag(int zigSpeed, int vertSpeed){
        this.positionX += (this.zigDirection * zigSpeed);
        if(this.positionX <= 0) {
            this.positionX = 0;
            this.zigDirection = 1;
        } else if (this.positionX >= this.screenWidth - this.width) {
            this.positionX = this.screenWidth - this.width;
            this.zigDirection = -1;
        }

        if(goingDown) {
            this.positionY += vertSpeed;
            if (this.positionY >= screenHeight/2 - this.height) goingDown = false;
        }
        else {
            this.positionY -= vertSpeed;
            if(this.positionY <= 0) goingDown = true;
        }
    }

    /** Manage the Dash Pattern */
    public void dashPattern(int dashSpeed) {
        switch (this.dashOption) {
            case FOLLOWING:
                this.follow(dashSpeed);
                this.chargeCooldown.reset();
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
        this.changeToPowerUpMode();

        this.fixedPositionX = this.player.getPositionX() - this.getWidth()/2 + this.player.getWidth()/2; 
        this.fixedPositionY = this.player.getPositionY() - this.getHeight()/2 + this.player.getHeight()/2;

        double angle = Math.atan2(fixedPositionY - positionY, fixedPositionX - positionX);

        this.dashVelX = dashSpeed * Math.cos(angle);
        this.dashVelY = dashSpeed * Math.sin(angle);

        this.dashOption = DashPattern.CHARGING;

        this.subDashVelX = 0;
        this.subDashVelY = 0;
    }

    /** Charge for a While Before Starting Dash Pattern */
    public void charge() {
        if (this.chargeCooldown.checkFinished()) {
            this.dashOption = DashPattern.DASHING;

            this.move(-shakeOffX, -shakeOffY);
            this.shakeOffX = 0;
            this.shakeOffY = 0;

        } else {
            this.move(-shakeOffX, -shakeOffY);

            int shakeAmount = 2;
            this.shakeOffX = java.util.concurrent.ThreadLocalRandom.current().nextInt(-1, 2) * shakeAmount;
            this.shakeOffY = java.util.concurrent.ThreadLocalRandom.current().nextInt(-1, 2) * shakeAmount;

            this.move(this.shakeOffX, this.shakeOffY);
        }
    }

    /** Dash Pattern */
    public void dash() {
        double distanceSq = (double)(fixedPositionX - positionX) * (fixedPositionX - positionX) + (double)(fixedPositionY - positionY) * (fixedPositionY - positionY);
        double speedSq = dashVelX * dashVelX + dashVelY * dashVelY;
        
		if (distanceSq <= speedSq || !this.isPowerUp) {

            if (this.isPowerUp) {
                this.changeToPowerUpMode();
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
                this.move(moveX, moveY);

                this.subDashVelX -= moveX;
                this.subDashVelY -= moveY;
            }
        }
    }

    public void changeToPowerUpMode() {
        if (this.isPowerUp) {
            this.isPowerUp = false;
            this.setColor(Color.RED);
            this.setPositionX(positionX+30);
            this.setPositionY(positionY+30);
            this.setHeight(80);
            this.setWidth(100);
            this.animationCooldown.reset();
            this.spriteType = DrawManager.SpriteType.FinalBoss1;
        } else {
            this.isPowerUp = true;
            this.setColor(Color.YELLOW);
            this.setPositionX(positionX-30);
            this.setPositionY(positionY-30);
            this.setHeight(140);
            this.setWidth(160);
            this.animationPowerUpCooldown.reset();
            this.spriteType = DrawManager.SpriteType.FinalBossPowerUp1;
        }
    }

    /** first shooting pattern of final boss */
    public Set<BossBullet> shoot1(){
        if(this.shootCooldown1.checkFinished()){
            this.shootCooldown1.reset();
            Set<BossBullet> bullets = new HashSet<>();
            int arr[] = {0,1,-1,2,-2};
            for (int i : arr){
                BossBullet bullet = new BossBullet(this.getPositionX() + this.getWidth() / 2 - 3,this.getPositionY() + this.getHeight(), i,4,6,10,Color.yellow);
                bullets.add(bullet);
            }
            return bullets;
        }
        return java.util.Collections.emptySet();
    }

    /** second shooting pattern of final boss */
    public Set<BossBullet> shoot2() {
        if (this.shootCooldown2.checkFinished()) {
            this.shootCooldown2.reset();
            Set<BossBullet> bullets = new HashSet<>();
            int randomX = (int) (Math.random() * screenWidth);
            BossBullet bullet = new BossBullet(randomX, 1, 0, 2,6,10,Color.yellow);
            bullets.add(bullet);
            return bullets;
        }
        return java.util.Collections.emptySet();
    }
    
    /** third shooting pattern of final boss */
    public Set<BossBullet> shoot3() {
        Set<BossBullet> bullets = new HashSet<>();
        if (this.shootCooldown3.checkFinished()) {
            this.shootCooldown3.reset();
         // if (!(this.getPositionX() == 0 || this.getPositionX() == 400)){
                BossBullet bullet1 = new BossBullet(this.getPositionX() + this.getWidth() / 2 - 3 + 70, this.positionY, 0, 5,6,10,Color.blue);
                BossBullet bullet2 = new BossBullet(this.getPositionX() + this.getWidth() / 2 - 3 - 70, this.positionY, 0, 5,6,10,Color.blue);
                bullets.add(bullet1);
                bullets.add(bullet2);
         // }
        }
        return bullets;
    }

    /** Laser firing pattern of final boss */
    public Set<BossLaser> laserShoot() {
        Set<BossLaser> lasers = new HashSet<>();

        switch (this.laserOption) {
            case IDLE:
                if (this.laserCooldown.checkFinished()) {
                    this.laserOption = LaserPattern.CHARGING;
                    this.chargeLaserCooldown.reset();

                    this.changeToPowerUpMode();
                }
                break;

            case CHARGING:
                if (this.chargeLaserCooldown.checkFinished()) {
                    this.laserOption = LaserPattern.FIRING;
                    this.laserDuration.reset();

                    for (int i=0; i<9; i++) {
                        BossLaser laser = new BossLaser(this.getPositionX()+30, this.getPositionY()+140 + 80*i, 100, 80, this, i, this.laserDuration);
                        lasers.add(laser);
                    }
                }
                break;

            case FIRING:
                if (this.laserDuration.checkFinished()) {
                    this.laserOption = LaserPattern.IDLE;
                    this.laserCooldown.reset();

                    this.changeToPowerUpMode();
                }
                break;

            default:
                break;
            }
        return lasers;
    }

    public void setClear() {
        if (this.isPowerUp) {
            this.changeToPowerUpMode();
        }
    }

    /** flag final boss' destroy */
    @Override
    public void destroy(){
        if(!this.isDestroyed){
            this.spriteType = DrawManager.SpriteType.FinalBossDeath;
            this.isDestroyed = true;
        }
    }

    /** check final boss' destroy */
    @Override
    public boolean isDestroyed(){
        return this.isDestroyed;
    }

    @Override
    public void draw(DrawManager drawManager) {
        drawManager.drawEntity(this, this.positionX, this.positionY);
    }
}