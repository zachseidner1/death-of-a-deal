package edu.cornell.gdiac.main;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class SlopedModel extends PolygonObstacle {

  public SlopedModel(float[] points) {
    super(points);
    // TODO modify as needed
  }

  public SlopedModel(float[] points, float x, float y) {
    super(points, x, y);
    // TODO modify as needed
  }

  // TODO P4 complete this class how you see fit
  /*
  This should be a customizable slope, so it would be good if we could have 3 vertices and then
  a platform would be created based off of that. I think you could try using BoxObstacle to
  implement this, but if you find a better way feel free to do so.

  Also you will need to add some of your own JSON parsing code and JSON properties for the sloped
  platform.
   */


  public void initialize(AssetDirectory directory, JsonValue json) {
    // TODO P4
  }
}