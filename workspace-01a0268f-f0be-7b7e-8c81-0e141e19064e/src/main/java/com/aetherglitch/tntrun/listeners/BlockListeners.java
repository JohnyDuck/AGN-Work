package com.aetherglitch.tntrun.listeners;

import com.aetherglitch.tntrun.AetherGlitchTNTRun;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListeners implements Listener {
    private final AetherGlitchTNTRun plugin;
    public BlockListeners(AetherGlitchTNTRun plugin){ this.plugin=plugin;}

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        if(plugin.getGameManager().isInGame(e.getPlayer())){
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlace(BlockPlaceEvent e){
        if(plugin.getGameManager().isInGame(e.getPlayer())){
            e.setCancelled(true);
        }
    }
}
