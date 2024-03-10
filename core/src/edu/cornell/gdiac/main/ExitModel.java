/*
 * ExitModel.java
 *
 * This is a refactored version of the exit door from Lab 4.  We have made it a specialized
 * class so that we can import its properties from a JSON file.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * JSON version, 3/2/2016
 */
package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import java.lang.reflect.Field;

/**
 * A sensor obstacle representing the end of the level
 * <p>
 * Note that the constructor does very little.  The true initialization happens by reading the JSON
 * value.
 */
public class ExitModel extends BoxObstacle {

  /**
   * Create a new ExitModel with degenerate settings
   */
  public ExitModel() {
    super(0, 0, 1, 1);
    setSensor(true);
  }

  /**
   * Initializes the exit door via the given JSON value
   * <p>
   * The JSON value has been parsed and is part of a bigger level file.  However, this JSON value is
   * limited to the exit subtree
   *
   * @param directory the asset manager
   * @param json      the JSON subtree defining the exit
   */
  public void initialize(AssetDirectory directory, JsonValue json) {
    setName(json.getString("name"));
    setPosition(json.getFloat("x"), json.getFloat("y"));
    setDimension(json.getFloat("width"), json.getFloat("height"));
    JsonValue properties = json.get("properites");
    Color debugColor = null;
    int debugOpacity = -1;

    while (properties != null){
      switch (json.getString("name")){
        case "bodytype":
          setBodyType(json.getString("value").equals("static") ? BodyDef.BodyType.StaticBody
              : BodyDef.BodyType.DynamicBody);
          break;
        case "density":
          setDensity(json.getFloat("value"));
          break;
        case "friction":
          setFriction(json.getFloat("value"));
          break;
        case "restitution":
          setRestitution(json.getFloat("value"));
          break;
        case "debugcolor":
          try {
            String cname = json.get("debugcolor").asString().toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color) field.get(null));
          } catch (Exception e) {
            debugColor = null; // Not defined
          }
          setDebugColor(debugColor);
          break;
        case "debugopacity":
          debugOpacity = json.get("debugopacity").asInt();
          setDebugColor(getDebugColor().mul(debugOpacity / 255.0f));
          break;
        case "texture":
          String key = json.getString("value");
          TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
          setTexture(texture);
          break;
        default:
          //Print statement for debugging
          System.out.println("Missing a property");
          break;
      }

      properties = properties.next();
    }
  }
}
