/*
 * playerModel.java
 *
 * This is a refactored version of playerModel that allows us to read its properties
 * from a JSON file.  As a result, it has a lot more getter and setter "hooks" than
 * in lab.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * JSON version, 3/2/2016
 */
package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import java.lang.reflect.Field;

/**
 * Player avatar for the plaform game.
 * <p>
 * Note that the constructor does very little.  The true initialization happens by reading the JSON
 * value.
 */
public class PlayerModel extends CapsuleObstacle {
  // Physics constants
  /**
   * The initial density of the player configured from the JSON
   */
  private final float INITIAL_DENSITY = 1.0f;
  /**
   * frozen density
   */
  private final float FROZEN_DENSITY = 50.0f;
  /**
   * Cache for internal force calculations
   */
  private final Vector2 v2Cache = new Vector2();
  /**
   * The factor to multiply by the input
   */
  private float force;
  /**
   * The amount to slow the character down
   */
  private float damping;
  /**
   * The maximum character speed
   */
  private float maxspeed;
  /**
   * Cooldown (in animation frames) for jumping
   */
  private int jumpLimit;
  /**
   * The current horizontal movement of the character.
   * <p></p>
   * Is 1 if the player is moving right, and -1 otherwise
   */
  private float movement;
  /**
   * Which direction is the character facing
   */
  private boolean faceRight;

  // SENSOR FIELDS
  /**
   * Whether our feet are on the ground
   */
  private boolean isGrounded;
  /**
   * How long until we can jump again
   */
  private int jumpCooldown;
  /**
   * Whether we are actively jumping
   */
  private boolean isJumping;
  // SENSOR FIELDS
  /**
   * The velocity the character initially gains on jump
   */
  private float jumpVelocity;
  /**
   * Whether we are actively trying to drop
   */
  private boolean isDropping;
  /**
   * Whether we are currently frozen
   */
  private boolean isFrozen;
  /**
   * Ground sensor to represent our feet
   */
  private GroundSensor groundSensor;
  private Fixture groundSensorFixture;
  /**
   * Body sensor
   */
  private BodySensor bodySensor;
  private Fixture bodySensorFixture;
  /**
   * For the color to draw sensors in debug
   */
  private Color sensorColor;
  /**
   * Tint for drawing the color (blue if isFrozen)
   */
  private Color color;
  /**
   * The texture to use in the frozen state
   */
  private TextureRegion frozenTexture;

  /**
   * The multiplier gravity receives down for a low jump (short hop)
   */
  private float lowJumpMultiplier;
  /**
   * The multiplier gravity receives when the player is falling
   */
  private float fallMultiplier;

  /**
   * Creates a new player with degenerate settings
   * <p>
   * The main purpose of this constructor is to set the initial capsule orientation.
   */
  public PlayerModel() {
    super(0, 0, 0.5f, 1.0f);
    setFixedRotation(true);

    // Gameplay attributes
    isGrounded = false;
    isJumping = false;
    isFrozen = false;
    faceRight = true;
    color = Color.WHITE;
    setDensity(1);

    jumpCooldown = 0;
  }

  /**
   * @return The multiplier gravity receives for a low jump (short hop), when the player is moving
   * upwards but not holding the jump button
   */
  public float getLowJumpMultiplier() {
    return lowJumpMultiplier;
  }

  /**
   * @return The multiplier gravity receives as the player is falling
   */
  public float getFallMultiplier() {
    return fallMultiplier;
  }


  /**
   * Returns left/right movement of this character.
   * <p>
   * This is the result of input times player force.
   *
   * @return left/right movement of this character.
   */
  public float getMovement() {
    return movement;
  }

  /**
   * Sets left/right movement of this character.
   * <p>
   * This is the result of input times player force.
   *
   * @param value left/right movement of this character.
   */
  public void setMovement(float value) {
    movement = value / 10F;
    if (isFrozen) {
      movement = 0;
    }
    // Change facing if appropriate
    if (movement < 0) {
      faceRight = false;
    } else if (movement > 0) {
      faceRight = true;
    }
  }

  public void setJumpVelocity(float value) {
    jumpVelocity = value;
  }

  /**
   * Returns true if the player is actively jumping.
   */
  public boolean getIsJumping() {
    return isJumping && jumpCooldown <= 0 && isGrounded;
  }

  /**
   * Sets whether the player is actively jumping.
   */

  public void setJumping(boolean value) {
    isJumping = value;
  }

  /**
   * Returns true if the player is actively dropping
   */
  public boolean getIsDropping() {
    return isDropping;
  }

