package edu.cornell.gdiac.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;
import java.lang.reflect.Field;

/**
 * Class that provides utility functions for parsing Tiled json and populating obstacles
 */
public class TiledJsonParser {

  public static Vector2 drawScale; // scale factor to convert to pixels : pixels / meter
  public static Vector2 meterScale; // scale factor to convert to meters (physics units) :
  public static float tiledHeight; // height of Tiled editor, used for mapping Tiled pixels to world (physics units)

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
}
