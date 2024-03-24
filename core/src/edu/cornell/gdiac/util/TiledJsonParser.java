package edu.cornell.gdiac.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.main.BouncePlatformModel;
import edu.cornell.gdiac.main.BreakablePlatformModel;
import edu.cornell.gdiac.main.ExitModel;
import edu.cornell.gdiac.main.FanModel;
import edu.cornell.gdiac.main.PlayerModel;
import edu.cornell.gdiac.main.SlopeModel;
import edu.cornell.gdiac.main.WindModel;
import edu.cornell.gdiac.main.WindModel.WindSide;
import edu.cornell.gdiac.main.WindModel.WindType;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.CapsuleObstacle;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;
import java.lang.reflect.Field;
import javax.sql.rowset.BaseRowSet;
import org.w3c.dom.Text;

/**
 * Class that provides utility functions for parsing Tiled json and populating obstacles
 */
public class TiledJsonParser {

  public static Color debugColor;
  public static int debugOpacity;
  public static Vector2 drawScale; // scale factor to convert to pixels : pixels / meter
  public static Vector2 meterScale; // scale factor to convert to meters (physics units) :
  public static int tiledHeight; // height of Tiled editor, used for mapping Tiled pixels to world (physics units)
  public static String type; // Type of object being initialized
  public static float sensorSizeX; // player sensor size x component for calculations
  public static float sensorSizeY; // player sensor size y component for calculations
  public static Color playerSensorColor; // player sensor color
  public static Vector2 windSource;
  public static float windBreadth;
  public static float windLength;
  public static float windStrength;
  public static float fanRotation;
  public static int numWindParticles;
  public static int windLengthParticleGrids;
  public static int windBreadthParticleGrids;
  public static WindSide windSide;
  public static WindType windType;
  public static TextureRegion fanTexture;
  public static TextureRegion windTexture;
  public static TextureRegion windParticleTexture;
  public static float[] points;

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
    obstacle.setPosition(json.getFloat("x") * (1 / drawScale.x),
        (tiledHeight - json.getFloat("y")) * (1 / drawScale.y));

    if (!type.equals("slope")){
      float width = json.getFloat("width") * (1/drawScale.x);
      float height = json.getFloat("height") * (1/drawScale.y);
      setSimpleObstacleDimension(obstacle, width, height);
    }

    // Loop through common properties of all objects and set obstacle fields
    JsonValue properties = json.get("properties").child();
    while (properties != null) {
      switch (properties.getString("name")) {
        case "bodytype":
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
          debugOpacity = properties.getInt("value");
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

    // Call object specific method for object-specific properties and set dimension
    switch (json.getString("name")) {
      case "player":
        assert (obstacle instanceof PlayerModel);
        PlayerModel player = (PlayerModel) obstacle;
        player.initialize(directory, json, tiledHeight);
        break;
      case "slope":
        assert (obstacle instanceof SlopeModel);
        SlopeModel slope = (SlopeModel) obstacle;
        slope.initialize(json);
        break;
      case "bounce":
        assert (obstacle instanceof BouncePlatformModel);
        BouncePlatformModel bounce = (BouncePlatformModel) obstacle;
        bounce.initialize(directory, json, tiledHeight);
        break;
      case "breakable":
        assert (obstacle instanceof BreakablePlatformModel);
        BreakablePlatformModel breakable = (BreakablePlatformModel) obstacle;
        breakable.initialize(directory, json, tiledHeight);
        break;
      case "fan":
        assert (obstacle instanceof FanModel);
        FanModel fan = (FanModel) obstacle;
        fan.initialize(directory, json, tiledHeight);
        break;
      case "exit":
        assert (obstacle instanceof ExitModel);
        ExitModel exit = (ExitModel) obstacle;
        break;
      default:
        break;
    }
  }

  public static void setSimpleObstacleDimension(SimpleObstacle obstacle, float width, float height){
    if (obstacle instanceof CapsuleObstacle){
      ((CapsuleObstacle) obstacle).setDimension(width,height);
    }
    if (obstacle instanceof BoxObstacle){
      ((BoxObstacle) obstacle).setDimension(width,height);
    }
  }
}