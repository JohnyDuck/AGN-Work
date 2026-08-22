package com.aetherglitch.tntrun.arena;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.ArrayList;
import java.util.List;

public class Arena {
    public String worldName;
    public Location center;
    public Location lobby;
    public int radius;
    public int floorY;
    public int voidY;
    public List<Location> spawns = new ArrayList<>();

    public World getWorld(org.bukkit.Server server){
        // try exact, then tntrun alternatives, then first world
        World w = server.getWorld(worldName);
        if(w!=null) return w;
        // fallback: try "tntrun" / "world_tntrun"
        w = server.getWorld("tntrun");
        if(w!=null) return w;
        w = server.getWorld("world_tntrun");
        if(w!=null) return w;
        // try case-insensitive
        for(World ww: server.getWorlds()){
            if(ww.getName().equalsIgnoreCase(worldName) || ww.getName().equalsIgnoreCase("tntrun")) return ww;
        }
        return server.getWorlds().isEmpty() ? null : server.getWorlds().get(0);
    }

    public List<Integer> getFloors(){
        List<Integer> floors = new ArrayList<>();
        // if floorY and voidY defined, create layers every 6 blocks (sand + 5 air gap)
        int gap = 6;
        int y = floorY;
        int bottom = voidY + 2; // leave 2 blocks for lava
        // generate up to 4 layers or until bottom
        for(int i=0; i<6 && y >= bottom; i++){
            floors.add(y);
            y -= gap;
        }
        if(floors.isEmpty()) floors.add(floorY);
        return floors;
    }

    public int getLowestFloor(){
        List<Integer> f = getFloors();
        return f.get(f.size()-1);
    }

    public boolean isInside(Location loc){
        if(loc==null || center==null) return false;
        World w = getWorld(org.bukkit.Bukkit.getServer());
        if(w!=null && !loc.getWorld().getName().equals(w.getName())){
            // allow if loc world equals resolved world
            if(!loc.getWorld().getName().equals(worldName) && !loc.getWorld().getName().equalsIgnoreCase(worldName)){
                // check fallback names
                String lw = loc.getWorld().getName().toLowerCase();
                String aw = worldName.toLowerCase();
                if(!lw.equals(aw) && !lw.equals("tntrun") && !lw.equals("world_tntrun")) return false;
            }
        }
        double dx = loc.getX() - center.getX();
        double dz = loc.getZ() - center.getZ();
        return dx*dx + dz*dz <= radius*radius + 1.0;
    }
}
