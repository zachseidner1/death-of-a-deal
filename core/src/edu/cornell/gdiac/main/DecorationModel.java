package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;

public class DecorationModel extends BoxObstacle {

  /**
   * Texture information for this object
   */
  protected PolygonRegion region;

  /**
   * The texture anchor upon region initialization
   */
  protected Vector2 anchor;

  /**
   * Texture for the decoration
   */
  protected TextureRegion texture;

  /**
   * The width and height of the box
   */
  private Vector2 dimension;

  /**
   * Create a new DecorationModel with default settings
   */
  public DecorationModel() {
    super(0, 0, 1, 1);
    region = null;
  }

  /**
   * Reset the polygon vertices in the shape to match the dimension.
   */
  @Override
  protected void resize(float width, float height) {
    super.resize(width, height);
    initRegion();
  }

  /**
   * Sets the current position for this physics body
   *
   * <p>This method does not keep a reference to the parameter.
   *
   * @param value the current position for this physics body
   */
  @Override
  public void setPosition(Vector2 value) {
    super.setPosition(value.x, value.y);
    initRegion();
  }

  /**
   * Sets the current position for this physics body
   *
   * @param x the x-coordinate for this physics body
   * @param y the y-coordinate for this physics body
   */
  @Override
  public void setPosition(float x, float y) {
    super.setPosition(x, y);
    initRegion();
  }

  /**
   * Sets the x-coordinate for this physics body
   *
   * @param value the x-coordinate for this physics body
   */
  @Override
  public void setX(float value) {
    super.setX(value);
    initRegion();
  }

  /**
   * Sets the y-coordinate for this physics body
   *
   * @param value the y-coordinate for this physics body
   */
  @Override
  public void setY(float value) {
    super.setY(value);
    initRegion();
  }

  /**
   * Sets the object texture for drawing purposes.
   *
   * <p>In order for drawing to work properly, you MUST set the drawScale. The drawScale converts
   * the physics units to pixels.
   *
   * @param value the object texture for drawing purposes.
   */
  @Override
  public void setTexture(TextureRegion value) {
    super.setTexture(value);
    initRegion();
  }

  /**
   * Sets the drawing scale for this physics object
   *
   * <p>The drawing scale is the number of pixels to draw before Box2D unit. Because mass is a
   * function of area in Box2D, we typically want the physics objects to be small. So we decouple
   * that scale from the physics object. However, we must track the scale difference to communicate
   * with the scene graph.
   *
   * <p>We allow for the scaling factor to be non-uniform.
   *
   * @param x the x-axis scale for this physics object
   * @param y the y-axis scale for this physics object
   */
  @Override
  public void setDrawScale(float x, float y) {
    super.setDrawScale(x, y);
    initRegion();
  }


  public void initialize(float x, float y, float tileSize, AssetDirectory directory,
      String tilekey) {
    setPosition(x * (1 / drawScale.x), y * (1 / drawScale.y));
    setDimension(tileSize * ((float) 1 / drawScale.x), tileSize * ((float) 1 / (drawScale.y)));
    setBodyType(BodyType.StaticBody);
    TextureRegion textureRegion = new TextureRegion(directory.getEntry(tilekey, Texture.class));
    setTexture(textureRegion);
    initRegion();

  }


  /**
   * Initializes a PolygonRegion to support a tiled texture.
   */
  private void initRegion() {
    if (texture == null) {
      return;
    }
    float[] scaled = new float[vertices.length];
    for (int ii = 0; ii < scaled.length; ii++) {
      if (ii % 2 == 0) {
        scaled[ii] = (vertices[ii] + getX()) * drawScale.x;
      } else {
        scaled[ii] = (vertices[ii] + getY()) * drawScale.y;
      }
    }
    short[] tris = {0, 1, 3, 3, 2, 1};
    anchor = new Vector2(getX(), getY());
    region = new PolygonRegion(texture, scaled, tris);
  }

  /**
   * Draws the decoration to the canvas.
   *
   * @param canvas The canvas to draw onto
   */
  public void draw(GameCanvas canvas) {
    if (region != null) {

      canvas.draw(
          region,
          Color.WHITE,
          0,
          0,
          (getX() - anchor.x) * drawScale.x,
          (getY() - anchor.y) * drawScale.y,
          getAngle(),
          1,
          1);
    }
  }
}
