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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
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
  private float defaultSpeed;

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

  private float distanceToDoor = 0.0f;

  private int timer = 0;
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

  /**
   * Sets the distance between an NPC and a door based on their locations. The distance is
   * calculated using the Euclidean distance between the two points.
   *
   * @param npcLoc  The Vector2 location of the NPC.
   * @param doorLoc The Vector2 location of the door.
   */
  public void setDistance(Vector2 npcLoc, Vector2 doorLoc) {
    distanceToDoor = npcLoc.dst(doorLoc);
  }

  /**
   * Sets the stop status for the NPC. When set to true, the NPC or character may stop moving.
   *
   * @param stop The boolean flag to set the stop status.
   */
  public void setStop(boolean stop) {
    isStop = stop;
  }

  /**
   * Sets a timer for time needed to reach the goal.
   *
   * @param time The time to set for the timer, in seconds.
   */
  public void setTimer(int time) {
    timer = time;
  }

  /**
   * Returns the upper limit on player left-right movement.
   * <p>
   * This does NOT apply to vertical movement.
   *
   * @return the upper limit on player left-right movement.
   */
  public float getdefaultSpeed() {
    return defaultSpeed;
  }

  /**
   * Sets the upper limit on player left-right movement.
   * <p>
   * This does NOT apply to vertical movement.
   *
   * @param value the upper limit on player left-right movement.
   */
  public void setdefaultSpeed(float value) {
    defaultSpeed = value;
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
  public void initialize(JsonValue json) {
    setName(json.get("name").asString());

    JsonValue properties = json.get("properties").child();
    while (properties != null) {
      switch (properties.getString("name")) {
        case "defaultspeed":
          setdefaultSpeed(properties.getFloat("value"));
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
        case "timer":
          int time = properties.getInt("value");
          setTimer(time);
          break;
        default:
          break;
      }

      properties = properties.next();
    }
  }


  /**
   * Applies the movement to the NPC
   */
  public void applyMovement() {
    float targetSpeed = distanceToDoor / timer;

    if (targetSpeed == 0) {
      targetSpeed = defaultSpeed;
    }

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