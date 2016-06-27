package net.Andrewcpu.Minigame.Elytra;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by stein on 4/10/2016.
 */
public class ElytraTeam {
    private Collection<Collection<Location>> pillars = new ArrayList<>();
    private ElytraTeamColor teamColor;
    private List<Player> players = new ArrayList<>();
    private int score = 0;

    public ElytraTeam(ElytraTeamColor teamColor) {
        this.teamColor = teamColor;
    }

    public void addPillar(Collection<Location> locations){
        Collection<Collection<Location>> locs = getPillars();
        locs.add(locations);
        setPillars(locs);
    }

    public int getScore() {
        return score;
    }
    public void join(Player player){
        if(players.contains(player))
            return;
        if(Main.getInstance().arena.getPlayerTeam(player)!=null){
            Main.getInstance().arena.getPlayerTeam(player).removePlayer(player);
        }
        for(Player player1 : getPlayers()){
            player1.sendMessage(Main.header + ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " has joined your team.");
        }
        player.sendMessage(ChatColor.BLUE + "Team> " +ChatColor.GRAY+ "You have joined the " + ChatColor.valueOf(teamColor.toString()) +  teamColor.toString() + ChatColor.GRAY + " team.");
        players.add(player);
    }
    public void removePlayer(Player player){
        players.remove(player);
    }
    public void setScore(int score) {
        this.score = score;
    }

    public int getCurrentSize(){
        return getPlayers().size();
    }

    public Collection<Collection<Location>> getPillars() {
        return pillars;
    }

    public void setPillars(Collection<Collection<Location>> pillars) {
        this.pillars = pillars;
    }

    public ElytraTeamColor getTeamColor() {
        return teamColor;
    }

    public void setTeamColor(ElytraTeamColor teamColor) {
        this.teamColor = teamColor;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }
}
