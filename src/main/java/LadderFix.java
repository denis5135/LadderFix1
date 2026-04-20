import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class LadderFix extends JavaPlugin implements Listener {
    
    private Set<UUID> onLadder = new HashSet<>();
    private Map<UUID, Location> lastLoc = new HashMap<>();
    private Map<UUID, Integer> stuckTicks = new HashMap<>();
    
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Периодическая помощь при подъёме
        new BukkitRunnable() {
            public void run() {
                for (UUID uuid : onLadder) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline() && !p.isDead()) {
                        helpClimb(p);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
        
        getLogger().info("LadderFix enabled - Fixed ladder climbing!");
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location loc = p.getLocation();
        Material block = loc.getBlock().getType();
        Material blockBelow = loc.clone().subtract(0, 0.1, 0).getBlock().getType();
        
        // Проверяем, на лестнице ли игрок
        boolean isOnLadderBlock = block == Material.LADDER || blockBelow == Material.LADDER;
        
        if (isOnLadderBlock) {
            onLadder.add(p.getUniqueId());
            
            // Проверяем застревание
            Location last = lastLoc.get(p.getUniqueId());
            if (last != null) {
                double dy = loc.getY() - last.getY();
                double dx = Math.abs(loc.getX() - last.getX());
                double dz = Math.abs(loc.getZ() - last.getZ());
                
                // Если игрок двигается горизонтально, но не вертикально = застрял
                if ((dx > 0.01 || dz > 0.01) && Math.abs(dy) < 0.001) {
                    int ticks = stuckTicks.getOrDefault(p.getUniqueId(), 0) + 1;
                    stuckTicks.put(p.getUniqueId(), ticks);
                    
                    if (ticks >= 2) {
                        // Даём толчок вверх
                        p.setVelocity(new Vector(0, 0.15, 0));
                        stuckTicks.put(p.getUniqueId(), 0);
                    }
                } else {
                    stuckTicks.remove(p.getUniqueId());
                }
            }
            lastLoc.put(p.getUniqueId(), loc.clone());
            
        } else {
            onLadder.remove(p.getUniqueId());
            lastLoc.remove(p.getUniqueId());
            stuckTicks.remove(p.getUniqueId());
        }
    }
    
    private void helpClimb(Player p) {
        Vector vel = p.getVelocity();
        
        // Если игрок на лестнице и пытается подняться
        // W + взгляд вверх = подъём
        if (isMovingForward(p) && p.getLocation().getPitch() < -30) {
            p.setVelocity(new Vector(vel.getX(), 0.2, vel.getZ()));
        }
        // S + взгляд вниз = спуск
        else if (isMovingBackward(p) && p.getLocation().getPitch() > 30) {
            p.setVelocity(new Vector(vel.getX(), -0.2, vel.getZ()));
        }
        // Shift = спуск
        else if (p.isSneaking()) {
            p.setVelocity(new Vector(vel.getX(), -0.25, vel.getZ()));
        }
        // Просто стоит на лестнице — не падаем
        else if (vel.getY() < 0 && !p.isOnGround()) {
            p.setVelocity(new Vector(vel.getX(), 0, vel.getZ()));
        }
    }
    
    private boolean isMovingForward(Player p) {
        Vector dir = p.getLocation().getDirection();
        Vector vel = p.getVelocity();
        return dir.getX() * vel.getX() + dir.getZ() * vel.getZ() > 0.03;
    }
    
    private boolean isMovingBackward(Player p) {
        Vector dir = p.getLocation().getDirection();
        Vector vel = p.getVelocity();
        return dir.getX() * vel.getX() + dir.getZ() * vel.getZ() < -0.03;
    }
}
