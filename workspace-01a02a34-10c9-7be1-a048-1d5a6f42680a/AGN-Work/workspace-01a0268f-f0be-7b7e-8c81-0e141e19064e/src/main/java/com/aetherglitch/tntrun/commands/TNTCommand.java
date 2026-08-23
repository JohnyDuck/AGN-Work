package com.aetherglitch.tntrun.commands;

import com.aetherglitch.tntrun.AetherGlitchTNTRun;
import com.aetherglitch.tntrun.arena.Arena;
import com.aetherglitch.tntrun.stats.PlayerStats;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TNTCommand implements CommandExecutor, TabCompleter {
    private final AetherGlitchTNTRun plugin;
    public TNTCommand(AetherGlitchTNTRun plugin){ this.plugin=plugin; }

    private void msg(CommandSender s, String path){
        String txt = plugin.getConfig().getString("messages.prefix","ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ » ") + plugin.getConfig().getString("messages."+path, path);
        s.sendMessage(LegacyComponentSerializer.legacySection().deserialize(txt.replace('&','§')));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(args.length==0){
            sendHelp(sender);
            return true;
        }
        String sub=args[0].toLowerCase();
        switch(sub){
            case "join":
                if(!(sender instanceof Player p)){ sender.sendMessage("Только для игроков"); return true; }
                plugin.getGameManager().join(p);
                break;
            case "leave":
                if(!(sender instanceof Player p)){ sender.sendMessage("Только для игроков"); return true; }
                plugin.getGameManager().leave(p,true);
                break;
            case "stats":
                handleStats(sender, args);
                break;
            case "top":
                handleTop(sender);
                break;
            case "reload":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                plugin.reloadAll();
                msg(sender,"reloaded");
                break;
            case "start":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                if(sender instanceof Player pl) plugin.getGameManager().forceStart(pl);
                else {
                    // console
                    Player dummy = null;
                    // find any online player to pass? just call with null check
                    plugin.getGameManager().forceStart(null);
                    sender.sendMessage("Forced start");
                }
                break;
            case "stop":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                Player pl = sender instanceof Player ? (Player)sender : null;
                plugin.getGameManager().forceStop(pl);
                if(pl==null) sender.sendMessage("Stopped");
                break;
            case "setlobby":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                if(!(sender instanceof Player p)){ sender.sendMessage("Только для игроков"); return true; }
                Arena a = plugin.getArenaManager().getActive();
                a.lobby = p.getLocation().clone();
                a.worldName = p.getWorld().getName();
                plugin.getArenaManager().save();
                msg(sender,"lobby-set");
                break;
            case "setcenter":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                if(!(sender instanceof Player p)){ sender.sendMessage("Только для игроков"); return true; }
                Arena ar = plugin.getArenaManager().getActive();
                ar.center = p.getLocation().clone();
                ar.worldName = p.getWorld().getName();
                // floor y = block under
                ar.floorY = p.getLocation().getBlockY() - 1;
                plugin.getArenaManager().save();
                msg(sender,"center-set");
                break;
            case "setradius":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                if(args.length<2){ sender.sendMessage("/tntrun setradius <число>"); return true; }
                try{
                    int r=Integer.parseInt(args[1]);
                    plugin.getArenaManager().getActive().radius=r;
                    plugin.getArenaManager().save();
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((plugin.getConfig().getString("messages.prefix","") + plugin.getConfig().getString("messages.radius-set","Радиус %radius%").replace("%radius%",String.valueOf(r))).replace('&','§')));
                }catch(NumberFormatException e){ sender.sendMessage("Число!");}
                break;
            case "setworld":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                if(args.length<2){ sender.sendMessage("/tntrun setworld <мир>"); return true; }
                World w = plugin.getServer().getWorld(args[1]);
                if(w==null){ sender.sendMessage("Мир не найден"); return true; }
                plugin.getArenaManager().getActive().worldName=w.getName();
                plugin.getArenaManager().save();
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((plugin.getConfig().getString("messages.prefix","")+ plugin.getConfig().getString("messages.world-set","Мир %world%").replace("%world%",w.getName())).replace('&','§')));
                break;
            case "addspawn":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                if(!(sender instanceof Player p)){ sender.sendMessage("Только для игроков"); return true; }
                plugin.getArenaManager().getActive().spawns.add(p.getLocation().clone());
                plugin.getArenaManager().save();
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((plugin.getConfig().getString("messages.prefix","")+ plugin.getConfig().getString("messages.spawn-added","Добавлено").replace("%count%",String.valueOf(plugin.getArenaManager().getActive().spawns.size()))).replace('&','§')));
                break;
            case "clearspawns":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                plugin.getArenaManager().getActive().spawns.clear();
                plugin.getArenaManager().save();
                msg(sender,"spawns-cleared");
                break;
            case "setfloor":
                if(!sender.hasPermission("aetherglitch.admin")){ msg(sender,"no-permission"); return true; }
                if(args.length<2){ sender.sendMessage("/tntrun setfloor <y>"); return true; }
                try{
                    int y=Integer.parseInt(args[1]);
                    plugin.getArenaManager().getActive().floorY=y;
                    plugin.getArenaManager().save();
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((plugin.getConfig().getString("messages.prefix","")+ plugin.getConfig().getString("messages.floor-set","Y %y%").replace("%y%",String.valueOf(y))).replace('&','§')));
                }catch(Exception e){ sender.sendMessage("Число!");}
                break;
            default:
                sendHelp(sender);
        }
        return true;
    }

    private void handleStats(CommandSender sender, String[] args){
        String target = null;
        if(args.length>=2) target=args[1];
        else if(sender instanceof Player) target=sender.getName();
        else { sender.sendMessage("/tntrun stats <ник>"); return; }
        PlayerStats ps = plugin.getStatsManager().getByName(target);
        if(ps==null){
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((plugin.getConfig().getString("messages.prefix","")+ "Игрок не найден").replace('&','§')));
            return;
        }
        String header = plugin.getConfig().getString("messages.stats-header","— Статистика %player% —").replace("%player%", ps.name);
        String line = plugin.getConfig().getString("messages.stats-line","Побед: %wins%").replace("%wins%",String.valueOf(ps.wins)).replace("%games%",String.valueOf(ps.games)).replace("%winrate%",String.format("%.1f",ps.winrate()));
        String prefix = plugin.getConfig().getString("messages.prefix","ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ » ");
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((prefix+header).replace('&','§')));
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((prefix+line).replace('&','§')));
    }

    private void handleTop(CommandSender sender){
        List<PlayerStats> top = plugin.getStatsManager().top(plugin.getConfig().getInt("placeholders.top-size",10));
        String prefix = plugin.getConfig().getString("messages.prefix","ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ » ");
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((prefix + plugin.getConfig().getString("messages.top-header","— Топ —")).replace('&','§')));
        if(top.isEmpty()){
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((prefix + plugin.getConfig().getString("messages.top-empty","Пусто")).replace('&','§')));
            return;
        }
        int i=1;
        for(PlayerStats ps: top){
            String line = plugin.getConfig().getString("messages.top-line","#%pos% %player%").replace("%pos%",String.valueOf(i)).replace("%player%",ps.name).replace("%wins%",String.valueOf(ps.wins)).replace("%games%",String.valueOf(ps.games));
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((prefix+line).replace('&','§')));
            i++;
        }
    }

    private void sendHelp(CommandSender s){
        s.sendMessage("§7--- ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ TNT-Run ---");
        s.sendMessage("§e/tntrun join §7- войти");
        s.sendMessage("§e/tntrun leave §7- выйти");
        s.sendMessage("§e/tntrun stats [ник] §7- статистика");
        s.sendMessage("§e/tntrun top §7- топ");
        if(s.hasPermission("aetherglitch.admin")){
            s.sendMessage("§c[admin] /tntrun setlobby, setcenter, setradius <r>, setfloor <y>, setworld <мир>, addspawn, clearspawns");
            s.sendMessage("§c[admin] /tntrun start, stop, reload");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args){
        List<String> out=new ArrayList<>();
        if(args.length==1){
            for(String v: List.of("join","leave","stats","top","reload","start","stop","setlobby","setcenter","setradius","setworld","addspawn","clearspawns","setfloor")){
                if(v.startsWith(args[0].toLowerCase())) out.add(v);
            }
        }
        return out;
    }
}
