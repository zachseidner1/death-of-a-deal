/*
 * ExitModel.java
 *
 * This is a refactored version of the exit door from Lab 4.  We have made it a specialized
 * class so that we can import its properties from a JSON file.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * JSON version, 3/2/2016
 */
package edu.cornell.gdiac.main;

import edu.cornell.gdiac.physics.obstacle.BoxObstacle;

/**
 * A sensor obstacle representing the end of the level
 * <p>
 * Note that the constructor does very little.  The true initialization happens by reading the JSON
 * value.
 */
public class ExitModel extends BoxObstacle {

  /**
   * Create a new ExitModel with degenerate settings
   */
  public ExitModel() {
    super(0, 0, 1, 1);
    setSensor(true);
  }
}

