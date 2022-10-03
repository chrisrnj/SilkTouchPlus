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

package com.epicnicity322.silktouchplus.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.command.TabCompleteRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.silktouchplus.SilkTouchPlus;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class ChangeTypeCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "changetype";
    }

    @Override
    public int getMinArgsAmount() {
        return 2;
    }

    @Override
    protected @NotNull CommandRunnable getNotEnoughArgsRunnable() {
        return (label, sender, args) -> SilkTouchPlus.getLanguage().send(sender, SilkTouchPlus.getLanguage().get("Change Type.Command.Invalid Arguments").replace("<label>", label));
    }

    @Override
    public @NotNull String getPermission() {
        return "silktouchplus.command.changetype";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return (label, sender, args) -> SilkTouchPlus.getLanguage().send(sender, SilkTouchPlus.getLanguage().get("General.No Permission"));
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = SilkTouchPlus.getLanguage();

        if (!(sender instanceof Player)) {
            lang.send(sender, lang.get("General.Not A Player"));
            return;
        }

        EntityType newType;
        try {
            newType = EntityType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            getNotEnoughArgsRunnable().run(label, sender, args);
            return;
        }

        Player player = (Player) sender;
        Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);

        if (block == null || block.getType() != Material.SPAWNER) {
            lang.send(sender, lang.get("Change Type.Command.Not A Spawner"));
            return;
        }

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        spawner.setSpawnedType(newType);
        spawner.update();
        lang.send(sender, lang.get("Change Type.Changed").replace("<type>", newType.name()));
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            if (args.length == 2) {
                for (EntityType type : EntityType.values()) {
                    if (type.name().startsWith(args[1])) completions.add(type.name());
                }
            }
        };
    }
}
