package entity;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import engine.Cooldown;
import engine.Core;

public class BossAttackManager {

    /**  Reference to the Final Boss entity */
    private FinalBoss boss;

    /** Cool time for shoot1 pattern */
    private int shoot1Cooldowntime;
    /** Cool time for laser pattern */
    private int laserCooldowntime;
    /** Cool time for laser duration */
    private int laserDurationtime;
    /** The difficulty level of boss */
    private int difficulty;
    /** Flag to check whether the phase 1 attacks have cleared */
    private boolean is_cleared = false;

    /** Shoot1's cool down */
    private Cooldown shootCooldown1;
    /** Shoot2's cool down */
    private Cooldown shootCooldown2;
    /** Shoot3's cool down */
    private Cooldown shootCooldown3;
    /** Shoot4's cool down */
    private Cooldown shootCooldown4;
    /** Cooldown for charging of laser */
    private Cooldown chargeLaserCooldown;
    /** Cooldown for laser pattern */
    private Cooldown laserCooldown;
    /** Duration of Laser cool down */
    private Cooldown laserDuration;

    /** Laser Pattern State */
    private enum LaserPattern {
        NONE,
        IDLE,
        CHARGING,
        FIRING,
    }

    /** Laser Pattern Current State */
    private LaserPattern laserOption;
    
    /** basic attribute of boss attack manager */
    public BossAttackManager(FinalBoss boss) {
        this.boss = boss;
        this.initializeCooldowns();
    }
    
    /** initialize cooldowns according to difficulty */
    private void initializeCooldowns() {

        this.difficulty = boss.getDifficulty();

        switch(this.difficulty) {
            case 1: // easy
                this.shoot1Cooldowntime = 2000;
                this.laserCooldowntime = 3000;
                this.laserDurationtime = 1600;
                break;
            case 2: // normal
                this.shoot1Cooldowntime = 1400;
                this.laserCooldowntime = 3000;
                this.laserDurationtime = 1600;
                break;
            case 3: // hard
                this.shoot1Cooldowntime = 900;
                this.laserCooldowntime = 3000;
                this.laserDurationtime = 1600;
                break;
            default:
                break;
        }

        laserOption = LaserPattern.NONE;

        this.shootCooldown1 = Core.getCooldown(this.shoot1Cooldowntime);
        this.shootCooldown2 = Core.getCooldown(400);
        this.shootCooldown3 = Core.getCooldown(300);
        this.shootCooldown4 = Core.getCooldown(5000);
        this.chargeLaserCooldown = Core.getCooldown(1500);
        this.laserCooldown = Core.getCooldown(this.laserCooldowntime);
        this.laserDuration = Core.getCooldown(this.laserDurationtime);
    }

    /** process attack logic of final boss */
    public Set<BossAttack> processAttacks() {
        Set<BossAttack> attacks = new HashSet<>();

        if(boss.getHealPoint() > boss.getMaxHp()*FinalBoss.PHASE_2_HP_THRESHOLD) {
            if (this.difficulty == 1) {
                attacks.addAll(this.shoot1());
                attacks.addAll(this.shoot2());
            } else {
                attacks.addAll(this.shoot3());
            }
        } else if (boss.getHealPoint() > boss.getMaxHp()*FinalBoss.PHASE_3_HP_THRESHOLD) {
            activateLaserPattern();
            attacks.addAll(this.shoot1());
            attacks.addAll(this.shoot2());
            attacks.addAll(this.laserShoot());
        } else {
            deactivateLaserPattern();
            if (this.difficulty != 1) {
                attacks.addAll(this.shoot4());
            }
            attacks.addAll(this.shoot2());
        }

        return attacks;
    }

