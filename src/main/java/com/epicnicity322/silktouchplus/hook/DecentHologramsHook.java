/*
 * SilkTouchPlus - Minecraft Spigot plugin that allows spawners to be obtained with Silk Touch II.
 * Copyright (C) 2023  Christiano Rangel
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

package com.epicnicity322.silktouchplus.hook;

import com.epicnicity322.silktouchplus.SilkTouchPlus;
import com.epicnicity322.silktouchplus.util.HologramHandler;
import com.epicnicity322.silktouchplus.util.SilkTouchPlusUtil;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public final class DecentHologramsHook implements HologramHandler {
    private static int id = 0;
    private final @NotNull SilkTouchPlus plugin;
    private final @NotNull HashMap<Location, Hologram> holograms = new HashMap<>();
    private boolean enabled = true;

    public DecentHologramsHook(@NotNull SilkTouchPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void createHologram(@NotNull CreatureSpawner spawner, double health) {
        if (!enabled) return;
        Location location = spawner.getLocation();
        PersistentDataContainer container = spawner.getPersistentDataContainer();

        if (container.getOrDefault(plugin.hologramEnabled, PersistentDataType.INTEGER, 0) != 1) {
            removeHologram(location);
            return;
        }

        String[] lines = SilkTouchPlusUtil.separateLines(SilkTouchPlus.getLanguage().getColored("Spawner Hologram").replace("<type>", spawner.getSpawnedType().name()).replace("<health>", Double.toString(health)).replace("<health_percentage>", SilkTouchPlusUtil.formatHealth(health)));

        if (lines.length == 0 || lines[0].isEmpty()) {
            removeHologram(location);
            return;
        }

        Hologram hologram = holograms.get(location);

        if (hologram == null) {
            holograms.put(location, hologram = new Hologram("stphologram" + id++, location.clone().add(0.5, 2.0, 0.5), false));
            hologram.showAll();
        } else {
            hologram.removePage(0);
        }

        HologramPage page = hologram.getPage(0);
        if (page == null) page = hologram.insertPage(0);
        for (String line : lines) page.addLine(new HologramLine(page, page.getNextLineLocation(), line));
    }

    @Override
    public void removeHologram(@NotNull Location location) {
        Hologram hologram = holograms.remove(location);
        if (hologram != null) hologram.delete();
    }

    @Override
    public void clear() {
        holograms.entrySet().removeIf(entry -> {
            entry.getValue().delete();
            return true;
        });
    }
}
