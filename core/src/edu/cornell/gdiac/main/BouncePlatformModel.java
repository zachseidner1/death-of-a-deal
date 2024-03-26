package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import java.lang.reflect.Field;

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

  public void initialize(AssetDirectory directory, JsonValue json, int gSizeY) {
    float x = json.getFloat("x") * (1 / drawScale.x);
    float y = (gSizeY - json.getFloat("y")) * (1 / drawScale.y);

    setPosition(x, y);
    setDimension(json.getFloat("width") * (1 / drawScale.x),
        json.getFloat("height") * (1 / drawScale.y));
    JsonValue properties = json.get("properties").child();

    String key = json.getString("gid");
    System.out.println("key = " + key);
    TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
    setTexture(texture);

    while (properties != null) {
      switch (properties.getString("name")) {
        case "bodytype":
          setBodyType(properties.getString("value").equals("static") ? BodyDef.BodyType.StaticBody
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
        case "debugcolor":
          try {
            String cname = properties.getString("value").toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color) field.get(null));
          } catch (Exception e) {
            debugColor = null; // Not defined
          }
          break;
        case "debugopacity":
          int opacity = properties.getInt("value");
          setDebugColor(debugColor.mul(opacity / 255.0f));
          break;
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