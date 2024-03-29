/*
 * GameController.java
 *
 * This combines the WorldController with the mini-game specific PlatformController
 * in the last lab.  With that said, some of the work is now offloaded to the new
 * LevelModel class, which allows us to serialize and deserialize a level.
 *
 * This is a refactored version of WorldController from Lab 4.  It separate the
 * level out into a new class called LevelModel.  This model is, in turn, read
 * from a JSON file.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * JSON version, 3/2/2016
 */
package edu.cornell.gdiac.main;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.util.ScreenListener;
import java.util.ArrayList;


/**
 * Gameplay controller for the game.
 * <p>
 * This class does not have the Box2d world.  That is stored inside of the LevelModel object, as the
 * world settings are determined by the JSON file.  However, the class does have all of the
 * controller functionality, including collision listeners for the active level.
 * <p>
 * You will notice that asset loading is very different.  It relies on the singleton asset manager
 * to manage the various assets.
 */
public class GameController implements Screen {
  // ASSETS
  /**
   * Exit code for quitting the game
   */
  public static final int EXIT_QUIT = 0;
  /**
   * How many frames after winning/losing do we continue?
   */
  public static final int EXIT_COUNT = 120;
  /**
   * The amount of time for a physics engine step.
   */
  public static final float WORLD_STEP = 1 / 60.0f;
  /**
   * Number of velocity iterations for the constrain solvers
   */
  public static final int WORLD_VELOC = 6;
  /**
   * Number of position iterations for the constrain solvers
   */
  public static final int WORLD_POSIT = 2;
  private static ArrayList<String> levels;
  /**
   * Mute the game for convenience while testing
   */
  private final boolean IS_MUTED = true;

  // THESE ARE CONSTANTS BECAUSE WE NEED THEM BEFORE THE LEVEL IS LOADED
  /**
   * Need an ongoing reference to the asset directory
   */
  protected AssetDirectory directory;
  /**
   * The font for giving messages to the player
   */
  protected BitmapFont displayFont;
  /**
   * Reference to the game canvas
   */
  protected GameCanvas canvas;
  /**
   * Reference to the game level
   */
  protected LevelModel level;
  /**
   * Mark set to handle more sophisticated collision callbacks
   */
  protected ObjectSet<Fixture> sensorFixtures;
  /**
   * The JSON defining the level model
   */
  private JsonValue levelFormat;
  /**
   * The jump sound.  We only want to play once.
   */
  private SoundEffect jumpSound;
  private long jumpId = -1;
  /**
   * Listener that will update the player mode when we are done
   */
  private ScreenListener listener;
  /**
   * Collision controller
   */
  private CollisionController collisionController;
  /**
   * Whether or not this is an active controller
   */
  private boolean active;
  /**
   * Whether we have completed this level
   */
  private boolean complete;
  /**
   * Whether we have failed at this world (and need a reset)
   */
  private boolean failed;
  /**
   * Countdown active for winning or losing
   */
  private int countdown;
  /**
   * Counter for keep track of meter
   */
  private float meterCounter;
  /**
   * Timer of the game
   */
  private float timer;
  private boolean isJumpPressedLastFrame = false;
  private boolean isJumpRelease = false;
  private float jumpTimer = 0f;
  private int levelNumber = 1;

  /**
   * Creates a new game world
   * <p>
   * The physics bounds and drawing scale are now stored in the LevelModel and defined by the
   * appropriate JSON file.
   */
  public GameController() {
    level = new LevelModel();
    complete = false;
    failed = false;
    active = false;
    countdown = -1;
    timer = 100;
    // create CollisionController, which is extended from ContactListener
    collisionController = new CollisionController(level);
    setComplete(false);
    setFailure(false);
    sensorFixtures = new ObjectSet<Fixture>();
  }

  /**
   * Returns true if the level is completed.
   * <p>
   * If true, the level will advance after a countdown
   *
   * @return true if the level is completed.
   */
  public boolean isComplete() {
    return complete;
  }

