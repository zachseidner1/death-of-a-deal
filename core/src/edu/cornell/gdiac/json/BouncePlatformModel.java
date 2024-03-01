package edu.cornell.gdiac.json;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;

public class BouncePlatformModel {
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
  private float bounceForce;

  public float getBounceForce() {
    return bounceForce;
  }

  public void initialize(AssetDirectory directory, JsonValue json) {
    // TODO P2
  }
}