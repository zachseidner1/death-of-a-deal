package edu.cornell.gdiac.main;

import com.badlogic.gdx.physics.box2d.Body;
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
  private boolean complete;
  private float bounceForce = 10f;

  public CollisionController(LevelModel levelModel) {
    this.level = levelModel;
    this.sensorFixtures = new ObjectSet<Fixture>();
  }

  /**
   * Return the complete flag to GameController
   *
   * @return
   */
  public boolean isComplete() {
    return complete;
  }

  /**
   * Change the complete flag to true if player touches the goal
   *
   * @param value
   */
  public void setComplete(boolean value) {
    complete = value;
  }

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

      // Check if any fixture is a bounce platform
      BouncePlatformModel bouncePlatform = null;
      if (bd1 instanceof BouncePlatformModel) {
        bouncePlatform = (BouncePlatformModel) bd1;
      } else if (bd2 instanceof BouncePlatformModel) {
        bouncePlatform = (BouncePlatformModel) bd2;
      }

      // See if we have landed on the ground.
      if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
          (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
        avatar.setGrounded(true);
        sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground

        // If we landed on a bounce platform, handle the bounce
        if (bouncePlatform != null) {
          handleBounce(avatar, bouncePlatform);
        }
      }

      // Check for win condition
      if ((bd1 == avatar && bd2 == door) ||
          (bd1 == door && bd2 == avatar)) {
        setComplete(true);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

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

  public void preSolve(Contact contact, Manifold oldManifold) {
    // Pre-solve collision handling
  }

  public void postSolve(Contact contact, ContactImpulse impulse) {
    // Post-solve collision handling
  }

  public void handleBounce(PlayerModel player, BouncePlatformModel bouncePlatform) {
    // Check if the player is above the bounce platform
    if (player.getPosition().y > bouncePlatform.getPosition().y) {
      // Apply the bounce force to the player
      player.setBounce(bounceForce);
    }
  }
}