    /** first shooting pattern of final boss */
    public Set<BossBullet> shoot1(){
        if(this.shootCooldown1.checkFinished() && (this.laserOption == LaserPattern.IDLE || this.laserOption == LaserPattern.NONE)) {
            this.shootCooldown1.reset();
            Set<BossBullet> bullets = new HashSet<>();
            int arr[] = {0,1,-1,2,-2};
            for (int i : arr){
                BossBullet bullet = new BossBullet(boss.getPositionX() + boss.getWidth() / 2 - 3,boss.getPositionY() + boss.getHeight(), i,4,6,10,Color.yellow);
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
            int randomX = (int) (Math.random() * boss.getScreenWidth());
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
                BossBullet bullet1 = new BossBullet(boss.getPositionX() + boss.getWidth() / 2 - 3 + 70, boss.getPositionY(), 0, 5,6,10,Color.blue);
                BossBullet bullet2 = new BossBullet(boss.getPositionX() + boss.getWidth() / 2 - 3 - 70, boss.getPositionY(), 0, 5,6,10,Color.blue);
                bullets.add(bullet1);
                bullets.add(bullet2);
        }
        return bullets;
    }

    /** fourth shooting pattern of final boss */
    public Set<BossBullet> shoot4(){
        if(this.shootCooldown4.checkFinished() && this.laserDuration.checkFinished()){
            this.shootCooldown4.reset();
            Set<BossBullet> bullets = new HashSet<>();
            int bulletCount = 12;
            int speed;
            if (this.difficulty == 3) {
                speed = 7;
            } else {
                speed = 5;
            }

            for (int i=0; i<bulletCount; i++) {
                double angle = 2* Math.PI * i / bulletCount;

                double velX = Math.cos(angle) * speed;
                double velY = Math.sin(angle) * speed;

                BossBullet bullet = new BossBullet(boss.getPositionX() + boss.getWidth() / 2 - 3, boss.getPositionY() + boss.getHeight() / 2,
                                    (int)velX, (int)velY, 6, 10, Color.BLUE);
                bullets.add(bullet);
            }
            return bullets;
        }
        return java.util.Collections.emptySet();
    }

    /** Laser firing pattern of final boss */
    public Set<BossLaser> laserShoot() {
        Set<BossLaser> lasers = new HashSet<>();

        switch (this.laserOption) {
            case IDLE:
                if (this.laserCooldown.checkFinished()) {
                    this.laserOption = LaserPattern.CHARGING;
                    this.chargeLaserCooldown.reset();

                    boss.changeToPowerUpMode();
                }
                break;

            case CHARGING:
                if (this.chargeLaserCooldown.checkFinished()) {
                    this.laserOption = LaserPattern.FIRING;
                    this.laserDuration.reset();

                    for (int i=0; i<9; i++) {
                        BossLaser laser = new BossLaser(boss.getPositionX() + FinalBoss.OFFSET, boss.getPositionY() + FinalBoss.POWERUP_HEIGHT + FinalBoss.NORMAL_HEIGHT*i, FinalBoss.NORMAL_WIDTH, FinalBoss.NORMAL_HEIGHT, boss, i, this.laserDuration);
                        lasers.add(laser);
                    }
                }
                break;

            case FIRING:
                if (this.laserDuration.checkFinished()) {
                    this.laserOption = LaserPattern.IDLE;
                    this.laserCooldown.reset();

                    boss.changeToPowerUpMode();
                }
                break;

            default:
                break;
            }
        return lasers;
    }

    /** Clear attacks if boss phase1 is changed to phase2 */
    public boolean shouldClearAttacks() {
        if (boss.getHealPoint() > boss.getMaxHp()*FinalBoss.PHASE_3_HP_THRESHOLD &&
            boss.getHealPoint() <= boss.getMaxHp()*FinalBoss.PHASE_2_HP_THRESHOLD) {
            if (this.difficulty != 1 && !this.is_cleared) {
                this.is_cleared = true;
                return true;
            }
        }

        return false;
    }

    /** Check whether laser is activated */
    public boolean isLaserActivate() {
        return this.laserOption != LaserPattern.NONE;
    }

    /** preprocess before laser Pattern */
    public void activateLaserPattern() {
        if (this.laserOption == LaserPattern.NONE) {
                this.laserOption = LaserPattern.IDLE;
                this.laserCooldown.reset();
        }
    }

    /** Set after finishing the laser pattern */
    public void deactivateLaserPattern() {
        if (this.laserOption != LaserPattern.NONE && this.laserDuration.checkFinished()) {
                this.laserOption = LaserPattern.NONE;
                boss.setClear();
        }
    }

}
