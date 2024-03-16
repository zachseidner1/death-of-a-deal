package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.SimpleObstacleJsonParser;

public class BouncePlatformModel extends PlatformModel {

  /**
   * The vertical force to be applied to the player when the player bounces on the platform while
   * frozen.
   */
  private float bounceCoefficient;
  private float maxSpeed;

  public BouncePlatformModel() {
    super();
    region = null;
    bounceCoefficient = 0.0f;
    maxSpeed=0;
  }

  public float getCoefficient() {
    return bounceCoefficient;
  }

  public void setCoefficient(float c) {
    bounceCoefficient = c;
  }
  public float getMaxSpeed() {
    return maxSpeed;
  }

  public void setMaxSpeed(float c) {
    maxSpeed = c;
  }
  public void initialize(AssetDirectory directory, JsonValue json) {
    setName(json.name());

    float[] pos = json.get("pos").asFloatArray();
    float[] size = json.get("size").asFloatArray();
    setPosition(pos[0], pos[1]);
    setDimension(size[0], size[1]);
    float coefficient = json.get("coefficient").asFloat();
    float speed = json.get("maxspeed").asFloat();
    setCoefficient(coefficient);
    setMaxSpeed(speed);
    SimpleObstacleJsonParser.initPlatformFromJson(this, directory, json);
  }

  public void draw(GameCanvas canvas) {
    if (region != null) {
      canvas.draw(region, Color.RED, 0, 0, (getX() - anchor.x) * drawScale.x,
          (getY() - anchor.y) * drawScale.y, getAngle(), 1, 1);
    }
  }
}