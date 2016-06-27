package net.Andrewcpu.Minigame.Elytra;

import net.Andrewcpu.MinigameAPI.ActionBar;
import net.Andrewcpu.MinigameAPI.GemManager;
import net.Andrewcpu.MinigameAPI.chat.ChatManager;
import net.Andrewcpu.MinigameAPI.cosmeds.CosmeticManager;
import net.Andrewcpu.MinigameAPI.scoreboard.GameBoard;
import net.Andrewcpu.MinigameAPI.sheep.InteractableEntity;
import net.Andrewcpu.MinigameAPI.sheep.InteractableSheep;
import net.Andrewcpu.MinigameAPI.sheep.Interaction;
import net.Andrewcpu.MinigameAPI.sheep.NPCManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.*;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by stein on 4/10/2016.
 */
public class Main extends JavaPlugin implements Listener {
    public static String header = ChatColor.BLUE + "Game>" + ChatColor.GRAY+ " ";
    private static Main instance = null;
    public Arena arena = new Arena();
    public static Main getInstance(){
        return instance;
    }
    private EnderCrystal crystal = null;
    private BossBar infoBar = null;
    private String defaultInfo = "";
    private List<Material> exceptions = new ArrayList<>(), explosionExceptions = new ArrayList<>();
    public Location lobby  = null, center = null;
    private HashMap<UUID,Integer> gems = new HashMap<>();
    public HashMap<Player,Integer> captureTime = new HashMap<>();
    public HashMap<Player,Integer> playerScores = new HashMap<>();
    public void onEnable(){
        instance = this;
        getServer().getPluginManager().registerEvents(this,this);
        infoBar = Bukkit.createBossBar(ChatColor.RED + "" + ChatColor.BOLD + "Elytra Wars " + ChatColor.BLACK + "-" + ChatColor.GRAY + " andrewcpu.net", BarColor.PINK, BarStyle.SOLID, BarFlag.DARKEN_SKY);
        defaultInfo = infoBar.getTitle();
        updateLocations();
        updateSheep();
        for(Player p : Bukkit.getOnlinePlayers()){
            ElytraTeam red = arena.getRedTeam();
            ElytraTeam green = arena.getGreenTeam();
            if(red.getCurrentSize()>green.getCurrentSize()){
                if(green.getCurrentSize()<5){
                    green.join(p);
                }
                else
                    p.kickPlayer(ChatColor.GRAY + "Sorry, that game is full.");
            }
            else if(green.getCurrentSize()>red.getCurrentSize()){
                if(red.getCurrentSize()<5){
                    red.join(p);
                }
                else
                    p.kickPlayer(ChatColor.GRAY + "Sorry, that game is full.");
            }
            else{
                red.join(p);
            }

            infoBar.addPlayer(p);
            p.teleport(lobby);
            arena.selectClass(p,ElytraClass.WARRIOR);
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, ()->{
            if(arena.isGameStarted()) {
                arena.tick();
                flightScores();
                for(Player p : Bukkit.getOnlinePlayers()){
                    if(p.getLocation().distance(center)>= 75 + ((LocationManager.getLocation("Arena.Red.Spawn").distance(center) + LocationManager.getLocation("Arena.Green.Spawn").distance(center)) / 2) || p.getLocation().getBlockY()<=0 || p.getLocation().getBlockY()>=255){
                        p.damage(1);
                        p.sendTitle(ChatColor.RED + "You have left the playable zone",ChatColor.GRAY + "Please return to the game");
                    }
                    if(p.getLocation().getBlockY()<=0){
                        p.setVelocity(p.getVelocity().add(new Vector(0,1,0)));
                        if(p.getLevel()-5<0)
                            p.setLevel(0);
                        else
                            p.setLevel(p.getLevel()-5);
                    }
                }
            }
            else{
                for(Player p : Bukkit.getOnlinePlayers()){
                    p.setScoreboard(getPlayerScoreboard(p));
                }
            }
        },20,20);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, ()->{updateGems();},200, 200);

        exceptions.add(Material.STATIONARY_WATER);
        exceptions.add(Material.WATER);
        exceptions.add(Material.GRASS_PATH);
        exceptions.add(Material.STATIONARY_LAVA);
        exceptions.add(Material.LAVA);
        for(Material m : Material.values()){
            if(m.toString().contains("CHEST") || m.toString().contains("DOOR") || m.toString().contains("STAIRS") || m.toString().contains("GLASS") || m.toString().contains("GATE") || m.toString().contains("FENCE") || m.toString().contains("CHORUS")){
                exceptions.add(m);
            }
        }
        exceptions.add(Material.LADDER);
        exceptions.add(Material.BED_BLOCK);
        exceptions.add(Material.ENCHANTMENT_TABLE);
        exceptions.add(Material.ENDER_PORTAL_FRAME);
        exceptions.add(Material.IRON_BARDING);
        exceptions.add(Material.DAYLIGHT_DETECTOR);
        exceptions.add(Material.DAYLIGHT_DETECTOR_INVERTED);

