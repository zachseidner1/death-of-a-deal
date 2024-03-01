package edu.cornell.gdiac.json;

import com.badlogic.gdx.physics.box2d.Contact;

public class CollisionController {

  /* TODO P5 add fields that you may need
  This could include the level, avatar, bounce pad, etc..
  If you notice that after adding these the gameplay controller doesn't need it, remove it from the
  gameplay controller.

  TODO P5 Also when you are handling collisions, make sure that you don't directly modify game controller values.
  For example for detecting level completion, we could have an "is complete" flag in the level model,
  updating this and then the GameController reads from that to avoid cyclic dependencies.
   */
  private LevelModel levelModel;

  public CollisionController(LevelModel levelModel) {
    this.levelModel = levelModel;
  }

  public void handleContact(Contact contact) {
    // TODO P5 this method should call other collision methods based on which two objects are colliding
    // P5 should work on getting this the default collisions to work using this method.

    // TODO P2 check if we landed on bounce pad, call and implement handleBounce
  }

  public void handleBounce(PlayerModel player, BouncePlatformModel bouncePlatform) {
    // TODO P2 implement and call this function
  }
}