  /**
   * Sets whether the playter is actively dropping.
   */
  public void setDropping(boolean value) {
    isDropping = value;
  }

  /**
   * Returns true if the player is on the ground.
   *
   * @return true if the player is on the ground.
   */
  public boolean isGrounded() {
    return isGrounded;
  }

  /**
   * Sets whether the player is on the ground.
   *
   * @param value whether the player is on the ground.
   */
  public void setGrounded(boolean value) {
    isGrounded = value;
  }

  /**
   * Returns whether the player is frozen
   */
  public boolean getIsFrozen() {
    return isFrozen;
  }

  /**
   * Sets whether the player is frozen
   *
   * @param value true if the player is frozen, false otherwise
   */
  public void setFrozen(boolean value) {
    isFrozen = value;
    setDensity(isFrozen ? FROZEN_DENSITY : INITIAL_DENSITY);
  }

  /**
   * Returns how much force to apply to get the player moving
   * <p>
   * Multiply this by the input to get the movement value.
   *
   * @return how much force to apply to get the player moving
   */
  public float getForce() {
    return force;
  }

  /**
   * Sets how much force to apply to get the player moving
   * <p>
   * Multiply this by the input to get the movement value.
   *
   * @param value how much force to apply to get the player moving
   */
  public void setForce(float value) {
    force = value;
  }

  /**
   * Returns how hard the brakes are applied to get a player to stop moving
   *
   * @return how hard the brakes are applied to get a player to stop moving
   */
  public float getDamping() {
    return damping;
  }

  /**
   * Sets how hard the brakes are applied to get a player to stop moving
   *
   * @param value how hard the brakes are applied to get a player to stop moving
   */
  public void setDamping(float value) {
    damping = value;
  }

  /**
   * Returns the upper limit on player left-right movement.
   * <p>
   * This does NOT apply to vertical movement.
   *
   * @return the upper limit on player left-right movement.
   */
  public float getMaxSpeed() {
    return maxspeed;
  }

  /**
   * Sets the upper limit on player left-right movement.
   * <p>
   * This does NOT apply to vertical movement.
   *
   * @param value the upper limit on player left-right movement.
   */
  public void setMaxSpeed(float value) {
    maxspeed = value;
  }

  /**
   * Returns the cooldown limit between jumps
   *
   * @return the cooldown limit between jumps
   */
  public int getJumpLimit() {
    return jumpLimit;
  }

  /**
   * Sets the cooldown limit between jumps
   *
   * @param value the cooldown limit between jumps
   */
  public void setJumpLimit(int value) {
    jumpLimit = value;
  }

  /**
   * Returns true if this character is facing right
   *
   * @return true if this character is facing right
   */
  public boolean isFacingRight() {
    return faceRight;
  }

  /**
   * Initializes the player via the given JSON value
   * <p>
   * The JSON value has been parsed and is part of a bigger level file.  However, this JSON value is
   * limited to the player subtree
   *
   * @param directory the asset manager
   * @param json      the JSON subtree defining the player
   */
  public void initialize(AssetDirectory directory, JsonValue json, int gSizeY) {
    setName(json.get("name").asString());

    // Set position and dimension
    float x = json.getFloat("x") * (1 / drawScale.x);
    float y = (gSizeY - json.getFloat("y")) * (1 / drawScale.y);
    setPosition(x, y);

    float width = json.getFloat("width") * (1 / drawScale.x);
    float height = json.getFloat("height") * (1 / drawScale.y);
    setDimension(width, height);

    JsonValue properties = json.get("properties").child();
    Color debugColor = null;
    int debugOpacity = -1;
    while (properties != null) {
      switch (properties.getString("name")) {
        case "bodytype":
          setBodyType(
            properties.get("value").asString().equals("static") ? BodyDef.BodyType.StaticBody
              : BodyDef.BodyType.DynamicBody);
          break;
        case "density":
          setDensity(properties.getFloat("value"));
          break;
        case "friction":
          setFriction(properties.getFloat("value"));
          break;
        case "restitution":
          setRestitution(properties.getFloat("value"));
          break;
        case "force":
          setForce(properties.getFloat("value"));
          break;
        case "damping":
          setDamping(properties.getFloat("value"));
          break;
        case "maxspeed":
          setMaxSpeed(properties.getFloat("value"));
          break;
        case "jumpvelocity":
          setJumpVelocity(properties.getFloat("value"));
          break;
        case "jumplimit":
          setJumpLimit(properties.getInt("value"));
          break;
        case "debugcolor":
          try {
            String cname = properties.getString("value").toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color) field.get(null));
          } catch (Exception e) {
            debugColor = null;
          }
          setDebugColor(debugColor);
          break;
        case "debugopacity":
          debugOpacity = properties.getInt("value");
          setDebugColor(getDebugColor().mul(debugOpacity / 255.0f));
          break;
        case "texture":
          String key = properties.getString("value");
          TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
          frozenTexture = new TextureRegion(directory.getEntry("frozen", Texture.class));
          setTexture(texture);
          break;
        case "sensorcolor":
          try {
            String cname = properties.getString("value").toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            sensorColor = new Color((Color) field.get(null));
          } catch (Exception e) {
            sensorColor = null; // Not defined
          }
          break;
        case "sensoropacity":
          int opacity = properties.get("value").asInt();
          if (sensorColor != null) {
            sensorColor.mul(opacity / 255.0f);
          }
          break;
        case "fallMultiplier":
          fallMultiplier = properties.getFloat("value");
        case "lowJumpMultiplier":
          lowJumpMultiplier = properties.getFloat("value");
        default:
          break;
      }

