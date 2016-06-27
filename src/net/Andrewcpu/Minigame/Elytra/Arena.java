package net.Andrewcpu.Minigame.Elytra;

import net.Andrewcpu.MinigameAPI.GemManager;
import net.Andrewcpu.MinigameAPI.cosmeds.CosmeticManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Created by stein on 4/10/2016.
 */
public class Arena {
    private boolean gameStarted = false;
    private ElytraTeam redTeam = new ElytraTeam(ElytraTeamColor.RED);
    private ElytraTeam greenTeam = new ElytraTeam(ElytraTeamColor.GREEN);
    private int maxPlayers = 10;
    private double max_time = 300;
    private int time = 0;
    private HashMap<Player,ElytraClass> playerClasses = new HashMap<>();
    
    public void selectClass(Player player, ElytraClass elytraClass){
        if(playerClasses.containsKey(player)){
            if(playerClasses.get(player)==elytraClass)
                return;
            playerClasses.remove(player);
        }
        playerClasses.put(player,elytraClass);
        String sep = ChatColor.GREEN + "" + ChatColor.STRIKETHROUGH + "=====================";
        if(elytraClass==ElytraClass.WARRIOR){
            player.sendMessage(sep);

            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "     Kit " + ChatColor.RED + elytraClass.name().toString());
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "     20HP");
            player.sendMessage(ChatColor.RED + "     3XP / sec");
            player.sendMessage(ChatColor.RED + "     1 Arrow per Shot");
            player.sendMessage("");

