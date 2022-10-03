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
import com.epicnicity322.silktouchplus.util.SilkTouchPlusUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public final class SpawnerEntityDeathListener implements Listener {
    private static final @NotNull Random random = new Random();
    private final @NotNull SilkTouchPlus plugin;
    private final @NotNull SpawnerClickListener clickListener;
    private final @NotNull ItemStack specialRepairItem = new ItemStack(Material.ENDER_EYE);
    private double dropChance = 0.01;
    private boolean onlySpawnerLootCanRepair = true;

    public SpawnerEntityDeathListener(@NotNull SilkTouchPlus plugin, @NotNull SpawnerClickListener clickListener) {
        this.plugin = plugin;
        this.clickListener = clickListener;
        setSpecialRepairItem(Material.ENDER_EYE, true);
    }

    public void setOnlySpawnerLootCanRepair(boolean onlySpawnerLootCanRepair) {
        this.onlySpawnerLootCanRepair = onlySpawnerLootCanRepair;
    }

    public void setDropChance(double dropChance) {
        if (dropChance < 0.0) dropChance = 0.0;
        if (dropChance > 100.0) dropChance = 100.0;
        this.dropChance = dropChance;
    }

    public void setSpecialRepairItem(@NotNull Material material, boolean glowing) {
        specialRepairItem.setType(material);
        ItemMeta meta = specialRepairItem.getItemMeta();
        MessageSender lang = SilkTouchPlus.getLanguage();

        if (meta == null) {
            specialRepairItem.setType(Material.ENDER_EYE);
            meta = Objects.requireNonNull(specialRepairItem.getItemMeta());
        }
        if (glowing) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
        }

        meta.setDisplayName(lang.getColored("Repair.Special Repair Item.Display Name"));
        meta.setLore(Arrays.asList(SilkTouchPlusUtil.separateLines(lang.getColored("Repair.Special Repair Item.Lore")
                .replace("<health>", Double.toString(clickListener.specialRepairAmount)).replace("<health_percentage>", clickListener.formattedSpecialRepairAmount))));
        meta.getPersistentDataContainer().set(plugin.spawnerSpecialRepairItem, PersistentDataType.INTEGER, 1);
        meta.addItemFlags(ItemFlag.values());
        specialRepairItem.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Special Repair Item must only drop when a mob is killed by a player.
        if (dropChance == 0.0) return;
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();
        if (!(entity instanceof Mob)) return;
        if (!(damager instanceof Player)) return;

        if (((Mob) entity).getHealth() - event.getFinalDamage() <= 0.0) {
            if (damager.hasPermission("silktouchplus.special") && (random.nextDouble() * 100.0) <= dropChance) {
                MessageSender lang = SilkTouchPlus.getLanguage();
                entity.getWorld().dropItemNaturally(entity.getLocation(), specialRepairItem.clone());
                lang.send(event.getDamager(), lang.get("Repair.Special Repair Item.Drop")
                        .replace("<health>", Double.toString(clickListener.specialRepairAmount))
                        .replace("<health_percentage>", clickListener.formattedSpecialRepairAmount));
            }
            if (!onlySpawnerLootCanRepair) {
                entity.getPersistentDataContainer().set(plugin.repairLootEntity, PersistentDataType.INTEGER, 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Spawner entity drops must be added "spawner_loot" key, so it can be checked if the loot is same type as the
        //spawner when repairing it.

        Entity entity = event.getEntity();
        if (!(entity instanceof Mob)) return;

        if (entity.getPersistentDataContainer().has(plugin.repairLootEntity, PersistentDataType.INTEGER)) {
            EntityEquipment equipment = ((Mob) entity).getEquipment();
            String type = entity.getType().name();

            for (ItemStack drop : event.getDrops()) {
                // Don't want equipment to be categorized as mob spawner repair loot.
                if (isSimilarToEquipment(equipment, drop)) continue;

                ItemMeta meta = drop.getItemMeta();
                if (meta == null) continue;

                meta.getPersistentDataContainer().set(plugin.repairLoot, PersistentDataType.STRING, type);
                meta.setLore(Arrays.asList(SilkTouchPlusUtil.separateLines(SilkTouchPlus.getLanguage().getColored("Repair.Loot Lore")
                        .replace("<type>", type).replace("<health>", Double.toString(clickListener.lootRepairAmount))
                        .replace("<health_percentage>", clickListener.formattedLootRepairAmount))));
                drop.setItemMeta(meta);
            }
        }
    }

    private boolean isSimilarToEquipment(@Nullable EntityEquipment equipment, @NotNull ItemStack drop) {
        if (equipment == null) return false;
        return drop.isSimilar(equipment.getBoots()) || drop.isSimilar(equipment.getLeggings()) || drop.isSimilar(equipment.getChestplate())
                || drop.isSimilar(equipment.getHelmet()) || drop.isSimilar(equipment.getItemInMainHand()) || drop.isSimilar(equipment.getItemInOffHand());
    }
}
