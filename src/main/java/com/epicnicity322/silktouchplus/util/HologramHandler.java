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

package com.epicnicity322.silktouchplus.util;

import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.jetbrains.annotations.NotNull;

public interface HologramHandler {
    boolean isEnabled();

    void setEnabled(boolean enabled);

    void createHologram(@NotNull CreatureSpawner spawner, double health);

    void removeHologram(@NotNull Location location);

    void clear();
}
