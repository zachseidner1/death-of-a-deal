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
  final private FixtureDef windFixtureDef;
  final private Color windColor;
  /**
   * Whether breadth should be set as the texture width
   */
  boolean breadthAsWidth;
  private float windStrength = DEFAULT_WIND_STRENGTH;
  private WindType windType = DEFAULT_WIND_TYPE;
  private WindSide windSide;
  private TextureRegion windTexture;
  private TextureRegion windParticleTexture;
  /**
   * Represents the breadth over which the wind force is applied, starting from the wind origin
   */
  private float windBreadth;
  /**
   * Represents the length over which the wind force is applied, starting from the wind origin
   */
  private float windLength;
  private WindModel[] windParticles;
  private PolygonShape windShape;
  private float windRotation;
  private boolean isWindOn;
  private int numWindParticles;
  private int windLengthParticleGrids;
  private int windBreadthParticleGrids;

  WindModel() {
    windFixtureDef = new FixtureDef();
    // Set as sensor
    windFixtureDef.isSensor = true;

    // Initialize force and origins
    windSource = new Vector2();
    windCenter = new Vector2();
    windForce = new Vector2();

    // Default wind color
    windColor = new Color((float) Math.random() * 0.5f, (float) Math.random() * 0.5f, (float) Math.random(), 0.1f);
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
    int numWindParticles,
    int windLengthParticleGrids,
    int windBreadthParticleGrids,
    WindSide windSide,
    WindType windType,
    TextureRegion windTexture,
    TextureRegion windParticleTexture,
    float offsetX, // TODO: Remove but add to wind particles model
    float offsetY // TODO: Remove but add to wind particles model
  ) {

    this.windBreadth = windBreadth;
    this.windLength = windLength;
    this.windRotation = windRotation;
    this.windSide = windSide;
    this.windType = windType;
    this.windStrength = windStrength;
    this.windTexture = windTexture;
    this.windParticleTexture = windParticleTexture;
    this.numWindParticles = numWindParticles;
    this.windLengthParticleGrids = windLengthParticleGrids;
    this.windBreadthParticleGrids = windBreadthParticleGrids;

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
      new Vector2(windCenter.x - windSource.x + offsetX, windCenter.y - windSource.y + offsetY),
      (float) (windRotation / (Math.PI / 2))
    );
    windFixtureDef.shape = windShape;

    assert numWindParticles > 0;
    windParticles = new WindModel[numWindParticles];

    createWindParticles();
  }

  // TODO: Replace with wind particles instead

  /**
   * Creates wind particles depending on wind initialization information
   */
  private void createWindParticles() {
    // Standard is to uniformly spread particles (static fixtures right now)
    if (numWindParticles == 0) {
      return;
    }

    if (windBreadthParticleGrids == 0 && windLengthParticleGrids == 0) {
      windBreadthParticleGrids = windLengthParticleGrids = (int) Math.ceil(Math.sqrt(numWindParticles));
    } else {
      windLengthParticleGrids = windLengthParticleGrids == 0 ?
        (int) Math.ceil((float) numWindParticles / windBreadthParticleGrids) :
        windLengthParticleGrids;
      windBreadthParticleGrids = windBreadthParticleGrids == 0 ?
        (int) Math.ceil((float) numWindParticles / windLengthParticleGrids) :
        windBreadthParticleGrids;
    }

    // Grids should apply ample space for desired number of wind particles
    assert windLengthParticleGrids * windBreadthParticleGrids >= numWindParticles;

    // Prioritize wind length grids first
    int windParticleIndex = 0;
    float windBreadthGridDist = windBreadth / windBreadthParticleGrids;
    float windLengthGridDist = windLength / windLengthParticleGrids;
    // Bottom vertex of wind (not fan) dimensions corresponding to wind source origin
    float windSourceBottomX = windSource.x;
    float windSourceBottomY = windSource.y - windBreadth / 2;
    for (int breadthIndex = 0; breadthIndex < windBreadthParticleGrids; breadthIndex++) {
      for (int lengthIndex = 0; lengthIndex < windLengthParticleGrids; lengthIndex++) {
        // TODO: Replace with wind particles class, we currently test with simplified wind models
        if (windParticleIndex >= numWindParticles) {
          break;
        }

        WindModel windParticle = new WindModel();
        windParticle.initialize(
          (windSide == WindSide.LEFT ? -1 : 1) * lengthIndex * windLengthGridDist + windSourceBottomX, // TODO: For particles, should be set to just true wind force
          windBreadthGridDist / 2 + breadthIndex * windBreadthGridDist + windSourceBottomY, // TODO: Same as above, we would need to calibrate the draw logic for wind particles depending on wind source
          windBreadthGridDist,
          windLengthGridDist,
          windStrength,
          windRotation,
          0,
          0,
          0,
          windSide,
          windType,
          windParticleTexture,
          windParticleTexture,
          (windSide == WindSide.LEFT ? -1 : 1) * lengthIndex * windLengthGridDist,
          windBreadthGridDist / 2 + breadthIndex * windBreadthGridDist - windBreadth / 2
        );
        windParticles[windParticleIndex++] = windParticle;
      }
    }
  }

  public FixtureDef getWindFixtureDef() {
    return windFixtureDef;
  }

  public WindModel[] getWindParticles() {
    return windParticles;
  }

  public void turnWindOn(boolean turnOn) {
    isWindOn = turnOn;
    if (!turnOn) {
      windForce.set(0, 0);
    }

    if (windParticles != null) {
      for (int i = 0; i < windParticles.length; i++) {
        windParticles[i].turnWindOn(turnOn);
      }
    }
  }

  // TODO: Make private and figure out how out wind particles can utilize this method (by simply calling it)

  /**
   * Returns the wind force applied at a contact position
   *
   * @param (x,y) the point of contact in world coordinates, used for determining wind force
   * @return wind force applied to the object at contact position
   */
  public Vector2 findWindForce(float x, float y) {
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
      case Constant:
        windForce.scl(windStrength);
        break;
      case Exponential:
        // TODO: Implement
        // float decayRate = 0.5f;
//        float decayScale = (float) Math.exp(-decayRate * norm / windLength);
//        windForce.scl(windStrength * decayScale);
      default:
        // TODO: Implement
//
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

    // Draw particles
    if (windParticles != null) {
      for (int i = 0; i < windParticles.length; i++) {
        WindModel windParticle = windParticles[i];
        windParticle.draw(
          canvas,
          drawScale
        );
      }
    }
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
    Default, // Simulate realistic wind physics
  }
}