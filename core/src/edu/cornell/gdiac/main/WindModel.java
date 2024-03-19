package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import edu.cornell.gdiac.util.MathUtil;

/**
 * Wrapper around wind particle initialization and organization.
 * Contains state of the wind as a collection of particles, including the wind container texture.
 */
public class WindModel {
  final private float DEFAULT_WIND_STRENGTH = 10.0f;
  final private WindType DEFAULT_WIND_TYPE = WindType.Constant;
  /**
   * Wind force cache
   */
  final private Vector2 windForceCache;
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
  private float windStrength = DEFAULT_WIND_STRENGTH;
  private WindType windType = DEFAULT_WIND_TYPE;
  private WindSide windSide;
  private PolygonRegion windPolyTexture;
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
  private WindParticleModel[] windParticles;
  private PolygonShape windShape;
  /**
   * Rotates wind windRotations about wind source
   */
  private float windRotation;
  private boolean isWindOn;
  private int numWindParticles;
  private int windLengthParticleGrids;
  private int windBreadthParticleGrids;
  private Vector2 drawScale;

  WindModel() {
    windFixtureDef = new FixtureDef();
    // Set as sensor
    windFixtureDef.isSensor = true;

    // Initialize force and origins
    windSource = new Vector2();
    windCenter = new Vector2();
    windForceCache = new Vector2();

    // Default wind color
    windColor = new Color((float) Math.random() * 0.5f, (float) Math.random() * 0.5f, (float) Math.random(), 0.05f);
  }

  /**
   * Set wind fields.
   * Note: changing or creating a new wind can only use this method to set wind state
   * Note: we are not concerned with rotation, which should indirectly set shape points. All we are
   * doing is initialize the wind model in the right, 0-degree rotation orientation.
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
    Vector2 drawScale
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
    this.drawScale = drawScale;
    this.windSource.set(windSourceX, windSourceY);

    float breadth2 = windBreadth / 2;
    float length2 = windLength / 2;

    { // Calculate wind area of effect center
      float centerX = windSourceX;
      float centerY = windSourceY;

      switch (windSide) {
        case LEFT:
          centerX -= length2;
          break;
        case RIGHT:
          centerX += length2;
          break;
      }

      windCenter.set(centerX, centerY);
    }

    windShape = new PolygonShape();
    windShape.setAsBox(
      length2,
      breadth2,
      new Vector2(windCenter.x - windSource.x, windCenter.y - windSource.y),
      windRotation
    );
    windFixtureDef.shape = windShape;

    assert numWindParticles > 0;
    if (numWindParticles > 0) {
      windParticles = new WindParticleModel[numWindParticles];
    }

    initWindParticles();
    initDrawing();
  }

  /**
   * Creates wind particles depending on wind initialization information
   */
  private void initWindParticles() {
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

    // Create wind particles
    for (int breadthIndex = 0; breadthIndex < windBreadthParticleGrids; breadthIndex++) {
      for (int lengthIndex = 0; lengthIndex < windLengthParticleGrids; lengthIndex++) {
        if (windParticleIndex >= numWindParticles) {
          break;
        }

        WindParticleModel windParticle = new WindParticleModel(
          windType,
          windParticleTexture,
          windLengthGridDist,
          windBreadthGridDist,
          (windSide == WindSide.LEFT ? -1 : 1) * lengthIndex * windLengthGridDist,
          windBreadthGridDist / 2 + breadthIndex * windBreadthGridDist - windBreadth / 2
        );

        windParticles[windParticleIndex++] = windParticle;
      }
    }
  }

  /**
   * Drawing and texture initialization logic
   */
  public void initDrawing() {
    boolean shouldFlipX = windSide == WindSide.LEFT;
    windTexture.flip(shouldFlipX, false);
    windTexture.setRegion(0, 0, windLength / 2, windBreadth / 2);
    windTexture.setRegionWidth((int) (windLength * drawScale.x));
    windTexture.setRegionHeight((int) (windBreadth * drawScale.y));

    int numVertices = windShape.getVertexCount();
    float[] scaled = new float[numVertices * 2];
    short[] tris = {0, 1, 3, 3, 2, 1};
    Vector2 tempPoint = new Vector2();

    // Weird, but polygon shape gets rotated about its origin, so it is not relative to the windSource origin: does not treat it as (0,0)
    // Solution is to shift the reference point that was once the origin (in the default right, 0 degree orientation)
    // Reference point is the average of the first and last point
    windShape.getVertex(0, tempPoint);
    float shiftX = tempPoint.x;
    float shiftY = tempPoint.y;
    windShape.getVertex(numVertices - 1, tempPoint);
    shiftX += tempPoint.x;
    shiftY += tempPoint.y;
    shiftX /= 2;
    shiftY /= 2; // shift is now the average of the first and last points

    for (int ii = 0; ii < scaled.length; ii++) {
      if (ii % 2 == 0) {
        windShape.getVertex(ii / 2, tempPoint);
        scaled[ii] = (tempPoint.x - shiftX) * drawScale.x;
      } else {
        scaled[ii] = (tempPoint.y - shiftY) * drawScale.y;
      }
    }

    windPolyTexture = new PolygonRegion(windTexture, scaled, tris);
  }

