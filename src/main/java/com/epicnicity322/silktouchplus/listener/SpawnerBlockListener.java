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

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.silktouchplus.SilkTouchPlus;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class SpawnerBlockListener implements Listener {
    private final @NotNull SilkTouchPlus plugin;
    @NotNull List<String> breakTools = Arrays.asList("DIAMOND_PICKAXE", "NETHERITE_PICKAXE");
    private int silkTouchLevel = 2;

    public SpawnerBlockListener(@NotNull SilkTouchPlus plugin) {
        this.plugin = plugin;
    }

    public void setBreakTools(@NotNull List<String> breakTools) {
        this.breakTools = breakTools;
    }

    public void setSilkTouchLevel(int silkTouchLevel) {
        this.silkTouchLevel = silkTouchLevel;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        Location location = block.getLocation();

        SpawnerSpawnListener.removeSpawnerRender(location);

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!breakTools.contains(hand.getType().name())) return;
        if (hand.getEnchantmentLevel(Enchantment.SILK_TOUCH) != silkTouchLevel) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        String type = spawner.getSpawnedType().name();
        MessageSender lang = SilkTouchPlus.getLanguage();

        if (player.hasPermission("silktouchplus.drop." + type)) {
            block.getWorld().dropItemNaturally(location, SilkTouchPlus.getSpawner(spawner));
            event.setExpToDrop(0);
            lang.send(player, lang.get("Drop.Dropped").replace("<type>", type));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        PersistentDataContainer data = Objects.requireNonNull(event.getItemInHand().getItemMeta()).getPersistentDataContainer();
        String type = data.get(plugin.spawnerType, PersistentDataType.STRING);

        if (type == null) return;

        Block block = event.getBlockPlaced();

        if (block.getType() != Material.SPAWNER) {
            block.setType(Material.SPAWNER);
        }

        CreatureSpawner spawner = (CreatureSpawner) block.getState();

        double health = data.getOrDefault(plugin.spawnerHealth, PersistentDataType.DOUBLE, 1.0);

        spawner.setSpawnedType(EntityType.valueOf(type));
        spawner.getPersistentDataContainer().set(plugin.spawnerHealth, PersistentDataType.DOUBLE, health);
        spawner.getPersistentDataContainer().set(plugin.hologramEnabled, PersistentDataType.INTEGER, 1);
        spawner.update();
        SilkTouchPlus.getLanguage().send(event.getPlayer(), SilkTouchPlus.getLanguage().get("Placed").replace("<type>", type));
        SpawnerSpawnListener.addSpawnerRender(block.getLocation(), spawner, health);
        SpawnerSpawnListener.renderHealth();
    }
}
