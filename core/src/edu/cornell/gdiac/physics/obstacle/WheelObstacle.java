/*
 * WheelObstacle.java
 *
 * Sometimes you want circles instead of boxes. This class gives it to you.
 * Note that the shape must be circular, not Elliptical.  If you want to make
 * an ellipse, you will need to use the PolygonObstacle class.
 *
 * Author: Walker M. White
 * Modified to support custom debug colors
 * Version: 3/2/2016
 */
package edu.cornell.gdiac.physics.obstacle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import edu.cornell.gdiac.main.GameCanvas;

/**
 * Circle-shaped model to support collisions.
 * <p>
 * Unless otherwise specified, the center of mass is as the center.
 */
public class WheelObstacle extends SimpleObstacle {

  /**
   * Shape information for this circle
   */
  protected CircleShape shape;
  /**
   * A cache value for the fixture (for resizing)
   */
  private Fixture geometry;
  /**
   * The color to show off the debug shape
   */
  private Color debugColor;

  /**
   * Creates a new circle at the origin.
   * <p>
   * The size is expressed in physics units NOT pixels.  In order for drawing to work properly, you
   * MUST set the drawScale. The drawScale converts the physics units to pixels.
   *
   * @param radius The wheel radius
   */
  public WheelObstacle(float radius) {
    this(0, 0, radius);
  }

  /**
   * Creates a new circle object.
   * <p>
   * The size is expressed in physics units NOT pixels.  In order for drawing to work properly, you
   * MUST set the drawScale. The drawScale converts the physics units to pixels.
   *
   * @param x      Initial x position of the circle center
   * @param y      Initial y position of the circle center
   * @param radius The wheel radius
   */
  public WheelObstacle(float x, float y, float radius) {
    super(x, y);
    debugColor = Color.YELLOW;
    shape = new CircleShape();
    shape.setRadius(radius);
  }

  /**
   * Returns the radius of this circle
   *
   * @return the radius of this circle
   */
  public float getRadius() {
    return shape.getRadius();
  }

  /**
   * Sets the radius of this circle
   *
   * @param value the radius of this circle
   */
  public void setRadius(float value) {
    shape.setRadius(value);
    markDirty(true);
  }

  /**
   * Returns the color to display the physics outline
   *
   * @return the color to display the physics outline
   */
  public Color getDebugColor() {
    return debugColor;
  }

  /**
   * Sets the color to display the physics outline
   *
   * @param value the color to display the physics outline
   */
  public void setDebugColor(Color value) {
    debugColor = value;
  }

  /**
   * Create new fixtures for this body, defining the shape
   * <p>
   * This is the primary method to override for custom physics objects
   */
  protected void createFixtures() {
    if (body == null) {
      return;
    }

    releaseFixtures();

    // Create the fixture
    fixture.shape = shape;
    geometry = body.createFixture(fixture);
    markDirty(false);
  }

  /**
   * Release the fixtures for this body, reseting the shape
   * <p>
   * This is the primary method to override for custom physics objects
   */
  protected void releaseFixtures() {
    if (geometry != null) {
      body.destroyFixture(geometry);
      geometry = null;
    }
  }

  /**
   * Draws the outline of the physics body.
   * <p>
   * This method can be helpful for understanding issues with collisions.
   *
   * @param canvas Drawing context
   */
  public void drawDebug(GameCanvas canvas) {
    if (debugColor != null) {
      canvas.drawPhysics(shape, debugColor, getX(), getY(), drawScale.x, drawScale.y);
    }
  }

}