package edu.cornell.gdiac.main;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import edu.cornell.gdiac.physics.obstacle.Obstacle;

/**
 * A factory wrapper class for creating sensor fixtures that are boxes (rectangular)
 */
public abstract class BoxFixtureSensor<T extends Obstacle> {
  protected T obstacle; // Contains the fixture-owning body
  protected FixtureDef sensorFixtureDef;
  protected Vector2 center; // Relative to fixture-owning body
  protected Vector2 dimensions; // (width2, height2)

  /**
   * (x, y): center, relative to the fixture owner (body)
   * (width2, height2): half dimensiosn from center
   */
  public BoxFixtureSensor(T obstacle, float x, float y, float width2, float height2) {
    this.obstacle = obstacle;

    // Initialize shape and fixture definition
    PolygonShape sensorShape = new PolygonShape();
    center = new Vector2(x, y);
    dimensions = new Vector2(width2, height2);
    sensorShape.setAsBox(width2, height2, center, 0);

    sensorFixtureDef = new FixtureDef();
    sensorFixtureDef.isSensor = true;
    sensorFixtureDef.shape = sensorShape;
  }

  public FixtureDef getFixtureDef() {
    return sensorFixtureDef;
  }

  public T getPlatform() {
    return obstacle;
  }

  /**
   * Callback to be called right before contact resolution with sensor
   */
  public abstract void beginContact(Obstacle obs);

  /**
   * Callback to be called right after contact resolution with sensor
   */
  public abstract void endContact(Obstacle obs);
}