  public FixtureDef getFixtureDef() {
    return windFixtureDef;
  }

  public WindParticleModel[] getWindParticles() {
    return windParticles;
  }

  public void turnWindOn(boolean turnOn) {
    isWindOn = turnOn;
    if (!turnOn) {
      windForceCache.set(0, 0);
    }
  }

  /**
   * Draws the wind container and particles
   */
  protected void draw(GameCanvas canvas, Vector2 drawScale) {
    if (!isWindOn) {
      return;
    }

    canvas.draw(
      windPolyTexture,
      windColor,
      windSource.x * drawScale.x,
      windSource.y * drawScale.y,
      (windSide == WindSide.LEFT ? -1 : 1) * windLength * drawScale.x,
      (windSide == WindSide.LEFT ? -1 : 1) * windBreadth * drawScale.y
    );

    // Draw particles
    if (windParticles != null) {
      for (WindParticleModel windParticle : windParticles) {
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

  /**
   * Wrapper around wind particle fixture def that holds wind force behavior. Note that this is not concerned
   * with when to apply the force and merely contains logic for what kind of wind force is applied.
   */
  public class WindParticleModel {
    final private FixtureDef particleFixtureDef;
    final private PolygonShape particleShape;
    final private Color particleColor;
    /**
     * Separate wind type and texture parameter sas different particles can potentially have different behaviors
     */
    private WindType windType;
    private TextureRegion particleTexture;
    private float width, height;
    /**
     * The center of this particle; relative to containing wind source position
     */
    private float posX, posY;

    public WindParticleModel(WindType windType, TextureRegion particleTexture, float width, float height, float posX, float posY) {
      this.windType = windType;
      this.particleTexture = particleTexture;
      this.width = width;
      this.height = height;
      this.posX = posX;
      this.posY = posY;

      particleColor = new Color((float) Math.random() * 0.5f, (float) Math.random() * 0.5f, (float) Math.random(), 0.05f);

      particleShape = new PolygonShape();
      particleShape.setAsBox(
        width / 2,
        height / 2,
        new Vector2(posX, posY),
        windRotation
      );

      particleFixtureDef = new FixtureDef();
      particleFixtureDef.isSensor = true;
      particleFixtureDef.shape = particleShape;
    }

    /**
     * Returns the wind force applied at a contact position
     *
     * @param (x,y) the point of contact in world coordinates, used for determining wind force
     * @return wind force applied to the object at contact position
     */
    public Vector2 getForce(float x, float y) {
      if (!isWindOn) {
        assert windForceCache.x == 0 && windForceCache.y == 0;
        return windForceCache;
      }

      // Use force cache to temporarily store
      float normX = x - windSource.x;
      float normY = y - windSource.y;
      windForceCache.set(normX, normY);
      // Distance from wind source
      float norm = MathUtil.getMagnitude(windForceCache);
      float proximityRatio = Math.min(norm / windLength, 1);

      // Set the wind direction
      windForceCache.set(
        (float) Math.cos(windRotation),
        (float) Math.sin(windRotation));

      // Opposite force if left side
      windForceCache.scl((windSide == WindSide.LEFT ? -1 : 1));

      switch (windType) {
        case Constant:
          windForceCache.scl(windStrength);
          break;
        case Exponential:
          // TODO: Currently defaults to 0.5, could possibly set this in Tiled?
          float decayRate = 0.5f;
          float decayScale = (float) Math.exp(-decayRate * proximityRatio);
          windForceCache.scl(windStrength * decayScale);
          break;
        default:
          // Natural wind physics
          float dampingFactor = (float) Math.pow(1 - proximityRatio, 2);
          windForceCache.scl(windStrength * windStrength * dampingFactor);
          break;
      }

      return windForceCache;
    }

    protected FixtureDef getFixtureDef() {
      return particleFixtureDef;
    }

    protected void draw(GameCanvas canvas, Vector2 drawScale) {
      if (!isWindOn) {
        return;
      }

      // TODO: Draw based on vertices of shape in the future (animation)
    }
  }
}