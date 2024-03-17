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
import edu.cornell.gdiac.main.WindModel.WindParticleModel;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.util.MathUtil;

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

  private static Vector2 getForceVector(SlopeModel slope) {
    float slopeAngle = slope.getSlopeAngle();
    float forceMagnitude = slope.getSlopeFrozenForce();

    Vector2 force;
    if (slopeAngle >= 0 && slopeAngle <= Math.PI) {
      // Slope is pointing down left
      force = new Vector2(
        (float) -Math.cos(slopeAngle) * forceMagnitude,
        (float) -Math.sin(slopeAngle) * forceMagnitude
      );
    } else {
      // Slope is pointing down right
      force = new Vector2((float) Math.cos(slopeAngle) * forceMagnitude,
        (float) Math.sin(slopeAngle) * forceMagnitude);
    }
    return force;
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

      handleWindContact(contact, fix1, fix2, bd2, bd1, avatar, fd2, fd1);

      // Check for win condition
      if ((bd1 == avatar && bd2 == door) ||
        (bd1 == door && bd2 == avatar)) {
        level.setComplete(true);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleWindContact(Contact contact, Fixture fix1, Fixture fix2, Obstacle bd2,
                                 Obstacle bd1,
                                 PlayerModel avatar, Object fd2, Object fd1) {
    boolean is1WindFixture = fix1.getUserData() instanceof WindParticleModel;
    boolean is2WindFixture = fix2.getUserData() instanceof WindParticleModel;
    WindParticleModel windParticle = null;
    Obstacle obj = null;

    if (is1WindFixture) {
      windParticle = (WindParticleModel) fix1.getUserData();
      obj = bd2;
    } else if (is2WindFixture) {
      windParticle = (WindParticleModel) fix2.getUserData();
      obj = bd1;
    }

    // On wind contact callback
    if (windParticle != null && obj != null) {
      // Should not continue detection with static body
      if (obj.getBodyType() == BodyType.StaticBody) {
        contact.setEnabled(false);
      } else {
        // Apply wind force
        Vector2 windForce = windParticle.getForce(obj.getX(), obj.getY());
        obj.getBody().applyForce(windForce, obj.getPosition(), true);
      }
    }

    boolean isWind = is1WindFixture || is2WindFixture || fix1.getUserData() instanceof WindModel || fix2.getUserData() instanceof WindModel;

    // See if we have landed on the ground
    if (!isWind && ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
      (avatar.getSensorName().equals(fd1) && avatar != bd2))) {
      avatar.setGrounded(true);
      sensorFixtures.add(avatar == bd1 ? fix2 : fix1);
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
    preSolveSlope(contact, plyr, body1, body2);
    preSolveBreak(contact, plyr, body1, body2);
  }

  /**
   * Called after the physics engine has solved a contact
   *
   * @param contact The contact that was solved
   * @param impulse The impulse generated by the physics engine to resolve the contact
   */
  public void postSolve(Contact contact, ContactImpulse impulse) {
    Fixture fix1 = contact.getFixtureA();
    Fixture fix2 = contact.getFixtureB();
    Body body1 = fix1.getBody();
    Body body2 = fix2.getBody();
    PlayerModel plyr = level.getAvatar();
    postSolveBounce(contact, plyr, body1, body2);
  }

  public void preSolveBounce(Contact contact, PlayerModel plyr, Body body1, Body body2) {
    try {
      Obstacle bd1 = (Obstacle) body1.getUserData();
      Obstacle bd2 = (Obstacle) body2.getUserData();
      if (bd1.equals(plyr)) {
        if (bd2 instanceof BouncePlatformModel) {
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

  public void preSolveBreak(Contact contact, PlayerModel plyr, Body body1, Body body2) {
    try {
      Obstacle bd1 = (Obstacle) body1.getUserData();
      Obstacle bd2 = (Obstacle) body2.getUserData();
      if (bd1.equals(plyr)) {
        if (bd1 instanceof BreakablePlatformModel) {
          BreakablePlatformModel breakablePlatform = (BreakablePlatformModel) bd1;
          if (MathUtil.getMagnitude(plyr.getLinearVelocity())
            > breakablePlatform.getBreakMinVelocity()
            || breakablePlatform.isBroken()
          ) {
            breakablePlatform.setBroken(true);
            contact.setEnabled(false);
          }
        }
      }
      if (bd2.equals(plyr)) {
        if (bd1 instanceof BreakablePlatformModel) {
          BreakablePlatformModel breakablePlatform = (BreakablePlatformModel) bd1;
          if (MathUtil.getMagnitude(plyr.getLinearVelocity())
            > breakablePlatform.getBreakMinVelocity()
            || breakablePlatform.isBroken()
          ) {
            breakablePlatform.setBroken(true);
            contact.setEnabled(false);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void preSolveSlope(Contact contact, PlayerModel plyr, Body body1, Body body2) {
    try {
      Obstacle bd1 = (Obstacle) body1.getUserData();
      Obstacle bd2 = (Obstacle) body2.getUserData();

      if ((bd1.equals(plyr) && bd2 instanceof SlopeModel) || (bd2.equals(plyr)
        && bd1 instanceof SlopeModel)) {
        SlopeModel slope = (bd1 instanceof SlopeModel) ? (SlopeModel) bd1 : (SlopeModel) bd2;

        // Only add extra force when player is frozen
        if (plyr.getIsFrozen()) {
          Vector2 force = getForceVector(slope);
          plyr.getBody().applyForce(force, plyr.getPosition(), true);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void postSolveBounce(Contact contact, PlayerModel plyr, Body body1, Body body2) {
    try {
      Obstacle bd1 = (Obstacle) body1.getUserData();
      Obstacle bd2 = (Obstacle) body2.getUserData();
      applyBounceVelocity(plyr, bd1, bd2);
      applyBounceVelocity(plyr, bd2, bd1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void applyBounceVelocity(PlayerModel playerModel, Obstacle bd1, Obstacle bd2) {
    if (bd1.equals(playerModel)) {
      if (bd2 instanceof BouncePlatformModel) {
        if (playerModel.getIsFrozen()) {
          BouncePlatformModel bplt = (BouncePlatformModel) bd2;
          float maxSpeed = bplt.getMaxSpeed();
          float xSpeed = playerModel.getLinearVelocity().x;
          float ySpeed = playerModel.getLinearVelocity().y;
          if (xSpeed > maxSpeed) {
            playerModel.setVX(maxSpeed);
          } else if (xSpeed < -maxSpeed) {
            playerModel.setVX(-maxSpeed);
          }
          if (ySpeed > maxSpeed) {
            playerModel.setVY(maxSpeed);
          } else if (ySpeed < -maxSpeed) {
            playerModel.setVY(-maxSpeed);
          }
        }
      }
    }
  }
}
