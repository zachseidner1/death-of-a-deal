package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import java.lang.reflect.Field;
import edu.cornell.gdiac.util.SimpleObstacleJsonParser;

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
  private float bounceCoefficient;

  public float getCoefficient() {
    return bounceCoefficient;
  }
  public void setCoefficient(float c){
    bounceCoefficient=c;
  }
  public BouncePlatformModel() {
    super();
    region = null;
    bounceCoefficient=0.0f;
  }
  public void initialize(AssetDirectory directory, JsonValue json) {
    // TODO P2
    // Used red as debug color for bouncy platforms
    setName(json.name());

    float[] pos = json.get("pos").asFloatArray();
    float[] size = json.get("size").asFloatArray();
    setPosition(pos[0], pos[1]);
    setDimension(size[0], size[1]);
    float coefficient = json.get("coefficient").asFloat();
    setCoefficient(coefficient);
    SimpleObstacleJsonParser.initPlatformFromJson(this,directory,json);
  }
  public void draw(GameCanvas canvas) {
    if (region != null) {
      canvas.draw(region, Color.RED, 0, 0, (getX() - anchor.x) * drawScale.x,
          (getY() - anchor.y) * drawScale.y, getAngle(), 1, 1);
    }
  }
}