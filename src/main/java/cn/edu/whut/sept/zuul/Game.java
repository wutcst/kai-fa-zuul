/**
 * 该类是“World-of-Zuul”应用程序的主类。
 * 《World of Zuul》是一款简单的文本冒险游戏。用户可以在一些房间组成的迷宫中探险。
 * 你们可以通过扩展该游戏的功能使它更有趣!.
 *
 * 如果想开始执行这个游戏，用户需要创建Game类的一个实例并调用“play”方法。
 *
 * Game类的实例将创建并初始化所有其他类:它创建所有房间，并将它们连接成迷宫；它创建解析器
 * 接收用户输入，并将用户输入转换成命令后开始运行游戏。
 *
 * @author  Michael Kölling and David J. Barnes
 * @version 1.0
 */
package cn.edu.whut.sept.zuul;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game
{
    private Parser parser;
    private Player player;
    private boolean finished;
    private List<Room> rooms;
    private Random random;

    public Game()
    {
        this(new Random());
    }

    public Game(Random random)
    {
        createRooms();
        this.random = random;
        parser = new Parser();
        finished = false;
    }

    private void createRooms()
    {
        Room outside, theater, pub, lab, office, library, garden, gate, defenseRoom;

        // create the rooms
        outside = new Room("outside", "outside the main entrance of the university");
        theater = new Room("theater", "in a lecture theater used for project rehearsals");
        pub = new Room("pub", "in the campus cafe where students trade project tips");
        lab = new Room("lab", "in a computing lab full of build logs");
        office = new Room("office", "in the computing admin office");
        library = new Room("library", "in the quiet library archive");
        garden = new Room("garden", "in the botanical garden beside the software building");
        gate = new Room("gate", "in the humming teleport gate room");
        defenseRoom = new Room("defense", "in the software engineering practice defense classroom");

        // initialise room exits
        outside.setExit("east", theater);
        outside.setExit("south", lab);
        outside.setExit("west", pub);
        outside.setExit("north", library);

        theater.setExit("west", outside);
        theater.setExit("north", gate);
        theater.setExit("east", defenseRoom);

        pub.setExit("east", outside);
        pub.setExit("south", garden);

        lab.setExit("north", outside);
        lab.setExit("east", office);

        office.setExit("west", lab);
        office.setExit("north", gate);

        library.setExit("south", outside);
        library.setExit("east", garden);

        garden.setExit("north", pub);
        garden.setExit("west", library);

        gate.setExit("south", theater);
        gate.setExit("west", office);
        gate.setTeleportRoom(true);

        defenseRoom.setExit("west", theater);

        placeItems(outside, theater, pub, lab, office, library, garden, gate, defenseRoom);
        rooms = new ArrayList<Room>();
        rooms.add(outside);
        rooms.add(theater);
        rooms.add(pub);
        rooms.add(lab);
        rooms.add(office);
        rooms.add(library);
        rooms.add(garden);
        rooms.add(gate);
        rooms.add(defenseRoom);

        player = new Player("adventurer", outside, 8);
    }

    private void placeItems(Room outside, Room theater, Room pub, Room lab, Room office,
                            Room library, Room garden, Room gate, Room defenseRoom)
    {
        outside.addItem(new Item("map", "a campus map with hand-written notes", 1));
        theater.addItem(new Item("slides", "presentation slides for the practice defense", 1));
        pub.addItem(new Item("coin", "a lucky coin for the final presentation", 1));
        lab.addItem(new Item("laptop", "a laptop prepared for the project demo", 4));
        office.addItem(new Item("pass", "a defense pass signed by the project tutor", 1));
        office.addItem(new Item("cookie", "a magic cookie that improves your carrying capacity", 0, 5));
        library.addItem(new Item("report", "the printed software engineering practice report", 2));
        garden.addItem(new Item("flower", "a small flower to calm the team before defense", 1));
        gate.addItem(new Item("usb", "a USB drive containing the final runnable demo", 1));
        defenseRoom.addItem(new Item("rubric", "the scoring rubric pinned near the defense desk", 1));
    }

    public void play()
    {
        printWelcome();

        // Enter the main command loop.  Here we repeatedly read commands and
        // execute them until the game is over.

        while (! finished) {
            Command command = parser.getCommand();
            if(command == null) {
                System.out.println("I don't understand...");
            } else {
                boolean shouldQuit = command.execute(this);
                finished = finished || shouldQuit;
            }
        }

        System.out.println("Thank you for playing.  Good bye.");
    }

    private void printWelcome()
    {
        System.out.println();
        System.out.println("Welcome to the World of Zuul!");
        System.out.println("World of Zuul is now a campus defense adventure game.");
        System.out.println("Collect the report, laptop, slides and pass, then reach the defense classroom.");
        System.out.println(getQuestStatus());
        System.out.println("Type 'help' if you need help.");
        System.out.println();
        System.out.println(player.getCurrentRoom().getLongDescription());
    }

    public Room getCurrentRoom() {
        return player.getCurrentRoom();
    }

    public void setCurrentRoom(Room room){
        player.teleportTo(room);
    }

    public Player getPlayer()
    {
        return player;
    }

    public boolean goRoom(String direction)
    {
        Room nextRoom = player.getCurrentRoom().getExit(direction);
        if(nextRoom == null) {
            return false;
        }

        if(!canEnter(nextRoom)) {
            return true;
        }

        player.moveTo(nextRoom);
        teleportIfNeeded();
        checkWinCondition();
        return true;
    }

    public boolean goBack()
    {
        boolean moved = player.goBack();
        if(moved) {
            teleportIfNeeded();
            checkWinCondition();
        }
        return moved;
    }

    public TakeResult takeItem(String itemName)
    {
        Room room = player.getCurrentRoom();
        Item item = room.findItem(itemName);
        if(item == null) {
            return TakeResult.notFound();
        }

        if(!player.canCarry(item)) {
            return TakeResult.tooHeavy(item);
        }

        Item removed = room.removeItem(itemName);
        player.take(removed);
        return TakeResult.taken(removed);
    }

    public Item dropItem(String itemName)
    {
        Item item = player.removeItem(itemName);
        if(item != null) {
            player.getCurrentRoom().addItem(item);
        }
        return item;
    }

    public Item eatCookie()
    {
        Item cookie = player.removeItem("cookie");
        if(cookie == null || !cookie.isMagicCookie()) {
            if(cookie != null) {
                player.take(cookie);
            }
            return null;
        }

        player.increaseCarryCapacity(cookie.getCapacityBonus());
        return cookie;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public String getQuestStatus()
    {
        StringBuilder builder = new StringBuilder("Defense checklist:");
        builder.append("\n - Report: ").append(player.hasItem("report") ? "ready" : "missing");
        builder.append("\n - Demo laptop: ").append(player.hasItem("laptop") ? "ready" : "missing");
        builder.append("\n - Slides: ").append(player.hasItem("slides") ? "ready" : "missing");
        builder.append("\n - Defense pass: ").append(player.hasItem("pass") ? "ready" : "missing");
        builder.append("\n - Final goal: enter the defense classroom with all required materials.");
        return builder.toString();
    }

    private boolean canEnter(Room room)
    {
        if(room.getId().equals("defense")) {
            if(hasDefenseMaterials()) {
                return true;
            }

            System.out.println("The defense classroom is not ready for you yet.");
            System.out.println("You still need: " + missingDefenseMaterials() + ".");
            return false;
        }

        if(room.getId().equals("gate") && !player.hasItem("usb")) {
            System.out.println("The teleport gate refuses to start without the final demo USB.");
            System.out.println("Find the USB drive before using the gate.");
            return false;
        }

        return true;
    }

    private boolean hasDefenseMaterials()
    {
        return player.hasItem("report")
                && player.hasItem("laptop")
                && player.hasItem("slides")
                && player.hasItem("pass");
    }

    private String missingDefenseMaterials()
    {
        StringBuilder builder = new StringBuilder();
        appendMissing(builder, "report");
        appendMissing(builder, "laptop");
        appendMissing(builder, "slides");
        appendMissing(builder, "pass");
        return builder.toString();
    }

    private void appendMissing(StringBuilder builder, String itemName)
    {
        if(player.hasItem(itemName)) {
            return;
        }

        if(builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(itemName);
    }

    private void teleportIfNeeded()
    {
        Room currentRoom = player.getCurrentRoom();
        if(!currentRoom.isTeleportRoom()) {
            return;
        }

        Room destination = randomRoomExcept(currentRoom);
        player.teleportTo(destination);
        System.out.println("The teleport gate flashes and moves you to another place.");
    }

    private Room randomRoomExcept(Room excludedRoom)
    {
        Room destination = excludedRoom;
        while(destination == excludedRoom) {
            destination = rooms.get(random.nextInt(rooms.size()));
        }
        return destination;
    }

    private void checkWinCondition()
    {
        if(player.getCurrentRoom().getId().equals("defense")
                && hasDefenseMaterials()) {
            System.out.println("You enter the defense classroom with every required material.");
            System.out.println(getQuestStatus());
            System.out.println("The team completes the software engineering practice defense successfully!");
            finished = true;
        }
    }

    public static class TakeResult
    {
        private final Item item;
        private final boolean taken;
        private final boolean tooHeavy;

        private TakeResult(Item item, boolean taken, boolean tooHeavy)
        {
            this.item = item;
            this.taken = taken;
            this.tooHeavy = tooHeavy;
        }

        public static TakeResult taken(Item item)
        {
            return new TakeResult(item, true, false);
        }

        public static TakeResult tooHeavy(Item item)
        {
            return new TakeResult(item, false, true);
        }

        public static TakeResult notFound()
        {
            return new TakeResult(null, false, false);
        }

        public Item getItem()
        {
            return item;
        }

        public boolean isTaken()
        {
            return taken;
        }

        public boolean isTooHeavy()
        {
            return tooHeavy;
        }
    }
}
