package dev.server.mobkillmoney;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class MobKillMoney extends JavaPlugin implements Listener {

    private Economy economy;
    private final Map<EntityType, Double> rewards = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadRewards();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    private void loadRewards() {
        ConfigurationSection section = getConfig().getConfigurationSection("rewards");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                EntityType type = EntityType.valueOf(key);
                rewards.put(type, section.getDouble(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!getConfig().getBoolean("enabled")) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null && getConfig().getBoolean("player-kills-only")) return;

        EntityType type = event.getEntityType();

        if (type == EntityType.PLAYER &&
                !getConfig().getBoolean("pay-for-player-kills")) return;

        if (!rewards.containsKey(type)) return;

        double amount = rewards.get(type);
        economy.depositPlayer(killer, amount);

        String msg = getConfig().getString("message")
                .replace("{amount}", String.valueOf(amount))
                .replace("{mob}", type.name());

        killer.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
}
