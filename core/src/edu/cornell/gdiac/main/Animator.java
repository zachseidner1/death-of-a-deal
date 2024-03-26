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
      int cols, int size, int x, int y, int width, int height, int frame){
    // get texture from directory and call FilmStrip constructor
    Texture sheet = directory.getEntry(key, Texture.class);
    frames = new FilmStrip(sheet, rows, cols, size, x, y, width, height);

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
        setNextFrame();
        break;
      case "fan":
        fan = (FanModel) obstacle;
        frames = fan.getFanFrames();
        setNextFrame();
        break;
      default:
        break;
    }
  }

  public static void setNextFrame(){
    int nextFrame = frames.getFrame() + 1;
    if (nextFrame > frames.getSize()){
      frames.setFrame(1);
    } else {
      frames.setFrame(nextFrame);
    }
  }
}