            player.sendMessage(sep);
        }
        else if(elytraClass==ElytraClass.GHOST){
            player.sendMessage(sep);

            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "     Kit " + ChatColor.RED + elytraClass.name().toString());
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "     5HP");
            player.sendMessage(ChatColor.RED + "     2XP / sec");
            player.sendMessage(ChatColor.RED + "     1 Arrow per Shot");
            player.sendMessage(ChatColor.RED + "     Invisible on Land");
            player.sendMessage("");

            player.sendMessage(sep);
        }
        else if(elytraClass==ElytraClass.TANK){
            player.sendMessage(sep);

            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "     Kit " + ChatColor.RED + elytraClass.name().toString());
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "     10HP");
            player.sendMessage(ChatColor.RED + "     1XP / sec");
            player.sendMessage(ChatColor.RED + "     10 Arrow per Shot");
            player.sendMessage("");

            player.sendMessage(sep);
        }
        else if(elytraClass==ElytraClass.HEALER){
            player.sendMessage(sep);

            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "     Kit " + ChatColor.RED + elytraClass.name().toString());
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "     5HP");
            player.sendMessage(ChatColor.RED + "     5XP / sec");
            player.sendMessage(ChatColor.RED + "     3 Arrow per Shot");
            player.sendMessage(ChatColor.RED + "     Heals Nearby Teammates");
            player.sendMessage("");

            player.sendMessage(sep);
        }
        player.sendMessage(ChatColor.BLUE + "Kit> "+ChatColor.GRAY + "You have selected the " + ChatColor.RED + elytraClass.name() + " " + ChatColor.GRAY + "class");
    }
    
    public ElytraClass getClass(Player player){
        return playerClasses.get(player);
    }

    public ElytraTeam getRedTeam() {
        return redTeam;
    }

    public void setRedTeam(ElytraTeam redTeam) {
        this.redTeam = redTeam;
    }

    public ElytraTeam getGreenTeam() {
        return greenTeam;
    }

    public void setGreenTeam(ElytraTeam greenTeam) {
        this.greenTeam = greenTeam;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public void start(){
        time = (int)max_time;
        Bukkit.broadcastMessage(Main.header + "Starting game...");
        Main.getInstance().getInfoBar().setProgress(1.0);
        Main.getInstance().getInfoBar().setTitle(ChatColor.RED + "" + ChatColor.BOLD + "Fight!");
        gameStarted = true;

        for(Player player : Bukkit.getOnlinePlayers()){
            if(getClass(player)==ElytraClass.TANK){
                player.setMaxHealth(20);
            }
            if(getClass(player)==ElytraClass.WARRIOR){
                player.setMaxHealth(40);
            }
            if(getClass(player)==ElytraClass.GHOST || getClass(player)==ElytraClass.HEALER){
                player.setMaxHealth(10);
            }
            stockPlayer(player);
            teleportPlayerToSpawnPoint(player);
            player.setFoodLevel(20);
            player.setHealth(player.getMaxHealth());
            player.setLevel(0);
            player.setExp(0);
        }
        CosmeticManager.setDoubleJumpEnabled(false);
    }
    public void stockPlayer(Player player){
        ItemStack compass = new ItemStack(Material.COMPASS,1);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(ChatColor.valueOf(getPlayerTeam(player).getTeamColor().toString()) + "" + ChatColor.BOLD + "Opponent Base");
        compass.setItemMeta(meta);
        player.setCompassTarget(getOppositeSpawn(getPlayerTeam(player)));
        player.getInventory().setItem(0,compass);
        player.getInventory().setItem(8,new ItemStack(Material.DIAMOND_SWORD,1));
        if(getClass(player)==ElytraClass.TANK){
            ItemStack tankAxe = new ItemStack(Material.DIAMOND_AXE, 1);
            ItemMeta meta1 = tankAxe.getItemMeta();
            meta1.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Fire Arrow " + ChatColor.GRAY + "(" + ChatColor.YELLOW +"x10" + ChatColor.GRAY + ")");
            tankAxe.setItemMeta(meta1);
            player.getInventory().setItem(4,tankAxe);
        } 
        else if(getClass(player)==ElytraClass.WARRIOR || getClass(player)==ElytraClass.GHOST){
            ItemStack warriorAxe = new ItemStack(Material.STONE_AXE, 1);
            ItemMeta meta1 = warriorAxe.getItemMeta();
            meta1.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Fire Arrow " + ChatColor.GRAY + "(" + ChatColor.YELLOW +"x1" + ChatColor.GRAY + ")");
            warriorAxe.setItemMeta(meta1);
            player.getInventory().setItem(4,warriorAxe);
        }
    }

    public void teleportPlayerToSpawnPoint(Player player){
        Location redSpawn = LocationManager.getLocation("Arena.RedSpawn");
        Location greenSpawn = LocationManager.getLocation("Arena.GreenSpawn");
        if(getPlayerTeam(player)==getRedTeam()){
            player.teleport(redSpawn);
        }
        if(getPlayerTeam(player)==getGreenTeam()){
            player.teleport(greenSpawn);
        }
    }
    public Location getTeamSpawn(ElytraTeam elytraTeam){
        if(elytraTeam.getTeamColor()==ElytraTeamColor.GREEN){
           return LocationManager.getLocation("Arena.GreenSpawn");
        }
        else{
            return LocationManager.getLocation("Arena.RedSpawn");
        }
    }
    public Location getOppositeSpawn(ElytraTeam elytraTeam){
        if(elytraTeam.getTeamColor()==ElytraTeamColor.GREEN){
           return LocationManager.getLocation("Arena.RedSpawn");
        }
        else{
            return LocationManager.getLocation("Arena.GreenSpawn");
        }
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public double getMax_time() {
        return max_time;
    }

    public void setMax_time(double max_time) {
        this.max_time = max_time;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void end(){
        CosmeticManager.setDoubleJumpEnabled(true);
        Bukkit.broadcastMessage(Main.header + "Game over!");
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> resetField(), 20 * 5);
        setGameStarted(false);
        ElytraTeam winningTeam = getWinningTeam();
        if(winningTeam==null){
            Bukkit.broadcastMessage(Main.header + "The game ended in a tie!");
        }
        else{
            Bukkit.broadcastMessage(Main.header + "The " + ChatColor.valueOf(winningTeam.getTeamColor().toString()) + winningTeam.getTeamColor().toString() +  ChatColor.YELLOW + " team has won the game!");
        }
        Main.getInstance().resetBossBar();
        for(Entity e : Bukkit.getWorld("world").getEntitiesByClass(Item.class)){
            e.remove();
        }
        for(Player player : Bukkit.getOnlinePlayers()){
            for(PotionEffect potionEffect : player.getActivePotionEffects()){
                player.removePotionEffect(potionEffect.getType());
            }
            awardGems(player);
            player.setMaxHealth(20);
            player.setHealth(20);
            Main.getInstance().deathManager(player,100, false);
            player.setLevel(0);
        }
        Main.getInstance().updateGems();
        time = (int)max_time;
        getRedTeam().setScore(0);
        getGreenTeam().setScore(0);
        Main.getInstance().playerScores.clear();
        Main.getInstance().captureTime.clear();

    }
    public void awardGems(Player player){
        int gems = 0;
        String seperator = ChatColor.GREEN + "" + ChatColor.STRIKETHROUGH + "=======================";
        player.sendMessage(seperator);
        player.sendMessage(ChatColor.DARK_GREEN + "+50 " + ChatColor.YELLOW + "For participation");
        if(Main.getInstance().captureTime.containsKey(player)){
            player.sendMessage(ChatColor.DARK_GREEN + "+" + (Main.getInstance().captureTime.get(player) * 3) + ChatColor.YELLOW + " For capturing the center");
            gems+=(Main.getInstance().captureTime.get(player) * 3);
        }
        if(Main.getInstance().playerScores.containsKey(player)){
            player.sendMessage(ChatColor.DARK_GREEN + "+" + (Main.getInstance().playerScores.get(player) * 2) + ChatColor.YELLOW + " For scoring points");
            gems+=(Main.getInstance().playerScores.get(player) * 2);
        }
        gems+=50;
        if(getWinningTeam()==null || getWinningTeam().getPlayers().contains(player)) {
            player.sendMessage(ChatColor.DARK_GREEN + "+" + getWinningTeam().getScore() + " " + ChatColor.YELLOW + "For " + ((getWinningTeam() == null ? "tying" : "winning")));
            gems+=getWinningTeam().getScore();
        }
        player.sendMessage(seperator);
        player.sendMessage(Main.header + "You earned " + ChatColor.GREEN + gems + " Gems");
        try {
            GemManager.setGems(player, GemManager.getGems(player) + gems);
        } catch (Exception e) {
            Bukkit.broadcastMessage(Main.header + "");
        }
    }
    public ElytraTeam getWinningTeam(){
        if(getRedTeam().getScore()>getGreenTeam().getScore()){
            return getRedTeam();
        }
        else if(getGreenTeam().getScore()>getRedTeam().getScore()){
            return getGreenTeam();
        }
        else
        {
            return null;
        }
    }
    public void queue(){
        Bukkit.broadcastMessage(Main.header + "Setting up players...");
        for(Player player : Bukkit.getOnlinePlayers()){
            player.setMaxHealth(20);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.getInventory().addItem(new ItemStack(Material.WOOL, 4, (byte)14));
            player.getInventory().addItem(new ItemStack(Material.WOOL, 4, (byte)5));
            player.teleport(player.getWorld().getSpawnLocation());
        }
        double step = 1.0/60.0;
        Main.getInstance().getInfoBar().setTitle(ChatColor.AQUA + "" + ChatColor.BOLD + "Place your defenses... ");
        for(int i = 0, time = 60; i<=60; i++){
            final int t = time;
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(),() ->{
                Main.getInstance().getInfoBar().setTitle(ChatColor.AQUA + "" + ChatColor.BOLD + "Place your defenses " + ChatColor.YELLOW + t + "/60 seconds");
                if(Main.getInstance().getInfoBar().getProgress()-step<0){
                    return;
                }
                Main.getInstance().getInfoBar().setProgress(Main.getInstance().getInfoBar().getProgress()-step);
            },time * 20);
            time--;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> start(), 61 * 20);
    }
    public List<Player> getPlayers(){
        List<Player> players = new ArrayList<>();
        for(Player p : getGreenTeam().getPlayers()){
            players.add(p);
        }
        for(Player p : getRedTeam().getPlayers()){
            players.add(p);
        }
        return players;
    }
    public void tick(){
        if(isGameStarted()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setScoreboard(Main.getInstance().getGameScoreboard(p));
                if(getClass(p)==ElytraClass.GHOST && p.hasPotionEffect(PotionEffectType.INVISIBILITY) && p.getGameMode()== GameMode.SURVIVAL){
                    Location loc = p.getLocation();
                    loc.add(0,0.1,0);
                    loc.getWorld().playEffect(loc, Effect.FOOTSTEP, 100);
                }
                if(getClass(p)==ElytraClass.HEALER){
                    for(Entity e : p.getNearbyEntities(7,7,7)){
                        if(e instanceof Player){
                            Player player = (Player)e;
                            if(getPlayerTeam(player)==getPlayerTeam(p) && getClass(player)!=ElytraClass.HEALER && player!=p){
                                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 10, 3));
                                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10, 3));
                            }
                        }
                    }
                }
            }
            if(Main.getInstance().getDominatingTeam()!=null){
                Main.getInstance().getDominatingTeam().setScore(Main.getInstance().getDominatingTeam().getScore()+5);
                for(Player player : Main.getInstance().getDominatingTeam().getPlayers()){
                    if(player.getLocation().distance(Main.getInstance().center)<=15){
                        int captureTime =(Main.getInstance().captureTime.containsKey(player) ? Main.getInstance().captureTime.get(player) : 0);
                        if(Main.getInstance().captureTime.containsKey(player))
                            Main.getInstance().captureTime.remove(player);
                        captureTime++;
                        Main.getInstance().captureTime.put(player,captureTime);
                        Collection<Location> locations = Main.getInstance().setFloor(player.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation(),2);
                        for(Location location : locations){
                            if(location.getBlock().getType()==Material.STAINED_CLAY){
                                location.getBlock().setData((byte)((getPlayerTeam(player).getTeamColor()==ElytraTeamColor.RED) ? 14 : 13));
                            }
                        }
                    }
                }
            }
            spawnGems();
            time--;
            if(time<=0){
                end();
            }
        }
    }
    private Random random = new Random();
    public void spawnGems(){
        Location redGems = LocationManager.getLocation("Arena.RedGems").add(0,1,0);
        Location greenGems = LocationManager.getLocation("Arena.GreenGems").add(0,1,0);
        int i = random.nextInt(3);
        int amount = random.nextInt(2);
        ItemStack randomItemStack;
        if(i==0){
            randomItemStack = new ItemStack(Material.DIAMOND,amount);
        }
        else if(i==1){
            randomItemStack = new ItemStack(Material.IRON_INGOT,amount);
        }
        else{
            randomItemStack = new ItemStack(Material.GOLD_INGOT,amount);
        }
        redGems.getWorld().dropItem(redGems, randomItemStack);
        greenGems.getWorld().dropItem(greenGems, randomItemStack);
    }
    public void resetField(){
        Bukkit.broadcastMessage(Main.header + "Resetting field...");
        for(Collection<Location> locations : redTeam.getPillars()){
            for(Location location : locations){
                location.getBlock().setType(Material.AIR);
            }
        }
        for(Collection<Location> locations : greenTeam.getPillars()){
            for(Location location : locations){
                location.getBlock().setType(Material.AIR);
            }
        }
    }
    public ElytraTeam getPlayerTeam(Player player){
        if(getRedTeam().getPlayers().contains(player)){
            return getRedTeam();
        }
        if(getGreenTeam().getPlayers().contains(player)){
            return getGreenTeam();
        }
        return null;
    }
}
