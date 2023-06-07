/*
 * SilkTouchPlus - Minecraft Spigot plugin that allows spawners to be obtained with Silk Touch II.
 * Copyright (C) 2022  Christiano Rangel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.epicnicity322.silktouchplus.listener;

import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.epicpluginlib.bukkit.reflection.type.PackageType;
import com.epicnicity322.epicpluginlib.core.util.ObjectUtils;
import com.epicnicity322.silktouchplus.SilkTouchPlus;
import com.epicnicity322.silktouchplus.util.HologramHandler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;

public final class SpawnerSpawnListener implements Listener {
    private static final @NotNull HashSet<RenderKey> healthRenderingSpawners = new HashSet<>();
    private final @NotNull SilkTouchPlus plugin;
    private double spawnDamage = 0.0005;

    public SpawnerSpawnListener(@NotNull SilkTouchPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Applies the destroyed effect to the block, according to the specified health.
     *
     * @param health The health of this block. 1.0 for max health, 0.0 for min health.
     * @throws IllegalArgumentException If location has no world.
     */
    public static void addSpawnerRender(@NotNull Location location, @NotNull CreatureSpawner spawner, double health) {
        World world = location.getWorld();
        if (world == null) throw new IllegalArgumentException("Location does not specify a world!");
        if (health < 0.0) health = 0.0;
        HologramHandler hologramHandler = SilkTouchPlus.getHologramHandler();
        if (hologramHandler != null) hologramHandler.createHologram(spawner, health);
        if (health > 1.0) health = 1.0;
        healthRenderingSpawners.add(new RenderKey(world, location.getBlockX(), location.getBlockY(), location.getBlockZ(), (int) (9.0 - (health * 10.0))));
    }

    public static void removeSpawnerRender(@NotNull Location location) {
        healthRenderingSpawners.removeIf(key -> key.blockX == location.getBlockX()
                && key.blockY == location.getBlockY()
                && key.blockZ == location.getBlockZ()
                && key.world.equals(location.getWorld()));
        HologramHandler hologramHandler = SilkTouchPlus.getHologramHandler();
        if (hologramHandler != null) hologramHandler.removeHologram(location);
    }

    public static void renderHealth() {
        for (RenderKey key : healthRenderingSpawners) {
            Object packetBreak = key.packetBreak();
            for (Player player : key.world.getPlayers()) ReflectionUtil.sendPacket(player, packetBreak);
        }
    }

    public void setSpawnDamage(double spawnDamage) {
        this.spawnDamage = spawnDamage;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = event.getSpawner();
        PersistentDataContainer container = spawner.getPersistentDataContainer();
        double currentHealth = container.getOrDefault(plugin.spawnerHealth, PersistentDataType.DOUBLE, 1.0);
        double newHealth;

        if (currentHealth > 0.0) {
            newHealth = currentHealth - spawnDamage;
            if (newHealth < 0.0) newHealth = 0.0;
        } else {
            event.setCancelled(true);
            return;
        }

        container.set(plugin.spawnerHealth, PersistentDataType.DOUBLE, newHealth);
        spawner.update();
        addSpawnerRender(spawner.getLocation(), spawner, newHealth);
        // Tagging entity as mob spawner entity, so when it dies, its drops can be tagged as repairable for this type of
        //spawner.
        event.getEntity().getPersistentDataContainer().set(plugin.repairLootEntity, PersistentDataType.INTEGER, 1);
    }

    private static final class RenderKey {
        private static final @NotNull Random random = new Random();
        private static final @NotNull Class<?> packetClass = Objects.requireNonNull(ObjectUtils.getOrDefault(ReflectionUtil.getClass("net.minecraft.network.protocol.game.PacketPlayOutBlockBreakAnimation"), ReflectionUtil.getClass("PacketPlayOutBlockBreakAnimation", PackageType.MINECRAFT_SERVER)));
        private static final @NotNull Class<?> blockPositionClass = Objects.requireNonNull(ObjectUtils.getOrDefault(ReflectionUtil.getClass("net.minecraft.core.BlockPosition"), ReflectionUtil.getClass("BlockPosition", PackageType.MINECRAFT_SERVER)));
        private static final @NotNull Constructor<?> packetConstructor = Objects.requireNonNull(ReflectionUtil.getConstructor(packetClass, int.class, blockPositionClass, int.class));
        private static final @NotNull Constructor<?> blockPositionConstructor = Objects.requireNonNull(ReflectionUtil.getConstructor(blockPositionClass, int.class, int.class, int.class));
        private final @NotNull World world;
        private final int blockX;
        private final int blockY;
        private final int blockZ;
        private final int animationProgress;
        private int entityId = 0;
        private Object blockPosition;
        private Object packetBreak;

        public RenderKey(@NotNull World world, int blockX, int blockY, int blockZ, int animationProgress) {
            this.world = world;
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.animationProgress = animationProgress;
        }

        private int entityId() {
            if (entityId == 0) {
                entityId = random.nextInt();
            }
            return entityId;
        }

        private @NotNull Object blockPosition() throws InvocationTargetException, InstantiationException, IllegalAccessException {
            if (blockPosition == null) {
                blockPosition = blockPositionConstructor.newInstance(blockX, blockY, blockZ);
            }
            return blockPosition;
        }

        private @NotNull Object packetBreak() {
            if (packetBreak == null) {
                try {
                    packetBreak = packetConstructor.newInstance(entityId(), blockPosition(), animationProgress);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return packetBreak;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RenderKey renderKey = (RenderKey) o;
            return animationProgress == renderKey.animationProgress &&
                    blockX == renderKey.blockX &&
                    blockY == renderKey.blockY &&
                    blockZ == renderKey.blockZ &&
                    world.equals(renderKey.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, blockX, blockY, blockZ, animationProgress);
        }
    }
}
