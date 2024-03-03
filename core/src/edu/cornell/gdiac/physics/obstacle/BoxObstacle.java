/*
 * BoxObject.java
 *
 * Given the name Box2D, this is your primary model class.  Most of the time,
 * unless it is a player controlled avatar, you do not even need to subclass
 * BoxObject.  Look through the code and see how many times we use this class.
 *
 * Author: Walker M. White
 * Modified to support custom debug colors
 * Version: 3/2/2016
 */
package edu.cornell.gdiac.physics.obstacle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import edu.cornell.gdiac.main.GameCanvas;

/**
 * Box-shaped model to support collisions.
 *
 * <p>Unless otherwise specified, the center of mass is as the center.
 */
public class BoxObstacle extends SimpleObstacle {

  /**
   * Shape information for this box
   */
  protected PolygonShape shape;

  /**
   * Cache of the polygon vertices (for resizing)
   */
  protected float[] vertices;

  /**
   * The width and height of the box
   */
  private Vector2 dimension;

  /**
   * A cache value for when the user wants to access the dimensions
   */
  private Vector2 sizeCache;

  /**
   * A cache value for the fixture (for resizing)
   */
  private Fixture geometry;

  /**
   * Creates a new box at the origin.
   *
   * <p>The size is expressed in physics units NOT pixels. In order for drawing to work properly,
   * you MUST set the drawScale. The drawScale converts the physics units to pixels.
   *
   * @param width  The object width in physics units
   * @param height The object width in physics units
   */
  public BoxObstacle(float width, float height) {
    this(0, 0, width, height);
  }

  /**
   * Creates a new box object.
   *
   * <p>The size is expressed in physics units NOT pixels. In order for drawing to work properly,
   * you MUST set the drawScale. The drawScale converts the physics units to pixels.
   *
   * @param x      Initial x position of the box center
   * @param y      Initial y position of the box center
   * @param width  The object width in physics units
   * @param height The object width in physics units
   */
  public BoxObstacle(float x, float y, float width, float height) {
    super(x, y);
    dimension = new Vector2(width, height);
    sizeCache = new Vector2();
    shape = new PolygonShape();
    vertices = new float[8];
    geometry = null;

    debugColor = Color.YELLOW;

    // Initialize
    resize(width, height);
  }

  /**
   * Returns the dimensions of this box
   *
   * <p>This method does NOT return a reference to the dimension vector. Changes to this vector
   * will not affect the shape. However, it returns the same vector each time its is called, and so
   * cannot be used as an allocator.
   *
   * @return the dimensions of this box
   */
  public Vector2 getDimension() {
    return sizeCache.set(dimension);
  }

  /**
   * Sets the dimensions of this box
   *
   * <p>This method does not keep a reference to the parameter.
   *
   * @param value the dimensions of this box
   */
  public void setDimension(Vector2 value) {
    setDimension(value.x, value.y);
  }

  /**
   * Sets the dimensions of this box
   *
   * @param width  The width of this box
   * @param height The height of this box
   */
  public void setDimension(float width, float height) {
    dimension.set(width, height);
    markDirty(true);
    resize(width, height);
  }

  /**
   * Returns the box width
   *
   * @return the box width
   */
  public float getWidth() {
    return dimension.x;
  }

  /**
   * Sets the box width
   *
   * @param value the box width
   */
  public void setWidth(float value) {
    sizeCache.set(value, dimension.y);
    setDimension(sizeCache);
  }

  /**
   * Returns the box height
   *
   * @return the box height
   */
  public float getHeight() {
    return dimension.y;
  }

  /**
   * Sets the box height
   *
   * @param value the box height
   */
  public void setHeight(float value) {
    sizeCache.set(dimension.x, value);
    setDimension(sizeCache);
  }

  /**
   * Reset the polygon vertices in the shape to match the dimension.
   */
  protected void resize(float width, float height) {
    // Make the box with the center in the center
    vertices[0] = -width / 2.0f;
    vertices[1] = -height / 2.0f;
    vertices[2] = -width / 2.0f;
    vertices[3] = height / 2.0f;
    vertices[4] = width / 2.0f;
    vertices[5] = height / 2.0f;
    vertices[6] = width / 2.0f;
    vertices[7] = -height / 2.0f;
    shape.set(vertices);
  }

  /**
   * Create new fixtures for this body, defining the shape
   *
   * <p>This is the primary method to override for custom physics objects
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
   *
   * <p>This is the primary method to override for custom physics objects
   */
  protected void releaseFixtures() {
    if (geometry != null) {
      body.destroyFixture(geometry);
      geometry = null;
    }
  }

  /**
   * Draws the outline of the physics body.
   *
   * <p>This method can be helpful for understanding issues with collisions.
   *
   * @param canvas Drawing context
   */
  public void drawDebug(GameCanvas canvas) {
    if (debugColor != null) {
      canvas.drawPhysics(shape, debugColor, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
    }
  }
}
