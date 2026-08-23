package com.aetherglitch.tntrun.listeners;

import com.aetherglitch.tntrun.AetherGlitchTNTRun;
import com.aetherglitch.tntrun.game.GameState;
import com.aetherglitch.tntrun.game.Hotbar;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;

public class GameListeners implements Listener {
    private final AetherGlitchTNTRun plugin;
    public GameListeners(AetherGlitchTNTRun plugin){ this.plugin=plugin; }

    @EventHandler
    public void onMove(PlayerMoveEvent e){
        Player p=e.getPlayer();
        if(!plugin.getGameManager().isInGame(p)) return;
        if(plugin.getConfig().getBoolean("game.freeze-until-start",true) && plugin.getGameManager().state != GameState.INGAME){
            if(e.getFrom().distanceSquared(e.getTo())>0.001){
                if(e.getFrom().distanceSquared(e.getTo())>0.01){
                    e.setCancelled(true);
                }
            }
            return;
        }
        if(plugin.getGameManager().state==GameState.INGAME && plugin.getGameManager().isPlaying(p)){
            plugin.getGameManager().handleMoveDecay(p);
            // убивает ТОЛЬКО лава — проверяем все вокруг ног + fallback по высоте если лавы нет
            boolean inLava = p.getLocation().getBlock().getType()==Material.LAVA
                    || p.getLocation().clone().subtract(0,0.2,0).getBlock().getType()==Material.LAVA
                    || p.getLocation().clone().subtract(0,1,0).getBlock().getType()==Material.LAVA
                    || p.getLocation().clone().add(0,0.5,0).getBlock().getType()==Material.LAVA;
            if(inLava){
                plugin.getGameManager().eliminate(p);
                return;
            }
            // fallback: если провалился ниже voidY (лава не поставлена) — тоже в лобби
            if(p.getLocation().getY() < plugin.getArenaManager().getActive().voidY + 1){
                plugin.getGameManager().eliminate(p);
            }
        }
        if(plugin.getGameManager().isSpectator(p) && p.getGameMode()==GameMode.SPECTATOR){
            if(p.getSpectatorTarget()==null && plugin.getArenaManager().getActive().center!=null){
                double dist = p.getLocation().distance(plugin.getArenaManager().getActive().center);
                if(dist > plugin.getArenaManager().getActive().radius + 30){
                    p.teleport(plugin.getArenaManager().getActive().center.clone().add(0,8,0));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player p)) return;
        if(!plugin.getGameManager().isInGame(p)) return;
        // весь урон отменен, но лава всегда элиминирует — даже если стоишь в ней без движения
        if(e.getCause()==EntityDamageEvent.DamageCause.LAVA || e.getCause()==EntityDamageEvent.DamageCause.FIRE || e.getCause()==EntityDamageEvent.DamageCause.FIRE_TICK){
            e.setCancelled(true);
            if(plugin.getGameManager().state==GameState.INGAME && plugin.getGameManager().isPlaying(p)){
                plugin.getGameManager().eliminate(p);
            }
            return;
        }
        if(e.getCause()==EntityDamageEvent.DamageCause.FALL || e.getCause()==EntityDamageEvent.DamageCause.DROWNING){
            e.setCancelled(true);
            return;
        }
        if(plugin.getGameManager().state != GameState.INGAME){
            e.setCancelled(true);
        } else {
            if(!plugin.getConfig().getBoolean("game.allow-pvp",false)) e.setCancelled(true);
        }
    }

    @EventHandler public void onFood(FoodLevelChangeEvent e){ if(e.getEntity() instanceof Player p && plugin.getGameManager().isInGame(p)) e.setCancelled(true); }
    @EventHandler public void onDrop(PlayerDropItemEvent e){ if(plugin.getGameManager().isInGame(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onSwap(PlayerSwapHandItemsEvent e){ if(plugin.getGameManager().isInGame(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPick(PlayerAttemptPickupItemEvent e){ if(plugin.getGameManager().isInGame(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e){ if(plugin.getGameManager().isInGame(e.getPlayer())) e.setCancelled(true); }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        boolean isOurGui = title.contains("Наблюдение") || title.contains("Статистика");
        if(isOurGui){
            e.setCancelled(true);
            if(e.getCurrentItem()==null || e.getCurrentItem().getType()==Material.AIR) return;
            if(title.contains("Наблюдение") && e.getCurrentItem().getType()==Material.PLAYER_HEAD){
                String name = e.getCurrentItem().getItemMeta().getDisplayName().replace("§a","").replace("§f","").trim();
                Player target = plugin.getServer().getPlayer(name);
                if(target!=null && plugin.getGameManager().isPlaying(target)){
                    p.closeInventory();
                    p.setSpectatorTarget(target);
                    p.sendMessage("§7Наблюдаешь за §a"+target.getName());
                }
            }
            return;
        }
        if(!plugin.getGameManager().isInGame(p)) return;
        // в игре — полный блок перемещения предметов (нельзя забирать/перекладывать)
        e.setCancelled(true);
    }

    @EventHandler public void onDrag(InventoryDragEvent e){ 
        if(e.getWhoClicked() instanceof Player p){
            String title = e.getView().getTitle();
            if(title.contains("Наблюдение") || title.contains("Статистика")){ e.setCancelled(true); return; }
            if(plugin.getGameManager().isInGame(p)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        Player p=e.getPlayer();
        if(!plugin.getGameManager().isInGame(p)) return;
        if(e.getAction()!=Action.RIGHT_CLICK_AIR && e.getAction()!=Action.RIGHT_CLICK_BLOCK) return;
        if(e.getItem()==null) return;
        Material t = e.getItem().getType();
        String name = e.getItem().getItemMeta()!=null ? e.getItem().getItemMeta().getDisplayName() : "";
        if(t==Material.RED_BED || name.contains("Выйти")){
            e.setCancelled(true);
            plugin.getGameManager().leave(p,true);
        } else if(t==Material.ENDER_EYE || name.contains("Наблюдение")){
            if(plugin.getGameManager().isSpectator(p)){
                e.setCancelled(true);
                Hotbar.openSpectateMenu(plugin, p);
            }
        } else if(t==Material.LIME_DYE || name.contains("Играть снова")){
            e.setCancelled(true);
            plugin.getGameManager().playAgain(p);
        } else if(t==Material.PLAYER_HEAD){
            e.setCancelled(true);
            org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, "§8Статистика — "+p.getName());
            com.aetherglitch.tntrun.stats.PlayerStats ps = plugin.getStatsManager().get(p.getUniqueId(), p.getName());
            inv.setItem(13, com.aetherglitch.tntrun.util.ItemUtil.playerHead(p, "§a"+p.getName(),
                    java.util.List.of("§7Побед: §a"+ps.wins, "§7Игр: §e"+ps.games, "§7Поражений: §c"+Math.max(0,ps.games-ps.wins), "§7Винрейт: §b"+String.format("%.1f",ps.winrate())+"%")));
            inv.setItem(11, com.aetherglitch.tntrun.util.ItemUtil.create(Material.DIAMOND, "§bПобеды: "+ps.wins, java.util.List.of("§7Всего побед")));
            inv.setItem(15, com.aetherglitch.tntrun.util.ItemUtil.create(Material.BOOK, "§eИгр: "+ps.games, java.util.List.of("§7Винрейт "+String.format("%.1f",ps.winrate())+"%")));
            p.openInventory(inv);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e){ plugin.getGameManager().handleQuit(e.getPlayer()); }
}
