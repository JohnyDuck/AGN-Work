package com.aetherglitch.tntrun.game;

import com.aetherglitch.tntrun.AetherGlitchTNTRun;
import com.aetherglitch.tntrun.stats.PlayerStats;
import com.aetherglitch.tntrun.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Hotbar {

    public static void giveLobby(AetherGlitchTNTRun plugin, Player p){
        p.getInventory().clear();
        // slot 0 - выйти
        p.getInventory().setItem(0, ItemUtil.create(Material.RED_BED, "§cВыйти §7[ПКМ]", List.of("§7Нажми чтобы выйти","§7и вернуться назад")));
        // slot 4 - голова со статой
        PlayerStats ps = plugin.getStatsManager().get(p.getUniqueId(), p.getName());
        p.getInventory().setItem(4, ItemUtil.playerHead(p, "§aТвоя статистика",
                List.of("§7Побед: §a"+ps.wins, "§7Игр: §e"+ps.games, "§7Поражений: §c"+Math.max(0,ps.games-ps.wins), "§7Винрейт: §b"+String.format("%.1f",ps.winrate())+"%")));
        p.updateInventory();
    }

    public static void givePlaying(AetherGlitchTNTRun plugin, Player p){
        // во время игры тоже даем минимум, чтобы можно было ливнуть, но не мешает паркуру
        p.getInventory().clear();
        p.getInventory().setItem(8, ItemUtil.create(Material.RED_BED, "§cВыйти §7[ПКМ]", List.of("§7Выйти из TNT-Run")));
        // голова в 4 — стата
        PlayerStats ps = plugin.getStatsManager().get(p.getUniqueId(), p.getName());
        p.getInventory().setItem(4, ItemUtil.playerHead(p, "§aСтатистика",
                List.of("§7Побед: §a"+ps.wins, "§7Игр: §e"+ps.games, "§7Винрейт: §b"+String.format("%.1f",ps.winrate())+"%")));
        p.updateInventory();
    }

    public static void giveSpectator(AetherGlitchTNTRun plugin, Player p){
        p.getInventory().clear();
        p.getInventory().setItem(0, ItemUtil.create(Material.RED_BED, "§cВыйти §7[ПКМ]", List.of("§7Выйти и вернуться назад")));
        p.getInventory().setItem(4, ItemUtil.create(Material.ENDER_EYE, "§aНаблюдение §7[ПКМ]", List.of("§7Телепорт к игрокам","§7Выбери голову игрока")));
        PlayerStats ps = plugin.getStatsManager().get(p.getUniqueId(), p.getName());
        p.getInventory().setItem(7, ItemUtil.playerHead(p, "§eТвоя статистика",
                List.of("§7Побед: §a"+ps.wins, "§7Игр: §e"+ps.games, "§7Поражений: §c"+Math.max(0,ps.games-ps.wins), "§7Винрейт: §b"+String.format("%.1f",ps.winrate())+"%")));
        p.getInventory().setItem(8, ItemUtil.create(Material.LIME_DYE, "§aИграть снова §7[ПКМ]", List.of("§7В лобби ожидания")));
        p.updateInventory();
    }

    public static void openSpectateMenu(AetherGlitchTNTRun plugin, Player p){
        List<Player> alive = plugin.getGameManager().players.stream().map(Bukkit::getPlayer).filter(pl->pl!=null).toList();
        Inventory inv = Bukkit.createInventory(null, 54, "§8Наблюдение — живые");
        int i=0;
        for(Player t: alive){
            if(i>=54) break;
            PlayerStats ps = plugin.getStatsManager().get(t.getUniqueId(), t.getName());
            ItemStack head = ItemUtil.playerHead(t, "§a"+t.getName(), List.of("§7Побед: §a"+ps.wins, "§7Игр: §e"+ps.games, "§7Нажми для телепорта"));
            inv.setItem(i++, head);
        }
        if(i==0){
            inv.setItem(22, ItemUtil.create(Material.BARRIER, "§cНикого нет", List.of("§7Все выбыли")));
        }
        p.openInventory(inv);
    }

    public static boolean isOurItem(ItemStack it){
        if(it==null || it.getType()==Material.AIR) return false;
        // все наши хотбар предметы — считаем нашими если имя содержит ключевые слова
        if(it.getItemMeta()==null || !it.getItemMeta().hasDisplayName()) return false;
        String n = it.getItemMeta().getDisplayName();
        return n.contains("Выйти") || n.contains("Наблюдение") || n.contains("Играть снова") || n.contains("статистика") || n.contains("Статистика");
    }
}
