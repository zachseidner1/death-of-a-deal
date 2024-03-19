package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.Obstacle;

public class PassThroughPlatformModel extends PlatformModel {
  private boolean isPassThrough;
  private PassThroughSensor bodySensor;
  private Fixture bodyFixture;
  private BottomSensor bottomSensor;
  private Fixture bottomFixture;
  private Color passThroughColor; // Color when pass through state
  private Color solidColor; // Color when solid state

  public PassThroughPlatformModel() {
    super();
    region = null;

    // Initially false, we set isPassThrough on interaction
    isPassThrough = false;

    // Default
    passThroughColor = new Color(1f, 1f, 1f, 0.3f);
    solidColor = new Color(passThroughColor);
    solidColor.a = 0.7f; // Differentiate alpha
  }

  public void setPassThrough(boolean pass) {
    isPassThrough = pass;

    // TODO: Add side effects, including toggling between fixture body vs physical body
  }

  @Override
  public void initializeAsTile(float x, float y, float tileSize, AssetDirectory directory,
                               String tileKey,
                               JsonValue tileProperties) {
    super.initializeAsTile(x, y, tileSize, directory, tileKey, tileProperties);

    Vector2 dimension = getDimension();
    initFixtureDefs(dimension.x, dimension.y);
  }

  /**
   * Initializes the fixture defs contained in this model.
   * (x, y) is the top-left corner of the platform
   */
  private void initFixtureDefs(float width, float height) {
    // Create the body fixture def
    bodySensor = new BodySensor(0, 0, width / 2, height / 2);

    // Create the bottom fixture def
    float centerYRel = -height / 2;
    float defaultSensorHeight = 0.05f;
    bottomSensor = new BottomSensor(0, centerYRel, width / 2, defaultSensorHeight);
  }

  @Override
  protected void createFixtures() {
    super.createFixtures();

    // Create the sensor fixtures
    FixtureDef bodyFixtureDef = bodySensor != null ? bodySensor.getFixtureDef() : null;
    if (bodyFixtureDef != null) {
      bodyFixture = body.createFixture(bodyFixtureDef);
      bodyFixture.setUserData(bodySensor);
    }

    FixtureDef bottomFixtureDef = bottomSensor != null ? bottomSensor.getFixtureDef() : null;
    if (bottomFixtureDef != null) {
      bottomFixture = body.createFixture(bottomFixtureDef);
      bottomFixture.setUserData(bottomSensor);
    }

    assert bodySensor != null;
    assert bodyFixtureDef != null;
    assert bottomSensor != null;
    assert bottomFixtureDef != null;
  }

  @Override
  protected void releaseFixtures() {
    super.releaseFixtures();

    if (body == null) {
      return;
    }

    // Destroy sensor fixtures
    if (bodyFixture != null) {
      body.destroyFixture(bodyFixture);
    }

    if (bottomFixture != null) {
      body.destroyFixture(bottomFixture);
    }
  }

  // If a platform is set to be passed through, increase the transparency of texture so that avatar can be seen
  @Override
  public void draw(GameCanvas canvas) {
    if (region != null) {
      canvas.draw(region, isPassThrough ? passThroughColor : solidColor, 0, 0, (getX() - anchor.x) * drawScale.x,
        (getY() - anchor.y) * drawScale.y, getAngle(), 1, 1);
    }

    // TODO: Draw sensors on debug?
  }

  /**
   * Sensor class for handling sensor collision logic and fixture initialization
   */
  public abstract class PassThroughSensor {
    protected FixtureDef sensorFixtureDef;
    protected Vector2 center; // Relative to platfor body
    protected Vector2 dimensions; // (width2, height2)

    /**
     * (x, y): center, relative to the platform
     * (width2, height2): half dimensiosn from center
     */
    public PassThroughSensor(float x, float y, float width2, float height2) {
      PolygonShape sensorShape = new PolygonShape();
      center = new Vector2(x, y);
      dimensions = new Vector2(width2, height2);
      sensorShape.setAsBox(width2, height2, center, 0);

      sensorFixtureDef = new FixtureDef();
      sensorFixtureDef.isSensor = true;
      sensorFixtureDef.shape = sensorShape;
    }

    public FixtureDef getFixtureDef() {
      return sensorFixtureDef;
    }

    public PassThroughPlatformModel getPlatform() {
      return PassThroughPlatformModel.this;
    }

    /**
     * Callback to be called right before contact resolution with sensor
     */
    public abstract void beforeContact(Obstacle obs);

    /**
     * Callback to be called right after contact resolution with sensor
     */
    public abstract void afterContact(Obstacle obs);
  }

  public class BottomSensor extends PassThroughSensor {
    public BottomSensor(float x, float y, float width2, float height2) {
      super(x, y, width2, height2);
    }

    @Override
    public void beforeContact(Obstacle obs) {
      System.out.println("This is working");
      boolean fromBottom = obs.getPosition().y < center.y;
      getPlatform().setPassThrough(fromBottom);
    }

    @Override
    public void afterContact(Obstacle obs) {

    }
  }

  public class BodySensor extends PassThroughSensor {
    public BodySensor(float x, float y, float width2, float height2) {
      super(x, y, width2, height2);
    }

    @Override
    public void beforeContact(Obstacle obs) {
    }

    @Override
    public void afterContact(Obstacle obs) {
      // Set back to platform after fall through
      getPlatform().setPassThrough(false);
    }
  }
}
