package edu.cornell.gdiac.main;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class SlopeModel extends PolygonObstacle {

  /**
   * Arbitrary force applied to players if frozen and on slope
   * <p></p>
   * Default value is 0 unless explicitly specified in Tiled
   */
  private float frozenImpulse = 0;


  /**
   * Record the angle of the slope
   */
  private float slopeAngle;

  public SlopeModel() {
    // Since we do not know points yet, initialize to box
    super(new float[]{0, 0, 1, 0, 1, 1, 0, 1}, 0, 0);
  }

  public void setFrozenImpulse(float frozenImpulse) {
    this.frozenImpulse = frozenImpulse;
  }

  public void initShapeAndBounds(float[] points) {
    initShapes(points);
    initBounds();
  }

  /**
   * Initializes the sloped platform via the given JSON value
   *
   * <p>The JSON value has been parsed and is part of a bigger level file. However, this JSON value
   * is limited to the platform subtree
   *
   * @param json the JSON subtree defining the platform
   */
  public void initialize(JsonValue json, int tHeight) {
    float[] points = new float[10];
    JsonValue polygon = json.get("polygon").child();
    int index = 0;
    while (polygon != null) {
      points[index] = polygon.getFloat("x") * (1 / drawScale.x);
      index++;
      points[index] = -1 * polygon.getFloat("y") * (1 / drawScale.y);
      index++;
      polygon = polygon.next();
    }
    initShapes(points);
    initBounds();

    JsonValue properties = json.get("properties").child();
    while (properties != null) {
      if (properties.getString("name").equals("frozenImpulse")) {
        frozenImpulse = properties.getFloat("value");
      }

      properties = properties.next();
      calculateSlopeAngle();
    }
  }

  /**
   * Calculates the angle of the slope based on the longest edge and stores it.
   */
  public void calculateSlopeAngle() {
    if (vertices.length < 4) {
      return; // Not enough vertices to form an edge
    }

    float longestEdgeLength = -1;
    Vector2 longestEdgeVector = new Vector2();

    // Loop through vertices to find the longest edge
    for (int i = 0; i < vertices.length; i += 2) {
      Vector2 startPoint = new Vector2(vertices[i], vertices[i + 1]);
      // Connect the last vertex with the first to close the shape
      Vector2 endPoint = (i + 2 < vertices.length) ? new Vector2(vertices[i + 2], vertices[i + 3])
          : new Vector2(vertices[0], vertices[1]);

      Vector2 edgeVector = new Vector2(endPoint.x - startPoint.x, endPoint.y - startPoint.y);
      float edgeLength = edgeVector.len2();
      if (edgeLength > longestEdgeLength) {
        longestEdgeLength = edgeLength;
        longestEdgeVector = edgeVector;
      }
    }

    slopeAngle = longestEdgeVector.angleRad();
  }

  /**
   * Returns the stored slope angle.
   *
   * @return The angle of the slope in radians.
   */
  public float getSlopeAngle() {
    return slopeAngle;
  }

  /**
   * Returns the force applied to the player when they touch the slope, in the direction opposite of
   * the hypotenuse
   *
   * @return the force applied to the player when they touch the slope, in the direction opposite of
   * the hypotenuse
   */
  public float getSlopeFrozenImpulse() {
    return frozenImpulse;
  }
}
