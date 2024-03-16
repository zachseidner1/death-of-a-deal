package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import java.lang.reflect.Field;

/**
 * Contains simple state for whether the fan is applying wind force
 */
public class FanModel extends PolygonObstacle {
  final private float DEFAULT_PERIOD = 10.0f;
  final private float DEFAULT_PERIOD_ON_RATIO = 1.0f;
  final private float DEFAULT_CURR_TIME = 0.0f;
  final private float DEFAULT_ROTATION = 0.0f;
  /**
   * When fan is active, represents the period in which a fan turns "on" and "off" once
   */
  private float period = DEFAULT_PERIOD;
  /**
   * When fan is active, represents proportion of period when the fan is "on" and applies a force
   */
  private float periodOnRatio = DEFAULT_PERIOD_ON_RATIO;
  /**
   * Current time for this fan model, in seconds
   */
  private float currentTime = DEFAULT_CURR_TIME;
  /**
   * Rotation of the fan
   */
  private float rotation;
  /**
   * Whether the fan is on and applying wind force (should only be true when fan is active)
   */
  private boolean isOn;
  /**
   * Whether the fan is actively trying to apply wind force (period continues)
   */
  private boolean isFanActive;
  /**
   * The side out of which the fan should direct its wind
   */
  private FanSide fanSide;

  /**
   * Contains the wind fixture def and wind force logic
   */
  private Wind wind;

  /**
   * The wind fixture
   */
  private Fixture windFixture;

  public FanModel() {
    // Since we do not know points yet, initialize to box
    super(new float[]{0, 0, 1, 0, 1, 1, 0, 1}, 0, 0);

    // Wind fixture creation
    wind = new Wind();
  }

  /**
   * Initializes the sloped platform via the given JSON value
   *
   * <p>The JSON value has been parsed and is part of a bigger level file. However, this JSON value
   * is limited to the platform subtree
   *
   * @param directory the asset manager
   * @param json      the JSON subtree defining the platform
   */
  public void initialize(AssetDirectory directory, JsonValue json, int gSizeY) {
    setName(json.getString("name"));

    float scaleFactorX = 1 / drawScale.x;
    float scaleFactorY = 1 / drawScale.y;

    // Note: (x, y) is top-left most point
    float x = json.getFloat("x") * scaleFactorX;
    float y = json.getFloat("y") * scaleFactorY;
    setPosition(x, y);

    float width = json.getFloat("width") * scaleFactorX;
    float height = json.getFloat("height") * scaleFactorY;

    float[] points = {x, y, x + width, y, x + width, y - height, x, y - height};
    initShapes(points);
    initBounds();

    JsonValue properties = json.get("properties").child();
    while (properties != null) {
      switch (properties.getString("name")) {
        case "BodyType":
          setBodyType(properties.getString("value").equals("static") ? BodyDef.BodyType.StaticBody
            : BodyDef.BodyType.DynamicBody);
          break;
        case "DebugColor":
          try {
            String cname = properties.getString("value").toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color) field.get(null));
          } catch (Exception e) {
            debugColor = null; // Not defined
          }
          break;
        case "Type":
          String type = properties.getString("value").toUpperCase();
          switch (type) {
            case "EXPONENTIAL":
              wind.windType = WindType.Exponential;
              break;
            default:
              wind.windType = WindType.Constant;
              break;
          }
          break;
        case "Side":
          // Determine fan side and wind origin, default is to use center of side
          String side = properties.getString("value").toUpperCase();
          switch (side) {
            case "LEFT":
              fanSide = FanSide.LEFT;
              wind.origin.set(x, y - height / 2.0f);
              break;
            case "TOP":
              fanSide = FanSide.TOP;
              wind.origin.set(x + width / 2.0f, y);
              break;
            case "BOTTOM":
              fanSide = FanSide.BOTTOM;
              wind.origin.set(x + width / 2.0f, y - height);
              break;
            default:
              fanSide = FanSide.RIGHT;
              wind.origin.set(x + width, y - height / 2.0f);
              break;
          }
          break;
        case "WindStrength":
          wind.windStrength = properties.getFloat("value");
          break;
        case "WindBreadth":
          wind.windBreadth = properties.getFloat("value") * scaleFactorX;
          break;
        case "WindLength":
          wind.windLength = properties.getFloat("value") * scaleFactorY;
          break;
        case "Period":
          period = properties.getFloat("value");
          break;
        case "PeriodOnRatio":
          periodOnRatio = properties.getFloat("value");
          break;
        case "Active":
          isFanActive = properties.getBoolean("value");
          break;
        case "Rotation":
          rotation = properties.getFloat("value");
          break;
        case "DebugOpacity":
          int opacity = properties.getInt("value");
          setDebugColor(debugColor.mul(opacity / 255.0f));
          break;
        case "FanTexture":
          String key = properties.getString("value");
          TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
          setTexture(texture);
          break;
        case "WindTexture":
          key = properties.getString("value");
          wind.setTexture(new TextureRegion(directory.getEntry(key, Texture.class)));
          break;
        default:
          break;
      }

