package com.aetherglitch.tntrun.game;

import com.aetherglitch.tntrun.AetherGlitchTNTRun;
import com.aetherglitch.tntrun.arena.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GameManager {
    private final AetherGlitchTNTRun plugin;
    public GameState state = GameState.WAITING;
    public final Set<UUID> players = ConcurrentHashMap.newKeySet();
    public final Set<UUID> spectators = ConcurrentHashMap.newKeySet();
    public final Map<UUID, SavedData> saved = new ConcurrentHashMap<>();
    private BukkitTask countdownTask;
    private int countdownLeft;
    private final Map<Location, BukkitTask> decayTasks = new ConcurrentHashMap<>();
    private final Map<Location, Material> original = new HashMap<>();
    private boolean invincible = false;
    // кто последний выбыл — он победитель, если выбыли ВСЕ (ничьей не бывает)
    private UUID lastEliminated = null;
    // watchdog: сколько тиков игра INGAME с <=1 живым игроком (страховка от зависшего конца игры)
    private int lowPlayersTicks = 0;
    // сколько игроков было в момент старта раунда (соло-игра = 1: конец только когда 0 живых)
    private int startPlayerCount = 0;

    public GameManager(AetherGlitchTNTRun plugin){ this.plugin=plugin; }

    public Arena arena(){ return plugin.getArenaManager().getActive(); }
    public int minPlayers(){ return plugin.getConfig().getInt("game.min-players",2); }
    public int maxPlayers(){ return plugin.getConfig().getInt("game.max-players",25); }
    public boolean isPlaying(Player p){ return players.contains(p.getUniqueId()); }
    public boolean isSpectator(Player p){ return spectators.contains(p.getUniqueId()); }
    public boolean isInGame(Player p){ return isPlaying(p) || isSpectator(p); }

    /** Запоминаем последнего выбывшего. */
    private void markEliminated(Player p){ lastEliminated = p.getUniqueId(); }

    /** Последний выбывший (онлайн), иначе любой онлайн-спектатор, иначе null. */
    private Player lastEliminatedOnline(){
        if(lastEliminated!=null){
            Player p = Bukkit.getPlayer(lastEliminated);
            if(p!=null) return p;
        }
        for(UUID u: spectators){
            Player p = Bukkit.getPlayer(u);
            if(p!=null) return p;
        }
        return null;
    }

    /**
     * Условие конца раунда по количеству игроков:
     *  - живых 0                          -> конец (побеждает последний выбывший, ничьей нет);
     *  - остался 1 живой И игра началась с 2+ -> конец (последний выживший победитель);
     *  - соло-игра (старт с 1 игрока): 1 живой — это НОРМА, игра идёт до его выбытия.
     */
    private boolean shouldEnd(){
        if(players.isEmpty()) return true;
        return players.size()==1 && startPlayerCount>=2;
    }

    /**
     * АВТО-КОНЕЦ ИГРЫ по количеству игроков (см. shouldEnd()).
     * Вызывается после ЛЮБОГО изменения состава (eliminate/leave/quit) + watchdog каждые 2 тика.
     */
    public synchronized void checkAutoEnd(){
        if(state != GameState.INGAME) return;
        if(!shouldEnd()) return;
        Player winner;
        if(players.size()==1){
            winner = Bukkit.getPlayer(players.iterator().next());
        } else {
            winner = lastEliminatedOnline();
        }
        endGame(winner);
    }

    /**
     * Watchdog (вызывается каждые 2 тика из главного класса):
     * 1) анти-кемп на спавне: decay под ногами работает и БЕЗ движения
     *    (раньше блок спавна не ломался, пока игрок не сдвинется);
     * 2) если игра INGAME и по количеству игроков раунд ОБЯЗАН закончиться,
     *    а не закончился >1 сек — событие элиминации упустили, игра зависла — форсируем конец.
     * ВАЖНО: в соло-игре 1 живой игрок — нормальное состояние, watchdog его НЕ кончает.
     */
    public void watchdog(){
        if(state==GameState.INGAME){
            // decay под ногами проверяем по таймеру — стоящий на месте игрок тоже роняет блок
            for(UUID u: new HashSet<>(players)){
                Player p = Bukkit.getPlayer(u);
                if(p!=null) handleMoveDecay(p);
            }
            if(shouldEnd()){
                lowPlayersTicks += 2;
                if(lowPlayersTicks >= 20){
                    plugin.getLogger().warning("TNT-Run watchdog: живых игроков осталось "+players.size()
                            +", конец игры сам не сработал — форсируем (state="+state+")");
                    checkAutoEnd();
                }
            } else {
                lowPlayersTicks = 0;
            }
        } else {
            lowPlayersTicks = 0;
        }
    }

    private String msg(String path){
        String prefix = plugin.getConfig().getString("messages.prefix","ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ » ");
        String m = plugin.getConfig().getString("messages."+path, path);
        return prefix + m;
    }
    private String rawMsg(String path){ return plugin.getConfig().getString("messages."+path, path); }
    private Component comp(String text){ return LegacyComponentSerializer.legacySection().deserialize(text.replace('&','§')); }
    private String apply(String s, Map<String,String> p){
        if(p==null) return s;
        for(Map.Entry<String,String> e: p.entrySet()) s=s.replace(e.getKey(), e.getValue());
        return s;
    }
    private void broadcastPrefix(String text){
        String prefix = plugin.getConfig().getString("messages.prefix","ᴀᴇᴛʜᴇʀɢʟɪᴛᴄʜ » ");
        Component c = comp(prefix + text);
        for(UUID u: players){ Player pl=Bukkit.getPlayer(u); if(pl!=null) pl.sendMessage(c); }
        for(UUID u: spectators){ Player pl=Bukkit.getPlayer(u); if(pl!=null) pl.sendMessage(c); }
    }

    public synchronized boolean join(Player p){
        if(players.contains(p.getUniqueId()) || spectators.contains(p.getUniqueId())){ p.sendMessage(comp(msg("already-in-game"))); return false; }
        if(state==GameState.INGAME){ p.sendMessage(comp(msg("game-already-started"))); return false; }
        if(state==GameState.RESTARTING){ p.sendMessage(comp("§cИгра перезапускается, подожди 3 сек.")); return false; }
        if(players.size() >= maxPlayers()){ p.sendMessage(comp(apply(msg("game-full"), Map.of("%max%", String.valueOf(maxPlayers()))))); return false; }
        Arena a = arena();
        if(a==null || a.getWorld(plugin.getServer())==null){
            p.sendMessage(comp("§cАрена не настроена! Попроси админа настроить arenas.yml"));
            return false;
        }
        saved.put(p.getUniqueId(), SavedData.capture(p));
        players.add(p.getUniqueId());
        p.setGameMode(GameMode.ADVENTURE);
        p.setHealth(20); p.setFoodLevel(20); p.setFireTicks(0); p.setFallDistance(0);
        p.setAllowFlight(false);
        p.getInventory().clear();
        Hotbar.giveLobby(plugin, p);
        if(plugin.getConfig().getBoolean("game.teleport-to-lobby-on-join",true) && a.lobby!=null){
            p.teleportAsync(a.lobby);
        }
        String joined = apply(rawMsg("joined"), Map.of("%player%", p.getName(), "%count%", String.valueOf(players.size()), "%max%", String.valueOf(maxPlayers())));
        broadcastPrefix(joined);
        if(state==GameState.WAITING && players.size() >= minPlayers()){
            startCountdown();
        }
        return true;
    }

    private void restoreAndTeleportBack(Player p){
        SavedData d = saved.remove(p.getUniqueId());
        if(d!=null){
            Location back = d.loc.clone();
            p.getInventory().clear();
            p.closeInventory();
            // сначала тп, потом рестор инвентаря чтобы не было дюпа и чтобы точно в тот же мир/коорды/поворот
            p.teleport(back);
            p.getInventory().clear();
            d.restore(p);
            // гарантируем точные координаты (телепорт + рестор могут сбить)
            if(!p.getLocation().equals(back)){
                p.teleport(back);
            }
        } else {
            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
            p.setSpectatorTarget(null);
            p.closeInventory();
        }
        p.setFallDistance(0);
        p.setFireTicks(0);
    }

    public synchronized void leave(Player p, boolean teleportBack){
        UUID u = p.getUniqueId();
        boolean wasPlayer = players.remove(u);
        boolean wasSpec = spectators.remove(u);
        if(!wasPlayer && !wasSpec) return;
        String left = apply(rawMsg("left"), Map.of("%player%", p.getName(), "%count%", String.valueOf(players.size()), "%max%", String.valueOf(maxPlayers())));
        broadcastPrefix(left);
        if(teleportBack){
            restoreAndTeleportBack(p);
            p.sendMessage(comp(msg("teleported-back")));
        } else {
            SavedData d = saved.remove(u);
            if(d!=null) { /* discarded */ }
            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
            p.setSpectatorTarget(null);
        }
        if(state==GameState.COUNTDOWN && players.size() < minPlayers()){
            cancelCountdown();
            broadcastPrefix(plugin.getConfig().getString("messages.countdown-cancelled","Старт отменён, недостаточно игроков"));
        }
        // авто-конец игры по количеству оставшихся игроков (1 остался — победа ему, 0 — последний выбывший)
        checkAutoEnd();
        if(players.isEmpty() && spectators.isEmpty() && state!=GameState.WAITING){
            resetArena();
            state = GameState.WAITING;
        }
    }

    public void handleQuit(Player p){
        UUID u = p.getUniqueId();
        boolean wasIn = players.contains(u) || spectators.contains(u);
        if(!wasIn) return;
        players.remove(u);
        spectators.remove(u);
        // не чистим saved — оставляем для возможного реконнекта, но если рестарт — saved останется в памяти, при выходе сохраняем?
        // для лива во время игры не восстанавливаем инвентарь сразу, ждем реконнекта или рестарта
        // авто-конец игры по количеству оставшихся игроков (1 остался — победа ему, 0 — последний выбывший)
        checkAutoEnd();
        if(state==GameState.COUNTDOWN && players.size() < minPlayers()){
            cancelCountdown();
            broadcastPrefix(plugin.getConfig().getString("messages.countdown-cancelled","Старт отменён"));
        }
        if(players.isEmpty() && spectators.isEmpty()){
            resetArena();
            state=GameState.WAITING;
        }
    }

    public void handleMoveDecay(Player p){
        if(state!=GameState.INGAME) return;
        if(!players.contains(p.getUniqueId())) return;
        if(invincible) return;
        // фиксим кемп на краю на шифте — проверяем все блоки под хитбоксом (0.6 радиус), а не только центр
        Location base = p.getLocation();
        double y = base.getY() - 0.5;
        double[][] offsets = {{0,0},{0.3,0},{-0.3,0},{0,0.3},{0,-0.3},{0.3,0.3},{0.3,-0.3},{-0.3,0.3},{-0.3,-0.3}};
        Set<Material> decayMats = getDecayMaterials();
        int delay = plugin.getConfig().getInt("blocks.decay-delay-ticks",10);
        for(double[] off : offsets){
            Block b = new Location(base.getWorld(), base.getX()+off[0], y, base.getZ()+off[1]).getBlock();
            Block b2 = b.getLocation().clone().subtract(0,1,0).getBlock(); // иногда игрок на 1 блок выше из-за прыжка
            for(Block cand : List.of(b, b2)){
                if(!decayMats.contains(cand.getType())) continue;
                Location bl = cand.getLocation().clone();
                if(decayTasks.containsKey(bl)) continue;
                if(!original.containsKey(bl)){
                    original.put(bl.clone(), cand.getType());
                }
                final Block block = cand;
                final Location locKey = bl;
                BukkitTask task = new BukkitRunnable(){
                    @Override public void run(){
                        decayTasks.remove(locKey);
                        if(block.getType().isAir()) return;
                        if(plugin.getConfig().getBoolean("blocks.particles",true)){
                            block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5,0.5,0.5), 10, 0.25,0.15,0.25, 0, block.getBlockData());
                        }
                        if(plugin.getConfig().getBoolean("blocks.sound-on-decay",true)){
                            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_SAND_BREAK, 0.7f, 1.1f);
                        }
                        block.setType(Material.AIR, false);
                        if(plugin.getConfig().getBoolean("blocks.instant-remove",true)){
                            Block underTnt = block.getLocation().subtract(0,1,0).getBlock();
                            if(underTnt.getType()==Material.TNT) underTnt.setType(Material.AIR,false);
                        }
                    }
                }.runTaskLater(plugin, delay);
                decayTasks.put(bl, task);
            }
        }
    }

    private Set<Material> getDecayMaterials(){
        List<String> list = plugin.getConfig().getStringList("blocks.decay-materials");
        Set<Material> set = new HashSet<>();
        for(String s: list) try{ set.add(Material.valueOf(s.toUpperCase())); }catch(Exception ignored){}
        if(set.isEmpty()) set.add(Material.SAND);
        return set;
    }

    private void startCountdown(){
        if(state!=GameState.WAITING) return;
        state = GameState.COUNTDOWN;
        countdownLeft = plugin.getConfig().getInt("game.countdown",60);
        final Set<Integer> announceSet = new HashSet<>(plugin.getConfig().getIntegerList("game.announce-at").isEmpty() ? List.of(60,30,20,10,5,4,3,2,1) : plugin.getConfig().getIntegerList("game.announce-at"));
        countdownTask = new BukkitRunnable(){
            @Override public void run(){
                if(players.size() < minPlayers()){
                    cancel(); cancelCountdown();
                    broadcastPrefix(plugin.getConfig().getString("messages.countdown-cancelled","Старт отменён"));
                    return;
                }
                if(announceSet.contains(countdownLeft)){
                    String txt = plugin.getConfig().getString("messages.countdown","Старт через %sec% сек.").replace("%sec%", String.valueOf(countdownLeft));
                    broadcastPrefix(txt);
                    for(UUID u: players){
                        Player pl = Bukkit.getPlayer(u);
                        if(pl!=null){
                            String titleStr = plugin.getConfig().getString("titles.countdown-title","Старт через <sec>").replace("<sec>", String.valueOf(countdownLeft));
                            String sub = plugin.getConfig().getString("titles.countdown-subtitle","Приготовься!");
                            pl.showTitle(Title.title(comp(titleStr), comp(sub), Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(800), Duration.ofMillis(300))));
                            try{
                                String snd = countdownLeft<=5 ? plugin.getConfig().getString("sounds.countdown-final","BLOCK_NOTE_BLOCK_PLING") : plugin.getConfig().getString("sounds.countdown","BLOCK_NOTE_BLOCK_HAT");
                                pl.playSound(pl.getLocation(), Sound.valueOf(snd), 1f, countdownLeft<=5?1.5f:1f);
                            }catch(Exception ignored){}
                        }
                    }
                }
                if(countdownLeft<=0){
                    cancel(); startGame(); return;
                }
                countdownLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void cancelCountdown(){
        if(countdownTask!=null){ countdownTask.cancel(); countdownTask=null; }
        if(state==GameState.COUNTDOWN) state = GameState.WAITING;
    }

    private void startGame(){
        state = GameState.INGAME;
        lastEliminated = null;
        lowPlayersTicks = 0;
        startPlayerCount = players.size();
        Arena a = arena();
        broadcastPrefix(plugin.getConfig().getString("messages.game-started","Игра началась! Беги!"));
        snapshotArena();
        List<Location> spawns = a.spawns;
        Random rnd = new Random();
        int idx=0;
        invincible = true;
        for(UUID u: new HashSet<>(players)){
            Player p = Bukkit.getPlayer(u);
            if(p==null) continue;
            Location spawn;
            if(!spawns.isEmpty()){
                spawn = spawns.get(idx % spawns.size()); idx++;
            } else {
                double angle = rnd.nextDouble()*Math.PI*2;
                double r = rnd.nextDouble()* (a.radius*0.75);
                double x = a.center.getX() + Math.cos(angle)*r;
                double z = a.center.getZ() + Math.sin(angle)*r;
                World w = a.getWorld(plugin.getServer());
                if(w==null) w=p.getWorld();
                spawn = new Location(w, x, a.floorY+1.5, z, (float)(rnd.nextDouble()*360), 0);
            }
            p.teleportAsync(spawn);
            p.setGameMode(GameMode.ADVENTURE);
            p.setAllowFlight(false);
            p.setFallDistance(0);
            Hotbar.givePlaying(plugin, p);
            String goTitle = plugin.getConfig().getString("titles.go-title","Поехали!");
            String goSub = plugin.getConfig().getString("titles.go-subtitle","Беги!");
            p.showTitle(Title.title(comp(goTitle), comp(goSub), Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(400))));
            try{ p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.start","ENTITY_ENDER_DRAGON_GROWL")), 1f,1f);}catch(Exception ignored){}
            plugin.getStatsManager().addGame(p.getUniqueId(), p.getName());
        }
        plugin.getStatsManager().save();
        new BukkitRunnable(){ @Override public void run(){ invincible=false; } }.runTaskLater(plugin, 40L);
    }

    private void snapshotArena(){
        original.clear();
        Arena a = arena();
        if(a==null || a.center==null || a.getWorld(plugin.getServer())==null) return;
        World w = a.getWorld(plugin.getServer());
        if(w==null) return;
        int r = a.radius;
        int cx = (int)Math.floor(a.center.getX());
        int cz = (int)Math.floor(a.center.getZ());
        boolean autoUnder = plugin.getConfig().getBoolean("blocks.auto-place-under", true);
        Material underMat = Material.matchMaterial(plugin.getConfig().getString("blocks.under-material","TNT"));
        // читаем кастомную карту under
        Map<Material,Material> customUnder = new HashMap<>();
        if(plugin.getConfig().isConfigurationSection("blocks.custom-under")){
            for(String k : plugin.getConfig().getConfigurationSection("blocks.custom-under").getKeys(false)){
                Material from = Material.matchMaterial(k);
                Material to = Material.matchMaterial(plugin.getConfig().getString("blocks.custom-under."+k));
                if(from!=null && to!=null) customUnder.put(from, to);
            }
        }
        // фиксим возврат арены: снимаем ВСЕ этажи от floorY до voidY, и гарантируем связку Песок+ТНТ
        for(int y = a.floorY; y > a.voidY; y--){
            for(int x=cx-r; x<=cx+r; x++){
                for(int z=cz-r; z<=cz+r; z++){
                    double dx = (x+0.5)-a.center.getX();
                    double dz = (z+0.5)-a.center.getZ();
                    if(dx*dx+dz*dz > r*r+0.5) continue;
                    Block b = w.getBlockAt(x,y,z);
                    Material t = b.getType();
                    boolean isDecay = getDecayMaterials().contains(t);
                    if(isDecay || t==Material.TNT){
                        original.put(b.getLocation().clone(), t);
                        // если это верхний decay-блок (песок/гравий) — гарантируем что под ним есть 1 ТНТ, а не 2-3; TNT сам под собой TNT не ставит
                        if(isDecay && t!=Material.TNT && autoUnder && underMat!=null){
                            Block under = w.getBlockAt(x,y-1,z);
                            Material expected = customUnder.getOrDefault(t, underMat);
                            // сохраняем то что есть, но если там воздух — запомним что там должен быть TNT для регена
                            if(under.getType()==Material.AIR || under.getType()==expected || under.getType()==Material.TNT){
                                Location ul = under.getLocation().clone();
                                if(!original.containsKey(ul)){
                                    // если там был воздух — при регене поставим TNT, если там был другой блок — восстановим как есть
                                    Material toSave = under.getType()==Material.AIR ? expected : under.getType();
                                    original.put(ul, toSave);
                                }
                            }
                        }
                    }
                }
            }
        }
        plugin.getLogger().info("Snapshot arena: "+original.size()+" blocks (песок+тнт связка) saved");
    }

    private void endGame(Player winner){
        if(state==GameState.RESTARTING) return;
        state = GameState.RESTARTING;
        // 1) Анонс победы + статистика — каждая часть отдельно защищена:
        //    ошибка где-то не должна мешать сбросу раунда (арена/возврат игроков/состояние)
        boolean arenaReset = false;
        try{
            for(BukkitTask t: decayTasks.values()) t.cancel();
            decayTasks.clear();
            invincible=true;
            // ничьей не бывает — если winner null, берем последнего выбывшего, иначе любого онлайн-спека
            if(winner==null){
                winner = lastEliminatedOnline();
            }
            if(winner!=null){
                broadcastPrefix(plugin.getConfig().getString("messages.win","Победил %winner%!").replace("%winner%", winner.getName()));
                for(UUID u: new HashSet<>(players)){
                    Player p=Bukkit.getPlayer(u);
                    if(p!=null){
                        p.showTitle(Title.title(comp("§aПобеда!"), comp("§7Ты последний выживший"), Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600))));
                        try{ p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.win","UI_TOAST_CHALLENGE_COMPLETE")),1f,1f);}catch(Exception ignored){}
                    }
                }
                for(UUID u: new HashSet<>(spectators)){
                    Player p=Bukkit.getPlayer(u);
                    if(p!=null){
                        // не показываем ничью — показываем поражение с победителем
                        if(p.getUniqueId().equals(winner.getUniqueId())){
                            p.showTitle(Title.title(comp("§aПобеда!"), comp("§7Ты победил!"), Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600))));
                        } else {
                            p.showTitle(Title.title(comp("§cПоражение"), comp("§7Победил "+winner.getName()), Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600))));
                        }
                        try{ p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.lose","ENTITY_VILLAGER_DEATH")),1f,0.8f);}catch(Exception ignored){}
                    }
                }
                try{ plugin.getStatsManager().addWin(winner.getUniqueId(), winner.getName()); }catch(Exception ex){ plugin.getLogger().log(Level.WARNING, "TNT-Run: ошибка записи победы", ex); }
            } else {
                // крайний случай: вообще никого онлайн — тихо регеним
                broadcastPrefix("Игра окончена.");
            }
            try{ plugin.getStatsManager().save(); }catch(Exception ex){ plugin.getLogger().log(Level.WARNING, "TNT-Run: ошибка сохранения статов", ex); }
        }catch(Exception ex){
            plugin.getLogger().log(Level.SEVERE, "TNT-Run: ошибка в endGame (анонс/статы) — продолжаем сброс", ex);
        }
        // 2) Реген арены — отдельно защищена, при сбое пробуем ещё раз
        try{ resetArena(); arenaReset = true; }catch(Exception ex){ plugin.getLogger().log(Level.SEVERE, "TNT-Run: ошибка регена арены", ex); }
        if(!arenaReset){ try{ resetArena(); }catch(Exception ignored){} }
        // 3) ПОЛНЫЙ СБРОС РАУНДА (= автоматом /tnt leave всем + /tnt stop):
        //    каждый игрок возвращается на свою точку входа со своим инвентарём.
        //    Сбой у одного игрока не останавливает возврат остальных.
        for(UUID u: new HashSet<>(players)){
            Player p=Bukkit.getPlayer(u);
            if(p==null) continue;
            try{ restoreAndTeleportBack(p); }
            catch(Exception ex){ plugin.getLogger().log(Level.WARNING, "TNT-Run: не удалось вернуть игрока "+p.getName(), ex); }
        }
        for(UUID u: new HashSet<>(spectators)){
            Player p=Bukkit.getPlayer(u);
            if(p==null) continue;
            try{ restoreAndTeleportBack(p); }
            catch(Exception ex){ plugin.getLogger().log(Level.WARNING, "TNT-Run: не удалось вернуть игрока "+p.getName(), ex); }
        }
        // 4) Чистое состояние ВСЕГДА — можно сразу /tnt join, без "перезапускается" и "уже в игре"
        players.clear();
        spectators.clear();
        state=GameState.WAITING;
        invincible=false;
        lowPlayersTicks=0;
    }

    public void eliminate(Player p){
        if(!players.contains(p.getUniqueId())) return;
        players.remove(p.getUniqueId());
        spectators.add(p.getUniqueId());
        markEliminated(p);
        // теперь после смерти сразу ГМ 0 и в лобби, а не ГМ 3 на месте смерти
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(false);
        p.setSpectatorTarget(null);
        p.setFallDistance(0);
        p.setFireTicks(0);
        p.getInventory().clear();
        Hotbar.giveSpectator(plugin, p);
        // null-гард: если арена вдруг null — не даем NPE прервать метод ДО проверки конца игры
        Arena a = arena();
        if(a!=null){
            if(a.lobby!=null) p.teleport(a.lobby);
            else if(a.center!=null) p.teleport(a.center.clone().add(0,5,0));
        }
        broadcastPrefix(plugin.getConfig().getString("messages.eliminated","%player% выбыл!").replace("%player%", p.getName()));
        p.showTitle(Title.title(comp("§cТы выбыл!"), comp("§7Ты в лобби — наблюдай или играй снова"), Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(400))));
        try{ p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.lose","ENTITY_VILLAGER_DEATH")),1f,0.9f);}catch(Exception ignored){}
        // АВТО-КОНЕЦ: остался 1 живой — он победитель; живых 0 — побеждает последний выбывший (сам игрок)
        checkAutoEnd();
    }

    public void playAgain(Player p){
        // должен телепортировать в лобби, а не просто гм 0
        if(!isInGame(p)) return;
        p.setGameMode(GameMode.ADVENTURE);
        p.setSpectatorTarget(null);
        p.getInventory().clear();
        Hotbar.giveLobby(plugin, p);
        if(arena().lobby!=null) p.teleportAsync(arena().lobby);
        else p.teleportAsync(arena().center.clone().add(0,10,0));
        if(spectators.remove(p.getUniqueId())){
            players.add(p.getUniqueId());
        }
        p.setFallDistance(0);
        if(state==GameState.WAITING && players.size() >= minPlayers()){
            startCountdown();
        }
    }

    private void resetArena(){
        for(BukkitTask t: decayTasks.values()) t.cancel();
        decayTasks.clear();
        // восстанавливаем каждый раунд фулл
        for(Map.Entry<Location,Material> e: original.entrySet()){
            Location l=e.getKey();
            if(l.getWorld()==null) continue;
            Block b=l.getBlock();
            b.setType(e.getValue(), false);
        }
        original.clear();
    }

    public void forceStart(Player sender){
        if(state==GameState.INGAME){ if(sender!=null) sender.sendMessage(comp(msg("game-already-started"))); return;}
        if(players.size()<1){ if(sender!=null) sender.sendMessage(comp(msg("not-enough-players"))); return;}
        if(countdownTask!=null) countdownTask.cancel();
        startGame();
        if(sender!=null) sender.sendMessage(comp(msg("forced-start")));
    }
    public void forceStop(Player sender){
        if(countdownTask!=null) countdownTask.cancel();
        for(BukkitTask t: decayTasks.values()) t.cancel();
        decayTasks.clear();
        broadcastPrefix(plugin.getConfig().getString("messages.forced-stop","Игра остановлена"));
        Set<UUID> all=new HashSet<>(); all.addAll(players); all.addAll(spectators);
        for(UUID u: all){
            Player p=Bukkit.getPlayer(u);
            if(p!=null){
                SavedData d = saved.remove(u);
                if(d!=null){
                    p.getInventory().clear();
                    d.restore(p);
                    p.teleportAsync(d.loc);
                } else {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.setSpectatorTarget(null);
                    p.getInventory().clear();
                }
            }
        }
        players.clear(); spectators.clear();
        resetArena(); state=GameState.WAITING; invincible=false;
        lastEliminated = null; lowPlayersTicks = 0; startPlayerCount = 0;
        if(sender!=null) sender.sendMessage(comp(msg("forced-stop")));
    }

    public void shutdown(){
        if(countdownTask!=null) countdownTask.cancel();
        for(BukkitTask t: decayTasks.values()) t.cancel();
        for(UUID u: new HashSet<>(players)){
            Player p=Bukkit.getPlayer(u);
            SavedData d = saved.get(u);
            if(p!=null && d!=null){
                p.getInventory().clear();
                d.restore(p);
                p.teleport(d.loc);
            }
        }
        for(UUID u: new HashSet<>(spectators)){
            Player p=Bukkit.getPlayer(u);
            SavedData d = saved.get(u);
            if(p!=null && d!=null){
                p.getInventory().clear();
                d.restore(p);
                p.teleport(d.loc);
            }
        }
    }
    public int getCountdownLeft(){ return countdownLeft; }
}
