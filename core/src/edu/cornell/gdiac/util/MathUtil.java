package edu.cornell.gdiac.util;

import com.badlogic.gdx.math.Vector2;

public class MathUtil {

  public static float getMagnitude(Vector2 vector) {
    return (float) Math.sqrt(vector.dot(vector));
  }
}