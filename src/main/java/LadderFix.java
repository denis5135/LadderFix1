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
        
        new BukkitRunnable() {
            public void run() {
                for (UUID uuid : onLadder) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline() && !p.isDead()) {
                        // Только для 1.8.9
                        if (getClientVersion(p) <= 340) {
                            fixLegacyLadder(p);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
        
        getLogger().info("LadderFix enabled - Fixed for 1.8.9!");
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        
        if (isOnLadder(p)) {
            onLadder.add(p.getUniqueId());
            
            // Только для 1.8.9
            if (getClientVersion(p) <= 340) {
                Location loc = p.getLocation();
                Location last = lastLoc.get(p.getUniqueId());
                
                if (last != null) {
                    double dy = loc.getY() - last.getY();
                    double dx = Math.abs(loc.getX() - last.getX());
                    double dz = Math.abs(loc.getZ() - last.getZ());
                    
                    // Игрок пытается двигаться горизонтально, но Y не меняется = ЗАСТРЯЛ В ХИТБОКСЕ
                    if ((dx > 0.01 || dz > 0.01) && Math.abs(dy) < 0.001) {
                        int ticks = stuckTicks.getOrDefault(p.getUniqueId(), 0) + 1;
                        stuckTicks.put(p.getUniqueId(), ticks);
                        
                        if (ticks >= 2) {
                            // Выталкиваем игрока ИЗ лестницы в сторону движения
                            Vector dir = p.getLocation().getDirection();
                            dir.setY(0);
                            dir.normalize();
                            p.setVelocity(dir.multiply(0.3));
                            stuckTicks.put(p.getUniqueId(), 0);
                        }
                    } else {
                        stuckTicks.remove(p.getUniqueId());
                    }
                }
                lastLoc.put(p.getUniqueId(), loc.clone());
            }
        } else {
            onLadder.remove(p.getUniqueId());
            lastLoc.remove(p.getUniqueId());
            stuckTicks.remove(p.getUniqueId());
        }
    }
    
    private boolean isOnLadder(Player p) {
        Location loc = p.getLocation();
        return loc.getBlock().getType() == Material.LADDER ||
               loc.clone().add(0, 1, 0).getBlock().getType() == Material.LADDER ||
               loc.clone().subtract(0, 0.1, 0).getBlock().getType() == Material.LADDER;
    }
    
    private void fixLegacyLadder(Player p) {
        Vector vel = p.getVelocity();
        float pitch = p.getLocation().getPitch();
        
        // W + взгляд вверх = ПОДЪЁМ
        if (isMovingForward(p) && pitch < -30) {
            p.setVelocity(new Vector(vel.getX(), 0.2, vel.getZ()));
        }
        // S + взгляд вниз = СПУСК
        else if (isMovingBackward(p) && pitch > 30) {
            p.setVelocity(new Vector(vel.getX(), -0.2, vel.getZ()));
        }
        // Shift = СПУСК
        else if (p.isSneaking()) {
            p.setVelocity(new Vector(vel.getX(), -0.25, vel.getZ()));
        }
        // НЕ ДЕРЖИМ ИГРОКА! Пусть падает если не двигается
        // (убираем удержание)
    }
    
    private boolean isMovingForward(Player p) {
        Vector dir = p.getLocation().getDirection();
        Vector vel = p.getVelocity();
        return dir.getX() * vel.getX() + dir.getZ() * vel.getZ() > 0.02;
    }
    
    private boolean isMovingBackward(Player p) {
        Vector dir = p.getLocation().getDirection();
        Vector vel = p.getVelocity();
        return dir.getX() * vel.getX() + dir.getZ() * vel.getZ() < -0.02;
    }
    
    private int getClientVersion(Player p) {
        try {
            Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
            Object api = via.getMethod("getAPI").invoke(null);
            Object ver = api.getClass().getMethod("getPlayerVersion", Player.class).invoke(api, p);
            return ver != null ? (int) ver : 47;
        } catch (Exception e) {
            return 47;
        }
    }
}
