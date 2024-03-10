package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;
import edu.cornell.gdiac.util.SimpleObstacleJsonParser;
import java.lang.reflect.Field;

public class SlopeModel extends PolygonObstacle {

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
  public void initialize(AssetDirectory directory, JsonValue json) {
    //json for sloped platform should be a polygon object
    setName(json.getString("name"));
    float[] points = json.get("polygon").asFloatArray();
    initShapes(points);

    //Unsure about this part
    setPosition(json.getFloat("x"), json.getFloat("y"));
    //For the example this is zero, so should I not be setting the dimension?
//    setDimension(json.getFloat("width"), json.getFloat("height"));
    //Assuming the polygon object has properties
    JsonValue properties = json.get("properties");
    while (properties != null){
      switch (properties.getString("name")){
        case "bodytype":
          break;
        case "density":
          break;
        case "friction":
          break;
        case "restitution":
          break;
        case "debugcolor":
          break;
        case "debugopacity":
          break;
        case "texture":
          break;
        default:
          //Print statement for debugging
          System.out.println("Missing a property");
          break;
      }

      properties = properties.next();
    }
  }

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
