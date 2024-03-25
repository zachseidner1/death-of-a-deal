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
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.CapsuleObstacle;
import java.lang.reflect.Field;

/**
 * NPC for the platform game.
 * <p>
 * Note that the constructor does very little.  The true initialization happens by reading the JSON
 * value.
 */
public class NPCModel extends CapsuleObstacle {

  /**
   * The maximum npc speed
   */
  private float maxspeed;

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
   * For the color to draw sensors in debug
   */
  private Color sensorColor;
  /**
   * Tint for drawing the color (blue if isFrozen)
   */
  private Color color;

  private boolean isStop;
  private Fixture bottomFixture; // Declare the bottom fixture.

  /**
   * Creates a new player with degenerate settings
   * <p>
   * The main purpose of this constructor is to set the initial capsule orientation.
   */
  public NPCModel() {
    super(0, 0, 0.5f, 1.0f);
    setFixedRotation(true);
    isStop = false;
    // Gameplay attributes
    color = Color.WHITE;
    setDensity(1);
    setGravityScale(0.0f);
  }

  public void setStop(boolean stop) {
    isStop = stop;
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

  @Override
  protected void createFixtures() {
    super.createFixtures();
    if (body != null && !body.getFixtureList().isEmpty()) {
      for (Fixture fixture : body.getFixtureList()) {
        // set each fixture as a sensor to make it pass-through
        fixture.setSensor(true);
      }
    }
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
        case "maxspeed":
          setMaxSpeed(properties.getFloat("value"));
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
   * Applies the movement to the NPC
   */
  public void applyMovement() {
    float targetSpeed = maxspeed;
    if (isStop) {
      targetSpeed = 0;
    }
    setVX(targetSpeed);
    setVY(0.0f);
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
      canvas.draw(texture, color, origin.x, origin.y,
          getX() * drawScale.x,
          getY() * drawScale.y, getAngle(), effect, 1.0f);
    }
  }
}