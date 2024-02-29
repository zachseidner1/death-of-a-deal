/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game.  It is the "static main" of
 * LibGDX.  In the first lab, we extended ApplicationAdapter.  In previous lab
 * we extended Game.  This is because of a weird graphical artifact that we do not
 * understand.  Transparencies (in 3D only) is failing when we use ApplicationAdapter.
 * There must be some undocumented OpenGL code in setScreen.
 *
 * This class differs slightly from the labs in that the AssetManager is now a
 * singleton and is not constructed by this class.
 *
 * Author: Walker M. White
 * Version: 3/2/2016
 */
package edu.cornell.gdiac.json;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;

/**
 * Root class for a LibGDX.
 * <p>
 * This class is technically not the ROOT CLASS. Each platform has another class above this (e.g. PC
 * games use DesktopLauncher) which serves as the true root.  However, those classes are unique to
 * each platform, while this class is the same across all plaforms. In addition, this functions as
 * the root class all intents and purposes, and you would draw it as a root class in an architecture
 * specification.
 */
public class GDXRoot extends Game implements ScreenListener {

  /**
   * AssetManager to load game assets (textures, sounds, etc.)
   */
  AssetDirectory directory;
  /**
   * Drawing context to display graphics (VIEW CLASS)
   */
  private GameCanvas canvas;
  /**
   * Player mode for the asset loading screen (CONTROLLER CLASS)
   */
  private LoadingMode loading;
  /**
   * Player mode for the the game proper (CONTROLLER CLASS)
   */
  private GameController controller;

  /**
   * Creates a new game from the configuration settings.
   */
  public GDXRoot() {
  }

  /**
   * Called when the Application is first created.
   * <p>
   * This is method immediately loads assets for the loading screen, and prepares the asynchronous
   * loader for all other assets.
   */
  public void create() {
    canvas = new GameCanvas();
    loading = new LoadingMode("jsons/assets.json", canvas, 1);

    // Initialize the three game worlds
    controller = new GameController();
    loading.setScreenListener(this);
    setScreen(loading);
  }

  /**
   * Called when the Application is destroyed.
   * <p>
   * This is preceded by a call to pause().
   */
  public void dispose() {
    // Call dispose on our children
    setScreen(null);
    controller.dispose();

    canvas.dispose();
    canvas = null;

    // Unload all of the resources
    if (directory != null) {
      directory.unloadAssets();
      directory.dispose();
      directory = null;
    }
    super.dispose();
  }

  /**
   * Called when the Application is resized.
   * <p>
   * This can happen at any point during a non-paused state but will never happen before a call to
   * create().
   *
   * @param width  The new width in pixels
   * @param height The new height in pixels
   */
  public void resize(int width, int height) {
    canvas.resize();
    super.resize(width, height);
  }

  /**
   * The given screen has made a request to exit its player mode.
   * <p>
   * The value exitCode can be used to implement menu options.
   *
   * @param screen   The screen requesting to exit
   * @param exitCode The state of the screen upon exit
   */
  public void exitScreen(Screen screen, int exitCode) {
    if (screen == loading) {
      directory = loading.getAssets();
      controller.gatherAssets(directory);
      controller.setScreenListener(this);
      controller.setCanvas(canvas);
      controller.reset();
      setScreen(controller);

      loading.dispose();
      loading = null;
    } else if (exitCode == GameController.EXIT_QUIT) {
      // We quit the main application
      Gdx.app.exit();
    }
  }

}