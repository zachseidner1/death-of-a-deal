/*
 * WallModel.java
 *
 * This is a refactored version of a platform from Lab 4.  We have made it a
 * specialized class for two reasons.  First, we want to allow platforms to be
 * specified as a rectangle, but allow a uniform texture tile.  The BoxObstacle
 * stretches the texture to fit, it does not tile it.  Therefore, we have
 * modified BoxObstacle to provide tile support.
 *
 * The second reason for using a specialized class is so that we can import its
 * properties from a JSON file.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * JSON version, 3/2/2016
 */
package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.util.TiledJsonParser;
import java.lang.reflect.Field;

/**
 * A polygon shape representing the screen boundary
 *
 * <p>Note that the constructor does very little. The true initialization happens by reading the
 * JSON value. In addition, this class overrides the drawing and positioning functions to provide a
 * tiled texture.
 */
public class PlatformModel extends BoxObstacle {

  /**
   * Texture information for this object
   */
  protected PolygonRegion region;

  /**
   * The texture anchor upon region initialization
   */
  protected Vector2 anchor;

  /**
   * Create a new PlatformModel with degenerate settings
   */
  public PlatformModel() {
    super(0, 0, 1, 1);
    region = null;
  }

  /**
   * Initializes a PolygonRegion to support a tiled texture
   *
   * <p>In order to keep the texture uniform, the drawing polygon position needs to be absolute.
   * However, this can cause a problem when we want to move the platform (e.g. a dynamic platform).
   * The purpose of the texture anchor is to ensure that the texture does not move as the object
   * moves.
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

  /**
   * Initializes the platform via the given JSON value
   *
   * <p>The JSON value has been parsed and is part of a bigger level file. However, this JSON value
   * is limited to the platform subtree
   *
   * @param directory the asset manager
   * @param json      the JSON subtree defining the platform
   */
  public void initialize(AssetDirectory directory, JsonValue json) {
    setName(json.name());
    float[] pos = json.get("pos").asFloatArray();
    float[] size = json.get("size").asFloatArray();
    setPosition(pos[0], pos[1]);
    setDimension(size[0], size[1]);

    TiledJsonParser.initPlatformFromJson(this, directory, json);
  }

  /**
   * Initializes a platform based on the assumption that it is a tile. This assumption implies the
   * body type is static and the object is square.
   *
   * @param x              x position of the tile (unscaled, just based on where it is in Tiled)
   * @param y              y position of the tile (unscaled, just based on where it is in Tiled)
   * @param directory      asset directory
   * @param tileKey        the key to the texture that is associated with the tile
   * @param tileProperties the properties of the tile as a JSON value
   */
  public void initializeAsTile(float x, float y, float tileSize, AssetDirectory directory,
      String tileKey,
      JsonValue tileProperties) {
    // Use the scale to convert pixel positions to box 2D positions
    float pixelScaleX = 1 / drawScale.x;
    float pixelScaleY = 1 / drawScale.y;
    setPosition(x * pixelScaleX, y * pixelScaleY);
    setDimension(tileSize * pixelScaleX, tileSize * pixelScaleY);
    setBodyType(BodyType.StaticBody);
    TextureRegion textureRegion = new TextureRegion(directory.getEntry(tileKey, Texture.class));
    setTexture(textureRegion);
    Color debugColor = null;
    int debugOpacity = -1;
    while (tileProperties != null) {
      switch (tileProperties.getString("name")) {
        case "density":
          setDensity(tileProperties.getFloat("value"));
          break;
        case "restitution":
          setRestitution(tileProperties.getFloat("value"));
          break;
        case "friction":
          setFriction(tileProperties.getFloat("value"));
          break;
        case "debugcolor":
          try {
            String cname = tileProperties.get("value").asString().toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color) field.get(null));
          } catch (Exception e) {
            debugColor = null; // Not defined
          }
          break;
        case "debugopacity":
          debugOpacity = tileProperties.getInt("value");
          break;
      }
      if (debugOpacity != -1 && debugColor != null) {
        debugColor.mul(debugOpacity / 255f);
        setDebugColor(debugColor);
      }

      tileProperties = tileProperties.next();
    }
  }

  /**
   * Draws the physics object.
   *
   * @param canvas Drawing context
   */
  public void draw(GameCanvas canvas) {
    // draw must be offset by 8 both ways, not really sure why right now
    // TODO: Maybe something to do with tilesize being 16 => 16 /2
    if (region != null) {
      canvas.draw(
          texture, (getX()) * drawScale.x - 8, (getY() * drawScale.y) - 8);
    }
  }
}