  /**
   * Sets whether the level is completed.
   * <p>
   * If true, the level will advance after a countdown
   *
   * @param value whether the level is completed.
   */
  public void setComplete(boolean value) {
    if (value) {
      countdown = EXIT_COUNT;
    }
    complete = value;

    level.setComplete(value);
  }

  /**
   * Returns true if the level is failed.
   * <p>
   * If true, the level will reset after a countdown
   *
   * @return true if the level is failed.
   */
  public boolean isFailure() {
    return failed;
  }

  /**
   * Sets whether the level is failed.
   * <p>
   * If true, the level will reset after a countdown
   *
   * @param value whether the level is failed.
   */
  public void setFailure(boolean value) {
    if (value) {
      countdown = EXIT_COUNT;
    }
    failed = value;
    level.setFailure(value);
  }

  /**
   * Returns true if this is the active screen
   *
   * @return true if this is the active screen
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Returns the canvas associated with this controller
   * <p>
   * The canvas is shared across all controllers
   *
   * @return the GameCanvas associated with this controller
   */
  public GameCanvas getCanvas() {
    return canvas;
  }

  /**
   * Sets the canvas associated with this controller
   * <p>
   * The canvas is shared across all controllers.  Setting this value will compute the drawing scale
   * from the canvas size.
   *
   * @param canvas the canvas associated with this controller
   */
  public void setCanvas(GameCanvas canvas) {
    this.canvas = canvas;
  }

  /**
   * Dispose of all (non-static) resources allocated to this mode.
   */
  public void dispose() {
    level.dispose();
    level = null;
    canvas = null;
  }

  /**
   * Gather the assets for this controller.
   * <p>
   * This method extracts the asset variables from the given asset directory. It should only be
   * called after the asset directory is completed.
   *
   * @param directory Reference to global asset manager.
   */
  public void gatherAssets(AssetDirectory directory) {
    levels = new ArrayList<String>();
    levels.add(0, "level0");
    levels.add(1, "level1");
    levels.add(2, "level2");
    // Access the assets used directly by this controller
    this.directory = directory;
    // Some assets may have not finished loading so this is a catch-all for those.
    directory.finishLoading();
    displayFont = directory.getEntry("display", BitmapFont.class);
    jumpSound = directory.getEntry("jump", SoundEffect.class);

    // This represents the level but does not BUILD it
    levelFormat = directory.getEntry(levels.get(1), JsonValue.class);
  }

  /**
   * Resets the status of the game so that we can play again.
   * <p>
   * This method disposes of the level and creates a new one. It will reread from the JSON file,
   * allowing us to make changes on the fly.
   */
  public void reset() {
    level.dispose();

    setComplete(false);
    setFailure(false);
    countdown = -1;
    // Reload the designated level
    level.populate(directory, levelFormat);
    timer = level.getTimer();
    canvas.startLevel();
    level.getWorld().setContactListener(collisionController);
  }

  /**
   * Returns whether to process the update loop
   * <p>
   * At the start of the update loop, we check if it is time to switch to a new game mode.  If not,
   * the update proceeds normally.
   *
   * @param dt Number of seconds since last animation frame
   * @return whether to process the update loop
   */
  public boolean preUpdate(float dt) {
    InputController input = InputController.getInstance();
    input.readInput(level.getBounds(), level.getScale());
    if (listener == null) {
      return true;
    }

    // Toggle debug
    if (input.didDebug()) {
      level.setDebug(!level.getDebug());
    }

    // Handle resets
    if (input.didReset()) {
      reset();
    }
    if (input.getNextLevel()) {
      if (levelNumber < levels.size() - 1) {
        levelNumber += 1;
        levelFormat = directory.getEntry(levels.get(levelNumber), JsonValue.class);
        reset();
        input.setNextLevel();
      } else {
        reset();
        input.setNextLevel();
      }
    }
    if (input.getPastLevel()) {
      if (levelNumber != 0) {
        levelNumber -= 1;
        levelFormat = directory.getEntry(levels.get(levelNumber), JsonValue.class);
        reset();
        input.setPastLevel();
      } else {
        reset();
        input.setPastLevel();
      }
    }

    // Now it is time to maybe switch screens.
    if (input.didExit()) {
      listener.exitScreen(this, EXIT_QUIT);
      return false;
    } else if (countdown > 0) {
      countdown--;
    } else if (countdown == 0) {
      reset();
    }

    if (!isFailure() && level.getAvatar().getY() < -1) {
      setFailure(true);
      return false;
    }

    level.breakPlatforms();

    return true;
  }

