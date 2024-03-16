package edu.cornell.gdiac.main;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
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
        level.setComplete(true);
      }

      // Determine if there is a "collision" with wind from fans
      boolean isBody1Fan = body1.getUserData() instanceof FanModel;
      boolean isBody2Fan = body2.getUserData() instanceof FanModel;
      FanModel fan = null;
      Obstacle obj = null;

      if (isBody1Fan) {
        fan = (FanModel) body1.getUserData();
        obj = bd2;
      } else if (isBody2Fan) {
        fan = (FanModel) body2.getUserData();
        obj = bd1;
      }

      // On wind contact callback
      if (fan != null && obj != null) {
        // Should not continue detection with static body
        if (obj.getBodyType() == BodyType.StaticBody) {
          contact.setEnabled(false);
        } else {
          // Apply wind force
          Vector2 windForce = fan.findWindForce(obj.getX(), obj.getY());
          System.out.println("Wind force: " + windForce.x + ", " + windForce.y);
          obj.getBody().applyForce(windForce, obj.getPosition(), true);
        }
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
    PlayerModel plyr = level.getAvatar();
    preSolveBounce(contact, plyr, body1, body2);
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

  public void preSolveBounce(Contact contact, PlayerModel plyr, Body body1, Body body2) {
    try {
      Obstacle bd1 = (Obstacle) body1.getUserData();
      Obstacle bd2 = (Obstacle) body2.getUserData();
      if (bd1.equals(plyr)) {
        if (bd1 instanceof BouncePlatformModel) {
          BouncePlatformModel bplt = (BouncePlatformModel) bd2;
          float c = bplt.getCoefficient();
          if (plyr.getIsFrozen()) {
            contact.setRestitution(c);
          }
        }
      }
      if (bd2.equals(plyr)) {
        if (bd1 instanceof BouncePlatformModel) {
          BouncePlatformModel bplt = (BouncePlatformModel) bd1;
          float c = bplt.getCoefficient();
          if (plyr.getIsFrozen()) {
            contact.setRestitution(c);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
