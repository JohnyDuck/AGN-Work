package com.aetherglitch.tntrun;

import com.aetherglitch.tntrun.arena.ArenaManager;
import com.aetherglitch.tntrun.commands.TNTCommand;
import com.aetherglitch.tntrun.game.GameManager;
import com.aetherglitch.tntrun.listeners.BlockListeners;
import com.aetherglitch.tntrun.listeners.GameListeners;
import com.aetherglitch.tntrun.placeholders.AetherPlaceholders;
import com.aetherglitch.tntrun.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AetherGlitchTNTRun extends JavaPlugin {
    private static AetherGlitchTNTRun instance;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private StatsManager statsManager;

    public static AetherGlitchTNTRun getInstance(){ return instance; }
    public ArenaManager getArenaManager(){ return arenaManager; }
    public GameManager getGameManager(){ return gameManager; }
    public StatsManager getStatsManager(){ return statsManager; }

    @Override
    public void onEnable(){
        instance=this;
        saveDefaultConfig();
        arenaManager = new ArenaManager(this);
        arenaManager.load();

        statsManager = new StatsManager(this);
        statsManager.load();

        gameManager = new GameManager(this);

        getServer().getPluginManager().registerEvents(new GameListeners(this), this);
        getServer().getPluginManager().registerEvents(new BlockListeners(this), this);

        TNTCommand cmd = new TNTCommand(this);
        getCommand("tntrun").setExecutor(cmd);
        getCommand("tntrun").setTabCompleter(cmd);
        if(getCommand("tnt")!=null){
            getCommand("tnt").setExecutor(cmd);
            getCommand("tnt").setTabCompleter(cmd);
        }
        if(getCommand("aetherglitch")!=null){
            getCommand("aetherglitch").setExecutor(cmd);
            getCommand("aetherglitch").setTabCompleter(cmd);
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI")!=null){
            AetherPlaceholders agt = new AetherPlaceholders(this);
            agt.register();
            try{ new com.aetherglitch.tntrun.placeholders.AetherPlaceholdersLegacy(agt).register(); }catch(Exception ignored){}
            getLogger().info("PlaceholderAPI hook enabled - ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ -> %agt_%");
        }

        long interval = getConfig().getLong("stats.save-interval-seconds",60)*20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, ()->statsManager.save(), interval, interval);

        // МИР АРЕНЫ: при старте сервера он может грузиться ПОЗЖЕ плагина — тогда центр/лобби
        // временно "в плохом мире" и /tnt join спавнит в обычном мире. Ждём загрузки мира и чиним.
        // (раньше это чинилось только ручным /tnt reload)
        new org.bukkit.scheduler.BukkitRunnable(){
            int tries = 0;
            @Override public void run(){
                if(arenaManager.fixWorlds()){ cancel(); return; }
                if(++tries >= 90){ // ~3 минуты ждём
                    cancel();
                    getLogger().warning("TNT-Run: мир арены '"+arenaManager.getActive().worldName+"' не загрузился за 3 минуты! "
                            + "Проверь имя мира в arenas.yml и что мир включён в server.properties. "
                            + "Локации арены сейчас в другом мире — /tnt join может спавнить не туда.");
                }
            }
        }.runTaskTimer(this, 20L, 20L);
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener(){
            @org.bukkit.event.EventHandler
            public void onWorldLoad(org.bukkit.event.world.WorldLoadEvent e){
                arenaManager.fixWorlds();
            }
        }, this);

        // БРУТФОРС лава — ВСЕ пути: даже если isPlaying багнутый, всё равно убьёт
        // ВАЖНО: весь блок в try/catch — одна ошибка не должна убить повторяющуюся задачу
        // (иначе лава/распад/watchdog перестанут работать до перезапуска сервера)
        Bukkit.getScheduler().runTaskTimer(this, ()->{
            try{
            if(gameManager==null || arenaManager==null || arenaManager.getActive()==null) return;
            if(gameManager.state!=com.aetherglitch.tntrun.game.GameState.INGAME) return;
            // WATCHDOG: анти-кемп (decay под ногами без движения) + страховка конца игры:
            // если по количеству игроков раунд обязан закончиться (0 живых, или 1 живой
            // в игре, начавшейся с 2+), а не закончился >1 сек — форсим. В соло-игре
            // 1 живой игрок — нормальное состояние, watchdog его НЕ кончает.
            gameManager.watchdog();
            for(Player p : Bukkit.getOnlinePlayers()){
                // проверяем в мире tntrun (любой вариант имени)
                String w = p.getWorld().getName().toLowerCase();
                String aw = arenaManager.getActive().worldName.toLowerCase();
                if(!w.equals(aw) && !w.equals("tntrun") && !w.equals("world_tntrun")) continue;
                double y = p.getLocation().getY();
                boolean isPlaying = gameManager.isPlaying(p);
                boolean isSpec = gameManager.isSpectator(p);
                boolean isInGame = isPlaying || isSpec;
                // логика: в лаве или ниже void — убиваем ЛЮБОГО isPlaying, а если isSpec но упал — просто тп в лобби
                boolean inLava = p.isInLava()
                        || p.getLocation().getBlock().getType()==Material.LAVA
                        || p.getLocation().clone().subtract(0,0.3,0).getBlock().getType()==Material.LAVA
                        || p.getLocation().clone().subtract(0,1,0).getBlock().getType()==Material.LAVA
                        || p.getLocation().clone().add(0,0.3,0).getBlock().getType()==Material.LAVA;
                boolean lowY = y < arenaManager.getActive().voidY + 5;
                // точка возврата: лобби, иначе центр арены (гард от null, чтобы не убить задачу)
                org.bukkit.Location fallback = arenaManager.getActive().lobby!=null
                        ? arenaManager.getActive().lobby
                        : arenaManager.getActive().center.clone().add(0,5,0);
                if((inLava || lowY) && isPlaying){
                    getLogger().info("LAVA-KILL "+p.getName()+" y="+String.format("%.1f",y)+" voidY="+arenaManager.getActive().voidY+" inLava="+inLava+" lowY="+lowY+" isPlaying="+isPlaying);
                    gameManager.eliminate(p);
                } else if((inLava || lowY) && isSpec){
                    // спек упал в лаву — верни в лобби чтобы не застрял
                    getLogger().info("LAVA-SPEC-TP "+p.getName());
                    p.teleport(fallback);
                    p.setGameMode(org.bukkit.GameMode.ADVENTURE);
                } else if((inLava || lowY) && !isInGame){
                    // даже если isPlaying багнутый и false — всё равно тп в лобби если в игре мир
                    if(y < arenaManager.getActive().voidY + 2){
                        getLogger().warning("LAVA-FORCE isPlaying=false но игрок в лаве! "+p.getName()+" y="+y+" players="+gameManager.players+" specs="+gameManager.spectators);
                        // форсим элиминацию через прямой тп
                        if(gameManager.players.contains(p.getUniqueId()) || gameManager.spectators.contains(p.getUniqueId())){
                            gameManager.eliminate(p);
                        } else {
                            p.teleport(fallback);
                            p.setGameMode(org.bukkit.GameMode.ADVENTURE);
                        }
                    }
                }
            }
            }catch(Exception ex){
                getLogger().severe("TNT-Run: ошибка в 2-тиковой задаче (задача продолжит работать): "+ex);
                ex.printStackTrace();
            }
        }, 2L, 2L);

        getLogger().info("ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ TNT-Run enabled. Active arena: "+arenaManager.getActiveName()+" world: "+arenaManager.getActive().worldName+" voidY="+arenaManager.getActive().voidY+" auto-end-fix (watchdog) enabled");
    }

    @Override
    public void onDisable(){
        if(gameManager!=null) gameManager.shutdown();
        if(statsManager!=null) statsManager.save();
        if(arenaManager!=null) arenaManager.save();
        getLogger().info("ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ disabled");
    }

    public void reloadAll(){
        reloadConfig();
        arenaManager.reload();
        arenaManager.fixWorlds(); // и при /tnt reload чиним мир, если мир арены уже загрузился
        statsManager.reloadFileRef();
        statsManager.load();
        getLogger().info("Reloaded voidY="+arenaManager.getActive().voidY);
    }
}
