package dev.server.mobkillmoney;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class MobKillMoney extends JavaPlugin implements Listener {

    private Economy economy;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private boolean enabled;
    private boolean playerKillsOnly;
    private boolean payForPlayerKills;
    private String actionbarTemplate;

    private final Map<EntityType, Double> rewards = new EnumMap<>(EntityType.class);
    private final Map<EntityType, String> mobRu = new EnumMap<>(EntityType.class);

    private final DecimalFormat df = new DecimalFormat("#.##");

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault Economy provider not found! (Need Vault + EssentialsX/other economy). Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        reloadAll();
        Bukkit.getPluginManager().registerEvents(this, this);

        // команда /mkm reload
        if (getCommand("mkm") != null) {
            getCommand("mkm").setExecutor(this);
        }

        getLogger().info("MobKillMoney enabled. Economy: " + economy.getName());
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    private void reloadAll() {
        reloadConfig();

        enabled = getConfig().getBoolean("enabled", true);
        playerKillsOnly = getConfig().getBoolean("player-kills-only", true);
        payForPlayerKills = getConfig().getBoolean("pay-for-player-kills", false);
        actionbarTemplate = getConfig().getString(
                "actionbar-message",
                "<green>+{amount} монет за убийство <yellow>{mob_ru}</yellow></green>"
        );

        rewards.clear();
        ConfigurationSection rew = getConfig().getConfigurationSection("rewards");
        if (rew != null) {
            for (String key : rew.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    rewards.put(type, rew.getDouble(key));
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Unknown EntityType in rewards: " + key);
                }
            }
        }

        mobRu.clear();
        // дефолтные русские имена на всякий случай
        Map<EntityType, String> defaults = defaultRuNames();
        mobRu.putAll(defaults);

        ConfigurationSection names = getConfig().getConfigurationSection("mob-names-ru");
        if (names != null) {
            for (String key : names.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    mobRu.put(type, names.getString(key, type.name()));
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Unknown EntityType in mob-names-ru: " + key);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!enabled) return;

        Player killer = event.getEntity().getKiller();
        if (playerKillsOnly && killer == null) return;

        EntityType type = event.getEntityType();
        if (type == EntityType.PLAYER && !payForPlayerKills) return;

        Double amount = rewards.get(type);
        if (amount == null || amount <= 0.0) return;
        if (killer == null) return;

        economy.depositPlayer(killer, amount);

        String ru = mobRu.getOrDefault(type, prettify(type.name()));
        String msg = actionbarTemplate
                .replace("{amount}", df.format(amount))
                .replace("{mob}", type.name())
                .replace("{mob_ru}", ru);

        Component comp = mm.deserialize(msg);
        killer.sendActionBar(comp);
    }

    // /mkm reload
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mkm")) return false;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mkm.reload")) {
                sender.sendMessage("§cНет прав: mkm.reload");
                return true;
            }
            reloadAll();
            sender.sendMessage("§aMobKillMoney: конфиг перезагружен.");
            return true;
        }

        sender.sendMessage("§eИспользование: /mkm reload");
        return true;
    }

    private static String prettify(String enumName) {
        // ENDERMAN -> Enderman (на крайний случай)
        String lower = enumName.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1).replace('_', ' ');
    }

    private static Map<EntityType, String> defaultRuNames() {
        Map<EntityType, String> m = new HashMap<>();
        m.put(EntityType.ZOMBIE, "Зомби");
        m.put(EntityType.SKELETON, "Скелет");
        m.put(EntityType.CREEPER, "Крипер");
        m.put(EntityType.ENDERMAN, "Эндермен");
        m.put(EntityType.SPIDER, "Паук");
        m.put(EntityType.CAVE_SPIDER, "Пещерный паук");
        m.put(EntityType.DROWNED, "Утопленник");
        m.put(EntityType.HUSK, "Кадавр");
        m.put(EntityType.STRAY, "Бродяга");
        m.put(EntityType.SLIME, "Слизень");
        m.put(EntityType.MAGMA_CUBE, "Магмовый куб");
        m.put(EntityType.WITCH, "Ведьма");
        m.put(EntityType.BLAZE, "Ифрит");
        m.put(EntityType.GHAST, "Гаст");
        m.put(EntityType.WITHER, "Иссушитель");
        m.put(EntityType.ENDER_DRAGON, "Эндер-дракон");
        m.put(EntityType.WARDEN, "Страж");
        return m;
    }
}
