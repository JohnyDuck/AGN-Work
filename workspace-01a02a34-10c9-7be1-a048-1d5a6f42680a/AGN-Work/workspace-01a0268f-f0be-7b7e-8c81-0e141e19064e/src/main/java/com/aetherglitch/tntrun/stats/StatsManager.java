package com.aetherglitch.tntrun.stats;

import com.aetherglitch.tntrun.AetherGlitchTNTRun;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatsManager {
    private final AetherGlitchTNTRun plugin;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();
    private File file;

    public StatsManager(AetherGlitchTNTRun plugin){
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), plugin.getConfig().getString("stats.file","stats.yml"));
    }

    public void load(){
        if(!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (IOException ignored){}
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for(String key : cfg.getKeys(false)){
            String name = cfg.getString(key+".name", key);
            int wins = cfg.getInt(key+".wins",0);
            int games = cfg.getInt(key+".games",0);
            try{
                UUID uuid = UUID.fromString(key);
                PlayerStats ps = new PlayerStats(name,wins,games);
                cache.put(uuid, ps);
                nameToUuid.put(name.toLowerCase(), uuid);
            }catch(IllegalArgumentException e){
                // legacy name key
            }
        }
    }

    public void save(){
        FileConfiguration cfg = new YamlConfiguration();
        for(Map.Entry<UUID, PlayerStats> e: cache.entrySet()){
            String k = e.getKey().toString();
            PlayerStats ps = e.getValue();
            cfg.set(k+".name", ps.name);
            cfg.set(k+".wins", ps.wins);
            cfg.set(k+".games", ps.games);
        }
        try{ cfg.save(file);} catch(IOException ex){ plugin.getLogger().warning("Could not save stats: "+ex.getMessage());}
    }

    public PlayerStats get(UUID uuid, String name){
        return cache.computeIfAbsent(uuid, k-> {
            PlayerStats ps = new PlayerStats(name);
            nameToUuid.put(name.toLowerCase(), uuid);
            return ps;
        });
    }

    public PlayerStats getByName(String name){
        UUID u = nameToUuid.get(name.toLowerCase());
        if(u!=null) return cache.get(u);
        return null;
    }

    public void addGame(UUID uuid, String name){
        PlayerStats ps = get(uuid, name);
        ps.name = name;
        ps.games++;
        nameToUuid.put(name.toLowerCase(), uuid);
    }

    public void addWin(UUID uuid, String name){
        PlayerStats ps = get(uuid, name);
        ps.name = name;
        ps.wins++;
        // games already counted
        nameToUuid.put(name.toLowerCase(), uuid);
    }

    public List<PlayerStats> top(int limit){
        return cache.values().stream()
                .sorted(Comparator.comparingInt((PlayerStats p)->p.wins).reversed().thenComparingInt(p->p.games))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void reloadFileRef(){
        this.file = new File(plugin.getDataFolder(), plugin.getConfig().getString("stats.file","stats.yml"));
    }
}
