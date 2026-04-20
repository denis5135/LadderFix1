import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class LadderFix extends JavaPlugin implements Listener {
    
    private Map<UUID, Location> lastLoc = new HashMap<>();
    private Map<UUID, Integer> stuck = new HashMap<>();
    
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        new BukkitRunnable() {
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getLocation().getBlock().getType() == Material.LADDER) {
                        if (p.getVelocity().getY() <= 0 && p.getLocation().getPitch() < -30) {
                            p.setVelocity(p.getVelocity().setY(0.1));
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
        
        getLogger().info("LadderFix enabled!");
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location loc = p.getLocation();
        
        if (loc.getBlock().getType() != Material.LADDER) return;
        
        Location last = lastLoc.get(p.getUniqueId());
        if (last == null) {
            lastLoc.put(p.getUniqueId(), loc.clone());
            return;
        }
        
        double dy = loc.getY() - last.getY();
        double dx = Math.abs(loc.getX() - last.getX());
        double dz = Math.abs(loc.getZ() - last.getZ());
        
        if ((dx > 0.01 || dz > 0.01) && dy < 0.001) {
            int ticks = stuck.getOrDefault(p.getUniqueId(), 0) + 1;
            stuck.put(p.getUniqueId(), ticks);
            if (ticks >= 2) {
                p.setVelocity(p.getVelocity().setY(0.08));
                stuck.put(p.getUniqueId(), 0);
            }
        } else {
            stuck.remove(p.getUniqueId());
        }
        
        lastLoc.put(p.getUniqueId(), loc.clone());
    }
}