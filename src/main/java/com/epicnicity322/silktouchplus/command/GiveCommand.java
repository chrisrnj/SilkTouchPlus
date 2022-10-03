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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class GiveCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "give";
    }

    @Override
    public int getMinArgsAmount() {
        return 2;
    }

    @Override
    protected @NotNull CommandRunnable getNotEnoughArgsRunnable() {
        return (label, sender, args) -> SilkTouchPlus.getLanguage().send(sender, SilkTouchPlus.getLanguage().get("Give.Invalid Arguments").replace("<label>", label));
    }

    @Override
    public @NotNull String getPermission() {
        return "silktouchplus.command.give";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return (label, sender, args) -> SilkTouchPlus.getLanguage().send(sender, SilkTouchPlus.getLanguage().get("General.No Permission"));
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = SilkTouchPlus.getLanguage();
        EntityType type;

        try {
            type = EntityType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            getNotEnoughArgsRunnable().run(label, sender, args);
            return;
        }

        double health = 100.0;

        if (args.length > 2) {
            try {
                health = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[2]));
                return;
            }
            if (health < 0.0) health = 0.0;
        }

        Player who;

        if (args.length > 3) {
            who = Bukkit.getPlayer(args[3]);
            if (who == null) {
                lang.send(sender, lang.get("General.Player Not Found").replace("<value>", args[3]));
                return;
            }
        } else {
            if (sender instanceof Player) {
                who = (Player) sender;
            } else {
                getNotEnoughArgsRunnable().run(label, sender, args);
                return;
            }
        }

        if (who.getInventory().addItem(SilkTouchPlus.newSpawner(type, health / 100.0)).isEmpty()) {
            lang.send(sender, lang.get("Give.Success").replace("<type>", type.name()).replace("<health>", Double.toString(health)).replace("<player>", who.getName()));
        } else {
            lang.send(sender, lang.get("Give.Full").replace("<player>", who.getName()));
        }
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            switch (args.length) {
                case 2:
                    for (EntityType type : EntityType.values()) {
                        if (type.name().startsWith(args[1])) completions.add(type.name());
                    }
                    break;
                case 3:
                    if ("100".startsWith(args[2])) completions.add("100");
                    break;
                case 4:
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().startsWith(args[3])) completions.add(player.getName());
                    }
                    break;
            }
        };
    }
}