      if (debugOpacity != -1 && debugColor != null) {
        debugColor.mul(debugOpacity / 255f);
        setDebugColor(debugColor);
      }

      properties = properties.next();
    }

    initFixtureDefs(width, height);
  }

  public void initFixtureDefs(float width, float height) {
    // Can be set in Tiled, but good-enough default here (we want a very thin sensor)
    float defaultSensorHeight = 0.005f;

    // Create the head fixture def
    bodySensor = new BodySensor(0, 0, width / 2, height / 2);

    // Create the bottom fixture def
    float groundYRel = -height / 2;
    // To represent the feet (smaller width)
    float defaultGroundSensorScale = 0.2f;
    groundSensor = new GroundSensor(0, groundYRel, width / 2 * defaultGroundSensorScale, defaultSensorHeight);
  }

  @Override
  public void createFixtures() {
    super.createFixtures();

    if (body == null) {
      return;
    }

    // Ground Sensor
    // -------------
    // We only allow the player to jump when he's on the ground.
    // Double jumping is not allowed.
    //
    // To determine whether the player is on the ground,
    // we create a thin sensor under his feet, which reports
    // collisions with the world but has no collision response.
    //
    // We can track what platform/obstacle the player is currently
    // standing on by keeping it in track in the state of the
    // ground sensor.

    // Head Sensor
    // -------------
    // We use this sensor to determine special contact logic when
    // the player collides with a platform by jumping up

    // Create the sensor fixtures
    FixtureDef groundFixtureDef = groundSensor != null ? groundSensor.getFixtureDef() : null;
    if (groundFixtureDef != null) {
      groundSensorFixture = body.createFixture(groundFixtureDef);
      groundSensorFixture.setUserData(groundSensor);
    }

    FixtureDef headFixtureDef = bodySensor != null ? bodySensor.getFixtureDef() : null;
    if (headFixtureDef != null) {
      bodySensorFixture = body.createFixture(headFixtureDef);
      bodySensorFixture.setUserData(bodySensor);
    }

    assert bodySensor != null;
    assert headFixtureDef != null;
    assert groundSensor != null;
    assert groundFixtureDef != null;
  }

  @Override
  public void releaseFixtures() {
    super.releaseFixtures();

    if (body == null) {
      return;
    }

    // Destroy sensor fixtures
    if (bodySensorFixture != null) {
      body.destroyFixture(bodySensorFixture);
    }

    if (groundSensorFixture != null) {
      body.destroyFixture(groundSensorFixture);
    }
  }


  /**
   * Applies the force to the body of this player
   * <p>
   * This method should be called after the force attribute is set.
   */
  public void applyForce() {
    if (!isActive()) {
      return;
    }
    // The speed the player wants to be at, indicated by their movement
    float targetSpeed = maxspeed * movement;

    float accelRate = 1.2F;
    // If the player is traveling faster than their target speed, do not change movement
    if (Math.abs(getVX()) > Math.abs(targetSpeed) && Math.signum(getVX()) == Math.signum(
      targetSpeed) && Math.abs(targetSpeed) > 0.01F) {
      accelRate = 0;
    }
    // If the player is trying to go to 0 speed, apply deceleration rate (faster)
    if (Math.abs(targetSpeed) < 0.01F) {
      accelRate = 1.8F;
    }

    // We move the player based on how far they are from their target speed
    float speedDif = targetSpeed - (getVX());
    float movement = speedDif * accelRate;
    v2Cache.set(movement, 0);

    // Jump!
    if (getIsJumping() && !getIsFrozen()) {
      setVY(jumpVelocity);

      // Clear platform in ground sensor
      groundSensor.clearPlatforms();
    }

    // If on top of a pass-through platform and pressing drop, set the tile to pass-through
    dropPassThroughPlatform();

    body.applyForce(v2Cache, getPosition(), true);
  }

  /**
   * Checks whether tile is pass-through and set to pass-through if player dropping
   */
  private void dropPassThroughPlatform() {
    if (!isDropping) {
      return;
    }

    if (groundSensor.currPlatform == null) {
      assert groundSensor.prevPlatform == null;
      return;
    }

    if (!(groundSensor.currPlatform instanceof PassThroughPlatformModel)) {
      return;
    }

    ((PassThroughPlatformModel) groundSensor.currPlatform).setPassThrough(true);

    if (groundSensor.prevPlatform == null || !(groundSensor.prevPlatform instanceof PassThroughPlatformModel)) {
      return;
    }

    assert !groundSensor.prevPlatform.equals(groundSensor.currPlatform);

    // Check whether the previous platform is a neighboring tile
    Vector2 currPlatformBodyCenter = groundSensor.currPlatform.getBody().getWorldCenter();
    Vector2 prevPlatformBodyCenter = groundSensor.prevPlatform.getBody().getWorldCenter();

    float xDiffThres = 0.5f; // Set to be within a tile width
    float yDiffThres = 0.005f; // Set to arbitrarily small amount to account for float arith
    boolean isNeighborX = Math.abs(currPlatformBodyCenter.x - prevPlatformBodyCenter.x) <= xDiffThres;
    boolean isNeighborY = Math.abs(currPlatformBodyCenter.y - prevPlatformBodyCenter.y) <= yDiffThres;

    if (isNeighborX && isNeighborY) {
      ((PassThroughPlatformModel) groundSensor.prevPlatform).setPassThrough(true);
    }
  }

  /**
   * Updates the object's physics state (NOT GAME LOGIC).
   * <p>
   * We use this method to reset cooldowns.
   *
   * @param dt Number of seconds since last animation frame
   */
  public void update(float dt) {
    super.update(dt);
  }

  /**
   * Draws the physics object.
   *
   * @param canvas Drawing context
   */
  public void draw(GameCanvas canvas) {
    if (texture != null) {
      float effect = faceRight ? 1.0f : -1.0f;
      canvas.draw(isFrozen ? frozenTexture : texture, color, origin.x, origin.y,
        getX() * drawScale.x,
        getY() * drawScale.y, getAngle(), effect, 1.0f);
    }
  }

  public class BodySensor extends BoxFixtureSensor<PlayerModel> {
    public BodySensor(float x, float y, float width2, float height2) {
      super(PlayerModel.this, x, y, width2, height2);
    }

    @Override
    public void beginContact(Obstacle obs, Fixture fixture) {
      // TODO: Can refactor breakable platform under this logic for consistency
    }

    @Override
    public void endContact(Obstacle obs, Fixture fixture) {
    }
  }

  public class GroundSensor extends BoxFixtureSensor<PlayerModel> {
    /**
     * The current platform the player is standing on
     */
    Obstacle currPlatform;

    /**
     * Previous platform (since player can be standing between two pass through tiles. Can also serve as cache.
     */
    Obstacle prevPlatform;

    public GroundSensor(float x, float y, float width2, float height2) {
      super(PlayerModel.this, x, y, width2, height2);

      clearPlatforms();
    }

    public void clearPlatforms() {
      currPlatform = prevPlatform = null;
    }

    @Override
    public void beginContact(Obstacle bodyData, Fixture fixture) {
      if (bodyData != currPlatform && !fixture.isSensor()) {
        prevPlatform = currPlatform;
        currPlatform = bodyData;
      }

      // TODO: Can migrate some of the logic (setGrounded) from collision controller here. Notice
      // that we are slowly drifting away from model territory, which is fine. If it gets to complicated,
      // it should be simple to call this enclosing class a PlayerController, and just make an inner
      // public inner PlayerModel class that is simpler and purely store core player states and getters/setters.
      // We can do this for other models too. In and of itself, this would make the initialization logic
      // to parse from Tiled json make more sense too, as separate from the actual model.
    }

    @Override
    public void endContact(Obstacle bodyData, Fixture fixture) {
    }
  }
}