/*
 * LevelMode.java
 *
 * This stores all of the information to define a level in our simple platform game.
 * We have an avatar, some walls, some platforms, and an exit.  This is a refactoring
 * of WorldController in Lab 4 that separates the level data from the level control.
 *
 * Note that most of the methods are getters and setters, as is common with models.
 * The gameplay behavior is defined by GameController.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * JSON version, 3/2/2016
 */
package edu.cornell.gdiac.main;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.util.PooledList;

/**
 * Represents a single level in our game
 *
 * <p>Note that the constructor does very little. The true initialization happens by reading the
 * JSON value. To reset a level, dispose it and reread the JSON.
 *
 * <p>The level contains its own Box2d World, as the World settings are defined by the JSON file.
 * However, there is absolutely no controller code in this class, as the majority of the methods are
 * getters and setters. The getters allow the GameController class to modify the level elements.
 */
public class LevelModel {

  /**
   * The initial air resistance of the level from the levels JSON
   */
  private final float INITIAL_AIR_RESISTANCE = 0.1f;

  /**
   * The Box2D world
   */
  protected World world;

  /**
   * The boundary of the world
   */
  protected Rectangle bounds;

  /**
   * The world scale
   */
  protected Vector2 scale;

  // Physics objects for the game
  /**
   * All the objects in the world.
   */
  protected PooledList<Obstacle> objects = new PooledList<Obstacle>();

  /**
   * Reference to the character avatar
   */
  private PlayerModel avatar;

  /**
   * Reference to the bounce pad (for collision detection)
   */
  private BouncePlatformModel bouncePlatformModel;

  /**
   * Reference to the goalDoor (for collision detection)
   */
  private ExitModel goalDoor;

  /**
   * Whether or not the level is in debug more (showing off physics)
   */
  private boolean debug;

  /**
   * Whether or not the level is completed
   */
  private boolean complete;
  /**
   * Time limit for the level
   */
  private float timer;
  /**
   * Air resistance scale to be applied to every obstacle in the level
   */
  private float airResistance = INITIAL_AIR_RESISTANCE;
  /**
   * Cache for internal force calculations
   */
  private Vector2 forceCache = new Vector2();

  /**
   * Creates a new LevelModel
   *
   * <p>The level is empty and there is no active physics world. You must read the JSON file to
   * initialize the level
   */
  public LevelModel() {
    world = null;
    bounds = new Rectangle(0, 0, 1, 1);
    scale = new Vector2(1, 1);
    debug = false;
  }

  /**
   * Returns the bounding rectangle for the physics world
   *
   * <p>The size of the rectangle is in physics, coordinates, not screen coordinates
   *
   * @return the bounding rectangle for the physics world
   */
  public Rectangle getBounds() {
    return bounds;
  }

  /**
   * Returns the scaling factor to convert physics coordinates to screen coordinates
   *
   * @return the scaling factor to convert physics coordinates to screen coordinates
   */
  public Vector2 getScale() {
    return scale;
  }

  /**
   * Returns a reference to the Box2D World
   *
   * @return a reference to the Box2D World
   */
  public World getWorld() {
    return world;
  }

  /**
   * Returns a reference to the player avatar
   *
   * @return a reference to the player avatar
   */
  public PlayerModel getAvatar() {
    return avatar;
  }

  /**
   * Returns a reference to the exit door
   *
   * @return a reference to the exit door
   */
  public ExitModel getExit() {
    return goalDoor;
  }

  /**
   * Returns whether this level is currently in debug node
   *
   * <p>If the level is in debug mode, then the physics bodies will all be drawn as wireframes
   * onscreen
   *
   * @return whether this level is currently in debug node
   */
  public boolean getDebug() {
    return debug;
  }

  /**
   * Sets whether this level is currently in debug node
   *
   * <p>If the level is in debug mode, then the physics bodies will all be drawn as wireframes
   * onscreen
   *
   * @param value whether this level is currently in debug node
   */
  public void setDebug(boolean value) {
    debug = value;
  }

  /**
   * Return level completion state to caller
   *
   * @return
   */
  public boolean getComplete() {
    return complete;
  }

  /**
   * Set the level to complete state or non-complete state
   *
   * @param value
   */
  public void setComplete(boolean value) {
    complete = value;
  }
  /**
   * Return the time limit
   */
  public float getTimer() {
    return timer;
  }
  /**
   * Lays out the game geography from the given JSON file
   *
   * @param directory   the asset manager
   * @param levelFormat the JSON file defining the level
   */
  public void populate(AssetDirectory directory, JsonValue levelFormat) {
    int tileWidth = levelFormat.getInt("tilewidth");
    int tileHeight = levelFormat.getInt("tileheight");
    int numTilesVertical = levelFormat.getInt("height");
    int numTilesHorizontal = levelFormat.getInt("width");

    float gravity = 0;
    float[] pSize = new float[2];
    JsonValue property = levelFormat.get("properties").child();
    // get map properties (applies to entire level)
    while (property != null) {
      switch (property.getString("name")) {
        case "airresistance":
          airResistance = property.getFloat("value");
          break;
        case "gravity":
          gravity = property.getFloat("value");
          break;
        // the width for box 2D physics
        case "pwidth":
          pSize[0] = property.getFloat("value");
          break;
        // the height for box 2D physics
        case "pheight":
          pSize[1] = property.getFloat("value");
          break;
      }
      property = property.next();
    }

    // graphics size is tile width * the number of tiles horizontally
    // by the tile height * the number of tiles vertically
    int[] gSize = {numTilesHorizontal * tileWidth, numTilesVertical * tileHeight};

    world = new World(new Vector2(0, gravity), false);
    bounds = new Rectangle(0, 0, pSize[0], pSize[1]);
    scale.x = gSize[0] / pSize[0];
    scale.y = gSize[1] / pSize[1];

    // Add level goal
//    goalDoor = new ExitModel();
//    goalDoor.initialize(directory, levelFormat.get("exit"));
//    goalDoor.setDrawScale(scale);
//    activate(goalDoor);

    JsonValue layer = levelFormat.get("layers").child();
    while (layer != null) {
      JsonValue tileProperties = null;
      if (layer.get("properties") != null) {
        tileProperties = layer.get("properties").child();
      }
      switch (layer.getString("name")) {
        case "level":
          makeTiles(numTilesHorizontal, numTilesVertical, layer.get("data").asIntArray(), tileWidth,
              tileHeight,
              directory, tileProperties);
          break;
        case "objects":
          if (layer.get("objects") != null) {
            makeObjects(directory, layer.get("objects").child(), gSize[1]);
          }
          break;
        case "deco":
          // TODO make decorations
          break;
      }
      layer = layer.next();
    }

    // Create dude
    // Does this need to change now?
//    avatar = new PlayerModel();
//    avatar.initialize(directory, levelFormat.get("avatar"));
//    avatar.setDrawScale(scale);
//    activate(avatar);
  }

