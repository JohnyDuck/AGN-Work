package com.aetherglitch.tntrun.placeholders;

import com.aetherglitch.tntrun.AetherGlitchTNTRun;
import com.aetherglitch.tntrun.stats.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.List;

public class AetherPlaceholders extends PlaceholderExpansion {
    private final AetherGlitchTNTRun plugin;
    public AetherPlaceholders(AetherGlitchTNTRun plugin){ this.plugin=plugin; }

    @Override public String getIdentifier(){ return "agt"; }
    @Override public String getAuthor(){ return "AetherGlitch"; }
    @Override public String getVersion(){ return plugin.getDescription().getVersion(); }
    @Override public boolean persist(){ return true; }

    @Override
    public String onPlaceholderRequest(Player p, String params){
        if(params==null) return "";
        String lower=params.toLowerCase();
        if(p!=null){
            PlayerStats ps = plugin.getStatsManager().get(p.getUniqueId(), p.getName());
            switch(lower){
                case "wins": return String.valueOf(ps.wins);
                case "games": return String.valueOf(ps.games);
                case "losses": return String.valueOf(Math.max(0, ps.games - ps.wins));
                case "winrate": return String.format("%.1f", ps.winrate());
                case "players": return String.valueOf(plugin.getGameManager().players.size());
                case "max_players": return String.valueOf(plugin.getGameManager().maxPlayers());
                case "state": return plugin.getGameManager().state.name();
                case "countdown": return String.valueOf(plugin.getGameManager().getCountdownLeft());
                case "brand": return "ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ";
            }
        }
        // also allow without player: top placeholders
        if(lower.startsWith("top_")){
            try{
                String[] parts = lower.split("_");
                // formats: top_1_name / top_1_wins / top_1_games / top_1_winrate / top_1_losses
                if(parts.length>=3){
                    int pos = Integer.parseInt(parts[1]);
                    String field = parts[2];
                    List<PlayerStats> top = plugin.getStatsManager().top(plugin.getConfig().getInt("placeholders.top-size",10));
                    if(pos<1 || pos>top.size()) return "";
                    PlayerStats ps = top.get(pos-1);
                    switch(field){
                        case "name": return ps.name;
                        case "wins": return String.valueOf(ps.wins);
                        case "games": return String.valueOf(ps.games);
                        case "losses": return String.valueOf(Math.max(0, ps.games - ps.wins));
                        case "winrate": return String.format("%.1f", ps.winrate());
                        default: return ps.name;
                    }
                }
            }catch(Exception e){ return ""; }
        }
        return "";
    }
}
