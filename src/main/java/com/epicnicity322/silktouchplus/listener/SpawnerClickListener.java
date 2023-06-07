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
import com.epicnicity322.silktouchplus.util.HologramHandler;
import com.epicnicity322.silktouchplus.util.SilkTouchPlusUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class SpawnerClickListener implements Listener {
    private final @NotNull SilkTouchPlus plugin;
    double lootRepairAmount = 0.00025;
    @NotNull String formattedLootRepairAmount = SilkTouchPlusUtil.formatHealth(lootRepairAmount);
    double specialRepairAmount = 2.0;
    @NotNull String formattedSpecialRepairAmount = SilkTouchPlusUtil.formatHealth(specialRepairAmount);
    private double maxRepairHealth = 1.0;

    public SpawnerClickListener(@NotNull SilkTouchPlus plugin) {
        this.plugin = plugin;
    }

    public void setLootRepairAmount(double lootRepairAmount) {
        if (lootRepairAmount < 0.0) lootRepairAmount = 0.0;
        this.lootRepairAmount = lootRepairAmount;
        formattedLootRepairAmount = SilkTouchPlusUtil.formatHealth(lootRepairAmount);
    }

    public void setSpecialRepairAmount(double specialRepairAmount) {
        if (specialRepairAmount < 0.0) specialRepairAmount = 0.0;
        this.specialRepairAmount = specialRepairAmount;
        formattedSpecialRepairAmount = SilkTouchPlusUtil.formatHealth(specialRepairAmount);
    }

    public void setMaxRepairHealth(double maxRepairHealth) {
        this.maxRepairHealth = maxRepairHealth;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) return;

        ItemStack hand = event.getItem();
        ItemMeta handMeta;
        if (hand == null || (handMeta = hand.getItemMeta()) == null) return;
        PersistentDataContainer handContainer = handMeta.getPersistentDataContainer();

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        PersistentDataContainer spawnerContainer = spawner.getPersistentDataContainer();
        String spawnerType = spawner.getSpawnedType().name();
        Player player = event.getPlayer();
        MessageSender lang = SilkTouchPlus.getLanguage();
        String lootType = handContainer.get(plugin.repairLoot, PersistentDataType.STRING);
        double newHealth;

        if (lootType != null) {
            event.setCancelled(true);

            if (!spawnerType.equals(lootType)) {
                lang.send(player, lang.get("Repair.Unknown Loot").replace("<type>", spawnerType));
                return;
            }

            double currentHealth = spawnerContainer.getOrDefault(plugin.spawnerHealth, PersistentDataType.DOUBLE, 1.0);

            if (currentHealth < maxRepairHealth) {
                newHealth = currentHealth + lootRepairAmount;
                if (newHealth > maxRepairHealth) newHealth = maxRepairHealth;
            } else {
                lang.send(player, lang.get("Repair.Fully Repaired").replace("<type>", spawnerType));
                return;
            }
        } else if (handContainer.has(plugin.spawnerSpecialRepairItem, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            double maxRepairHealth = Math.max(specialRepairAmount, this.maxRepairHealth);
            double currentHealth = spawnerContainer.getOrDefault(plugin.spawnerHealth, PersistentDataType.DOUBLE, 1.0);

            if (currentHealth < maxRepairHealth) {
                newHealth = currentHealth + specialRepairAmount;
                if (newHealth > maxRepairHealth) newHealth = maxRepairHealth;
            } else {
                lang.send(player, lang.get("Repair.Fully Repaired").replace("<type>", spawnerType));
                return;
            }
        } else {
            return;
        }

        hand.setAmount(hand.getAmount() - 1);
        spawnerContainer.set(plugin.spawnerHealth, PersistentDataType.DOUBLE, newHealth);
        spawner.update();
        lang.send(player, lang.get("Repair.Repaired").replace("<type>", spawnerType)
                .replace("<health>", Double.toString(newHealth)).replace("<health_percentage>", SilkTouchPlusUtil.formatHealth(newHealth)));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockClickNormal(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) return;
        Player player = event.getPlayer();

        // When both hands are clear and player is sneaking, toggle the hologram.
        if (player.getInventory().getItemInMainHand().getType().isAir() && player.getInventory().getItemInOffHand().getType().isAir()) {
            HologramHandler hologramHandler = SilkTouchPlus.getHologramHandler();

            if (player.isSneaking() && hologramHandler != null && hologramHandler.isEnabled() && player.hasPermission("silktouchplus.hologram")) {
                // Checking cooldown for preventing hologram toggling
                Optional<MetadataValue> lastToggle = player.getMetadata("last_hologram_toggle").stream().findAny();
                long now = System.currentTimeMillis();
                if (lastToggle.isPresent()) {
                    long toggleTime = now - lastToggle.get().asLong();

                    if (toggleTime <= 7500) return;
                }
                player.setMetadata("last_hologram_toggle", new FixedMetadataValue(plugin, now));

                // Toggling the hologram
                CreatureSpawner spawner = (CreatureSpawner) block.getState();
                PersistentDataContainer spawnerContainer = spawner.getPersistentDataContainer();
                boolean toggle = spawnerContainer.getOrDefault(plugin.hologramEnabled, PersistentDataType.INTEGER, 0) == 1;
                MessageSender lang = SilkTouchPlus.getLanguage();

                spawnerContainer.set(plugin.hologramEnabled, PersistentDataType.INTEGER, toggle ? 0 : 1);
                spawner.update();
                hologramHandler.createHologram(spawner, spawnerContainer.getOrDefault(plugin.spawnerHealth, PersistentDataType.DOUBLE, 1.0));

                if (toggle) {
                    lang.send(player, lang.get("Spawner Hologram Toggle.Disabled"));
                } else {
                    lang.send(player, lang.get("Spawner Hologram Toggle.Enabled"));
                }
            }
        }

        // Check for change type permission.

        if (event.getItem() == null) return;
        String typeName = event.getItem().getType().name();

        if (typeName.endsWith("_SPAWN_EGG")) {
            MessageSender lang = SilkTouchPlus.getLanguage();
            String newSpawnerType = typeName.substring(0, typeName.indexOf("_SPAWN_EGG"));

            if (player.hasPermission("silktouchplus.changetype." + newSpawnerType)) {
                lang.send(player, lang.get("Change Type.Changed").replace("<type>", newSpawnerType));
            } else {
                lang.send(player, lang.get("Change Type.No Permission").replace("<type>", newSpawnerType));
                event.setCancelled(true);
            }
        }
    }
}
