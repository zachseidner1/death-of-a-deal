package edu.cornell.gdiac.main;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.SimpleObstacleJsonParser;

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
    setName(json.name());
    float[] points = json.get("points").asFloatArray();
    initShapes(points);

    SimpleObstacleJsonParser.initPlatformFromJson(this, directory, json);
  }
}