  /**
   * The core gameplay loop of this world.
   * <p>
   * This method contains the specific update code for this mini-game. It does not handle
   * collisions, as those are managed by the parent class WorldController. This method is called
   * after input is read, but before collisions are resolved. The very last thing that it should do
   * is apply forces to the appropriate objects.
   *
   * @param dt Number of seconds since last animation frame
   */
  public void update(float dt) {
    // Check if the game has completed (if player touches the objective)
    setComplete(level.getComplete());
    setFailure(level.getFailure());

    // Process actions in object model
    InputController input = InputController.getInstance();
    PlayerModel avatar = level.getAvatar();
    NPCModel npc = level.getNPC();

    // Horizontal movement
    avatar.setMovement(InputController.getInstance().getHorizontal() * avatar.getForce());

    // Vertical movement
    float verticalInput = InputController.getInstance().getVertical();

    // Jump Mechanics
    // Check for the transition from pressed to not pressed to detect a jump release
    boolean isJumpPressed = verticalInput > 0;
    avatar.setJumping(isJumpPressed);

    // Drop Mechanics
    boolean isDropPressed = verticalInput < 0;
    avatar.setDropping(isDropPressed);

    // Change the gravity of the player only
    float gravity = level.getWorld().getGravity().y;
    // On fall, increase the gravity to add a more weighty feel
    if (avatar.getVY() < 0 && !avatar.isGrounded()) {
      avatar.setVY(
          avatar.getVY() + gravity * (avatar.getFallMultiplier() - 1) * dt);
    } else if (avatar.getVY() > 0 && (!isJumpPressed || avatar.getIsFrozen())
        && !avatar.isGrounded()) {
      // On jump, increase the gravity only if the user is not pressing up (or the user is frozen)
      // Allows the player to exert more control over vertical motion
      avatar.setVY(avatar.getVY() + gravity * (avatar.getLowJumpMultiplier() - 1) * dt);
    }

    if (avatar.getIsJumping() && !IS_MUTED) {
      jumpId = playSound(jumpSound, jumpId);
    }
    if (input.getTimerActive()) {
      timer -= dt;
      avatar.setFrozen(input.getFrozen());
      if (!isFailure() && timer <= 0) {
        setFailure(true);
      }

      if (complete || failed) {
        timer = 0;
      }
    } else {
      // Get input to see if f is just pressed and if so set frozen of the avatar to true
      // This method only works when the game is paused!
      avatar.setFrozen(input.getFrozen());
    }

    // Turn the physics engine crank
    level.getWorld().step(WORLD_STEP, WORLD_VELOC, WORLD_POSIT);
    level.applyAirResistance();
    // Update each fanModel with the current time
    level.getFans().forEach((fanModel) -> {
      fanModel.update(WORLD_STEP);
    });

    avatar.setFrozen(input.getFrozen());
    avatar.applyForce();
    npc.applyMovement();
  }

