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

  public void initialize(AssetDirectory directory, JsonValue json) {
    // Pretty sure we want to remove the code below to prevent code reuse
    String key = json.getString("gid");
    TextureRegion texture = new TextureRegion(directory.getEntry(key, Texture.class));
    setTexture(texture);

    JsonValue properties = json.get("properties").child();
    Color debugColor = null;
    int debugOpacity = -1;
    while (properties != null) {
      switch (properties.getString("name")) {
        case "breakminvelocity":
          setBreakMinVelocity(properties.getFloat("value"));
          break;
        default:
          break;
      }
      properties = properties.next();
    }
  }
}
