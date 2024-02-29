/*
 * WallModel.java
 *
 * This is a refactored version of the wall (screen boundary) from Lab 4.  We have made
 * it a specialized class so that we can import its properties from a JSON file.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * JSON version, 3/2/2016
 */
package edu.cornell.gdiac.json;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import java.lang.reflect.Field;


/**
 * A polygon shape representing the screen boundary
 * <p>
 * Note that the constructor does very little.  The true initialization happens by reading the JSON
 * value.
 */
public class WallModel extends PolygonObstacle {

  /**
   * Create a new WallModel with degenerate settings
   */
  public WallModel() {
    super(new float[]{0, 0, 1, 0, 1, 1, 0, 1}, 0, 0);
  }

  /**
   * Initializes the wall via the given JSON value
   * <p>
   * The JSON value has been parsed and is part of a bigger level file.  However, this JSON value is
   * limited to the wall subtree
   *
   * @param directory the asset manager
   * @param json      the JSON subtree defining the wall
   */
  public void initialize(AssetDirectory directory, JsonValue json) {
    setName(json.name());

    // Technically, we should do error checking here.
    // A JSON field might accidentally be missing
    float[] verts = json.get("boundary").asFloatArray();
    initShapes(verts);

    setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody
        : BodyDef.BodyType.DynamicBody);
    setDensity(json.get("density").asFloat());
    setFriction(json.get("friction").asFloat());
    setRestitution(json.get("restitution").asFloat());

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
    setDebugColor(debugColor);

    // Now get the texture from the AssetManager singleton
    String key = json.get("texture").asString();
    TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
    setTexture(texture);
  }
}
