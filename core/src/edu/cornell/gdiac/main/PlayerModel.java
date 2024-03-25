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
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.CapsuleObstacle;
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
   * Whether we are currently frozen
   */
  private boolean isFrozen;
  /**
   * Ground sensor to represent our feet
   */
  private Fixture sensorFixture;
  private PolygonShape sensorShape;
  /**
   * The name of the sensor for detection purposes
   */
  private String sensorName;
  /**
   * The color to paint the sensor in debug mode
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
   * Field to temporarily hold player sensor size x
   */
  private float sensorSizeX;

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
    sensorSizeX = 0;
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
   * Returns true if
   */

  /**
   * Returns true if the player is actively jumping.
   *
   * @return true if the player is actively jumping.
   */
  public boolean getIsJumping() {
    return isJumping && jumpCooldown <= 0 && isGrounded;
  }

  /**
   * Sets whether the player is actively jumping.
   *
   * @param value whether the player is actively jumping.
   */

  public void setJumping(boolean value) {
    isJumping = value;
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
   * Returns the name of the ground sensor
   * <p>
   * This is used by ContactListener
   *
   * @return the name of the ground sensor
   */
  public String getSensorName() {
    return sensorName;
  }

  /**
   * Sets the name of the ground sensor
   * <p>
   * This is used by ContactListener
   *
   * @param name the name of the ground sensor
   */
  public void setSensorName(String name) {
    sensorName = name;
  }

  /**
   * Returns true if this character is facing right
   *
   * @return true if this character is facing right
   */
  public boolean isFacingRight() {
    return faceRight;
  }


  public void initialize(AssetDirectory directory, JsonValue json) {
    frozenTexture = new TextureRegion(directory.getEntry("frozen", Texture.class));
    JsonValue properties = json.get("properties").child();
    while (properties != null) {
      switch (properties.getString("name")) {
        case "force":
          setForce(properties.getFloat("value"));
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
        case "sensorsizex":
          sensorSizeX = properties.getFloat("value");
          break;
        case "sensorsizey":
          Vector2 sensorCenter = new Vector2(0, -getHeight() / 2);
          float sSizeY = properties.getFloat("value");
          if (sensorSizeX == 0) {
            System.out.println("Sensor size X has not yet been set");
          }
          sensorShape = new PolygonShape();
          sensorShape.setAsBox(sensorSizeX, sSizeY, sensorCenter, 0.0f);
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
        case "sensorname":
          setSensorName(properties.getString("value"));
          break;
        case "fallMultiplier":
          fallMultiplier = properties.getFloat("value");
          break;
        case "lowJumpMultiplier":
          lowJumpMultiplier = properties.getFloat("value");
          break;
        default:
          break;
      }

      properties = properties.next();
    }
  }

  /**
   * Creates the physics Body(s) for this object, adding them to the world.
   * <p>
   * This method overrides the base method to keep your ship from spinning.
   *
   * @param world Box2D world to store body
   * @return true if object allocation succeeded
   */
  public boolean activatePhysics(World world) {
    // create the box from our superclass
    if (!super.activatePhysics(world)) {
      return false;
    }

    // Ground Sensor
    // -------------
    // We only allow the player to jump when he's on the ground.
    // Double jumping is not allowed.
    //
    // To determine whether or not the player is on the ground,
    // we create a thin sensor under his feet, which reports
    // collisions with the world but has no collision response.
    FixtureDef sensorDef = new FixtureDef();
    sensorDef.density = getDensity();
    sensorDef.isSensor = true;
    sensorDef.shape = sensorShape;
    sensorFixture = body.createFixture(sensorDef);
    sensorFixture.setUserData(getSensorName());

    return true;
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
    }

    body.applyForce(v2Cache, getPosition(), true);
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
}