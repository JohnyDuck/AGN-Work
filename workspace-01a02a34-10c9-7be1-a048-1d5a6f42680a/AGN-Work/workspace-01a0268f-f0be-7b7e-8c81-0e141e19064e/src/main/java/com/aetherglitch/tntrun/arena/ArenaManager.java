package com.aetherglitch.tntrun.arena;

import com.aetherglitch.tntrun.AetherGlitchTNTRun;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {
    private final AetherGlitchTNTRun plugin;
    private File file;
    private FileConfiguration cfg;
    private Arena active;
    private String activeName;

    public ArenaManager(AetherGlitchTNTRun plugin){
        this.plugin=plugin;
    }

    public void load(){
        file = new File(plugin.getDataFolder(), "arenas.yml");
        if(!file.exists()){
            plugin.saveResource("arenas.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        activeName = cfg.getString("active-arena","default");
        active = loadArena(activeName);
        if(active==null){
            // fallback create default
            active = new Arena();
            active.worldName = "world";
            World w = plugin.getServer().getWorlds().get(0);
            active.center = new Location(w,0.5,80,0.5);
            active.lobby = new Location(w,0.5,90,-25.5);
            active.radius=20; active.floorY=80; active.voidY=55;
        }
    }

    public void save(){
        if(cfg==null) return;
        // write active arena back
        if(active!=null && activeName!=null){
            String base = "arenas."+activeName;
            cfg.set(base+".world", active.worldName);
            cfg.set(base+".radius", active.radius);
            cfg.set(base+".floor-y", active.floorY);
            cfg.set(base+".void-y", active.voidY);
            if(active.center!=null){
                cfg.set(base+".center.x", active.center.getX());
                cfg.set(base+".center.y", active.center.getY());
                cfg.set(base+".center.z", active.center.getZ());
                cfg.set(base+".center.yaw", active.center.getYaw());
                cfg.set(base+".center.pitch", active.center.getPitch());
            }
            if(active.lobby!=null){
                cfg.set(base+".lobby.x", active.lobby.getX());
                cfg.set(base+".lobby.y", active.lobby.getY());
                cfg.set(base+".lobby.z", active.lobby.getZ());
                cfg.set(base+".lobby.yaw", active.lobby.getYaw());
                cfg.set(base+".lobby.pitch", active.lobby.getPitch());
            }
            List<Map<String,Object>> spawns = new ArrayList<>();
            for(Location l: active.spawns){
                Map<String,Object> m=new HashMap<>();
                m.put("x",l.getX()); m.put("y",l.getY()); m.put("z",l.getZ()); m.put("yaw",l.getYaw()); m.put("pitch",l.getPitch());
                spawns.add(m);
            }
            cfg.set(base+".spawns", spawns);
            cfg.set("active-arena", activeName);
        }
        try{ cfg.save(file);}catch(IOException e){ plugin.getLogger().warning("Could not save arenas.yml: "+e.getMessage());}
    }

    private Arena loadArena(String name){
        ConfigurationSection sec = cfg.getConfigurationSection("arenas."+name);
        if(sec==null) return null;
        Arena a=new Arena();
        a.worldName = sec.getString("world","tntrun");
        // auto-fix old default world_tntrun -> tntrun if tntrun exists and world_tntrun doesn't
        if(a.worldName.equals("world_tntrun") && plugin.getServer().getWorld("world_tntrun")==null && plugin.getServer().getWorld("tntrun")!=null){
            a.worldName = "tntrun";
        }
        a.radius = sec.getInt("radius",20);
        a.floorY = sec.getInt("floor-y",80);
        a.voidY = sec.getInt("void-y",55);
        // support new floors list if present
        if(sec.contains("floors")){
            List<Integer> list = sec.getIntegerList("floors");
            if(!list.isEmpty()) a.floorY = list.get(0);
        }
        a.center = readLoc(sec.getConfigurationSection("center"), a.worldName);
        a.lobby = readLoc(sec.getConfigurationSection("lobby"), a.worldName);
        // spawns
        List<?> list = sec.getList("spawns");
        if(list!=null){
            for(Object o: list){
                if(o instanceof Map){
                    @SuppressWarnings("unchecked")
                    Map<String,Object> m=(Map<String,Object>)o;
                    double x=toDouble(m.get("x"),0), y=toDouble(m.get("y"),a.floorY+1), z=toDouble(m.get("z"),0);
                    float yaw=(float)toDouble(m.get("yaw"),0), pitch=(float)toDouble(m.get("pitch"),0);
                    World w=plugin.getServer().getWorld(a.worldName);
                    if(w==null) w=plugin.getServer().getWorlds().get(0);
                    if(w!=null) a.spawns.add(new Location(w,x,y,z,yaw,pitch));
                }
            }
        }
        return a;
    }

    private Location readLoc(ConfigurationSection sec, String worldName){
        if(sec==null) return null;
        World w=plugin.getServer().getWorld(worldName);
        if(w==null) w=plugin.getServer().getWorld("tntrun");
        if(w==null) w=plugin.getServer().getWorld("world_tntrun");
        if(w==null) w=plugin.getServer().getWorlds().get(0);
        if(w==null) return null;
        double x=sec.getDouble("x",0), y=sec.getDouble("y",80), z=sec.getDouble("z",0);
        float yaw=(float)sec.getDouble("yaw",0), pitch=(float)sec.getDouble("pitch",0);
        Location l = new Location(w,x,y,z,yaw,pitch);
        // fix world mismatch: if resolved world name differs, update
        return l;
    }
    private double toDouble(Object o,double def){
        if(o instanceof Number) return ((Number)o).doubleValue();
        if(o instanceof String) try{return Double.parseDouble((String)o);}catch(Exception ignored){}
        return def;
    }

    public Arena getActive(){ return active; }
    public String getActiveName(){ return activeName; }
    public FileConfiguration getConfig(){ return cfg; }
    public void setActive(Arena a){ this.active=a; }
    public void reload(){ load(); }

    /** Найти мир по имени (регистронезависимо), null если ещё не загружен. */
    private World findWorld(String name){
        if(name==null) return null;
        World w = plugin.getServer().getWorld(name);
        if(w!=null) return w;
        for(World ww: plugin.getServer().getWorlds()){
            if(ww.getName().equalsIgnoreCase(name)) return ww;
        }
        return null;
    }

    /**
     * ФИКС МИРА АРЕНЫ. Проблема: при старте сервера мир арены (world_tntrun) ещё не
     * загружен, и readLoc() "роняет" центр/лобби/спавны в первый мир (world) —
     * из-за чего /tnt join спавнил в пещере в обычном мире, пока не сделали /tnt reload.
     * Когда мир появляется — переносим локации в него (координаты те же, мир правильный).
     * @return true если мир арены найден и локации в нём (фикс больше не нужен)
     */
    public boolean fixWorlds(){
        if(active==null) return false;
        World w = findWorld(active.worldName);
        if(w==null) return false; // мир ещё не загрузился
        boolean changed=false;
        if(active.center!=null && active.center.getWorld()!=null
                && !active.center.getWorld().getName().equalsIgnoreCase(w.getName())){
            active.center = new Location(w, active.center.getX(), active.center.getY(), active.center.getZ(), active.center.getYaw(), active.center.getPitch());
            changed=true;
        }
        if(active.lobby!=null && active.lobby.getWorld()!=null
                && !active.lobby.getWorld().getName().equalsIgnoreCase(w.getName())){
            active.lobby = new Location(w, active.lobby.getX(), active.lobby.getY(), active.lobby.getZ(), active.lobby.getYaw(), active.lobby.getPitch());
            changed=true;
        }
        for(int i=0;i<active.spawns.size();i++){
            Location s = active.spawns.get(i);
            if(s.getWorld()!=null && !s.getWorld().getName().equalsIgnoreCase(w.getName())){
                active.spawns.set(i, new Location(w, s.getX(), s.getY(), s.getZ(), s.getYaw(), s.getPitch()));
                changed=true;
            }
        }
        if(changed){
            plugin.getLogger().info("TNT-Run: мир арены '"+w.getName()+"' загружен — центр/лобби/спавны исправлены");
        }
        return true;
    }
}
