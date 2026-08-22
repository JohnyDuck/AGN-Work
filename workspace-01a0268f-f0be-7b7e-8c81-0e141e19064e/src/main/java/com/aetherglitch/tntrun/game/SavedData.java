package com.aetherglitch.tntrun.game;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SavedData {
    public Location loc;
    public ItemStack[] inv;
    public ItemStack[] armor;
    public ItemStack offhand;
    public GameMode gm;
    public double health;
    public int food;
    public float exp;
    public int level;
    public int fire;

    public static SavedData capture(Player p){
        SavedData d = new SavedData();
        d.loc = p.getLocation().clone();
        d.inv = p.getInventory().getContents().clone();
        d.armor = p.getInventory().getArmorContents().clone();
        d.offhand = p.getInventory().getItemInOffHand().clone();
        d.gm = p.getGameMode();
        d.health = p.getHealth();
        d.food = p.getFoodLevel();
        d.exp = p.getExp();
        d.level = p.getLevel();
        d.fire = p.getFireTicks();
        return d;
    }
    public void restore(Player p){
        p.getInventory().clear();
        p.getInventory().setContents(inv);
        p.getInventory().setArmorContents(armor);
        p.getInventory().setItemInOffHand(offhand);
        p.updateInventory();
        p.setGameMode(gm);
        try{ p.setHealth(Math.min(health, p.getMaxHealth())); }catch(Exception ignored){ p.setHealth(p.getMaxHealth()); }
        p.setFoodLevel(food);
        p.setExp(exp);
        p.setLevel(level);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setAllowFlight(gm==GameMode.CREATIVE || gm==GameMode.SPECTATOR);
        p.setSpectatorTarget(null);
        p.closeInventory();
    }
}
