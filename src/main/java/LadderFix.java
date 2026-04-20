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
    private Map<UUID, Integer> climbTick = new HashMap<>();
    
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Плавный подъём
        new BukkitRunnable() {
            public void run() {
                for (UUID uuid : onLadder) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline() && !p.isDead()) {
                        smoothClimb(p);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
        
        getLogger().info("LadderFix enabled - Smooth ladder climbing!");
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        
        // Проверяем, на лестнице ли игрок
        if (isOnLadder(p)) {
            onLadder.add(p.getUniqueId());
            
            // Убираем коллизию с лестницей
            p.setCollidable(false);
            
            // Если игрок пытается пройти сквозь лестницу — помогаем
            if (isStuckInLadder(p)) {
                Vector dir = p.getLocation().getDirection();
                p.setVelocity(dir.multiply(0.3));
            }
        } else {
            onLadder.remove(p.getUniqueId());
            climbTick.remove(p.getUniqueId());
            
            // Возвращаем коллизию
            if (!p.isCollidable()) {
                p.setCollidable(true);
            }
        }
    }
    
    private boolean isOnLadder(Player p) {
        Location loc = p.getLocation();
        Location below = loc.clone().subtract(0, 0.1, 0);
        
        return loc.getBlock().getType() == Material.LADDER || 
               below.getBlock().getType() == Material.LADDER;
    }
    
    private boolean isStuckInLadder(Player p) {
        Vector vel = p.getVelocity();
        Location loc = p.getLocation();
        
        // Проверяем, не застрял ли игрок в блоке лестницы
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Location check = loc.clone().add(dx * 0.5, 0, dz * 0.5);
                if (check.getBlock().getType() == Material.LADDER) {
                    // Игрок внутри хитбокса лестницы
                    return true;
                }
            }
        }
        return false;
    }
    
    private void smoothClimb(Player p) {
        Vector vel = p.getVelocity();
        float pitch = p.getLocation().getPitch();
        
        int tick = climbTick.getOrDefault(p.getUniqueId(), 0) + 1;
        climbTick.put(p.getUniqueId(), tick);
        
        // W + взгляд вверх = ПЛАВНЫЙ ПОДЪЁМ
        if (isMovingForward(p) && pitch < -40) {
            // Постепенно увеличиваем скорость для плавности
            double targetY = 0.25;
            double currentY = vel.getY();
            double newY = currentY + (targetY - currentY) * 0.3;
            p.setVelocity(new Vector(vel.getX(), newY, vel.getZ()));
        }
        // S + взгляд вниз = ПЛАВНЫЙ СПУСК
        else if (isMovingBackward(p) && pitch > 40) {
            double targetY = -0.25;
            double currentY = vel.getY();
            double newY = currentY + (targetY - currentY) * 0.3;
            p.setVelocity(new Vector(vel.getX(), newY, vel.getZ()));
        }
        // Shift = БЫСТРЫЙ СПУСК
        else if (p.isSneaking()) {
            p.setVelocity(new Vector(vel.getX(), -0.3, vel.getZ()));
        }
        // Стоим на лестнице — не падаем
        else if (vel.getY() < 0 && !p.isOnGround()) {
            p.setVelocity(new Vector(vel.getX(), 0, vel.getZ()));
        }
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
}
