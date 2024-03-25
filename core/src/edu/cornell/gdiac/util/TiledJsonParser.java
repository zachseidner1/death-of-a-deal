package edu.cornell.gdiac.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.main.BouncePlatformModel;
import edu.cornell.gdiac.main.BreakablePlatformModel;
import edu.cornell.gdiac.main.FanModel;
import edu.cornell.gdiac.main.PlayerModel;
import edu.cornell.gdiac.main.SlopeModel;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;
import java.lang.reflect.Field;

/**
 * Class that provides utility functions for parsing Tiled json and populating obstacles
 */
public class TiledJsonParser {

  private static Vector2 drawScale; // scale factor to convert to pixels : pixels / meter
  private static int tiledHeight; // height of Tiled editor, used for mapping Tiled pixels to world (physics units)
  private static String type; // Type of object being initialized

  /**
   * Parses json and initializes the obstacle's properties
   *
   * @param obstacle  The obstacle to be initiated
   * @param directory Asset directory to fetch textures
   * @param json      Json to be parsed
   */
  public static void initPlatformFromJson(
      SimpleObstacle obstacle, AssetDirectory directory, JsonValue json) {
    // Technically, we should do error checking here.
    // A JSON field might accidentally be missing
    obstacle.setBodyType(
        json.get("bodytype").asString().equals("static")
            ? BodyDef.BodyType.StaticBody
            : BodyDef.BodyType.DynamicBody);
    obstacle.setDensity(json.get("density").asFloat());
    obstacle.setFriction(json.get("friction").asFloat());
    obstacle.setRestitution(json.get("restitution").asFloat());

    // Reflection is best way to convert name to color
    Color debugColor;
    try {
      String cname = json.get("debugcolor").asString().toUpperCase();
      Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
      debugColor = new Color((Color) field.get(null));
    } catch (Exception e) {
      debugColor = null; // Not defined
    }
    int opacity = json.get("debugopacity").asInt();
    debugColor.mul(opacity / 255.0f);
    obstacle.setDebugColor(debugColor);

    // Now get the texture from the AssetManager singleton
    String key = json.get("texture").asString();
    TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
    obstacle.setTexture(texture);
  }

  /**
   * @param obstacle  obstacle to initialize
   * @param directory asset directory for use of assets
   * @param json      json to use for initialization
   */
  public static void initObjectFromJson(
      SimpleObstacle obstacle, AssetDirectory directory, JsonValue json, Vector2 dScale,
      int tHeight) {
    // Set parser fields, obstacle name, and obstacle position
    drawScale = dScale;
    obstacle.setDrawScale(drawScale);
    tiledHeight = tHeight;
    type = json.getString("name");
    obstacle.setName(type);
    float x = json.getFloat("x");
    float y = json.getFloat("y");
    setSimpleObstaclePosition(obstacle, x, y);

    if (!type.equals("slope")) {
      float width = json.getFloat("width") * (1 / drawScale.x);
      float height = json.getFloat("height") * (1 / drawScale.y);
      setSimpleObstacleDimension(obstacle, width, height);
    }

    // Loop through common properties of all objects and set obstacle fields
    JsonValue properties = json.get("properties").child();
    while (properties != null) {
      Color debugColor;
      switch (properties.getString("name")) {
        case "bodytype":
        case "BodyType":
          obstacle.setBodyType(
              properties.get("value").asString().equals("static") ? BodyDef.BodyType.StaticBody
                  : BodyDef.BodyType.DynamicBody);
          break;
        case "density":
          obstacle.setDensity(properties.getFloat("value"));
          break;
        case "friction":
          obstacle.setFriction(properties.getFloat("value"));
          break;
        case "restitution":
          obstacle.setRestitution(properties.getFloat("value"));
          break;
        case "debugcolor":
          try {
            String cname = properties.getString("value").toUpperCase();
            Field field = Class.forName(
                "com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color) field.get(null));
          } catch (Exception e) {
            debugColor = null;
          }
          obstacle.setDebugColor(debugColor);
          break;
        case "debugopacity":
          int debugOpacity = properties.getInt("value");
          obstacle.setDebugColor(obstacle.getDebugColor().mul(debugOpacity / 255.0f));
          break;
        case "texture":
          String key = properties.getString("value");
          TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
          obstacle.setTexture(texture);
          break;
        default:
          break;
      }

      properties = properties.next();
    }

    // Call object specific method for object-specific properties
    switch (json.getString("name")) {
      case "player":
        assert (obstacle instanceof PlayerModel);
        PlayerModel player = (PlayerModel) obstacle;
        player.initialize(directory, json);
        break;
      case "slope":
        assert (obstacle instanceof SlopeModel);
        SlopeModel slope = (SlopeModel) obstacle;
        slope.initialize(json);
        break;
      case "bounce":
        assert (obstacle instanceof BouncePlatformModel);
        BouncePlatformModel bounce = (BouncePlatformModel) obstacle;
        bounce.initialize(directory, json);
        break;
      case "breakable":
        assert (obstacle instanceof BreakablePlatformModel);
        BreakablePlatformModel breakable = (BreakablePlatformModel) obstacle;
        breakable.initialize(directory, json);
        break;
      case "fan":
        assert (obstacle instanceof FanModel);
        FanModel fan = (FanModel) obstacle;
        fan.initialize(directory, json);
        break;
      default:
        break;
    }
  }

  public static void setSimpleObstaclePosition(SimpleObstacle obstacle, float x, float y) {
    int offsetx = 0;
    int offsety = 0;
    if (type.equals("slope")) {
      // For now offset all slopes by -8 in x and 8 in y
      offsetx = -8;
      offsety = 8;
    } else if (type.equals("bounce")) {
      // For now offset all bounce platforms in x and y direction by 16
      // If x position is 0, only offset the y
      // I don't know why bounce platforms are acting this way, we will need to fix in future
      if (x > 0) {
        offsetx = 16;
      }
      offsety = 16;
    } else if (type.equals("breakable")) {
      offsety = 16;
    }
    x = (x + offsetx) * (1 / drawScale.x);
    y = ((tiledHeight - y) + offsety) * (1 / drawScale.y);
    obstacle.setPosition(x, y);
  }

  public static void setSimpleObstacleDimension(SimpleObstacle obstacle, float width,
      float height) {
    if (obstacle instanceof CapsuleObstacle) {
      ((CapsuleObstacle) obstacle).setDimension(width, height);
    }
    if (obstacle instanceof BoxObstacle) {
      ((BoxObstacle) obstacle).setDimension(width, height);
    }
  }
}