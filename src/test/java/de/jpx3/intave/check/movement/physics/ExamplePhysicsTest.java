package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.cache.MockFullBlockStaticPlane;
import de.jpx3.intave.block.fluid.FluidFlow;
import de.jpx3.intave.block.fluid.Fluids;
import de.jpx3.intave.block.shape.resolve.DrillResolver;
import de.jpx3.intave.block.shape.resolve.MockShapeResolverPipeline;
import de.jpx3.intave.player.collider.Colliders;
import de.jpx3.intave.player.collider.complex.Collider;
import de.jpx3.intave.player.collider.simple.SimpleCollider;
import de.jpx3.intave.share.BoundingBox;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.test.FakePlayerFactory;
import de.jpx3.intave.test.FakeWorldFactory;
import de.jpx3.intave.test.MockEmptyInventory;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserFactory;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.MovementMetadata;
import de.jpx3.intave.world.border.MockWorldBorder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.bukkit.GameMode.SURVIVAL;

public final class ExamplePhysicsTest {
  private static final UUID EMPTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private User testUser;
  private Player player;

  private final Collider collider = Colliders.anyCollider();
  private final FluidFlow waterflow = Fluids.anyWaterflow();
  private final SimpleCollider simpleCollider = Colliders.anySimpleCollider();
  private final PlayerInventory inventory = new MockEmptyInventory();

  @BeforeEach
  void setUp() {
    MinecraftVersion.setCurrentVersion(MinecraftVersions.VER1_21_4);
    com.comphenix.protocol.utility.MinecraftVersion.setCurrentVersion(com.comphenix.protocol.utility.MinecraftVersion.v1_21_4);

    MockShapeResolverPipeline mockDrill = new MockShapeResolverPipeline();
    DrillResolver.manualInit(mockDrill);

    if (waterflow == null) {
      throw new IllegalStateException("Waterflow is null");
    }

    WorldBorder worldBorder = new MockWorldBorder(
      new Location(null, 0, 0, 0)
    );

    World world = FakeWorldFactory.createWorld(
      (methodName, args) -> {
        switch (methodName) {
          case "isChunkLoaded":
          case "isChunkInUse":
            return true;
          case "getWorldBorder":
            return worldBorder;
        }
        return null;
      }
    );

    Location location = new Location(world, 0, 20, 0);
    player = FakePlayerFactory.createPlayer(
      (methodName, args) -> {
        switch (methodName) {
          case "getInventory":
            return inventory;
          case "getWorld":
            return world;
          case "getLocation":
            return location;
          case "getUniqueId":
            return EMPTY_ID;
          case "getActivePotionEffects":
            return Collections.emptyList();
          case "getHealth":
            return 20.0;
          case "getFoodLevel":
            return 20;
          case "isFlying":
          case "getAllowFlight":
          case "isSprinting":
          case "isSneaking":
            return false;
          case "getFallDistance":
            return 0.0f;
          case "getGameMode":
            return SURVIVAL;
          case "getFlySpeed":
          case "getWalkSpeed":
            return 0.2f;
          case "getEntityId":
            return 1;
        }
        return null;
      }
    );

    int protocolVersion = 47;
    MockFullBlockStaticPlane plane = new MockFullBlockStaticPlane();
    plane.horizontalFill(1);
    testUser = UserFactory.createTestUserFor(player, s -> {
      switch (s) {
        case "collider":
          return collider;
        case "waterflow":
          return waterflow;
        case "simplifiedCollider":
          return simpleCollider;
        case "blockCache":
          return plane;
        case "protocolVersion":
          return protocolVersion;
      }
      return null;
    });
    UserRepository.manuallyRegisterUser(player, testUser);
  }

  @Test
  public void testy() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    Simulator simulator = Simulators.PLAYER;
    Motion motion;
    MovementMetadata metadata = testUser.meta().movement();

    for (int i = 0; i < 200; i++) {
      motion = metadata.mutableBaseMotionCopy();
      simulator.simulatePreTick(
        testUser, motion.copy(), metadata
      );
      Simulation simulation = simulator.simulateTick(
        testUser, motion.copy(), metadata.unmodifiable(), MovementConfiguration.noAction().withJump()
      );
      motion = simulation.motion();
      simulator.simulateAfterTick(
        testUser, metadata, metadata.verifiedPosition(), motion
      );
      Location lastLocation = metadata.lastPosition().toLocation(player.getWorld());
      Location newLocation = lastLocation.clone().add(motion.toBukkitVector());
      metadata.setVerifiedLocation(
        lastLocation, "AUTOACCEPT"
      );
      metadata.positionX = newLocation.getX();
      metadata.positionY = newLocation.getY();
      metadata.positionZ = newLocation.getZ();
      metadata.setBoundingBox(
        BoundingBox.fromPosition(testUser, metadata.unmodifiable(), newLocation)
      );
      metadata.setBaseMotion(motion);
      System.out.println(motion);
    }
  }
}