  /**
   * Adds tiles to the level according to the data array provided by Tiled.
   *
   * @param cols           the number of columns of the data array
   * @param rows           the number of rows of the data array
   * @param data           the data array
   * @param tileWidth      the width of a tile in pixels
   * @param tileHeight     the height of a tile in pixels
   * @param directory      the asset directory
   * @param tileProperties additional tile properties
   */
  private void makeTiles(int cols, int rows, int[] data, int tileWidth, int tileHeight,
      AssetDirectory directory, JsonValue tileProperties) {
    for (int i = 0; i < data.length; i++) {
      if (data[i] != 0) {
        // i % numCols = how deep in x
        // i / numCols = how deep in y
        int xPos = (i % cols) * tileWidth;
        // subtract from full height since data starts at the top
        int yPos = tileHeight * rows - (i / cols) * tileHeight;
        PlatformModel obj = new PlatformModel();
        obj.setDrawScale(scale);
        obj.initializeAsTile(xPos, yPos, (float) tileHeight, directory, "" + data[i],
            tileProperties);
        activate(obj);
      }
    }
  }

  private void makeObjects(AssetDirectory directory, JsonValue objects, int gSizeY) {
    while (objects != null) {
      switch (objects.getString("name")) {
        case "player":
          avatar = new PlayerModel();
          avatar.setDrawScale(scale);
          avatar.initialize(directory, objects, gSizeY);
          activate(avatar);
          break;
        case "exit":
          goalDoor = new ExitModel();
          goalDoor.setDrawScale(scale);
          goalDoor.initialize(directory, objects, gSizeY);
          activate(goalDoor);
          break;
        case "slope":
          SlopeModel slope = new SlopeModel();
          slope.setDrawScale(scale);
          slope.initialize(directory, objects, gSizeY);
          activate(slope);
          break;
        case "Bounce":
          BouncePlatformModel platform= new BouncePlatformModel();
          platform.setDrawScale(scale);
          platform.initialize(directory,objects,gSizeY);
          activate(platform);
      }
      objects = objects.next();
    }
  }

  public void dispose() {
    for (Obstacle obj : objects) {
      obj.deactivatePhysics(world);
    }
    objects.clear();
    if (world != null) {
      world.dispose();
      world = null;
    }
  }

  /**
   * Immediately adds the object to the physics world
   *
   * <p>param obj The object to add
   */
  protected void activate(Obstacle obj) {
    assert inBounds(obj) : "Object is not in bounds";
    objects.add(obj);
    obj.activatePhysics(world);
  }

  /**
   * Applies air resistance to all objects
   * <p>
   * TODO: Figure out rotational air resistance
   */
  public void applyAirResistance() {
    for (Obstacle obj : objects) {
      // TODO: Refactor (can have obstacles implement air resistance interface to determine whether this force should be applied

      float velX = obj.getVX();
      float velY = obj.getVY();

      // Apply air resistance force opposite of velocity
      float airResistanceX = -Math.signum(velX) * airResistance * velX * velX;
      float airResistanceY = -Math.signum(velY) * airResistance * velY * velY;

      forceCache.set(airResistanceX, airResistanceY);
      obj.getBody().applyForce(forceCache, obj.getPosition(), true);
    }
  }

  /**
   * Returns true if the object is in bounds.
   *
   * <p>This assertion is useful for debugging the physics.
   *
   * @param obj The object to check.
   * @return true if the object is in bounds.
   */
  private boolean inBounds(Obstacle obj) {
    boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x + bounds.width);
    boolean vert = (bounds.y <= obj.getY() && obj.getY() <= bounds.y + bounds.height);
    return horiz && vert;
  }

  /**
   * Draws the level to the given game canvas
   *
   * <p>If debug mode is true, it will outline all physics bodies as wireframes. Otherwise it will
   * only draw the sprite representations.
   *
   * @param canvas the drawing context
   */
  public void draw(GameCanvas canvas) {
    canvas.clear();

    canvas.begin();
    for (Obstacle obj : objects) {
      obj.draw(canvas);
    }
    canvas.end();

    if (debug) {
      canvas.beginDebug();
      for (Obstacle obj : objects) {
        obj.drawDebug(canvas);
      }
      canvas.endDebug();
    }
  }
}
