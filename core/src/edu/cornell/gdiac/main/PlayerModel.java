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
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
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
  private final float FROZEN_DENSITY = 100.0f;
  /**
   * Whether the character has a higher density in the frozen state which causes them to slide
   */
  private boolean shouldSlide;
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
   * The impulse for the character jump
   */
  private float jumppulse;
  /**
   * Cooldown (in animation frames) for jumping
   */
  private int jumpLimit;
  /**
   * The current horizontal movement of the character
   */
  private float movement;
  /**
   * Which direction is the character facing
   */
  private boolean faceRight;
  /**
   * Whether our feet are on the ground
   */
  private boolean isGrounded;

  /**
   * Whether we are actively bouncing
   */

  // SENSOR FIELDS
  /**
   * How long until we can jump again
   */
  private int jumpCooldown;
  /**
   * Whether we are actively jumping
   */
  private boolean isJumping;
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
   * Cache for internal force calculations
   */
  private Vector2 forceCache = new Vector2();
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
    shouldSlide = false;
    color = Color.WHITE;
    sensorSizeX = 0;

    jumpCooldown = 0;
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
    movement = value;
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

  /**
   * Returns true if
   */

  /**
   * Returns true if the player is actively jumping.
   *
   * @return true if the player is actively jumping.
   */
  public boolean isJumping() {
    return isJumping && jumpCooldown <= 0 && isGrounded;
  }

  /**
   * Sets whether the player is actively jumping.
   *
   * @param value whether the player is actively jumping.
   */
  public void setJumping(boolean value) {
    isJumping = value && !isFrozen;
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
  public boolean isFrozen() {
    return isFrozen;
  }

  /**
   * Sets whether the player is frozen
   *
   * @param value true if the player is frozen, false otherwise
   */
  public void setFrozen(boolean value) {
    isFrozen = value;
    if (isFrozen) {
      if (shouldSlide) {
        setDensity(FROZEN_DENSITY);
      }

      body.applyLinearImpulse(0, shouldSlide ? -10 : -1, 0, 0, true);
    } else {
      if (shouldSlide) {
        setDensity(INITIAL_DENSITY);
      }

    }
  }

  public void setShouldSlide(boolean value) {
    shouldSlide = value;
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
   * Returns the upward impulse for a jump.
   *
   * @return the upward impulse for a jump.
   */
  public float getJumpPulse() {
    return jumppulse;
  }

  /**
   * Sets the upward impulse for a jump.
   *
   * @param value the upward impulse for a jump.
   */
  public void setJumpPulse(float value) {
    jumppulse = value;
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
    float x = json.getFloat("x") * (1/drawScale.x);
    float y = (gSizeY - json.getFloat("y"))* (1/drawScale.y);
    setPosition(x, y);
    float width = json.getFloat("width") * (1/drawScale.x);
    float height = json.getFloat("height") * (1/drawScale.y);
    setDimension(width, height);

    JsonValue properties = json.get("properties").child();
    Color debugColor = null;
    int debugOpacity = -1;
    while (properties != null){
      switch (properties.getString("name")){
        case "bodytype":
          setBodyType(properties.get("value").asString().equals("static") ? BodyDef.BodyType.StaticBody
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
        case "jumppulse":
          setJumpPulse(properties.getFloat("value"));
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
        case "sensorsizex" :
          sensorSizeX = properties.getFloat("value");
        case "sensorsizey":
          Vector2 sensorCenter = new Vector2(0, -getHeight() / 2);
          float sSizeY = properties.getFloat("value");
          if (sensorSizeX == 0){
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
          if (sensorColor != null){
            sensorColor.mul(opacity / 255.0f);
          }
          break;
        case "sensorname":
          setSensorName(properties.getString("value"));
          break;
        default:
          break;
      }

      if (debugOpacity != -1 && debugColor != null) {
        debugColor.mul(debugOpacity / 255f);
        setDebugColor(debugColor);
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

    // Don't want to be moving. Damp out player motion
    if (getMovement() == 0f) {
      forceCache.set(-getDamping() * getVX(), 0);
      body.applyForce(forceCache, getPosition(), true);
    }

    // Velocity too high, clamp it
    if (Math.abs(getVX()) >= getMaxSpeed()) {
      setVX(Math.signum(getVX()) * getMaxSpeed());
    } else {
      forceCache.set(getMovement(), 0);
      body.applyForce(forceCache, getPosition(), true);
    }

    // Jump!
    if (isJumping()) {
      forceCache.set(0, getJumpPulse());
      body.applyLinearImpulse(forceCache, getPosition(), true);
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
    // Apply cooldowns
    if (isJumping()) {
      jumpCooldown = getJumpLimit();
    } else {
      jumpCooldown = Math.max(0, jumpCooldown - 1);
    }

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