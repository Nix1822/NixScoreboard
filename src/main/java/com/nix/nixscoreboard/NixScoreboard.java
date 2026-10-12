package com.nix.nixscoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NixScoreboard extends JavaPlugin implements Listener {

    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::updateAll, 20L, 20L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        update(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    private void update(Player player) {
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(),
                id -> Bukkit.getScoreboardManager().getNewScoreboard());

        Component title = AMP.deserialize(getConfig().getString("title", "NixScoreboard"));
        Objective objective = board.getObjective("nixsb");
        if (objective == null) {
            objective = board.registerNewObjective("nixsb", Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.displayName(title);
        }

        for (String entry : new ArrayList<>(board.getEntries())) {
            board.resetScores(entry);
        }

        List<String> lines = getConfig().getStringList("lines");
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i)
                    .replace("%player%", player.getName())
                    .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("%max%", String.valueOf(Bukkit.getMaxPlayers()))
                    .replace("%ping%", String.valueOf(player.getPing()));

            // Each entry must be unique, so add invisible reset codes
            String entry = SECTION.serialize(AMP.deserialize(text)) + "\u00a7r".repeat(i);
            objective.getScore(entry).setScore(lines.size() - i);
        }

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            updateAll();
            sender.sendMessage("NixScoreboard reloaded.");
            return true;
        }
        return false;
    }
}
