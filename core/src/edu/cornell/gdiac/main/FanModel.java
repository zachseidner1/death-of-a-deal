package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.main.WindModel.WindParticleModel;
import edu.cornell.gdiac.main.WindModel.WindSide;
import edu.cornell.gdiac.main.WindModel.WindType;

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
  public void initialize(AssetDirectory directory, JsonValue json) {
    float rotation = -1 * json.getFloat("rotation") / (float) (Math.PI / 2);
    setAngle(rotation);
    // Fan offset: don't know why we have to offset this
    setY(getY() - 0.8f);

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
              windSource.set(getX(), getY() - getHeight() / 2);
              break;
            default:
              fanSide = WindSide.RIGHT;
              windSource.set(getX() + getWidth(), getY() - getHeight() / 2);
              break;
          }
          break;
        case "WindStrength":
          windStrength = properties.getFloat("value");
          break;
        case "WindBreadth":
          windBreadth = properties.getFloat("value") * (1 / drawScale.x);
          break;
        case "WindLength":
          windLength = properties.getFloat("value") * (1 / drawScale.y);
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

    // TODO: Figure out fan and wind rotation
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
   * Initialize wind properties. Called whenever there should be a change in the behavior of the
   * wind, as directed by this fan
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
   * // TODO: Will likely need to figure out a way to abstract away wind as a temporary fixture that
   * can be attached and destroyed in the existence of this model Set whether the fan is active
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
    canvas.draw(region, Color.BLUE, 0, 0, (getX() - anchor.x) * drawScale.x,
        (getY() - anchor.y) * drawScale.y, getAngle(), 1, 1);

    if (isFanActive) {
      // Draw wind texture
      wind.draw(canvas, drawScale);
    }
  }
}
