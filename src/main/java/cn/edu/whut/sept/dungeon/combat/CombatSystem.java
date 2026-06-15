package cn.edu.whut.sept.dungeon.combat;

public final class CombatSystem {
    private CombatSystem() {
    }

    public static int damage(int attackerAtk, int defenderDef) {
        return Math.max(1, attackerAtk - defenderDef);
    }
}
