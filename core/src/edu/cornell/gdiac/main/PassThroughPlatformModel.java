package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.main.PlayerModel.HeadSensor;
import edu.cornell.gdiac.physics.obstacle.Obstacle;

public class PassThroughPlatformModel extends PlatformModel {
  private boolean isPassThrough;
  private BodySensor bodySensor;
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
    solidColor.a = 0.6f; // Differentiate alpha
  }

  public boolean getPassThrough() {
    return isPassThrough;
  }

  public void setPassThrough(boolean pass) {
    isPassThrough = pass;

    // Set all fixtures to sensors
    for (Fixture fixture : body.getFixtureList()) {
      fixture.setSensor(isPassThrough);
    }

    // Reset body and bottom sensor fixtures to sensors
    bodyFixture.setSensor(true);
    bottomFixture.setSensor(true);
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
    // Center of this platform
    Vector2 bodyCenter = new Vector2(getPosition().x + width / 2, getPosition().y - height / 2);

    // Create the body fixture def
    bodySensor = new BodySensor(bodyCenter, new Vector2(0, 0), new Vector2(width / 2, height / 2));

    // Create the bottom fixture def
    float centerYRel = -height / 2;
    float defaultSensorHeight = 0.05f;
    bottomSensor = new BottomSensor(bodyCenter, new Vector2(0, centerYRel), new Vector2(width / 2, defaultSensorHeight));
  }

  @Override
  protected void createFixtures() {
    super.createFixtures();

    if (body == null) {
      return;
    }

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

  // Note: Fundamental feature is to determine when to set the platform to be pass-through vs. not,
  // and being careful of cases like not setting to solid when a player is still "in" the platform.

  public class BottomSensor extends BoxFixtureSensor<PassThroughPlatformModel> {
    public BottomSensor(Vector2 bodyCenter, Vector2 relCenter, Vector2 dimensions) {
      super(PassThroughPlatformModel.this, bodyCenter, relCenter, dimensions);
    }

    @Override
    public void beginContact(Obstacle obs, Object fixtureData) {
      if (!(fixtureData instanceof PlayerModel.HeadSensor)) {
        return;
      }

      // TODO: Fix world coordinate logic

      System.out.println(((HeadSensor) fixtureData).getWorldPosY());
      System.out.println(this.getWorldPosY());


      System.out.println(((HeadSensor) fixtureData).bodyCenter);
      System.out.println(this.bodyCenter);

      System.out.println("Sensor start called: " + this);
      // boolean fromBottom = obs.getPosition().y < center.y + getPosition().y; // TODO: Reenable
      getObstacle().setPassThrough(true);
    }

    @Override
    public void endContact(Obstacle obs, Object fixtureData) {
      if (!(fixtureData instanceof PlayerModel.HeadSensor)) {
        return;
      }

      System.out.println("Sensor end called: " + this);
    }
  }

  public class BodySensor extends BoxFixtureSensor<PassThroughPlatformModel> {
    public BodySensor(Vector2 bodyCenter, Vector2 relCenter, Vector2 dimensions) {
      super(PassThroughPlatformModel.this, bodyCenter, relCenter, dimensions);
    }

    @Override
    public void beginContact(Obstacle obs, Object fixtureData) {
    }

    @Override
    public void endContact(Obstacle obs, Object fixtureData) {
      boolean isPlayerGroundSensor = fixtureData instanceof PlayerModel.GroundSensor;
      boolean isPlayerHeadSensor = fixtureData instanceof PlayerModel.HeadSensor;

      if (!isPlayerGroundSensor && !isPlayerHeadSensor) {
        return;
      }

      BoxFixtureSensor<?> otherSensor = (BoxFixtureSensor<?>) fixtureData;

      float sensorCenterX = this.getWorldPosX();
      float sensorCenterY = this.getWorldPosY();
      float otherSensorCenterX = otherSensor.getWorldPosX();
      float otherSensorCenterY = otherSensor.getWorldPosY();

      boolean noOverlapX1 = sensorCenterX - this.dimensions.x > otherSensorCenterX + otherSensor.dimensions.x;
      boolean noOverlapX2 = sensorCenterX + this.dimensions.x < otherSensorCenterX - otherSensor.dimensions.x;
      boolean noOverlapY1 = sensorCenterY - this.dimensions.y > otherSensorCenterY + otherSensor.dimensions.y;
      boolean noOverlapY2 = sensorCenterY + this.dimensions.y < otherSensorCenterY - otherSensor.dimensions.y;

      boolean hasNoOverlap = noOverlapX1 || noOverlapX2 || noOverlapY1 || noOverlapY2;

//      getObstacle().setPassThrough(!hasNoOverlap); // TODO: Reenable
      getObstacle().setPassThrough(true);
    }
  }
}
