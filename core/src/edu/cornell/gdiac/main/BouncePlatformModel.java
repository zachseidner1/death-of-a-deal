package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import java.lang.reflect.Field;

public class BouncePlatformModel extends PlatformModel {
  /* TODO P2 complete this class
   * Make sure the bounce model is visually different from the platform in some way
   * you can probably copy and paste lots of code from the platform model (make sure to understand
   * and read what you copy and paste though!)
   * You will also want to add the bounce coefficient field, which I've already put in some methods
   * that you'll need to implement (others can code assuming they're implemented)
   *
   * Make sure to initialize bounce force via JSON as with other properties.
   */
  /**
   * The vertical force to be applied to the player when the player bounces on the platform while
   * frozen.
   */
  private float BounceCoefficient;

  public float getCoefficient() {
    return BounceCoefficient;
  }
  public void setCoefficient(float c){
    BounceCoefficient=c;
  }
  public BouncePlatformModel() {
    super();
    region = null;
    BounceCoefficient=0.0f;
  }
  public void initialize(AssetDirectory directory, JsonValue json) {
    // TODO P2
    // Used red as debug color for bouncy platforms
    setName(json.name());

    float[] pos = json.get("pos").asFloatArray();
    float[] size = json.get("size").asFloatArray();
    setPosition(pos[0], pos[1]);
    setDimension(size[0], size[1]);

    // Technically, we should do error checking here.
    // A JSON field might accidentally be missing
    setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody
        : BodyDef.BodyType.DynamicBody);
    setDensity(json.get("density").asFloat());
    setFriction(json.get("friction").asFloat());
    setRestitution(json.get("restitution").asFloat());
    // Setting the bounce coefficient
    float coefficient = json.get("coefficient").asFloat();
    setCoefficient(coefficient);

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
  public void draw(GameCanvas canvas) {
    if (region != null) {
      canvas.draw(region, Color.RED, 0, 0, (getX() - anchor.x) * drawScale.x,
          (getY() - anchor.y) * drawScale.y, getAngle(), 1, 1);
    }
  }
}