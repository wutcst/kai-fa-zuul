package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameState;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.JPanel;

public final class HudPanel extends JPanel {
    private static final int HUD_HEIGHT = 96;
    private GameState state;

    public HudPanel() {
        setPreferredSize(new Dimension(1280, HUD_HEIGHT));
        setBackground(new Color(24, 28, 34));
        setFocusable(false);
    }

    public void setState(GameState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.setColor(new Color(230, 234, 240));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        graphics.drawString(summaryText(), 20, 30);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        graphics.setColor(new Color(180, 188, 198));
        graphics.drawString(objectiveText(), 20, 58);
        graphics.drawString(messageText(), 20, 80);
    }

    private String summaryText() {
        if (state == null || !state.isStarted()) {
            return "Seed: -    Steps: 0";
        }
        return "Seed: " + state.getSeed()
                + "    Status: " + state.getStatus()
                + "    Steps: " + state.getPlayer().getSteps()
                + "    HP: " + state.getPlayer().getHp() + "/" + state.getPlayer().getMaxHp()
                + "    Lv: " + state.getPlayer().getLevel()
                + "    EXP: " + state.getPlayer().getExp()
                + "    ATK/DEF: " + state.getPlayer().getAtk() + "/" + state.getPlayer().getDef()
                + "    Gear: " + state.getPlayer().getWeapon() + "/" + state.getPlayer().getArmor()
                + "    Boost: +" + state.getPlayer().getCoffeeBoost()
                + "    Player: (" + state.getPlayer().getX() + ", " + state.getPlayer().getY() + ")"
                + "    Inventory: " + state.getInventory().summary();
    }

    private String objectiveText() {
        return "Objective: talk to NPCs, solve the Maven puzzle, gather materials, and reach the defense hall.";
    }

    private String messageText() {
        if (state == null) {
            return "Message: Ready.";
        }
        return "Message: " + state.getMessage();
    }
}
