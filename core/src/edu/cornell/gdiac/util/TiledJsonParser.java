package edu.cornell.gdiac.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.main.BouncePlatformModel;
import edu.cornell.gdiac.main.BreakablePlatformModel;
import edu.cornell.gdiac.main.FanModel;
import edu.cornell.gdiac.main.PlayerModel;
import edu.cornell.gdiac.main.SlopeModel;
import edu.cornell.gdiac.main.WindModel;
import edu.cornell.gdiac.main.WindModel.WindSide;
import edu.cornell.gdiac.main.WindModel.WindType;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;
import java.lang.reflect.Field;
import javax.sql.rowset.BaseRowSet;

/**
 * Class that provides utility functions for parsing Tiled json and populating obstacles
 */
public class TiledJsonParser {
  public static Vector2 drawScale; // scale factor to convert to pixels : pixels / meter
  public static Vector2 meterScale; // scale factor to convert to meters (physics units) :
  public static float tiledHeight; // height of Tiled editor, used for mapping Tiled pixels to world (physics units)
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
  public static TextureRegion windTexture;
  public static TextureRegion windParticleTexture;


  // TODO: Add specific json fields (with custom logic) + other general types, and this will use type parameter to parse the tiled json

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
   *
   * @param obstacle obstacle to initialize as object
   * @param directory asset directory for use of assets
   * @param json json to use for initialization
   * @param drawScale drawScale for object
   * @param tiledHeight tiledHeight for position calculation
   */
  public void initialize(SimpleObstacle obstacle, AssetDirectory directory, JsonValue json,
      Vector2 drawScale, float tiledHeight) {
    setStaticFields(drawScale, tiledHeight);
    initObjectFromJson(obstacle, directory, json);
  }

  /**
   *
   * @param dScale drawScale for object
   * @param tHeight tiledHeight for position calculation
   */
  public static void setStaticFields(Vector2 dScale, float tHeight) {
    drawScale = dScale;
    tiledHeight = tHeight;
  }

  /**
   *
   * @param obstacle obstacle to initialize
   * @param directory asset directory for use of assets
   * @param json json to use for initialization
   */
  public static void initObjectFromJson(
      SimpleObstacle obstacle, AssetDirectory directory, JsonValue json) {
      // Set type field and obstacle name
      type = json.getString("name");
      obstacle.setName(type);

      // Set position and other common properties
      obstacle.setPosition(json.getFloat("x") * (1/drawScale.x),
          (tiledHeight - json.getFloat("y")) * (1/drawScale.y));
      Color debugColor;
      int debugOpacity;
      JsonValue properties = json.get("properties").child();

      // Loop through common properties of all objects
      while (properties != null){
        switch (properties.getString("name")){
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
        }
        properties = properties.next();
      }

      // Call object specific method for remaining properties
      switch (type){
        case "player":
          initPlayerFromJson((PlayerModel) obstacle, directory, json);
          break;
        case "slope":
          initSlopeFromJson((SlopeModel) obstacle, directory, json);
          break;
        case "breakable":
          assert (obstacle instanceof BreakablePlatformModel);
          initBreakableFromJson((BreakablePlatformModel) obstacle, directory, json);
          break;
        case "bounce":
          assert (obstacle instanceof BouncePlatformModel);
          initBounceFromJson((BouncePlatformModel) obstacle, directory, json);
          break;
        case "fan":
          assert (obstacle instanceof FanModel);
          initFanFromJson((FanModel) obstacle, directory, json);
          break;
      }
  }

  /**
   *
   * @param player player model to initialize
   * @param directory asset directory
   * @param json json for player object
   */
  public static void initPlayerFromJson(
      PlayerModel player, AssetDirectory directory, JsonValue json) {
    // Set dimension and frozen texture
    float width = json.getFloat("width") * (1/drawScale.x);
    float height = json.getFloat("height") * (1/drawScale.y);
    player.setDimension(width,height);
    // Should we add this to tiled as well? We could have key property with the string "frozen"
    player.setFrozenTexture(new TextureRegion(directory.getEntry("frozen", Texture.class)));

    // Player properties
    JsonValue properties = json.get("properties").child();
    switch (properties.getString("name")) {
      case "force":
        player.setForce(properties.getFloat("value"));
        break;
      case "damping":
        player.setDamping(properties.getFloat("value"));
        break;
      case "maxspeed":
        player.setMaxSpeed(properties.getFloat("value"));
        break;
      case "jumpvelocity":
        player.setJumpVelocity(properties.getFloat("value"));
        break;
      case "jumplimit":
        player.setJumpLimit(properties.getInt("value"));
        break;
      case "sensorsizex":
        sensorSizeX = properties.getFloat("value");
        break;
      case "sensorsizey":
        Vector2 sensorCenter = new Vector2(0, -player.getHeight() / 2);
        sensorSizeY = properties.getFloat("value");
        if (sensorSizeX == 0) {
          System.out.println("Sensor size X has not yet been set");
        }
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(sensorSizeX, sensorSizeY, sensorCenter, 0.0f);
        player.setSensorShape(sensorShape);
        break;
      case "sensorcolor":
        try {
          String cname = properties.getString("value").toUpperCase();
          Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
          playerSensorColor = new Color((Color) field.get(null));
        } catch (Exception e) {
          playerSensorColor = null; // Not defined
        }
        player.setSensorColor(playerSensorColor);
        break;
      case "sensoropacity":
        int opacity = properties.getInt("value");
        if (playerSensorColor != null) {
          playerSensorColor.mul(opacity / 255.0f);
        }
        player.setSensorColor(playerSensorColor);
        break;
      case "sensorname":
        player.setSensorName(properties.getString("value"));
        break;
      case "fallMulitplier":
        player.setFallMultiplier(properties.getFloat("value"));
        break;
      case "lowJumpMultiplier":
        player.setLowJumpMultiplier(properties.getFloat("value"));
        break;
      default:
        break;
    }
  }

