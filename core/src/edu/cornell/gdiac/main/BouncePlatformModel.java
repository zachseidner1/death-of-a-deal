package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class BouncePlatformModel extends PlatformModel {

  /**
   * The vertical force to be applied to the player when the player bounces on the platform while
   * frozen.
   */
  private float bounceCoefficient;
  private float maxSpeed;
  private boolean vertical;

  private float defaultMaxSpeed;

  public BouncePlatformModel() {
    super();
    region = null;
    bounceCoefficient = 0.0f;
    maxSpeed = 0;
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

  public float getDefaultMaxSpeed() {
    return defaultMaxSpeed;
  }

  public boolean isVertical(){return vertical;}
  public void setVertical(boolean verticalOrNot){vertical=verticalOrNot;}

  public void initialize(AssetDirectory directory, JsonValue json) {
    JsonValue properties = json.get("properties").child();

    // Pretty sure we want to remove the code below to prevent code reuse
    String key = json.getString("gid");
    System.out.println("key = " + key);
    TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
    setTexture(texture);

    while (properties != null) {
      switch (properties.getString("name")) {
        case "max_speed":
          setMaxSpeed(properties.getFloat("value"));
          defaultMaxSpeed = properties.getFloat("value");
          break;
        case "coefficient":
          setCoefficient(properties.getFloat("value"));
          break;
        case "vertical":
          setVertical(properties.getBoolean("value"));
        default:
          break;
      }
      properties = properties.next();
    }
  }

  public void draw(GameCanvas canvas) {
    if (region != null) {
      canvas.draw(region, Color.RED, 0, 0, (getX() - anchor.x) * drawScale.x,
          (getY() - anchor.y) * drawScale.y, getAngle(), 1, 1);
    }
  }
}