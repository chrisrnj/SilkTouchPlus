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

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SilkTouchListener implements Listener {
    private final @NotNull SpawnerBlockListener blockListener;
    private boolean allowSilkTouchBookCombining = true;
    private boolean preventCustomSilkTouchRepair = true;

    public SilkTouchListener(@NotNull SpawnerBlockListener blockListener) {
        this.blockListener = blockListener;
    }

    public void setAllowSilkTouchBookCombining(boolean allowSilkTouchBookCombining) {
        this.allowSilkTouchBookCombining = allowSilkTouchBookCombining;
    }

    public boolean isAllowingSilkTouchBookCombining() {
        return allowSilkTouchBookCombining;
    }

    public void setPreventCustomSilkTouchRepair(boolean preventCustomSilkTouchRepair) {
        this.preventCustomSilkTouchRepair = preventCustomSilkTouchRepair;
    }

    public boolean isPreventingCustomSilkTouchRepair() {
        return preventCustomSilkTouchRepair;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        Inventory anvil = event.getInventory();
        ItemStack first = anvil.getItem(0);
        if (first == null) return;
        ItemStack second = anvil.getItem(1);
        if (second == null) return;

        if (allowSilkTouchBookCombining) {
            if (second.getType() == Material.ENCHANTED_BOOK && event.getViewers().stream().allMatch(viewer -> viewer.hasPermission("silktouchplus.combine"))) {
                if (first.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta firstMeta = (EnchantmentStorageMeta) Objects.requireNonNull(first.getItemMeta());
                    EnchantmentStorageMeta secondMeta = (EnchantmentStorageMeta) Objects.requireNonNull(second.getItemMeta());

                    if (firstMeta.getStoredEnchantLevel(Enchantment.SILK_TOUCH) == 1 && secondMeta.getStoredEnchantLevel(Enchantment.SILK_TOUCH) == 1) {
                        ItemStack silkTouchTwo = new ItemStack(Material.ENCHANTED_BOOK);
                        EnchantmentStorageMeta silkTouchTwoMeta = (EnchantmentStorageMeta) Objects.requireNonNull(silkTouchTwo.getItemMeta());
                        silkTouchTwoMeta.addStoredEnchant(Enchantment.SILK_TOUCH, 2, true);
                        silkTouchTwo.setItemMeta(silkTouchTwoMeta);
                        event.setResult(silkTouchTwo);
                        return;
                    }
                } else if (blockListener.breakTools.contains(first.getType().toString())) {
                    ItemStack result = event.getResult();

                    if (result != null) {
                        EnchantmentStorageMeta secondMeta = (EnchantmentStorageMeta) Objects.requireNonNull(second.getItemMeta());

                        if (secondMeta.getStoredEnchantLevel(Enchantment.SILK_TOUCH) == 2 || (first.getEnchantmentLevel(Enchantment.SILK_TOUCH) == 1
                                && secondMeta.getStoredEnchantLevel(Enchantment.SILK_TOUCH) == 1)) {
                            result.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 2);
                            return;
                        }
                    }
                }
            }
        }

        if (preventCustomSilkTouchRepair) {
            if (first.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 1 || second.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 1) {
                event.setResult(new ItemStack(Material.AIR));
            }
        }
    }
}