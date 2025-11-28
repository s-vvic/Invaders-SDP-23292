package entity;

import java.awt.Color;
import java.util.Set;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.GameState;

public class FinalBoss extends Entity implements BossEntity {

    /** Current health points of the final boss */
    private int healPoint;
    /** Maximum health points of the final boss */
    private int maxHp;
    /** Score value awarded when the final boss is destroyed */
    private final int pointValue;
    /** Flag indicating whether the final boss has been destroyed */
    private boolean isDestroyed;
    /** Distance between normal state position and Power-up state position*/
    public final static int OFFSET = 30;
    /** Normal width */
    public final static int NORMAL_WIDTH = 100;
    /** Normal height */
    public final static int NORMAL_HEIGHT = 80;
    /** power-up width */
    public final static int POWERUP_WIDTH = 160;
    /** power-up height */
    public final static int POWERUP_HEIGHT = 140;
    /** Constant about phase 2 HP threshold */
	public static final double PHASE_2_HP_THRESHOLD = 0.7;
	/** Constant about phase 3 HP threshold */
	public static final double PHASE_3_HP_THRESHOLD = 0.3;
    /** Manager of controling Attack logic */
    private BossAttackManager attackManager;
    /** Manager of controling Movement logic */
    private BossMovementManager movementManager;
    /** The State of Dash Power Up */
    private boolean isPowerUp = false;
    /** The difficulty level of boss */
    private int difficulty;
    /** Animation cool down */
    private Cooldown animationCooldown;
    /** PowerUp animation cool down */
    private Cooldown animationPowerUpCooldown;
    /** Screen width */
    private int screenWidth;
    /** Screen height */
    private int screenHeight;
    
    /** basic attribute of final boss */
    public FinalBoss(int positionX, int positionY, int screenWidth, int screenHeight, Ship ship, int difficulty){

        super(positionX,positionY,100,80, Color.RED);
        
        this.difficulty = difficulty;

        if (GameState.isDecreaseEnemyPower()) {
            this.healPoint = 1;
        } else {
            switch(this.difficulty) {
                case 1: // easy
                    this.healPoint = 60;
                    break;
                case 2: // normal
                    this.healPoint = 90;
                    break;
                case 3: // hard
                    this.healPoint = 120;
                    break;
                default:
                    break;
            }
        }

        this.maxHp = healPoint;
        this.pointValue = 1000;
        this.spriteType = DrawManager.SpriteType.FinalBoss1;
        this.isDestroyed = false;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.animationCooldown = Core.getCooldown(500);
        this.animationPowerUpCooldown = Core.getCooldown(250);

        this.attackManager = new BossAttackManager(this);
        this.movementManager = new BossMovementManager(this, ship);
    }
 
    @Override
    public void update(){
        runAnimation();

        this.movementManager.updateMovement();
    }

    /** call attack logic function of final boss of attackManager */
    public Set<BossAttack> processAttacks() {
        return this.attackManager.processAttacks();
    }

    /** call checking function whether need to clear attacks */
    public boolean shouldClearAttacks() {
        return this.attackManager.shouldClearAttacks();
    }

    /** run animation of final boss */
    public void runAnimation() {
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
    }

    /** move final boss by distanceX and distanceY */
    @Override
    public void move(int distanceX, int distanceY){
        this.positionX += distanceX;
        this.positionY += distanceY;
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
        if(this.healPoint <= 0) {
            this.destroy();
        }
    }

    @Override
    public int getHealPoint(){
        return this.healPoint;
    }

    /** get max hp of final boss */
    public int getMaxHp(){
        return  this.maxHp;
    }

    @Override
    public int getPointValue(){
        return this.pointValue;
    }

    /** change final boss state between power-up and normal*/
    public void changeToPowerUpMode() {
        if (this.isPowerUp) {
            this.isPowerUp = false;
            this.setColor(Color.RED);
            this.setPositionX(positionX+OFFSET);
            this.setPositionY(positionY+OFFSET);
            this.setHeight(NORMAL_HEIGHT);
            this.setWidth(NORMAL_WIDTH);
            this.animationCooldown.reset();
            this.spriteType = DrawManager.SpriteType.FinalBoss1;
        } else {
            this.isPowerUp = true;
            this.setColor(Color.YELLOW);
            this.setPositionX(positionX-OFFSET);
            this.setPositionY(positionY-OFFSET);
            this.setHeight(POWERUP_HEIGHT);
            this.setWidth(POWERUP_WIDTH);
            this.animationPowerUpCooldown.reset();
            this.spriteType = DrawManager.SpriteType.FinalBossPowerUp1;
        }
    }

    /** set clear state after changing to boss phase */
    public void setClear() {
        if (this.isPowerUp) {
            this.changeToPowerUpMode();
        }
    }

    /** check whether laser is activated */
    public boolean isLaserActivate() {
        return this.attackManager.isLaserActivate();
    }

    /** get screen width */
    public int getScreenWidth() {
        return this.screenWidth;
    }

    /** get screen height */
    public int getScreenHeight() {
        return this.screenHeight;
    }

    /** get difficulty level */
    public int getDifficulty() {
        return this.difficulty;
    }

    /** get the state of Power Up */
    public boolean getIsPowerUp() {
        return this.isPowerUp;
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