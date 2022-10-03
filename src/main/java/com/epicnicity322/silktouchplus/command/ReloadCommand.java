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
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.silktouchplus.SilkTouchPlus;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ReloadCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "reload";
    }

    @Override
    public @NotNull String getPermission() {
        return "silktouchplus.command.reload";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return (label, sender, args) -> SilkTouchPlus.getLanguage().send(sender, SilkTouchPlus.getLanguage().get("General.No Permission"));
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = SilkTouchPlus.getLanguage();
        if (SilkTouchPlus.reload()) {
            lang.send(sender, lang.get("Reload.Success"));
        } else {
            lang.send(sender, lang.get("Reload.Error"));
        }
    }
}