  /**
   * Draw the physics objects to the canvas
   * <p>
   * For simple worlds, this method is enough by itself.  It will need to be overriden if the world
   * needs fancy backgrounds or the like.
   * <p>
   * The method draws all objects in the order that they were added.
   *
   * @param delta The drawing context
   */
  public void draw(float delta) {
    canvas.clear();
    level.draw(canvas);
    InputController input = InputController.getInstance();

    // Display meter
    if (!complete && !failed) {
      displayFont.setColor(Color.BLACK);
      canvas.begin();
      String message = "";
      if (input.getTimerActive()) {
        message = "Timer: " + (int) Math.ceil(timer);
      }
      canvas.drawText(message, displayFont, canvas.getWidth() / 2f - 380, canvas.getHeight() - 120);
      canvas.drawText("L" + levelNumber, displayFont, canvas.getWidth() / 1.75f,
          canvas.getHeight() - 120);
      canvas.end();
    }

    // Final message
    if (complete) {
      displayFont.setColor(Color.YELLOW);
      canvas.begin(); // DO NOT SCALE
      canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
      canvas.end();
    } else if (failed) {
      displayFont.setColor(Color.RED);
      canvas.begin(); // DO NOT SCALE
      canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
      canvas.end();
    }
  }

  /**
   * Called when the Screen is resized.
   * <p>
   * This can happen at any point during a non-paused state but will never happen before a call to
   * show().
   *
   * @param width  The new width in pixels
   * @param height The new height in pixels
   */
  public void resize(int width, int height) {
    // IGNORE FOR NOW
  }

  /**
   * Called when the Screen should render itself.
   * <p>
   * We defer to the other methods update() and draw().  However, it is VERY important that we only
   * quit AFTER a draw.
   *
   * @param delta Number of seconds since last animation frame
   */
  public void render(float delta) {
    if (active) {
      if (preUpdate(delta)) {
        update(delta);
      }
      draw(delta);
    }
  }

  /**
   * Called when the Screen is paused.
   * <p>
   * This is usually when it's not active or visible on screen. An Application is also paused before
   * it is destroyed.
   */
  public void pause() {
    // We need this method to stop all sounds when we pause.
    if (jumpSound.isPlaying(jumpId)) {
      jumpSound.stop(jumpId);
    }
  }

  /**
   * Called when the Screen is resumed from a paused state.
   * <p>
   * This is usually when it regains focus.
   */
  public void resume() {
  }

  /**
   * Called when this screen becomes the current screen for a Game.
   */
  public void show() {
    // Useless if called in outside animation loop
    active = true;
  }

  /**
   * Called when this screen is no longer the current screen for a Game.
   */
  public void hide() {
    // Useless if called in outside animation loop
    active = false;
  }

  /**
   * Sets the ScreenListener for this mode
   * <p>
   * The ScreenListener will respond to requests to quit.
   */
  public void setScreenListener(ScreenListener listener) {
    this.listener = listener;
  }


  /**
   * Method to ensure that a sound asset is only played once.
   * <p>
   * Every time you play a sound asset, it makes a new instance of that sound. If you play the
   * sounds to close together, you will have overlapping copies. To prevent that, you must stop the
   * sound before you play it again.  That is the purpose of this method.  It stops the current
   * instance playing (if any) and then returns the id of the new instance for tracking.
   *
   * @param sound   The sound asset to play
   * @param soundId The previously playing sound instance
   * @return the new sound instance for this asset.
   */
  public long playSound(SoundEffect sound, long soundId) {
    return playSound(sound, soundId, 1.0f);
  }

  /**
   * Method to ensure that a sound asset is only played once.
   * <p>
   * Every time you play a sound asset, it makes a new instance of that sound. If you play the
   * sounds to close together, you will have overlapping copies. To prevent that, you must stop the
   * sound before you play it again.  That is the purpose of this method.  It stops the current
   * instance playing (if any) and then returns the id of the new instance for tracking.
   *
   * @param sound   The sound asset to play
   * @param soundId The previously playing sound instance
   * @param volume  The sound volume
   * @return the new sound instance for this asset.
   */
  public long playSound(SoundEffect sound, long soundId, float volume) {
    if (soundId != -1 && sound.isPlaying(soundId)) {
      sound.stop(soundId);
    }
    return sound.play(volume);
  }
}