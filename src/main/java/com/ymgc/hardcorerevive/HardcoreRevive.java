package com.ymgc.hardcorerevive;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.WeakHashMap;

@Mod("hardcorerevive")
public class HardcoreRevive {
    private static final WeakHashMap<Player, Boolean> revivingPlayers = new WeakHashMap<>();

    public HardcoreRevive() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (shouldRevive(player)) {
            event.setCanceled(true);
            revivePlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (shouldRevive(player)) {
            revivePlayer(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (shouldRevive(player)) {
            revivePlayer(player);
        }
    }

    private boolean shouldRevive(Player player) {
        if (player.level().isClientSide) return false;

        boolean isHardcore = player.level().getLevelData().isHardcore();

        boolean isDeadOrSpectator = player.isDeadOrDying() ||
                (player instanceof ServerPlayer sp &&
                        sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR);

        return isHardcore && isDeadOrSpectator;
    }

    private void revivePlayer(ServerPlayer player) {
        if (revivingPlayers.containsKey(player)) return;

        try {
            revivingPlayers.put(player, true);

            player.setGameMode(GameType.SURVIVAL);

            player.setHealth(player.getMaxHealth());

            player.removeAllEffects();

            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0F);

            teleportToRespawnPoint(player);

        } finally {
            revivingPlayers.remove(player);
        }
    }

    private void teleportToRespawnPoint(ServerPlayer player) {
        ServerLevel respawnDimension = player.server.getLevel(player.getRespawnDimension());
        if (respawnDimension == null) {
            respawnDimension = player.server.overworld();
        }

        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos == null) {
            respawnPos = respawnDimension.getSharedSpawnPos();
        }

        Vec3 spawnPoint = Vec3.atBottomCenterOf(respawnPos);

        player.teleportTo(
                respawnDimension,
                spawnPoint.x,
                spawnPoint.y,
                spawnPoint.z,
                player.getRespawnAngle(),
                0
        );
    }
}