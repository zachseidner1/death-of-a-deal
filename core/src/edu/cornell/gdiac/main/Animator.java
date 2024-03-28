package edu.cornell.gdiac.main;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.physics.obstacle.SimpleObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class Animator {
  public static FilmStrip frames;
  public static PlayerModel player;
  public static FanModel fan;

  public static void create(SimpleObstacle obstacle, AssetDirectory directory, String key, int rows,
      int cols, int frame){
    // get texture from directory and call FilmStrip constructor
    Texture sheet = directory.getEntry(key, Texture.class);
    frames = new FilmStrip(sheet, rows, cols);
    frames.setFrame(frame);

    switch (obstacle.getName()){
      case "player":
        player = (PlayerModel) obstacle;
        player.setPlayerFrames(frames);
        break;
      case "fan":
        fan = (FanModel) obstacle;
        fan.setFanFrames(frames);
        break;
      default:
        break;
    }
}

  public static void getTextureRegion(SimpleObstacle obstacle){
    switch (obstacle.getName()){
      case "player":
        player = (PlayerModel) obstacle;
        frames = player.getPlayerFrames();
        player.setPlayerFrames(setNextFrame());
        break;
      case "fan":
        fan = (FanModel) obstacle;
        frames = fan.getFanFrames();
        fan.setFanFrames(setNextFrame());
        break;
      default:
        break;
    }
  }

  public static FilmStrip setNextFrame(){
    assert frames != null;
    int nextFrame = frames.getFrame() + 1;
    if (nextFrame >= frames.getSize()) {
      nextFrame = 1;
      frames = frames.copy();
    }
    frames.setFrame(nextFrame);
    return frames;
  }
}
