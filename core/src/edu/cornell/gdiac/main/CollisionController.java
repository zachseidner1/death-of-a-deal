package edu.cornell.gdiac.main;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;

public class CollisionController implements ContactListener {

  /**
   * Arbitrary bounce impulse that are used for bouncing up
   */
  private final float BOUNCE_FORCE = 10f;
  /**
   * Mark set to handle more sophisticated collision callbacks
   */
  protected ObjectSet<Fixture> sensorFixtures;
  private LevelModel level;

  /**
   * Set up the collision model based on Level Model & Create sensorFixtures to track active bodies
   *
   * @param levelModel The level model passed down from game controller
   */
  public CollisionController(LevelModel levelModel) {
    this.level = levelModel;
    this.sensorFixtures = new ObjectSet<Fixture>();
  }

  /**
   * Called when two fixtures begin to touch. This method is triggered during the simulation step at
   * the start of a contact between two fixtures
   *
   * @param contact The contact point containing information about the two fixtures that have come
   *                into contact
   */
  public void beginContact(Contact contact) {

    Fixture fix1 = contact.getFixtureA();
    Fixture fix2 = contact.getFixtureB();

    Body body1 = fix1.getBody();
    Body body2 = fix2.getBody();
    Object fd1 = fix1.getUserData();
    Object fd2 = fix2.getUserData();

    try {
      Obstacle bd1 = (Obstacle) body1.getUserData();
      Obstacle bd2 = (Obstacle) body2.getUserData();

      PlayerModel avatar = level.getAvatar();
      BoxObstacle door = level.getExit();

      // See if we have landed on the ground
      if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
          (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
        avatar.setGrounded(true);
        sensorFixtures.add(avatar == bd1 ? fix2 : fix1);
      }

      // Check for win condition
      if ((bd1 == avatar && bd2 == door) ||
          (bd1 == door && bd2 == avatar)) {
        System.out.println("Completed");
        level.setComplete(true);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Called when two fixtures cease to touch. This method is invoked during the simulation step when
   * a contact between two fixtures ends. Useful for detecting when entities separate after a
   * collision and can be used to reset states or properties
   *
   * @param contact The contact point that contains information about the two fixtures that have
   *                ceased to be in contact
   */
  public void endContact(Contact contact) {
    Fixture fix1 = contact.getFixtureA();
    Fixture fix2 = contact.getFixtureB();

    Body body1 = fix1.getBody();
    Body body2 = fix2.getBody();
    Object fd1 = fix1.getUserData();
    Object fd2 = fix2.getUserData();

    Object bd1 = body1.getUserData();
    Object bd2 = body2.getUserData();

    PlayerModel avatar = level.getAvatar();
    if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
        (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
      sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
      if (sensorFixtures.size == 0) {
        avatar.setGrounded(false);
      }
    }
  }
  /**
   * Called before the physics engine solves a contact
   *
   * @param contact     The contact about to be solved
   * @param oldManifold The manifold for the contact before it is solved, containing information
   *                    about how the two shapes are touching
   */
  public void preSolve(Contact contact, Manifold oldManifold) {
    // Pre-solve collision handling
    Fixture fix1 = contact.getFixtureA();
    Fixture fix2 = contact.getFixtureB();
    Body body1 = fix1.getBody();
    Body body2 = fix2.getBody();
    PlayerModel ply=level.getAvatar();
    try {
      Obstacle bd1 = (Obstacle) body1.getUserData();
      Obstacle bd2 = (Obstacle) body2.getUserData();
    if (bd1.equals(ply)){
      if( bd1 instanceof BouncePlatformModel){
      BouncePlatformModel bplt=(BouncePlatformModel) bd2;
      float c= bplt.getCoefficient();
      if (ply.isFrozen()){
        contact.setRestitution(c);
      }}
    }
    if (bd2.equals(ply)){
      if( bd1 instanceof BouncePlatformModel){
      BouncePlatformModel bplt=(BouncePlatformModel) bd1;
      float c= bplt.getCoefficient();
      if (ply.isFrozen()){
        contact.setRestitution(c);
      }
      }
    }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Called after the physics engine has solved a contact
   *
   * @param contact The contact that was solved
   * @param impulse The impulse generated by the physics engine to resolve the contact
   */
  public void postSolve(Contact contact, ContactImpulse impulse) {
    // Post-solve collision handling
  }

  /**
   * Handles the interaction between the player and a bounce platform
   *
   * @param player         The player model that is interacting with the bounce platform
   * @param bouncePlatform The bounce platform model that the player is interacting with
   */

}
