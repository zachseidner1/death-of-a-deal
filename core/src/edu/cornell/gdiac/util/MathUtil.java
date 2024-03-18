package edu.cornell.gdiac.util;

import com.badlogic.gdx.math.Vector2;

public class MathUtil {

  public static float getMagnitude(Vector2 vector) {
    return (float) Math.sqrt(vector.dot(vector));
  }

  /**
   * Rotates src point about the pivot and sets the resulting point from the transformation in dst.
   * If no destination vector is provided, returns the result in a new vector
   *
   * @param angle: how much to rotate, in radians
   */
  public static Vector2 rotateAroundPivot(Vector2 pivot, Vector2 src, Vector2 dst, float angle) {
    float xDiff = src.x - pivot.x;
    float yDiff = src.y - pivot.y;
    float x = (float) (xDiff * Math.cos(angle) - yDiff * Math.sin(angle) + pivot.x);
    float y = (float) (xDiff * Math.sin(angle) + yDiff * Math.cos(angle) + pivot.y);

    if (dst != null) {
      dst.set(x, y);
      return dst;
    }

    return new Vector2(x, y);
  }
}