        explosionExceptions.add(Material.LOG);
        explosionExceptions.add(Material.STAINED_CLAY);
        explosionExceptions.add(Material.END_ROD);
        explosionExceptions.add(Material.STANDING_BANNER);
        explosionExceptions.add(Material.WALL_BANNER);
        explosionExceptions.add(Material.LADDER);
        explosionExceptions.add(Material.TORCH);
        arena.getRedTeam().setScore(0);
        arena.getGreenTeam().setScore(0);
        updateGems();
    }
    public void updateGems(){
        gems.clear();
        for(Player player : Bukkit.getOnlinePlayers()){
            updateGems(player);
        }
    }
    public void updateGems(Player player){
        if(gems.containsKey(player.getUniqueId())){
            gems.remove(player.getUniqueId());
        }
        try {
            gems.put(player.getUniqueId(), GemManager.getGems(player));
        } catch (Exception e) {
        }
    }
    public void updateLocations(){
        lobby = LocationManager.getLocation("Arena.Lobby");
        center = LocationManager.getLocation("Arena.Center");
    }
    public void flightScores(){
        for(Player p : Bukkit.getOnlinePlayers()){
            if(p.isSneaking() && p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType()==Material.AIR && p.getLocation().getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getType()==Material.AIR){
                if(p.getLevel()>=1){
                    p.setLevel(p.getLevel()-1);
                }
            }
            else{
                if(p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType()!=Material.AIR)
                    if(arena.getClass(p)==ElytraClass.HEALER){
                        p.setLevel(p.getLevel() + 5);
                    }
                    else if(arena.getClass(p)==ElytraClass.WARRIOR){
                        p.setLevel(p.getLevel()+3);
                    }
                    else if(arena.getClass(p)==ElytraClass.GHOST){
                        p.setLevel(p.getLevel()+2);
                    }
                    else if(arena.getClass(p)==ElytraClass.TANK){
                        p.setLevel(p.getLevel()+1);
                    }
            }
        }
    }
    public Scoreboard getGameScoreboard(Player p){
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("Game","dummy");
        objective.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Elytra Wars");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Score empty3 = objective.getScore("    ");
        empty3.setScore(17);
        Score red = objective.getScore(ChatColor.DARK_RED +"" + ChatColor.BOLD + "Red Team");
        red.setScore(16);
        Score redScore = objective.getScore(arena.getRedTeam().getScore() + "");
        redScore.setScore(15);

        Score empty = objective.getScore(" ");
        empty.setScore(14);

        Score green = objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Green Team");
        green.setScore(13);
        Score greenScore = objective.getScore(arena.getGreenTeam().getScore() + "");
        greenScore.setScore(12);

        Score empty2 = objective.getScore("  ");
        empty2.setScore(11);

        Score captured = objective.getScore(ChatColor.YELLOW + "" + ChatColor.BOLD + "Controlling Center");
        captured.setScore(10);

        Score capturing = objective.getScore((getDominatingTeam() == null ? "No one" : getDominatingTeam().getTeamColor().toString()));
        capturing.setScore(9);

        Score empty4 = objective.getScore("    ");
        empty4.setScore(8);

        Score time = objective.getScore(ChatColor.YELLOW + "" + ChatColor.BOLD + "Remaining Time");
        time.setScore(7);

        double tm = arena.getTime();
        DecimalFormat df = new DecimalFormat("#.#");
        String tS = "";
        if(tm>=60){
            tm/=60;
            tS = df.format(tm);
        }
        else{
            tS = (int)tm + "";
        }

        Score timeLeft = objective.getScore(tS);
        timeLeft.setScore(6);
        Score empty5 = objective.getScore("          ");
        empty5.setScore(5);
        Score teamListTitle = objective.getScore(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Team List");
        teamListTitle.setScore(4);
        int players = 3;
        for(Player player : arena.getPlayerTeam(p).getPlayers()){
            Score score = objective.getScore(player.getName());
            score.setScore(players);
            players--;
        }
        return scoreboard;
    }
    public List<Player> getCenterPlayers(){
        List<Player> players = new ArrayList<>();
        for(Player player : Bukkit.getOnlinePlayers()){
            if(player.getLocation().distance(center)<=15 && player.getGameMode()==GameMode.SURVIVAL && !player.isGliding() & player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType()==Material.STAINED_CLAY){
                players.add(player);
            }
        }
        return players;
    }
    public ElytraTeam getDominatingTeam(){
        List<Player> players= getCenterPlayers();
        int redTeam = 0;
        int greenTeam = 0;
        for(Player player : players){
            if(arena.getPlayerTeam(player).getTeamColor()==ElytraTeamColor.RED){
                redTeam++;
            }
            else if(arena.getPlayerTeam(player).getTeamColor()==ElytraTeamColor.GREEN){
                greenTeam++;
            }
        }
        if(players.size()>0){
            if(redTeam==players.size()){
                return arena.getRedTeam();
            }
            else if(greenTeam==players.size()){
                return arena.getGreenTeam();
            }
            else{
                return null;
            }
        }
        else {
            return null;
        }
    }
    public Scoreboard getPlayerScoreboard(Player player){
        if(!gems.containsKey(player.getUniqueId()))
            gems.put(player.getUniqueId(),0);
        int playerGems = gems.get(player.getUniqueId());
        GameBoard gameBoard = new GameBoard(ChatColor.YELLOW + "-- " + ChatColor.WHITE + ChatColor.BOLD + "Elytra " + ChatColor.RED + "" + ChatColor.BOLD + "Wars" + ChatColor.YELLOW + " --");
        gameBoard.addEntry(16," ");
        gameBoard.addEntry(15, ChatColor.YELLOW + "" +  ChatColor.BOLD + "Players");
        gameBoard.addEntry(14, Bukkit.getOnlinePlayers().size() + "/" + arena.getMaxPlayers());
        gameBoard.addEntry(13, "  ");
        gameBoard.addEntry(12, ChatColor.YELLOW + "" + ChatColor.BOLD + "Team");
        gameBoard.addEntry(11, arena.getPlayerTeam(player).getTeamColor().toString());
        gameBoard.addEntry(10, "   ");
        gameBoard.addEntry(9, ChatColor.AQUA + "" + ChatColor.BOLD + "Class");
        gameBoard.addEntry(8, arena.getClass(player).name());
        gameBoard.addEntry(7, "    ");
        gameBoard.addEntry(6, ChatColor.GREEN + "" + ChatColor.BOLD + "Gems");
        gameBoard.addEntry(5, ChatColor.WHITE + "" + playerGems);
        return gameBoard.toScoreboard();
    }
    @EventHandler
    public void onPickup(PlayerPickupItemEvent event){
            Player player = event.getPlayer();
            ElytraTeam team = arena.getPlayerTeam(player);
            if(team == arena.getRedTeam()){
                if(event.getItem().getLocation().getBlock().getRelative(BlockFace.DOWN).getData()==14){
                    event.setCancelled(true);
                    return;
                }
            }
            if(team == arena.getGreenTeam()){
                if(event.getItem().getLocation().getBlock().getRelative(BlockFace.DOWN).getData()==13){
                    event.setCancelled(true);
                    return;
                }
            }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event){
        if(event.getPlayer().getGameMode()==GameMode.SURVIVAL && arena.isGameStarted() && (event.getItemDrop().getItemStack().getType()==Material.DIAMOND || event.getItemDrop().getItemStack().getType()==Material.GOLD_INGOT || event.getItemDrop().getItemStack().getType()==Material.IRON_INGOT || event.getItemDrop().getItemStack().getType().toString().contains("AXE"))){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractAtEntityEvent event){
        if(event.getHand()== EquipmentSlot.HAND){
            //Sheep sheep = (Sheep)event.getRightClicked();
            if(NPCManager.isEntityInteractable(event.getRightClicked()))
                NPCManager.getInteractableEntity(event.getRightClicked()).getInteraction().onInteract(event.getPlayer());
//            if(sheep.getColor()==DyeColor.RED){
//                arena.getRedTeam().join(event.getPlayer());
//            }
//            else if(sheep.getColor()==DyeColor.GREEN){
//                arena.getGreenTeam().join(event.getPlayer());
//            }
        }
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        getInfoBar().addPlayer(event.getPlayer());
        Location location = LocationManager.getLocation("Arena.Lobby");
        event.getPlayer().teleport(location);
        event.getPlayer().setExp(0);
        event.getPlayer().setLevel(0);

        ElytraTeam red = arena.getRedTeam();
        ElytraTeam green = arena.getGreenTeam();

        if(red.getCurrentSize()>green.getCurrentSize()){
            if(green.getCurrentSize()<5){
                green.join(event.getPlayer());
            }
            else
                event.getPlayer().kickPlayer(ChatColor.GRAY + "Sorry, that game is full.");
        }
        else if(green.getCurrentSize()>red.getCurrentSize()){
            if(red.getCurrentSize()<5){
                red.join(event.getPlayer());
            }
            else
                event.getPlayer().kickPlayer(ChatColor.GRAY + "Sorry, that game is full.");
        }
        else{
            red.join(event.getPlayer());
        }
        arena.selectClass(event.getPlayer(),ElytraClass.WARRIOR);
        event.setJoinMessage(ChatColor.DARK_GRAY + "Join> " + ChatColor.GRAY + event.getPlayer().getName());
        if(Bukkit.getOnlinePlayers().size()==arena.getMaxPlayers()){
            for(int i = 0, time = 10; i<=10; i++){
                final int t = time;
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    @Override
                    public void run() {
                        Bukkit.broadcastMessage(header + "Game starting in " + ChatColor.WHITE + "" + ChatColor.BOLD + t + " seconds");
                        if(t==0){
                            arena.start();
                        }
                    }
                }, i * 20);
                time--;
            }
        }
        if(arena.isGameStarted()){
            arena.teleportPlayerToSpawnPoint(event.getPlayer());
        }
        event.getPlayer().getInventory().setContents(new ItemStack[36]);
        event.getPlayer().updateInventory();

        updateGems(event.getPlayer());
        //todo add to team
    }
    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        arena.getPlayerTeam(event.getPlayer()).removePlayer(event.getPlayer());
        event.setQuitMessage(ChatColor.DARK_GRAY + "Quit> " + ChatColor.GRAY + event.getPlayer().getName());
    }
    public void resetBossBar(){
        infoBar.setProgress(1);
        infoBar.setTitle(defaultInfo);
    }
    public BossBar getInfoBar() {
        return infoBar;
    }

    public void setInfoBar(BossBar infoBar) {
        this.infoBar = infoBar;
    }

    public void onDisable(){
        arena.resetField();
        for(Entity e : Bukkit.getWorld("world").getEntitiesByClass(Arrow.class)){
            e.remove();
        }
        for(Player p : Bukkit.getOnlinePlayers()){
            infoBar.removePlayer(p);
        }
        NPCManager.cleanup();
    }

    @EventHandler
    public void onProjectile(ProjectileHitEvent event){
        TNTPrimed tnt = (TNTPrimed)event.getEntity().getWorld().spawnEntity(event.getEntity().getLocation(),EntityType.PRIMED_TNT);
        tnt.setFuseTicks(0);
        tnt.setFireTicks(0);
        tnt.setIsIncendiary(false);
        event.getEntity().remove();
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event){
        int bottomY = 255;
        for(Block b : event.blockList()){
            if(b.getLocation().getBlockY() < bottomY){
                bottomY = b.getLocation().getBlockY();
            }
        }
        for(Block b : event.blockList()){
            if(explosionExceptions.contains(b.getType())){
                continue;
            }
            BlockState s = b.getState();
            if(b.getType()!=Material.TNT){
                b.setType(Material.AIR);
                long time = random.nextInt(30);
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, ()->{
                    s.update(true, true);
                    b.getWorld().playEffect(b.getLocation(), Effect.TILE_BREAK, new MaterialData(b.getType()).getItemTypeId(), b.getData());
                    b.setType(s.getType());
                    b.setData(s.getRawData());
                }, (b.getLocation().getBlockY() - (bottomY / 2)) * 10);
            }
            else{
                TNTPrimed tnt = (TNTPrimed)b.getWorld().spawnEntity(b.getLocation(),EntityType.PRIMED_TNT);
                tnt.setFuseTicks(10);
                tnt.setFireTicks(0);
                tnt.setIsIncendiary(false);
                b.setType(Material.AIR);
            }

        }
        event.blockList().clear();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event){
        if(event.getBlock().getType()==Material.GLASS && arena.isGameStarted())
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, ()->{event.getBlock().setType(Material.GLASS);}, 20 * 20);
        else
            event.setCancelled((arena.isGameStarted() ? true : !(event.getPlayer().isOp())));

    }


    @EventHandler
    public void onChat(AsyncPlayerChatEvent event){
        try {
            ChatManager.sendMessage(event.getPlayer().getUniqueId(),event.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        event.setFormat((event.getPlayer().isOp() ? ChatColor.RED : ChatColor.GRAY ) + event.getPlayer().getName() + " " + ChatColor.WHITE + event.getMessage());
    }

    private Random random = new Random();
    @EventHandler
    public void onPlace(BlockPlaceEvent event){
        if(!arena.isGameStarted()){
            if(event.getBlock().getType()==Material.WOOL){
                byte data = event.getBlock().getData();
                ElytraTeamColor color = (data==14 ? ElytraTeamColor.RED : ElytraTeamColor.GREEN);
                ((ElytraTeam)(color==ElytraTeamColor.RED ? arena.getRedTeam() : arena.getGreenTeam())).addPillar(createPillar(color,random.nextInt(40 - 15) + 15, event.getBlock().getLocation()));
                event.getPlayer().sendMessage(header + "You created a pillar...");
            }
        }
        else{
            event.setBuild(false);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFly(EntityToggleGlideEvent event)
    {
        if(!event.isGliding() && event.getEntity() instanceof Player && ((Player)event.getEntity()).getGameMode()== GameMode.SURVIVAL && ((Player)event.getEntity()).getLocation().getBlock().getRelative(BlockFace.DOWN).getType()== Material.AIR && arena.isGameStarted())
        {
            ((Player) event.getEntity()).setGliding(true);
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof Item){
            event.setCancelled(true);
        }
        if(event.getEntity() instanceof Sheep){
            event.setCancelled(true);
        }
        if(event.getCause()== EntityDamageEvent.DamageCause.FALL){
            if(noFallDamage.contains(event.getEntity())){
                event.setCancelled(true);
            }
        }
        if(event.getEntity() instanceof Player) {
            if (deathManager((Player) event.getEntity(), event.getDamage(), true)) {
                event.setDamage(0);
                event.setCancelled(true);
            }
        }
        }
    public boolean deathManager(Player player, double damage, boolean deathMessage){
            if(player.getHealth() - damage<=0) {
                BossBar bossBar = Bukkit.createBossBar(ChatColor.GRAY + "Respawning in...", BarColor.RED, BarStyle.SEGMENTED_10, BarFlag.DARKEN_SKY);
                bossBar.addPlayer(player);
                bossBar.setProgress(1);
                if(deathMessage) {
                    Bukkit.broadcastMessage(header + ChatColor.RED + player.getName() + " " + ChatColor.GRAY + "died.");

                }
                int respawn = 5;
                player.sendTitle(ChatColor.RED + "You died...", ChatColor.GRAY + "Respawning in " + (respawn) + ChatColor.GRAY + " seconds...");
                //player.setMaxHealth(20);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setGameMode(GameMode.SPECTATOR);
                if(player.getKiller()!=null){
                    player.setSpectatorTarget(player.getKiller());
                    ActionBar.sendActionBar(player,ChatColor.GRAY + "You were killed by " + ChatColor.RED + player.getKiller().getName());
                }
                else{
                    Player p = null;
                    double dist = 10000000;
                    for(Player dec : Bukkit.getOnlinePlayers()){
                        if(dec==player || dec.getGameMode()== GameMode.SPECTATOR)
                            continue;
                        if(dec.getLocation().distance(player.getLocation())<dist){
                            p = dec;
                            dist = dec.getLocation().distance(player.getLocation());
                        }
                    }
                    if(p!=null){
                        player.setSpectatorTarget(p);
                        ActionBar.sendActionBar(player,ChatColor.GRAY + "You are now spectating " + ChatColor.RED + p.getName());
                    }
                }
                double step = 1.0 / (double) respawn;
                for (int i = respawn, time = 0; i >= 0; i--) {
                    final int sec = i;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                        bossBar.setTitle(ChatColor.GRAY + "Respawning in " + ChatColor.RED + sec + ChatColor.GRAY + " seconds...");
                        bossBar.setProgress(bossBar.getProgress() - step);
                    }, time * 20);
                    time++;
                }
                //List<ItemStack> items = new ArrayList<>();
                for (ItemStack i : player.getInventory().getContents()) {
                    if (i != null && i.getType()!=Material.DIAMOND_SWORD && i.getType()!=Material.COMPASS && !i.getType().toString().contains("AXE")) {
                        player.getLocation().getWorld().dropItem(player.getLocation(), i);
                    }
                }
                player.getInventory().setContents(new ItemStack[36]);

                for (ItemStack i : player.getInventory().getArmorContents()) {
                    if (i != null) {
                        player.getLocation().getWorld().dropItem(player.getLocation(), i);
                    }
                }
                player.getInventory().setArmorContents(new ItemStack[4]);
//                if (player.getLocation().distance(lobby) >= 75) {
//                    TNTPrimed primed = (TNTPrimed) player.getWorld().spawnEntity(player.getLocation(), EntityType.PRIMED_TNT);
//                    primed.setFuseTicks(0);
//                    primed.setGlowing(true);
//                }
                while(player.getLocation().getBlockY()<=10){
                    player.teleport(player.getLocation().add(0,15,0));
                }

                player.updateInventory();
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    bossBar.removePlayer(player);
                    player.setGameMode(GameMode.SURVIVAL);
                    if (arena.isGameStarted()) {
                        arena.teleportPlayerToSpawnPoint(player);
                    }
                    else {
                        player.teleport(lobby);
                    }
                    if(arena.isGameStarted()) {
                        arena.stockPlayer(player);
                    }
                }, 20 * respawn);
                noFallDamage.add(player);
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    noFallDamage.remove(player);
                }, 20 * (respawn + 10));
                return true;
            }
        return false;
    }
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){
        if(arena.isGameStarted()){
            event.setRespawnLocation(arena.getTeamSpawn(arena.getPlayerTeam(event.getPlayer())));
        }
    }
    private List<Player> noFallDamage = new ArrayList<>();
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event){
        if( event.getDamager() instanceof Arrow) {
            event.setDamage(100);
            if (deathManager((Player) event.getEntity(), event.getDamage(), true)) {
                event.setDamage(0);
                event.setCancelled(true);
            }
        }
        if(event.getEntity().getLocation().distance(lobby)<=75){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        if(event.getPlayer().getItemInHand().getType().toString().contains("AXE") && event.getClickedBlock()==null && event.getPlayer().isGliding() && arena.isGameStarted() && event.getPlayer().getLevel()>0 && event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType()==Material.AIR){
            if(arena.getClass(event.getPlayer())==ElytraClass.WARRIOR || arena.getClass(event.getPlayer())==ElytraClass.GHOST){
                event.getPlayer().setLevel(event.getPlayer().getLevel()-1);
                event.getPlayer().launchProjectile(Arrow.class);
            }
            else if(arena.getClass(event.getPlayer())==ElytraClass.TANK){
                event.getPlayer().setLevel(event.getPlayer().getLevel()-5);
                for(int i = 0; i<=10; i++){
                    event.getPlayer().launchProjectile(Arrow.class);
                }
            }
            else if(arena.getClass(event.getPlayer())==ElytraClass.HEALER){
                event.getPlayer().setLevel(event.getPlayer().getLevel()-3);
                for(int i = 0; i<=2; i++){
                    event.getPlayer().launchProjectile(Arrow.class);
                }
            }

        }
    }

    public List<Location> createPillar(ElytraTeamColor team, int height, Location location){
        List<Location> blocks = new ArrayList<>();
        for(int i = 0; i<=height; i++){
            blocks.addAll(setFloor(location,2));
            location.add(0,1,0);
        }
        return blocks;
    }

    @EventHandler
    public void onInteractInventory(InventoryInteractEvent event){
        if(arena.isGameStarted() && event.getWhoClicked().getGameMode()==GameMode.SURVIVAL){
            event.setCancelled(true);
        }
    }
    public void score(Player player){
        int score = 0;
        for(ItemStack i : player.getInventory().getContents()){
            if(i==null || i.getType()==Material.AIR)
                continue;
            if(i.getType()==Material.DIAMOND){
                score+=(i.getAmount() * 3);
            }
            if(i.getType()==Material.GOLD_INGOT){
                score+=(i.getAmount() * 1);
            }
            if(i.getType()==Material.IRON_INGOT){
                score+=(i.getAmount() * 2);
            }
        }
        if(score>0) {
            int pScore = 0;
            if(playerScores.containsKey(player)) {
                pScore+=playerScores.get(player);
                playerScores.remove(player);
            }
            pScore+=score;
            playerScores.put(player,pScore);
            Bukkit.broadcastMessage(header + ChatColor.valueOf(arena.getPlayerTeam(player).getTeamColor().toString()) + player.getName() + ChatColor.GRAY + " has just scored " + ChatColor.BLUE + score + ChatColor.GRAY + " points");
            player.getInventory().setContents(new ItemStack[36]);
            player.updateInventory();
            arena.stockPlayer(player);
            arena.getPlayerTeam(player).setScore(arena.getPlayerTeam(player).getScore() + score);
        }
    }
    @EventHandler
    public void onToggle(PlayerToggleFlightEvent event){
        if(event.isFlying() && event.getPlayer().getGameMode()==GameMode.SURVIVAL){
            CosmeticManager.activateDoubleJump(event.getPlayer());
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onMove(PlayerMoveEvent event){
        event.getPlayer().setCollidable(false);
        boolean f = false;
        event.getPlayer().getNearbyEntities(2,2,2).forEach((entity -> {}));
//        if(!arena.isGameStarted())
//        {
//            Vector vector = event.getTo().toVector().subtract(event.getFrom().toVector());
//            event.getPlayer().setVelocity(vector.multiply(5));
//        }
        if(event.getPlayer().getGameMode()==GameMode.SURVIVAL){
            if(CosmeticManager.isDoubleJumpEnabled()){
                event.getPlayer().setAllowFlight(true);
            }
            else{
                event.getPlayer().setAllowFlight(false);
            }
        }
        if(arena.getClass(event.getPlayer())==ElytraClass.GHOST && arena.isGameStarted()){
            if(event.getPlayer().isGliding()){
                event.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            if(event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType()!=Material.AIR){
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,5000,1,false,false));
            }
        }
        if(event.getPlayer().isGliding()){
            event.getPlayer().getWorld().spigot().playEffect(event.getTo(),Effect.COLOURED_DUST);
        }
        if(!arena.isGameStarted() && event.getTo().getBlockY()<0){
            event.getPlayer().teleport(lobby);
            event.getPlayer().setVelocity(new Vector(0,0,0));
        }
        event.getPlayer().setFoodLevel(20);
        if(event.getPlayer().getGameMode()== GameMode.SURVIVAL && arena.isGameStarted()){
            Player player = event.getPlayer();
            ElytraTeam team = arena.getPlayerTeam(player);
            if(team == arena.getRedTeam()){
                if(event.getTo().getBlock().getRelative(BlockFace.DOWN).getData()==14){
                    score(event.getPlayer());
                }
            }
            if(team == arena.getGreenTeam()){
                if(event.getTo().getBlock().getRelative(BlockFace.DOWN).getData()==13){
                    score(event.getPlayer());
                }
            }



            if(event.getFrom().distance(center)>=250 && event.getTo().distance(center)<=250){
                event.getPlayer().sendTitle("","");
            }
            boolean gogo = (event.getTo()).getBlock().getRelative(BlockFace.DOWN).getType()==Material.AIR ;
            boolean wearingBoots = true;
            boolean leveled = event.getPlayer().getLevel()>0;
            if(event.getTo().distance(lobby)<=75)
                event.getPlayer().setVelocity(event.getTo().toVector().subtract(lobby.toVector()));
            else{
                if(event.getTo().getBlock().getRelative(BlockFace.DOWN).getType()==Material.AIR && event.getPlayer().isSneaking() && wearingBoots && leveled){
                    event.getPlayer().setGliding(gogo);
                }
                if(!leveled){
                    event.getPlayer().setGliding(false);
                }
                if(event.getPlayer().isSneaking() && event.getPlayer().isGliding() && wearingBoots && leveled){
                    event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection());
                }
            }
        }
        if(event.getTo().getBlock().getType()!=Material.AIR && arena.isGameStarted()){
            Location location = event.getTo();
            while(location.getBlock().getType()!=Material.AIR && location.getBlock().getType().isSolid()&& !exceptions.contains(location.getBlock().getType())){
                location.add(0,1,0);
            }
            if(location.getBlock().getType().isSolid()&& !exceptions.contains(location.getBlock().getType())) {
                event.getPlayer().teleport(location);
            }
        }
    }
    public Collection<Location> setFloor(Location center, int radius) {
        List<Location> locations = new ArrayList<>();
        for (int xMod = -radius; xMod <= radius; xMod++) {
            for (int zMod = -radius; zMod <= radius; zMod++) {
                Block theBlock = center.getBlock().getRelative(xMod, 0, zMod);
                locations.add(theBlock.getLocation());
            }
        }
        return locations;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("start")){
            arena.start();
        }
        if(command.getName().equalsIgnoreCase("end")){
            arena.end();
        }
        if(command.getName().equalsIgnoreCase("queue")){
            arena.queue();
        }
        if(command.getName().equalsIgnoreCase("elytra")){
            if(args.length==0) {
                sendHelp(sender);
                return true;
            }
            if(args[0].equalsIgnoreCase("set")){
                if(args.length!=3 ||!(sender instanceof Player)) {
                    sendHelp(sender);
                    return true;
                }
                if(args[1].equalsIgnoreCase("red")){
                    Player player = (Player)sender;
                    Location location = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
                    if(args[2].equalsIgnoreCase("spawn")){
                        LocationManager.setLocation("Arena.RedSpawn",location.add(0,1,0));
                        player.sendMessage(header + "Set " + ChatColor.RED + "Red" + " " + ChatColor.GRAY + "spawn location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                    }
                    else if(args[2].equalsIgnoreCase("gem")){
                        LocationManager.setLocation("Arena.RedGems",location);
                        player.sendMessage(header + "Set " + ChatColor.RED + "Red" + " " + ChatColor.GRAY + "gem location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");

                    }
                    else{
                        sendHelp(sender);
                    }
                     return true;
                }
                else if(args[1].equalsIgnoreCase("green")){
                    Player player = (Player)sender;
                    Location location = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
                    if(args[2].equalsIgnoreCase("spawn")){
                        LocationManager.setLocation("Arena.GreenSpawn",location.add(0,1,0));
                        player.sendMessage(header + "Set " + ChatColor.GREEN + "Green" + " " + ChatColor.GRAY + "spawn location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                    }
                    else if(args[2].equalsIgnoreCase("gem")){
                        LocationManager.setLocation("Arena.GreenGems",location);
                        player.sendMessage(header + "Set " + ChatColor.GREEN + "Green" + " " + ChatColor.GRAY + "gem location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");

                    }
                    else{
                        sendHelp(sender);
                    }
                    return true;
                }
                else{
                    sendHelp(sender);
                    return true;
                }
            }
            else if(args[0].equalsIgnoreCase("update")){
                if(args[1].equalsIgnoreCase("lobby")){
                    Location location = (sender instanceof Player ? ((Player)sender).getLocation() : null);
                    if(location==null){
                        sender.sendMessage(header + "You must be a player to preform this command");
                        return true;
                    }
                    LocationManager.setLocation("Arena.Lobby",location);
                    sender.sendMessage(header + "Set " + ChatColor.BLUE + "Lobby" + " " + ChatColor.GRAY + "location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                    updateLocations();
                    return true;
                }
                else if(args[1].equalsIgnoreCase("center")){
                    Location location = (sender instanceof Player ? ((Player)sender).getLocation() : null);
                    if(location==null){
                        sender.sendMessage(header + "You must be a player to preform this command");
                        return true;
                    }
                    LocationManager.setLocation("Arena.Center",location);
                    sender.sendMessage(header + "Set " + ChatColor.BLUE + "Center" + " " + ChatColor.GRAY + "location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                    updateLocations();
                    return true;
                }
                else if(args[1].equalsIgnoreCase("red")){
                    //sheepcode
                    if(sender instanceof Player){
                        Player player = (Player)sender;
                        Location location = player.getLocation();
                        LocationManager.setLocation("Arena.Sheep.Red",location);
                        sender.sendMessage(header + "Set " + ChatColor.RED + "Red" + " " + ChatColor.GRAY + " sheep spawn location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                        updateSheep();
                        return true;
                    }
                    else{
                        sendHelp(sender);
                        return true;
                    }
                }
                else if(args[1].equalsIgnoreCase("green")){
                    if(sender instanceof Player){
                        Player player = (Player)sender;
                        Location location = player.getLocation();
                        LocationManager.setLocation("Arena.Sheep.Green",location);
                        sender.sendMessage(header + "Set " + ChatColor.GREEN + "Green" + " " + ChatColor.GRAY + " sheep spawn location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                        updateSheep();
                        return true;
                    }
                    else{
                        sendHelp(sender);
                        return true;
                    }
                }
                else if(args[1].equalsIgnoreCase("tank")){
                    if(sender instanceof Player){
                        Player player = (Player)sender;
                        Location location = player.getLocation();
                        LocationManager.setLocation("Arena.Sheep.Tank",location);
                        sender.sendMessage(header + "Set " + ChatColor.AQUA + "Tank" + " " + ChatColor.GRAY + " sheep spawn location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                        updateSheep();
                        return true;
                    }
                    else{
                        sendHelp(sender);
                        return true;
                    }
                }
                else if(args[1].equalsIgnoreCase("warrior")){
                    if(sender instanceof Player){
                        Player player = (Player)sender;
                        Location location = player.getLocation();
                        LocationManager.setLocation("Arena.Sheep.Warrior",location);
                        sender.sendMessage(header + "Set " + ChatColor.DARK_AQUA + "Warrior" + " " + ChatColor.GRAY + " sheep spawn location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                        updateSheep();
                        return true;
                    }
                    else{
                        sendHelp(sender);
                        return true;
                    }
                }
                else if(args[1].equalsIgnoreCase("ghost")){
                    if(sender instanceof Player){
                        Player player = (Player)sender;
                        Location location = player.getLocation();
                        LocationManager.setLocation("Arena.Sheep.Ghost",location);
                        sender.sendMessage(header + "Set " + ChatColor.WHITE + "Ghost" + " " + ChatColor.GRAY + " sheep spawn location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                        updateSheep();
                        return true;
                    }
                    else{
                        sendHelp(sender);
                        return true;
                    }
                }
                else if(args[1].equalsIgnoreCase("healer")){
                    if(sender instanceof Player){
                        Player player = (Player)sender;
                        Location location = player.getLocation();
                        LocationManager.setLocation("Arena.Sheep.Healer",location);
                        sender.sendMessage(header + "Set " + ChatColor.WHITE + "Healer" + " " + ChatColor.GRAY + " sheep spawn location. (" + ChatColor.RED + location.getBlockX() + ", " + location.getBlockY()+", " + location.getBlockZ() + ChatColor.GRAY + ")");
                        updateSheep();
                        return true;
                    }
                    else{
                        sendHelp(sender);
                        return true;
                    }
                }
                else{
                    sendHelp(sender);
                }
                return true;
            }
        }
        return true;
    }
    public void updateSheep(){
//        for(Entity e : Bukkit.getWorld("world").getEntitiesByClass(Sheep.class)){
//            Sheep s = (Sheep)e;
//            String strippedName = ChatColor.stripColor(s.getCustomName());
//            if(strippedName.equalsIgnoreCase("Red") || strippedName.equalsIgnoreCase("Green")){
//                e.remove();
//            }
//        }
        NPCManager.cleanup();


        int redTeam = NPCManager.createSheep(ChatColor.RED + "" + ChatColor.BOLD + "Red", LocationManager.getLocation("Arena.Sheep.Red"), DyeColor.RED, new Interaction() {
            @Override
            public void onInteract(Player player) {
                arena.getRedTeam().join(player);
            }
        });
        int greenTeam = NPCManager.createSheep(ChatColor.GREEN + "" + ChatColor.BOLD + "Green", LocationManager.getLocation("Arena.Sheep.Green"), DyeColor.GREEN, new Interaction() {
            @Override
            public void onInteract(Player player) {
                arena.getGreenTeam().join(player);
            }
        });
        int warriorClass = NPCManager.createZombie(ChatColor.GOLD + "" + ChatColor.BOLD + "Warrior Class", LocationManager.getLocation("Arena.Sheep.Warrior"), new ItemStack(Material.STONE_AXE), new ItemStack(Material.STONE_AXE), new Interaction() {
            @Override
            public void onInteract(Player player) {
                arena.selectClass(player,ElytraClass.WARRIOR);
            }
        }, false);
        int tankClass = NPCManager.createZombie(ChatColor.GOLD + "" + ChatColor.BOLD + "Tank Class", LocationManager.getLocation("Arena.Sheep.Tank"), new ItemStack(Material.DIAMOND_AXE), new ItemStack(Material.DIAMOND_AXE), new Interaction() {
            @Override
            public void onInteract(Player player) {
                arena.selectClass(player,ElytraClass.TANK);
            }
        }, false);
        int ghostClass = NPCManager.createZombie(ChatColor.GOLD + "" + ChatColor.BOLD + "Ghost Class", LocationManager.getLocation("Arena.Sheep.Ghost"), new ItemStack(Material.SPLASH_POTION, 1), new ItemStack(Material.STONE_AXE, 1), new Interaction() {
            @Override
            public void onInteract(Player player) {
                arena.selectClass(player,ElytraClass.GHOST);
            }
        },false);
        int healerClass = NPCManager.createZombie(ChatColor.GOLD + "" + ChatColor.BOLD + "Healer Class", LocationManager.getLocation("Arena.Sheep.Healer"), new ItemStack(Material.POTION, 1), new ItemStack(Material.STONE_AXE, 1), new Interaction() {
            @Override
            public void onInteract(Player player) {
                arena.selectClass(player,ElytraClass.HEALER);
            }
        },false);
    }
    @EventHandler
    public void entityCombust(EntityCombustEvent event){
        event.setCancelled(true);
    }
    public void sendHelp(CommandSender sender){
        sender.sendMessage(header + "I'm sorry, you have run an invalid command.");
        HashMap<String,String> commands = new HashMap<>();
        commands.put("/elytra set [red/green] [gem/spawn]","Set the [gem] spawn location for a team");
        commands.put("/elytra update [lobby/center/red/green/warrior/tank/ghost]","Update a spawn point");

        for(String s : commands.keySet()){
            sender.sendMessage(header + s + " " + ChatColor.GRAY + ": " + ChatColor.RED + commands.get(s));
        }
    }
}
