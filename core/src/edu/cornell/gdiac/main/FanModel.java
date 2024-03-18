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
import edu.cornell.gdiac.main.WindModel.WindParticleModel;
import edu.cornell.gdiac.main.WindModel.WindSide;
import edu.cornell.gdiac.main.WindModel.WindType;
import edu.cornell.gdiac.util.MathUtil;
import java.lang.reflect.Field;

/**
 * Contains simple state for whether the fan is applying wind force. Creates and owns ephemeral wind
 * models to be created when the fan is on.
 */
public class FanModel extends PlatformModel {
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
  private float fanRotation = DEFAULT_ROTATION;
  /**
   * Whether the fan is actively trying to apply wind force (period continues)
   */
  private boolean isFanActive;
  private WindSide fanSide;
  /**
   * Contains the wind fixture def and wind force logic
   */
  private WindModel wind;
  private Fixture windFixture;
  private Fixture[] windParticleFixtures;

  public FanModel() {
    // Degenerate settings
    super();

    // Wind fixture creation
    wind = new WindModel();
  }

  /**
   * Initializes the fan platform via the given JSON value
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

    fanRotation = (float) -Math.toRadians(json.getFloat("rotation"));
    setAngle(fanRotation);

    float width = json.getFloat("width") * scaleFactorX;
    float height = json.getFloat("height") * scaleFactorY;
    setDimension(width, height);

    // TODO: Will likely abstract this away as a util function for compatible parsing of Tiled rotation
    // Note: Tiled uses rotation about the top-left corner of the rectangle, while our impl uses the center.
    // We perform some trig position calculations to address the difference in how rotation is treated.

    // topLeft is top-left most point
    float topLeftX = json.getFloat("x") * scaleFactorX;
    float topLeftY = (gSizeY - json.getFloat("y")) * scaleFactorY;
    Vector2 topLeft = new Vector2(topLeftX, topLeftY);

    // center0 is center of rectangle with rotation 0
    float centerX0 = topLeftX + width / 2;
    float centerY0 = topLeftY - height / 2;
    Vector2 center0 = new Vector2(centerX0, centerY0);

    // Rotate center0 about top-left corner with fanRotation
    Vector2 center = new Vector2();
    MathUtil.rotateAroundPivot(topLeft, center0, center, fanRotation);
    float x = center.x;
    float y = center.y;
    setPosition(x, y);

    // Wind wrapper fields
    Vector2 windSource = new Vector2();
    WindType windType = null;
    TextureRegion windTexture = null;
    TextureRegion windParticleTexture = null;
    float windStrength = -1, windBreadth = -1, windLength = -1;
    int numWindParticles = -1, windLengthParticleGrids = -1, windBreadthParticleGrids = -1;

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
              windType = WindType.Exponential;
              break;
            case "CONSTANT":
              windType = WindType.Constant;
              break;
            default:
              windType = WindType.Default;
              break;
          }
          break;
        case "Side":
          // Determine fan side and wind origin, default is to use center of side
          String side = properties.getString("value").toUpperCase();
          switch (side) {
            case "LEFT":
              fanSide = WindSide.LEFT;
              windSource.set(x, y - height / 2);
              break;
            default:
              fanSide = WindSide.RIGHT;
              windSource.set(x + width, y - height / 2);
              break;
          }
          break;
        case "WindStrength":
          windStrength = properties.getFloat("value");
          break;
        case "WindBreadth":
          windBreadth = properties.getFloat("value") * scaleFactorX;
          break;
        case "WindLength":
          windLength = properties.getFloat("value") * scaleFactorY;
          break;
        case "NumWindParticles":
          numWindParticles = properties.getInt("value");
          assert numWindParticles >= 0;
          windParticleFixtures = new Fixture[numWindParticles];
          break;
        case "WindLengthParticleGrids":
          windLengthParticleGrids = properties.getInt("value");
          break;
        case "WindBreadthParticleGrids":
          windBreadthParticleGrids = properties.getInt("value");
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
          windTexture = new TextureRegion(directory.getEntry(key, Texture.class));
          break;
        case "WindParticleTexture":
          key = properties.getString("value");
          windParticleTexture = new TextureRegion(directory.getEntry(key, Texture.class));
          break;
        default:
          break;
      }

      properties = properties.next();
    }

    // Configure shape and configure wind fixture
    initializeWind(
      windSource.x,
      windSource.y,
      windBreadth,
      windLength,
      windStrength,
      fanRotation,
      numWindParticles,
      windLengthParticleGrids,
      windBreadthParticleGrids,
      fanSide,
      windType,
      windTexture,
      windParticleTexture
    );
  }

  /**
   * Initialize wind properties. Called whenever there should be a change in the behavior of the wind, as directed by this fan
   */
  public void initializeWind(
    float windSourceX,
    float windSourceY,
    float windBreadth,
    float windLength,
    float windStrength,
    float windRotation,
    int numWindParticles,
    int windLengthParticleGrids,
    int windBreadthParticleGrids,
    WindSide windSide,
    WindType windType,
    TextureRegion windTexture,
    TextureRegion windParticleTexture
  ) {

    wind.initialize(
      windSourceX,
      windSourceY,
      windBreadth,
      windLength,
      windStrength,
      windRotation,
      numWindParticles,
      windLengthParticleGrids,
      windBreadthParticleGrids,
      windSide,
      windType,
      windTexture,
      windParticleTexture
    );
  }

