package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

/**
 * Wrapper around wind fixture that holds wind force behavior. Note that this is not concerned with
 * when to apply the force and merely contains logic for what kind of wind force is applied
 */
public class WindModel {
  final private float DEFAULT_WIND_STRENGTH = 10.0f;
  final private WindType DEFAULT_WIND_TYPE = WindType.Constant;
  /**
   * Wind force cache
   */
  final private Vector2 windForce;
  /**
   * Origin from which the wind is applied (in world coordinates)
   */
  final private Vector2 windSource;
  /**
   * Center of the wind bounding box (in world coordinates)
   */
  final private Vector2 windCenter;
  /**
   * Whether breadth should be set as the texture width
   */
  boolean breadthAsWidth;
  private float windStrength = DEFAULT_WIND_STRENGTH;
  private WindType windType = DEFAULT_WIND_TYPE;
  private WindSide windSide;
  private TextureRegion windTexture;
  /**
   * Represents the breadth over which the wind force is applied, starting from the wind origin
   */
  private float windBreadth;
  /**
   * Represents the length over which the wind force is applied, starting from the wind origin
   */
  private float windLength;
  private FixtureDef windFixtureDef;
  private PolygonShape windShape;
  private float windRotation;
  private Color windColor;
  private boolean isWindOn;

  WindModel() {
    windFixtureDef = new FixtureDef();
    // Set as sensor
    windFixtureDef.isSensor = true;

    // Initialize force and origins
    windSource = new Vector2();
    windCenter = new Vector2();
    windForce = new Vector2();

    // Default color
    windColor = new Color(0.5f, 0.5f, 1f, 0.1f);
  }

  /**
   * Set wind fields
   */
  public void initialize(
    float windSourceX,
    float windSourceY,
    float windBreadth,
    float windLength,
    float windStrength,
    float windRotation,
    WindSide windSide,
    WindType windType,
    TextureRegion windTexture) {

    this.windBreadth = windBreadth;
    this.windLength = windLength;
    this.windRotation = windRotation;
    this.windSide = windSide;
    this.windType = windType;
    this.windStrength = windStrength;
    this.windTexture = windTexture;

    float breadth2 = windBreadth / 2;
    float length2 = windLength / 2;
    windSource.set(windSourceX, windSourceY);

    { // Calculate wind area of effect center
      float centerX = windSourceX;
      float centerY = windSourceY;

      switch (windSide) {
        case LEFT:
          breadthAsWidth = false;
          centerX -= length2;
          break;
        case RIGHT:
          breadthAsWidth = false;
          centerX += length2;
          break;
        case TOP:
          breadthAsWidth = true;
          centerY += length2;
          break;
        default:
          breadthAsWidth = true;
          centerY -= length2;
          break;
      }

      windCenter.set(centerX, centerY);
    }

    windShape = new PolygonShape();
    float width2 = breadthAsWidth ? breadth2 : length2;
    float height2 = breadthAsWidth ? length2 : breadth2;
    windShape.setAsBox(
      width2,
      height2,
      new Vector2(windCenter.x - windSource.x, windCenter.y - windSource.y),
      (float) (windRotation / (Math.PI / 2))
    );
    windFixtureDef.shape = windShape;
  }

  public FixtureDef getWindFixtureDef() {
    return windFixtureDef;
  }

  public void turnWindOn(boolean turnOn) {
    isWindOn = turnOn;
    if (!turnOn) {
      windForce.set(0, 0);
    }
  }

  /**
   * Returns the wind force applied at a contact position
   *
   * @param (x,y) the point of contact in world coordinates, used for determining wind force
   * @return wind force applied to the object at contact position
   */
  protected Vector2 findWindForce(float x, float y) {
    if (!isWindOn) {
      assert windForce.x == 0 && windForce.y == 0;
      return windForce;
    }

    // TODO: Figure why forces are not accurate here
    float normX = x - windSource.x;
    float normY = y - windSource.y;
    float norm = (float) Math.sqrt(normX * normX + normY * normY);
    normX /= norm;
    normY /= norm;

    // TODO: Temporarily set to pure directional force (will need to change if rotation exists)
    windForce.set(
      windSide == WindSide.LEFT ? -1 : windSide == WindSide.RIGHT ? 1 : 0,
      windSide == WindSide.BOTTOM ? -1 : windSide == WindSide.TOP ? 1 : 0);

    switch (windType) {
      case Exponential:
        // TODO: Implement
//        float decayRate = 0.5f;
//        float decayScale = (float) Math.exp(-decayRate * norm / windLength);
//        windForce.scl(windStrength * decayScale);
        break;
      default:
        windForce.scl(windStrength);
        break;
    }

    return windForce;
  }

  /**
   * // TODO: Can be refactored with binding drawing with wind region coordinates
   * Draws the wind
   */
  protected void draw(GameCanvas canvas, Vector2 drawScale) {
    if (!isWindOn) {
      return;
    }

    boolean shouldFlipX = windSide == WindSide.LEFT;
    boolean shouldFlipY = windSide == WindSide.BOTTOM;
    windTexture.flip(shouldFlipX, false);
    float assetRotation = windSide == WindSide.TOP ? 90 : windSide == WindSide.BOTTOM ? -90 : 0;
    float width = breadthAsWidth ? windBreadth : windLength;
    float height = breadthAsWidth ? windLength : windBreadth;
    windTexture.setRegion(0, 0, width / 2, height / 2);

    // TODO: Figure out asset rotation
    // TODO: find how to set texture origin
    canvas.draw(
      windTexture,
      windColor,
      0,
      0,
      windSource.x * drawScale.x,
      (windSource.y - height / 2) * drawScale.y,
      (shouldFlipX ? -1 : 1) * width * drawScale.x,
      (shouldFlipY ? -1 : 1) * height * drawScale.y
    );
  }

  /**
   * Enumeration representing the side out of which wind direction is applied from the fan
   */
  public enum WindSide {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
  }

  /**
   * Enumeration of different types of winds, which corresponds to how force is applied
   */
  public enum WindType {
    Constant, // Constant force
    Exponential, // Exponential decay
  }
}