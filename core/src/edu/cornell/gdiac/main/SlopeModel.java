package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import java.lang.reflect.Field;

public class SlopeModel extends PolygonObstacle {

  /**
   * Arbitrary force applied to players if frozen and on slope
   */
  public final float SLOPE_FROZEN_FORCE = 1000.0f;

  /**
   * Record the friction of the force, could be useful when dealing with frozen (not used)
   */
  private float frictionCoefficient;

  public SlopeModel() {
    // Since we do not know points yet, initialize to box
    super(new float[]{0, 0, 1, 0, 1, 1, 0, 1}, 0, 0);
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

    float x = json.getFloat("x") * (1 / drawScale.x);
    float y = (gSizeY - json.getFloat("y")) * (1 / drawScale.y);
    setPosition(x, y);

    float[] points = new float[10];
    JsonValue polygon = json.get("polygon").child();
    int index = 0;
    while (polygon != null) {
      points[index] = polygon.getFloat("x") * (1 / drawScale.x);
      index++;
      points[index] = -1 * polygon.getFloat("y") * (1 / drawScale.y);
      index++;
      polygon = polygon.next();
    }
    initShapes(points);
    initBounds();

    JsonValue properties = json.get("properties").child();
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
          this.frictionCoefficient = properties.getFloat("value");
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
        case "texture":
          String key = properties.getString("value");
          TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
          setTexture(texture);
          break;
        default:
          break;
      }

      properties = properties.next();
    }
  }

  /**
   * Gets the angle of the slope in radians.
   *
   * @return The angle of the slope in radians.
   */
  public float getSlopeAngle() {

    Vector2 point1 = new Vector2(vertices[0], vertices[1]);
    Vector2 point2 = new Vector2(vertices[2], vertices[3]);

    Vector2 slopeVector = new Vector2(point2.x - point1.x, point2.y - point1.y);
    System.out.println(slopeVector);
    return slopeVector.angleRad();
  }

  /**
   * Gets the friction coefficient of the slope.
   *
   * @return The friction coefficient.
   */
  public float getFrictionCoefficient() {
    return this.frictionCoefficient;
  }
}
