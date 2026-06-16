package cn.edu.whut.sept.dungeon.ai;

import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.entity.Enemy;
import cn.edu.whut.sept.dungeon.projectile.Projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EnemyAiResult {
    private final GameState.PlayerState player;
    private final List<Enemy> enemies;
    private final List<Projectile> projectiles;
    private final String message;

    public EnemyAiResult(GameState.PlayerState player, List<Enemy> enemies,
                         List<Projectile> projectiles, String message) {
        this.player = player;
        this.enemies = Collections.unmodifiableList(new ArrayList<Enemy>(enemies));
        this.projectiles = Collections.unmodifiableList(new ArrayList<Projectile>(projectiles));
        this.message = message;
    }

    public GameState.PlayerState getPlayer() {
        return player;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public String getMessage() {
        return message;
    }
}
