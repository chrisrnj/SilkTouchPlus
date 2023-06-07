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

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.regex.Pattern;

public final class SilkTouchPlusUtil {
    private static final @NotNull DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    private static final @NotNull DecimalFormat format = new DecimalFormat("#.##", symbols);
    private static final @NotNull Pattern lineSpliterator = Pattern.compile("<line>");

    private SilkTouchPlusUtil() {
    }

    public static void setSeparatorInHealthFormat(char separator) {
        symbols.setDecimalSeparator(separator);
        format.setDecimalFormatSymbols(symbols);
    }

    public static @NotNull String formatHealth(double health) {
        return format.format(health * 100) + "%";
    }

    public static @NotNull String[] separateLines(@NotNull String string) {
        if (string.isEmpty()) return new String[0];
        return lineSpliterator.split(string);
    }
}
