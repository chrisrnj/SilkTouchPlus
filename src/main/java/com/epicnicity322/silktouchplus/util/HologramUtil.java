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

package com.epicnicity322.silktouchplus.util;

import com.epicnicity322.silktouchplus.SilkTouchPlus;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class HologramUtil {
    private final @NotNull SilkTouchPlus plugin;
    private final @NotNull HashMap<Location, Hologram> holograms = new HashMap<>();
    private boolean enabled = true;

    public HologramUtil(@NotNull SilkTouchPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void createHologram(@NotNull CreatureSpawner spawner, double health) {
        if (!enabled) return;
        Location location = spawner.getLocation();
        PersistentDataContainer container = spawner.getPersistentDataContainer();
        if (container.getOrDefault(plugin.hologramEnabled, PersistentDataType.INTEGER, 0) != 1) {
            removeHologram(location);
            return;
        }
        String[] lines = SilkTouchPlusUtil.separateLines(SilkTouchPlus.getLanguage().getColored("Spawner Hologram")
                .replace("<type>", spawner.getSpawnedType().name()).replace("<health>", Double.toString(health))
                .replace("<health_percentage>", SilkTouchPlusUtil.formatHealth(health)));

        if (lines.length == 0 || lines[0].isEmpty()) {
            removeHologram(location);
            return;
        }

        Hologram hologram = holograms.get(location);

        if (hologram == null) {
            Location hologramLocation = location.clone().add(0.5, 2.0, 0.5);
            for (Hologram h : HologramsAPI.getHolograms(plugin)) {
                if (h.getLocation().equals(hologramLocation)) {
                    hologram = h;
                    holograms.put(location, hologram);
                    hologram.clearLines();
                    break;
                }
            }
            if (hologram == null)
                holograms.put(location, hologram = HologramsAPI.createHologram(plugin, hologramLocation));
        } else {
            hologram.clearLines();
        }

        for (String line : lines) hologram.appendTextLine(line);
    }

    public void removeHologram(@NotNull Location location) {
        Hologram hologram = holograms.remove(location);
        if (hologram != null) hologram.delete();
    }

    public void clear() {
        holograms.entrySet().removeIf(entry -> {
            entry.getValue().delete();
            return true;
        });
    }
}
