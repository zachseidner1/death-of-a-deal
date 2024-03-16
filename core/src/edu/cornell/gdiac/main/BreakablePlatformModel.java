package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import java.lang.reflect.Field;

public class BreakablePlatformModel extends PlatformModel {

  /**
   * Min force required to break breakable platform
   */
  public float breakMinVelocity;

  private boolean broken;

  public BreakablePlatformModel() {
    super();
    region = null;
    this.breakMinVelocity = 0f;
    this.broken = false;
  }

  public float getBreakMinVelocity() {
    return breakMinVelocity;
  }

  public void setBreakMinVelocity(float value) {
    breakMinVelocity = value;
  }


  public boolean notBroken() {
    return !broken;
  }

  public boolean isBroken() {
    return broken;
  }

  public void setBroken(boolean bool) {
    broken = bool;
  }

  public void initialize(AssetDirectory directory, JsonValue json, int gSizeY) {
    setName(json.getString("name"));

    float x = json.getFloat("x") * (1 / drawScale.x);
    float y = (gSizeY - json.getFloat("y")) * (1 / drawScale.y);
    setPosition(x, y);
    float width = json.getFloat("width") * (1 / drawScale.x);
    float height = json.getFloat("height") * (1 / drawScale.y);
    setDimension(width, height);

    JsonValue properties = json.get("properties").child();
    Color debugColor = null;
    int debugOpacity = -1;
    while (properties != null) {
      switch (properties.getString("name")) {
        case "bodytype":
          setBodyType(
              properties.getString("value").equals("static") ? BodyDef.BodyType.StaticBody
                  : BodyDef.BodyType.DynamicBody);
          break;
        case "density":
          setDensity(properties.getFloat("value"));
          break;
        case "restitution":
          setRestitution(properties.getFloat("value"));
          break;
        case "friction":
          setFriction(properties.getFloat("value"));
          break;
        case "debugcolor":
          try {
            String cname = properties.get("value").asString().toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color) field.get(null));
          } catch (Exception e) {
            debugColor = null; // Not defined
          }
          break;
        case "debugopacity":
          debugOpacity = properties.getInt("value");
          break;
        case "breakminvelocity":
          setBreakMinVelocity(properties.getFloat("value"));
          break;
        case "texture":
          String key = properties.getString("value");
          TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
          setTexture(texture);
        default:
          break;
      }
      if (debugOpacity != -1 && debugColor != null) {
        debugColor.mul(debugOpacity / 255f);
        setDebugColor(debugColor);
      }

      properties = properties.next();
    }
  }
}
