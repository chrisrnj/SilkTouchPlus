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

package com.epicnicity322.silktouchplus;

import com.epicnicity322.epicpluginlib.bukkit.command.CommandManager;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.logger.Logger;
import com.epicnicity322.epicpluginlib.core.EpicPluginLib;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationHolder;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationLoader;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.tools.Version;
import com.epicnicity322.epicpluginlib.core.util.ObjectUtils;
import com.epicnicity322.silktouchplus.command.ChangeTypeCommand;
import com.epicnicity322.silktouchplus.command.GiveCommand;
import com.epicnicity322.silktouchplus.command.ReloadCommand;
import com.epicnicity322.silktouchplus.hook.DecentHologramsHook;
import com.epicnicity322.silktouchplus.hook.HolographicDisplaysHook;
import com.epicnicity322.silktouchplus.listener.*;
import com.epicnicity322.silktouchplus.util.HologramHandler;
import com.epicnicity322.silktouchplus.util.SilkTouchPlusUtil;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public final class SilkTouchPlus extends JavaPlugin {
    private static final @NotNull Path folder = Paths.get("plugins", "SilkTouchPlus");
    private static final @NotNull Logger logger = new Logger("&3[&2SilkTouchPlus&3] ");
    private static final @NotNull MessageSender lang = new MessageSender(() -> Configurations.config.getConfiguration().getString("Language").orElse("EN_US"),
            Configurations.langEN_US.getDefaultConfiguration());
    private static @Nullable SilkTouchPlus instance;
    private static @Nullable HologramHandler hologramHandler;

    static {
        lang.addLanguage("EN_US", Configurations.langEN_US);
        lang.addLanguage("PT_BR", Configurations.langPT_BR);
    }

    public final @NotNull NamespacedKey spawnerType = new NamespacedKey(this, "spawner_type");
    public final @NotNull NamespacedKey spawnerHealth = new NamespacedKey(this, "spawner_health");
    public final @NotNull NamespacedKey repairLootEntity = new NamespacedKey(this, "repair_loot_entity");
    public final @NotNull NamespacedKey repairLoot = new NamespacedKey(this, "repair_loot");
    public final @NotNull NamespacedKey spawnerSpecialRepairItem = new NamespacedKey(this, "spawner_special_repair_item");
    public final @NotNull NamespacedKey hologramEnabled = new NamespacedKey(this, "hologram_enabled");
    private final @NotNull SpawnerBlockListener spawnerBreak = new SpawnerBlockListener(this);
    private final @NotNull SilkTouchListener spawnerInventory = new SilkTouchListener(spawnerBreak);
    private final @NotNull SpawnerSpawnListener spawnerSpawn = new SpawnerSpawnListener(this);
    private final @NotNull SpawnerClickListener spawnerClick = new SpawnerClickListener(this);
    private final @NotNull SpawnerEntityDeathListener spawnerEntityDeath = new SpawnerEntityDeathListener(this, spawnerClick);
    private @Nullable BukkitTask renderTask;

    public SilkTouchPlus() {
        instance = this;
        logger.setLogger(getLogger());
    }

    public static @NotNull MessageSender getLanguage() {
        return lang;
    }

    public static @NotNull ItemStack getSpawner(@NotNull CreatureSpawner spawner) {
        if (instance == null)
            throw new UnsupportedOperationException("Cannot create a spawner with SilkTouchPlus unloaded.");
        return newSpawner(spawner.getSpawnedType(), spawner.getPersistentDataContainer().getOrDefault(instance.spawnerHealth, PersistentDataType.DOUBLE, 1.0));
    }

    public static @NotNull ItemStack newSpawner(@NotNull EntityType type, double health) {
        if (instance == null)
            throw new UnsupportedOperationException("Cannot create a spawner with SilkTouchPlus unloaded.");
        Configuration config = Configurations.config.getConfiguration();
        ItemStack item = new ItemStack(ObjectUtils.getOrDefault(Material.getMaterial(config.getString("Drop.Spawner Item.Material").orElse("SPAWNER")), Material.SPAWNER));
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            item = new ItemStack(Material.SPAWNER);
            meta = Objects.requireNonNull(item.getItemMeta());
        }

        meta.getPersistentDataContainer().set(instance.spawnerType, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(instance.spawnerHealth, PersistentDataType.DOUBLE, health);
        if (config.getBoolean("Drop.Spawner Item.Glowing").orElse(false)) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
        }
        meta.setDisplayName(lang.getColored("Drop.Spawner Item.Display Name").replace("<type>", type.name()));
        meta.setLore(Arrays.asList(SilkTouchPlusUtil.separateLines(lang.getColored("Drop.Spawner Item.Lore")
                .replace("<type>", type.name()).replace("<health>", Double.toString(health))
                .replace("<health_percentage>", SilkTouchPlusUtil.formatHealth(health)))));
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public static @Nullable HologramHandler getHologramHandler() {
        return hologramHandler;
    }

    /**
     * Reloads configurations and listeners of SilkTouchPlus.
     *
     * @return Whether the plugin reloaded successfully.
     */
    public static boolean reload() {
        if (instance == null) return false;

        HashMap<ConfigurationHolder, Exception> exceptions = Configurations.loader.loadConfigurations();

        exceptions.forEach((holder, exception) -> {
            logger.log("Failed to load '" + holder.getPath().getFileName() + "' configuration:", ConsoleLogger.Level.ERROR);
            exception.printStackTrace();
        });
        if (!exceptions.isEmpty()) {
            logger.log("Please fix any YAML Syntax errors then type '/stp reload' to reload configurations.", ConsoleLogger.Level.ERROR);
        }

        Configuration config = Configurations.config.getConfiguration();

        SilkTouchPlusUtil.setSeparatorInHealthFormat(config.getString("Health.Decimal Separator").orElse(".").charAt(0));
        if (hologramHandler != null) {
            hologramHandler.clear();
            hologramHandler.setEnabled(config.getBoolean("Holograms").orElse(true));
        }
        instance.loadListeners();
        if (instance.renderTask != null) {
            instance.renderTask.cancel();
        }
        if (config.getBoolean("Health.Show Damage Animation").orElse(false)) {
            instance.renderTask = instance.getServer().getScheduler().runTaskTimerAsynchronously(instance, SpawnerSpawnListener::renderHealth, 0, 300);
        }
        return exceptions.isEmpty();
    }

    private void loadListeners() {
        HandlerList.unregisterAll(this);
        PluginManager manager = getServer().getPluginManager();
        Configuration config = Configurations.config.getConfiguration();

        spawnerBreak.setBreakTools(config.getCollection("Drop.Break Tools", Object::toString));
        spawnerBreak.setSilkTouchLevel(config.getNumber("Drop.Silk Touch Level").orElse(2).intValue());
        manager.registerEvents(spawnerBreak, this);

        spawnerInventory.setAllowSilkTouchBookCombining(config.getBoolean("Silk Touch Two.Allow Silk Touch Book Combining").orElse(true));
        spawnerInventory.setPreventCustomSilkTouchRepair(config.getBoolean("Silk Touch Two.Prevent Custom Silk Touch Repair").orElse(true));
        if (spawnerInventory.isAllowingSilkTouchBookCombining() || spawnerInventory.isPreventingCustomSilkTouchRepair()) {
            manager.registerEvents(spawnerInventory, this);
        }

        spawnerSpawn.setSpawnWhitelist(config.getCollection("Spawn Whitelist", obj -> {
            try {
                return EntityType.valueOf(obj.toString());
            } catch (IllegalArgumentException e) {
                logger.log("Unknown entity type '" + obj + "' from Spawn Whitelist setting.");
                return null;
            }
        }));
        spawnerSpawn.setSpawnDamage(config.getNumber("Health.Spawn Damage").orElse(0.0005).doubleValue());
        manager.registerEvents(spawnerSpawn, this);

        spawnerClick.setLootRepairAmount(config.getNumber("Health.Loot Repair Amount").orElse(0.0010).doubleValue());
        spawnerClick.setSpecialRepairAmount(config.getNumber("Health.Special Repair Item.Repair Amount").orElse(2.0).doubleValue());
        spawnerClick.setMaxRepairHealth(config.getNumber("Health.Max Repair Health").orElse(1.0).doubleValue());
        manager.registerEvents(spawnerClick, this);

        spawnerEntityDeath.setSpecialRepairItem(ObjectUtils.getOrDefault(Material.getMaterial(config.getString("Health.Special Repair Item.Material").orElse("ENDER_EYE")), Material.ENDER_EYE),
                config.getBoolean("Health.Special Repair Item.Glowing").orElse(true));
        spawnerEntityDeath.setDropChance(config.getNumber("Health.Special Repair Item.Drop Chance").orElse(0.01).doubleValue());
        spawnerEntityDeath.setOnlySpawnerLootCanRepair(config.getBoolean("Health.Only Spawner Loot Can Repair").orElse(true));
        manager.registerEvents(spawnerEntityDeath, this);
    }

    @Override
    public void onEnable() {
        Version platform = EpicPluginLib.Platform.getVersion();

        if (platform.compareTo(new Version("1.14")) < 0) {
            logger.log("I'm sorry, but version " + platform + " is not supported.", ConsoleLogger.Level.ERROR);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand mainCommand = getCommand("silktouchplus");

        if (mainCommand != null) {
            CommandManager.registerCommand(mainCommand, Arrays.asList(new ChangeTypeCommand(), new GiveCommand(), new ReloadCommand()),
                    (label, sender, args) -> {
                        lang.send(sender, lang.get("Help.Header").replace("<version>", getDescription().getVersion()));
                        if (sender.hasPermission("silktouchplus.command.changetype")) {
                            lang.send(sender, lang.get("Help.ChangeType").replace("<label>", label));
                        }
                        if (sender.hasPermission("silktouchplus.command.give")) {
                            lang.send(sender, lang.get("Help.Give").replace("<label>", label));
                        }
                        if (sender.hasPermission("silktouchplus.command.reload")) {
                            lang.send(sender, lang.get("Help.Reload").replace("<label>", label));
                        }
                    },
                    (label, sender, args) -> lang.send(sender, lang.get("General.Unknown Command").replace("<label>", label)));
        }

        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            try {
                hologramHandler = new DecentHologramsHook(this);
                logger.log("DecentHolograms was found and hooked.");
            } catch (Throwable t) {
                logger.log("Could not hook to DecentHolograms:", ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        } else if (getServer().getPluginManager().getPlugin("HolographicDisplays") != null) {
            try {
                hologramHandler = new HolographicDisplaysHook(this);
                logger.log("HolographicDisplays was found and hooked.");
            } catch (Throwable t) {
                logger.log("Could not hook to HolographicDisplays:", ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        }

        reload();
    }

    private static final class Configurations {
        private static final @NotNull ConfigurationLoader loader = new ConfigurationLoader();
        private static final @NotNull ConfigurationHolder config = new ConfigurationHolder(folder.resolve("config.yml"), "" +
                "Language: EN_US\n" +
                "\n" +
                "# Only players with permission silktouchplus.drop.* are allowed.\n" +
                "Drop:\n" +
                "  # List of tools required to break a spawner and have it drop its item.\n" +
                "  Break Tools: [ DIAMOND_PICKAXE, NETHERITE_PICKAXE ]\n" +
                "  # The level of silk touch the tool needs to have to drop the spawner.\n" +
                "  # Set to 0 for no silk touch required, or 2 if you have Silk Touch Two enabled.\n" +
                "  Silk Touch Level: 2\n" +
                "  # Properties of spawner item. You can find name and lore in Language file.\n" +
                "  Spawner Item:\n" +
                "    Material: 'SPAWNER' # Can have different material types.\n" +
                "    Glowing: true\n" +
                "\n" +
                "Silk Touch Two:\n" +
                "  # Prevents items that have silk touch greater than 1 from being repaired.\n" +
                "  Prevent Custom Silk Touch Repair: true\n" +
                "  # Allows Books of Silk Touch being combined into level 2.\n" +
                "  # Only players with permission silktouchplus.combine are allowed.\n" +
                "  Allow Silk Touch Book Combining: true\n" +
                "\n" +
                "# Spawners will show holograms when placed.\n" +
                "# Only player with permission silktouchplus.hologram can crouch and right-click to toggle.\n" +
                "# Compatible with: HolographicDisplays and DecentHolograms.\n" +
                "Holograms: true\n" +
                "\n" +
                "# A new spawner has 1.0 health max. It gets damage every time a mob is spawned, and can be fed later to\n" +
                "#recover its health back to 1.0.\n" +
                "# A damaged spawner does not spawn any entities and can get destroyed.\n" +
                "Health:\n" +
                "  # The damage the spawner takes every time a mob spawns from it.\n" +
                "  Spawn Damage: 0.0005 # About 5000 mob spawns before it's fully depleted.\n" +
                "  # Makes the block look like it's broken according to the spawner's health.\n" +
                "  # This option can be laggy depending on the amount of spawners and players you have in your server!\n" +
                "  Show Damage Animation: true\n" +
                "  # The separator character for decimals in <health_percentage> tag.\n" +
                "  Decimal Separator: '.'\n" +
                "  Special Repair Item:\n" +
                "    Material: ENDER_EYE\n" +
                "    Glowing: true\n" +
                "    # Any monster and animal will drop this item with the given chance.\n" +
                "    # Set to 0 to disable.\n" +
                "    # Only players with permission silktouchplus.special can get it." +
                "    Drop Chance: 0.01 #Percent\n" +
                "    # The amount of health to add to the spawner when the player clicks on it with this item.\n" +
                "    Repair Amount: 2.0\n" +
                "  # The amount of health to add to the spawner when the player clicks on it using mob loot.\n" +
                "  Loot Repair Amount: 0.0010\n" +
                "  # How much should be the limit for repairing. If the item is a Special Repair Item and it repairs more\n" +
                "  #than this limit, the health is set to the Special Repair amount, instead of being summed.\n" +
                "  Max Repair Health: 1.0\n" +
                "  # If true, only the loot of entities spawned by mob spawners can be used to repair.\n" +
                "  # If false, loot of all entities killed by a player can be used as repair for spawners.\n" +
                "  Only Spawner Loot Can Repair: true\n" +
                "\n" +
                "# A whitelist of mobs allowed to spawn from spawners.\n" +
                "# Use it to prevent spawners from spawning unwanted mob types.\n" +
                "Spawn Whitelist:\n" +
                "#- 'BLAZE'\n" +
                "#- 'CAVE_SPIDER'\n" +
                "#- 'MAGMA_CUBE'\n" +
                "#- 'SILVERFISH'\n" +
                "#- 'SKELETON'\n" +
                "#- 'SPIDER'\n" +
                "#- 'ZOMBIE'");
        private static final @NotNull ConfigurationHolder langEN_US = new ConfigurationHolder(folder.resolve("Language").resolve("Language EN-US.yml"), "" +
                "General:\n" +
                "  No Permission: '&4You don''t have permission to do this.'\n" +
                "  #Variables: <value>\n" +
                "  Not A Number: '&cThe value \"&7<value>&c\" is not a number!'\n" +
                "  Not A Player: '&cYou must be a player to use this command!'\n" +
                "  #Variables: <value>\n" +
                "  Player Not Found: '&cThe player \"&7<value>&c\" was not found!'\n" +
                "  Prefix: '&3[&2SilkTouchPlus&3] '\n" +
                "  Unknown Command: '&cUnknown command! Type &7/<label>&c to see the list of commands.'\n" +
                "\n" +
                "#Variables: <type>\n" +
                "Change Type:\n" +
                "  Changed: '&aThis is a &f<type>&a spawner now!'\n" +
                "  No Permission: '<cooldown=5000> &4You don''t have permission to change a mob spawner type to &f<type>&4!'\n" +
                "  Command:\n" +
                "    Invalid Arguments: '&cInvalid arguments! You must use &7&n/<label> changetype <type>&c!'\n" +
                "    Not A Spawner: '&cYou must be looking at a spawner block to use this command.'\n" +
                "\n" +
                "#Variables: <type>\n" +
                "Drop:\n" +
                "  Dropped: '&fYou got a &a<type>&f spawner!'\n" +
                "  Spawner Item:\n" +
                "    Display Name: '&c<type> &4spawner'\n" +
                "    #Variables: <line> <health> <health_percentage>\n" +
                "    Lore: >-\n" +
                "      A creature spawner.\n" +
                "      <line>Mob: &c<type>\n" +
                "      <line>Health: &c<health_percentage>\n" +
                "\n" +
                "Help:\n" +
                "  #Variables: <version>\n" +
                "  Header: \"List of commands:\\n&3- Made by:&7 Epicnicity322\\n&3- Version: &7<version>\\n\"\n" +
                "  ChangeType: \"<noprefix> &8&n/<label> changetype <type>&8:\\n&7 Changes the type of the spawner you're looking at.\"\n" +
                "  Give: \"<noprefix> &8&n/<label> give <type> [health] [player]&8:\\n&7 Gives a spawner to a player.\"\n" +
                "  Reload: \"<noprefix> &8&n/<label> reload&8:\\n&7 Reloads the plugin.\"\n" +
                "\n" +
                "#Variables: <type>\n" +
                "Placed: '&fYou placed a &c<type>&f spawner on the ground!'\n" +
                "\n" +
                "Reload:\n" +
                "  Success: '&aPlugin reloaded successfully!'\n" +
                "  Error: '&cSome errors occurred while reloading the plugin, check console for more info.'\n" +
                "\n" +
                "#Variables: <type> <health> <health_percentage>\n" +
                "Repair:\n" +
                "  Fully Repaired: '<cooldown=5000> &f<type>&7 spawner is already fully repaired.'\n" +
                "  Repaired: '&f<type>&7 was repaired to: <health_percentage>'\n" +
                "  Unknown Loot: '<cooldown=5000> &cYou can not use this loot on a <type> spawner.'\n" +
                "  Special Repair Item:\n" +
                "    Drop: '&2You just got a &aSpecial Repair Item&2! Use it on a spawner to repair it by &2&l<health_percentage>&a!'\n" +
                "    Display Name: '&4&l&nSpecial Repair Item'\n" +
                "    #Variables: <line>\n" +
                "    Lore: 'Use me on any spawner to repair<line>it by &c&l<health_percentage>&5&o!'\n" +
                "  #Variables: <line>\n" +
                "  Loot Lore: 'Use me on a <type> spawner<line>to repair it by <health_percentage>.'\n" +
                "\n" +
                "# Only works if HolographicDisplays is enabled.\n" +
                "#Variables: <type> <health> <health_percentage> <line>\n" +
                "Spawner Hologram: >-\n" +
                "  &c<type>&4 spawner\n" +
                "  <line>&7Health: <health_percentage>\n" +
                "Spawner Hologram Toggle:\n" +
                "  Enabled: '&7Spawner hologram &aenabled&7!'\n" +
                "  Disabled: '&7Spawner hologram &cdisabled&7!'\n" +
                "\n" +
                "Give:\n" +
                "  Invalid Arguments: '&cInvalid arguments! You must use &7&n/<label> get <type> [health] [player]&c!'\n" +
                "  #Variables: <type> <health> <player>\n" +
                "  Success: '&7Gave a &f<type>&7 spawner with &f<health>%&7 health to &f<player>&7.'\n" +
                "  #Variables: <player>\n" +
                "  Full: '&cInventory of &7<player>&c is full!'\n");
        private static final @NotNull ConfigurationHolder langPT_BR = new ConfigurationHolder(folder.resolve("Language").resolve("Language PT-BR.yml"), "" +
                "General:\n" +
                "  No Permission: '&4Você não tem permissão para fazer isso.'\n" +
                "  #Variáveis: <value>\n" +
                "  Not A Number: '&cO valor \"&7<value>&c\" não é um número!'\n" +
                "  Not A Player: '&cVocê precisa ser um jogador para usar esse comando!'\n" +
                "  #Variáveis: <value>\n" +
                "  Player Not Found: '&cO jogador \"&7<value>&c\" não foi encontrado!'\n" +
                "  Prefix: '&3[&2SilkTouchPlus&3] '\n" +
                "  Unknown Command: '&cComando desconhecido! Use &7/<label>&c para ver a lista de comandos.'\n" +
                "\n" +
                "#Variáveis: <type>\n" +
                "Change Type:\n" +
                "  Changed: '&aIsso é um spawner de &f<type>&a agora!'\n" +
                "  No Permission: '<cooldown=5000> &4Você não tem permissão para mudar o tipo do mob spawner para &f<type>&4!'\n" +
                "  Command:\n" +
                "    Invalid Arguments: '&cArgumentos inválidos! Você precisa usar &7&n/<label> changetype <tipo>&c!'\n" +
                "    Not A Spawner: '&cVocê deve estar olhando a um bloco de spawner para usar esse comando.'\n" +
                "\n" +
                "#Variáveis: <type>\n" +
                "Drop:\n" +
                "  Dropped: '&fVocê conseguiu um spawner de &a<type>&f!'\n" +
                "  Spawner Item:\n" +
                "    Display Name: '&4Spawner de &c<type>'\n" +
                "    #Variáveis: <line> <health> <health_percentage>\n" +
                "    Lore: >-\n" +
                "      Um criador de mobs.\n" +
                "      <line>Mob: &c<type>\n" +
                "      <line>Vida: &c<health_percentage>\n" +
                "\n" +
                "Help:\n" +
                "  #Variáveis: <version>\n" +
                "  Header: \"Lista de comandos:\\n&3- Feito por:&7 Epicnicity322\\n&3- Versão: &7<version>\\n\"\n" +
                "  ChangeType: \"<noprefix> &8&n/<label> changetype <tipo>&8:\\n&7 Muda o tipo do spawner que você está olhando.\"\n" +
                "  Give: \"<noprefix> &8&n/<label> give <tipo> [vida] [jogador]&8:\\n&7 Dá um spawner a um jogador.\"\n" +
                "  Reload: \"<noprefix> &8&n/<label> reload&8:\\n&7 Recarrega o plugin.\"\n" +
                "\n" +
                "#Variáveis: <type>\n" +
                "Placed: '&fVocê colocou um spawner de &c<type>&f no chão!'\n" +
                "\n" +
                "Reload:\n" +
                "  Success: '&aPlugin recarregado com sucesso!'\n" +
                "  Error: '&cAlguns erros ocorreram ao recarregar o plugin, verifique o console para ver informações.'\n" +
                "\n" +
                "#Variáveis: <type> <health> <health_percentage>\n" +
                "Repair:\n" +
                "  Fully Repaired: '<cooldown=5000> &7Spawner de &f<type>&7 já está reparado.'\n" +
                "  Repaired: '&7Spawner &f<type>&7 foi reparado para: <health_percentage>'\n" +
                "  Unknown Loot: '<cooldown=5000> &cVocê não pode usar esse loot em um spawner de <type>.'\n" +
                "  Special Repair Item:\n" +
                "    Drop: '&2Você ganhou um &aItem Especial de Reparo&2! Use em um spawner para repará-lo para &2&l<health_percentage>&a!'\n" +
                "    Display Name: '&4&l&nItem Especial de Reparo'\n" +
                "    #Variáveis: <line>\n" +
                "    Lore: 'Use-me em um spawner para<line>repará-lo para &c&l<health_percentage>&5&o!'\n" +
                "  #Variáveis: <line>\n" +
                "  Loot Lore: 'Use-me em um spawner de <type><line>para repará-lo em <health_percentage>.'\n" +
                "\n" +
                "# Só funciona se HolographicDisplays estiver ativado.\n" +
                "#Variáveis: <type> <health> <health_percentage> <line>\n" +
                "Spawner Hologram: >-\n" +
                "  &4Spawner de &c<type>\n" +
                "  <line>&7Vida: <health_percentage>\n" +
                "Spawner Hologram Toggle:\n" +
                "  Enabled: '&7Holograma do spawner &aativado&7!'\n" +
                "  Disabled: '&7Holograma do spawner &cdesativado&7!'\n" +
                "\n" +
                "Give:\n" +
                "  Invalid Arguments: '&cArgumentos inválidos! Você precisa usar &7&n/<label> get <tipo> [vida] [jogador]&c!'\n" +
                "  #Variáveis: <type> <health> <player>\n" +
                "  Success: '&7Dado um spawner de &f<type>&7 com &f<health>%&7 de vida para &f<player>&7.'\n" +
                "  #Variáveis: <player>\n" +
                "  Full: '&cInventário de &7<player>&c está cheio!'\n");

        static {
            loader.registerConfiguration(config);
            loader.registerConfiguration(langEN_US);
            loader.registerConfiguration(langPT_BR);
        }
    }
}
