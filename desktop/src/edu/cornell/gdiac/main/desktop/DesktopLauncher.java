/*
 * DesktopLauncher.java
 *
 * LibGDX is a cross-platform development library. You write all of your code in
 * the core project.  However, you still need some extra classes if you want to
 * deploy on a specific platform (e.g. PC, Android, Web).  That is the purpose
 * of this class.  It deploys your game on a PC/desktop computer.
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.main.desktop;

import edu.cornell.gdiac.backend.GDXApp;
import edu.cornell.gdiac.backend.GDXAppSettings;
import edu.cornell.gdiac.main.GDXRoot;

/**
 * The main class of the game.
 * <p>
 * This class sets the window size and launches the game.  Aside from modifying the window size, you
 * should almost never need to modify this class.
 */
public class DesktopLauncher {

  /**
   * Classic main method that all Java programmers know.
   * <p>
   * This method simply exists to start a new LwjglApplication.  For desktop games, LibGDX is built
   * on top of LWJGL (this is not the case for Android).
   *
   * @param arg Command line arguments
   */
  public static void main(String[] arg) {
    GDXAppSettings config = new GDXAppSettings();
    config.title = "JSON Demo";
    config.width = 800;
    config.height = 600;
    config.fullscreen = false;
    config.resizable = false;
    config.foregroundFPS=60;
    config.backgroundFPS=60;
    new GDXApp(new GDXRoot(), config);
  }
}