      properties = properties.next();
    }

    // Configure shape and configure wind fixture
    initWind();
  }

  private void initWind() {
    wind.initialize();

    // Create appropriate polygon texture

  }

  @Override
  protected void createFixtures() {
    super.createFixtures();

    // Create fixture
    if (wind != null && wind.windFixtureDef != null) {
      windFixture = body.createFixture(wind.windFixtureDef);
    }
  }

  @Override
  protected void releaseFixtures() {
    super.releaseFixtures();

    // Destroy fixture
    if (windFixture != null) {
      body.destroyFixture(windFixture);
    }
  }


  /**
   * Callback to be used on collision with wind fixture sensor, should not set active status of sensor.
   * Returns the force to be acted on a non-static obstacle.
   */
  public Vector2 findWindForce(float x, float y) {
    // Check that sensor collision detected when fan is active and on
    if (isFanActive && isOn) {
      return wind.findWindForce(x, y);
    }

    // Set wind force to 0 otherwise
    wind.windForce.set(0, 0);
    return wind.windForce;
  }

  /**
   * Get active status of fan
   */
  public boolean getFanActive() {
    return isFanActive;
  }

  /**
   * Set whether the fan is active
   */
  public void setFanActive(boolean active) {
    isFanActive = active;
  }

  /**
   * Updates the internal timer of fan model to keep track with the global game loop time
   *
   * @param delta time passed since last update call
   */
  public void update(float delta) {
    if (period == 0) {
      assert periodOnRatio == 1;

      isOn = true;
      return;
    }

    currentTime += delta;

    // Current time resets for every period
    currentTime %= period;

    float periodComplete = currentTime / period;
    isOn = isFanActive && periodComplete <= periodOnRatio;
  }

  @Override
  public void draw(GameCanvas canvas) {
    super.draw(canvas);

    if (isFanActive && isOn) {
      // Draw wind texture
      wind.draw(canvas);
    }
  }

  /**
   * Enumeration representing the side out of which wind direction is applied from the fan
   */
  public enum FanSide {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
  }

  /**
   * Enumeration of different types of winds, which corresponds to how force is applied
   */
  public enum WindType {
    Constant, // Constant force
    Exponential, // Exponential decay
  }

  /**
   * Wrapper around wind fixture that holds wind force behavior. Note that this is not concerned with
   * when to apply the force and merely contains logic for what kind of wind force is applied
   */
  private class Wind {
    /**
     * Default fan type
     **/
    final private float DEFAULT_WIND_STRENGTH = 1.0f;
    /**
     * Default wind type
     **/
    final private WindType DEFAULT_WIND_TYPE = WindType.Constant;
    /**
     * Wind force cache
     */
    final private Vector2 windForce;
    /**
     * Whether breadth should be set as the texture width
     */
    boolean breadthAsWidth = true;
    /**
     * The strength of the wind, possible values from [1,10]
     */
    private float windStrength = DEFAULT_WIND_STRENGTH;
    /**
     * The wind type of this, the default is constant
     */
    private WindType windType = DEFAULT_WIND_TYPE;
    /**
     * Origin from which the wind is applied
     */
    private Vector2 windOrigin;
    /**
     * Wind texture
     */
    private TextureRegion windTexture;
    /**
     * Origin of the bounding box for the wind
     */
    private Vector2 origin;
    /**
     * Represents the breadth over which the wind force is applied, starting from the wind origin
     */
    private float windBreadth;
    /**
     * Represents the length over which the wind force is applied, starting from the wind origin
     */
    private float windLength;
    /**
     * Wind fixture
     */
    private FixtureDef windFixtureDef;
    /**
     * Shape corresponding to wind fixture
     */
    private PolygonShape windShape;

    Wind() {
      windFixtureDef = new FixtureDef();
      // Set as sensor
      windFixtureDef.isSensor = true;

      // Initialize force and origins
      origin = new Vector2();
      windOrigin = new Vector2();
      windForce = new Vector2();
    }

    /**
     * Precond: These fields have been initialized
     * Set wind fixture details
     */
    protected void initialize() {
      float breadth2 = windBreadth / 2;
      float length2 = windLength / 2;
      float centerX = windOrigin.x;
      float centerY = windOrigin.y;

      switch (fanSide) {
        case LEFT:
          centerX -= length2;
          breadthAsWidth = false;
          break;
        case RIGHT:
          centerX += length2;
          breadthAsWidth = false;
          break;
        case TOP:
          centerY += length2;
          break;
        default:
          centerY -= length2;
          break;
      }

      origin.set(centerX, centerY);

      windShape = new PolygonShape();
      windShape.setAsBox(breadthAsWidth ? breadth2 : length2, breadthAsWidth ? length2 : breadth2, origin, rotation);
      windFixtureDef.shape = windShape;
    }

    /**
     * Returns the wind force applied at a contact position
     *
     * @param (x,y) the point of contact in the scale of the drawing canvas, used for determining wind force
     * @return wind force applied to the object at contact position
     */
    protected Vector2 findWindForce(float x, float y) {
      float normX = x - windOrigin.x;
      float normY = y - windOrigin.y;
      float norm = (float) Math.sqrt(normX * normX + normY * normY);
      normX /= norm;
      normY /= norm;

      windForce.set(normX, normY);

      switch (windType) {
        case Exponential:
          float decayRate = 0.5f;
          float decayScale = (float) Math.exp(-decayRate * norm / windLength);
          windForce.scl(windStrength * decayScale);
          break;
        default:
          windForce.scl(windStrength);
          break;
      }

      return windForce;
    }

    public void setTexture(TextureRegion value) {
      windTexture = value;
    }

    /**
     * Draws the wind
     */
    protected void draw(GameCanvas canvas) {
      float width = breadthAsWidth ? windBreadth : windLength;
      float height = breadthAsWidth ? windLength : windBreadth;
      float originX = origin.x;
      float originY = origin.y;

      canvas.draw(windTexture, Color.RED, 0, 0, originX * drawScale.x, originY * drawScale.y, rotation, 1, 1);
    }
  }
}
