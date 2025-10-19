package screen;

import engine.Cooldown;
import engine.Core;
import entity.SoundButton;
import java.awt.Color;
import java.awt.event.KeyEvent;

/**
 * Implements the title screen.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class TitleScreen extends Screen {

	/** Milliseconds between changes in user selection. */
	private static final int SELECTION_TIME = 200;
	
	/** Time between changes in user selection. */
	private Cooldown selectionCooldown;

	/** Sound button on/off object. */
	private SoundButton soundButton;
	private int commandState = 0;

	/**
	 * Constructor, establishes the properties of the screen.
	 * 
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public TitleScreen(final int width, final int height, final int fps) {
		super(width, height, fps);

		// Defaults to play.
		this.returnCode = 2;
		this.soundButton = new SoundButton(0, 0);
		this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
		this.selectionCooldown.reset();
	}

	/**
	 * Starts the action.
	 * 
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();

		draw();
        if (this.selectionCooldown.checkFinished()
                && this.inputDelay.checkFinished()) {
            
            
            if (inputManager.isKeyDown(KeyEvent.VK_UP)
                    || inputManager.isKeyDown(KeyEvent.VK_W)) {
                
                if (this.commandState == 0 || this.commandState == 1) {
                    this.commandState++;
                } else {
                    
                    this.commandState = 0;
                }
                previousMenuItem();
                this.selectionCooldown.reset();
            }
            
            else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
                    || inputManager.isKeyDown(KeyEvent.VK_S)) {
               
                if (this.commandState == 2 || this.commandState == 3) {
                    this.commandState++;
                } else {
                    this.commandState = 0;
                }
                nextMenuItem();
                this.selectionCooldown.reset();
            }
          
            else if (inputManager.isKeyDown(KeyEvent.VK_LEFT)
                    || inputManager.isKeyDown(KeyEvent.VK_A)) {
              
                if (this.commandState == 4 || this.commandState == 6) {
                    this.commandState++;
                } else {
                    this.commandState = 0;
                }
                if (this.returnCode == 5) { 
                    this.returnCode = 4;
                    this.soundButton.setColor(Color.WHITE);
                }
                this.selectionCooldown.reset();
            }
    
            else if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)
                    || inputManager.isKeyDown(KeyEvent.VK_D)) {
               
                if (this.commandState == 5) {
                    this.commandState++;
                } 
              
                else if (this.commandState == 7) {
                    this.returnCode = 10;
                    this.isRunning = false;
                } else {
                    this.commandState = 0;
                }

                if (this.isRunning) { 
                    this.returnCode = 5; 
                    this.soundButton.setColor(Color.GREEN);
                    this.selectionCooldown.reset();
                }
            }
           
            else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
               
                this.commandState = 0;
                
                if (this.returnCode != 5) {
                    this.isRunning = false;
                } else {
                    this.soundButton.changeSoundState();
                    this.selectionCooldown.reset();
                }
            }
		}
	}

	/**
	 * Shifts the focus to the next menu item.
	 */
	private void nextMenuItem() {
		if (this.returnCode == 2)
			this.returnCode = 3;
		else if (this.returnCode == 3)
			this.returnCode = 6;
		else if (this.returnCode == 6)
			this.returnCode = 4;
		else if (this.returnCode == 4)
			this.returnCode = 0;
		else if (this.returnCode == 0)
			this.returnCode = 2;
		else if (this.returnCode == 5) {
			this.soundButton.setColor(Color.WHITE);
			this.returnCode = 0;
		}
	}

	/**
	 * Shifts the focus to the previous menu item.
	 */
	private void previousMenuItem() {
		if (this.returnCode == 2)
			this.returnCode = 0;
		else if (this.returnCode == 0)
			this.returnCode = 4;
		else if (this.returnCode == 4)
			this.returnCode = 6;
		else if (this.returnCode == 6)
			this.returnCode = 3;
		else if (this.returnCode == 3)
			this.returnCode = 2;
		else if (this.returnCode == 5) {
			this.soundButton.setColor(Color.WHITE);
			this.returnCode = 6;
		}
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawManager.drawTitle(this);
		drawManager.drawMenu(this, this.returnCode);
		drawManager.drawEntity(this.soundButton, this.width * 4 / 5 - 16,
				this.height * 4 / 5 - 16);

		drawManager.completeDrawing(this);
	}
}