  /**
   *
   * @param slope slope model to initialize
   * @param directory asset directory
   * @param json json for slope object
   */
  public static void initSlopeFromJson(
      SlopeModel slope, AssetDirectory directory, JsonValue json) {
    // init bounds

    // Slope properties
    JsonValue properties = json.get("properties").child();
    switch (properties.getString("name")) {
      default:
        break;
    }
  }

  /**
   *
   * @param bounce bounce platform model to initialize
   * @param directory asset directory
   * @param json json for bounce platform object
   */
  public static void initBounceFromJson(
      BouncePlatformModel bounce, AssetDirectory directory, JsonValue json) {
    // Set dimension

    // Bounce properties
    JsonValue properties = json.get("properties").child();

  }

  /**
   *
   * @param breakable breakable platform model to initialize
   * @param directory asset directory
   * @param json json for breakable platform object
   */
  public static void initBreakableFromJson(
      BreakablePlatformModel breakable, AssetDirectory directory, JsonValue json) {

  }

  /**
   *
   * @param fan fan model to initialize
   * @param directory asset directory
   * @param json json for fan object
   */
  public static void initFanFromJson(
      FanModel fan, AssetDirectory directory, JsonValue json) {
    float width = json.getFloat("width") * (1/drawScale.x);
    float height = json.getFloat("height") * (1/drawScale.y);
      fan.setDimension(width,height);

    fanRotation = 0f;
    JsonValue properties = json.get("properties").child();
    while (properties != null){
      switch (properties.getString("name")){
        case "Type":
          String type = properties.getString("value").toUpperCase();
          switch (type){
            case "EXPONENTIAL":
              windType = WindType.Exponential;
              break;
            case "CONSTANT":
              windType = WindType.Constant;
              break;
            default:
              windType = WindType.Default;
          }
          break;
        case "Side":
          String side = properties.getString("value").toUpperCase();
          switch (side){
            case "LEFT":
              windSide = WindSide.LEFT;
              fan.setFanSide(windSide);
              windSource.set(fan.getX(), fan.getY() - height / 2);
              break;
            default:
              windSide = WindSide.LEFT;
              fan.setFanSide(windSide);
              windSource.set(fan.getX() + width, fan.getY()- height);
              break;
          }
          break;
        case "WindStrength":
          windStrength = properties.getFloat("value");
          break;
        case "WindBreadth":
          windBreadth = properties.getFloat("value") * (1/drawScale.x);
          break;
        case "WindLength":
          windLength = properties.getFloat("value") * (1/drawScale.y);
          break;
        case "NumWindParticles":
          numWindParticles = properties.getInt("value");
          assert numWindParticles >= 0;
          fan.setWindParticleFixtures(new Fixture[numWindParticles]);
          break;
        case "WindBreadthParticleGrids":
          windBreadthParticleGrids = properties.getInt("value");
          break;
        case "WindLengthParticleGrids":
          windLengthParticleGrids = properties.getInt("value");
          break;
        case "Period":
          fan.setPeriod(properties.getFloat("value"));
          break;
        case "PeriodOnRatio":
          fan.setPeriodOnRatio(properties.getFloat("value"));
          break;
        case "Active":
          fan.setActive(properties.getBoolean("value"));
          break;
        case "WindTexture":
          String key = properties.getString("value");
          windTexture = new TextureRegion(directory.getEntry(key, Texture.class));
          break;
        case "WindParticleTexture":
          String key2 = properties.getString("value");
          windParticleTexture = new TextureRegion(directory.getEntry(key2, Texture.class));
          break;
        default:
          break;
      }

      properties = properties.next();
    }

    fan.initializeWind(
        windSource.x,
        windSource.y,
        windBreadth,
        windLength,
        windStrength,
        fanRotation,
        numWindParticles,
        windLengthParticleGrids,
        windBreadthParticleGrids,
        windSide,
        windType,
        windTexture,
        windParticleTexture);

  }

  public static void setObjectDimension(BoxObstacle obstacle, JsonValue json){
    float width = json.getFloat("width") * (1/drawScale.x);
    float height = json.getFloat("height") * (1/drawScale.y);
    obstacle.setDimension(width,height);
  }
}