  @Override
  protected void createFixtures() {
    super.createFixtures();

    if (wind == null) {
      return;
    }

    FixtureDef windFixtureDef = wind.getFixtureDef();
    // Create fixture
    if (windFixtureDef != null) {
      windFixture = body.createFixture(windFixtureDef);
      // Sets the user data to instance of Wind
      windFixture.setUserData(wind);
    }

    WindParticleModel[] windParticles = wind.getWindParticles();
    if (windParticles == null) {
      return;
    }
    assert windParticleFixtures != null;
    for (int i = 0; i < windParticles.length; i++) {
      WindParticleModel particle = windParticles[i];

      assert particle != null;

      Fixture windParticleFixture = body.createFixture(particle.getFixtureDef());
      windParticleFixture.setUserData(particle);
      windParticleFixtures[i] = windParticleFixture;
    }
  }

  @Override
  protected void releaseFixtures() {
    super.releaseFixtures();

    // Destroy fixture
    if (windFixture != null) {
      body.destroyFixture(windFixture);
    }

    if (windParticleFixtures != null) {
      for (Fixture windParticleFixture : windParticleFixtures) {
        if (windParticleFixture != null) {
          body.destroyFixture(windParticleFixture);
        }
      }
    }
  }

  /**
   * Get active status of fan
   */
  public boolean getFanActive() {
    return isFanActive;
  }

  /**
   * // TODO: Will likely need to figure out a way to abstract away wind as a temporary fixture that can be attached and destroyed in the existence of this model
   * Set whether the fan is active
   */
  public void setFanActive(boolean active) {
    isFanActive = active;
    if (!isFanActive) {
      wind.turnWindOn(false);
    }
  }

  /**
   * Updates the internal timer of fan model to keep track with the global game loop time
   *
   * @param delta time passed since last update call
   */
  public void update(float delta) {
    if (period == 0) {
      assert periodOnRatio == 1;
      wind.turnWindOn(true);
      return;
    }

    currentTime += delta;

    // Current time resets for every period
    currentTime %= period;

    float periodComplete = currentTime / period;
    wind.turnWindOn(isFanActive && periodComplete <= periodOnRatio);
  }

  @Override
  public void draw(GameCanvas canvas) {
    // Need to determine bottom left corner
    canvas.draw(
      region,
      Color.BLUE,
      getX() * drawScale.x,
      getY() * drawScale.y,
      0,
      0,
      fanRotation,
      1,
      1
    );

    if (isFanActive) {
      // Draw wind texture
      wind.draw(canvas, drawScale);
    }
  }
}
