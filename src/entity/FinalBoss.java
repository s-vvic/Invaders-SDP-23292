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

    private Cooldown animationCooldown;
    /** Shoot1's cool down */
    private Cooldown shootCooldown1;
    /** Shoot2's cool down */
    private Cooldown shootCooldown2;
    /** Shoot3's cool down */
    private Cooldown shootCooldown3;
    /** charging's cool down */
    private Cooldown chargeCooldown;
    /** Dash's cool down */
    private Cooldown dashCooldown;

    /** Dash Pattern State */
    private enum DashPattern {
        FOLLOWING,
        CHARGING,
        DASH,
    }

    /** Dash Pattern Current State */
    private DashPattern dashOption;

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
        this.dashOption = DashPattern.FOLLOWING;

        this.animationCooldown = Core.getCooldown(500);
        this.shootCooldown1 = Core.getCooldown(5000);
        this.shootCooldown2 = Core.getCooldown(400);
        this.shootCooldown3 = Core.getCooldown(300);
        this.chargeCooldown = Core.getCooldown(2000);
        this.dashCooldown = Core.getCooldown(6000);

    }

    /** for vibrant moving with final boss
     * final boss spritetype is the same with special enemy and enemyshipA, because final boss spritetype have not yet implemented
     * becasue final boss is single object, moving and shooting pattern are included in update methods
     */
    @Override
    public void update(){
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
        movePattern();

    }

    /** decrease boss' healpoint */
    @Override
    public void takeDamage(int damage){
        this.healPoint -= damage;
        if(GameState.isDecreaseEnemyPower()){
				SoundManager.stop("sfx/meow.wav");
            	SoundManager.play("sfx/meow.wav");
			}
		else{
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
            this.moveZigzag(4,3);
        } 
        else if (this.healPoint > this.maxHp/4) {
            switch (this.dashOption) {
                case FOLLOWING:
                    this.follow(10);
                    this.chargeCooldown.reset();
                    break;
                case CHARGING:
                    this.charge();
                    break;
                case DASH:
                    dash();
                    break;
            }
        }
        else {
            this.moveZigzag(2,1);
        }
    }

    /** move zigzag */
    public void moveZigzag(int zigSpeed, int vertSpeed){
        this.positionX += (this.zigDirection * zigSpeed);
        if(this.positionX <= 0 || this.positionX >= this.screenWidth-this.width){
            this.zigDirection *= -1;
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

    /** Fix the Position of Player Ship */
    public void follow(int dashSpeed) {
        this.dashCooldown.reset();
        this.fixedPositionX = this.player.getPositionX();
        this.fixedPositionY = this.player.getPositionY();

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
            this.dashOption = DashPattern.DASH;

            this.move(-shakeOffX, -shakeOffY);
            this.shakeOffX = 0;
            this.shakeOffY = 0;

        } else {

            this.move(-shakeOffX, -shakeOffY);

            int shakeAmount = 2;
            this.shakeOffX = (((int) (Math.random() * 3)) - 1) * shakeAmount;
            this.shakeOffY = (((int) (Math.random() * 3)) - 1) * shakeAmount;

            this.move(this.shakeOffX, this.shakeOffY);
        }
    }

    /** Dash Pattern */
    public void dash() {
        double distance = Math.sqrt(Math.pow(fixedPositionX-positionX, 2) + Math.pow(fixedPositionY-positionY, 2));
        double currentSpeed = Math.sqrt(dashVelX*dashVelX + dashVelY*dashVelY);

        if (distance <= currentSpeed || currentSpeed == 0) {
            this.positionX = this.fixedPositionX;
            this.positionY = this.fixedPositionY;

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