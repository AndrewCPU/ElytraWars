package net.Andrewcpu.Minigame.Elytra;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.awt.geom.Arc2D;

/**
 * Created by stein on 4/16/2016.
 */
public class LocationManager {
    public static Location getLocation(String loc){
        int x = Main.getInstance().getConfig().getInt(loc + ".X");
        int y = Main.getInstance().getConfig().getInt(loc + ".Y");
        int z = Main.getInstance().getConfig().getInt(loc + ".Z");
        float yaw = 0, pitch = 0;
        World world = Bukkit.getWorld("world");
        Location location = new Location(world,x,y,z);
        if(Main.getInstance().getConfig().isSet(loc + ".Yaw")) {
            yaw = Float.valueOf(((double)Main.getInstance().getConfig().get(loc + ".Yaw")) + "");
            pitch = Float.valueOf((double) Main.getInstance().getConfig().get(loc + ".Pitch") + "");
            location.setYaw(yaw);
            location.setPitch(pitch);
        //    Bukkit.broadcastMessage(location.toString());
        }
        return location;
    }
    public static void setLocation(String loc, Location location){
        FileConfiguration config = Main.getInstance().getConfig();
        config.set(loc + ".World",location.getWorld().getName());
        config.set(loc + ".X",location.getBlockX());
        config.set(loc + ".Y",location.getBlockY());
        config.set(loc + ".Z",location.getBlockZ());
        config.set(loc + ".Yaw",location.getYaw());
        config.set(loc + ".Pitch", location.getPitch());
        Main.getInstance().saveConfig();
        Main.getInstance().reloadConfig();
    }
}
