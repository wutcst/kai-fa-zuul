package cn.edu.whut.sept.zuul;

import java.util.HashMap;
import java.util.Iterator;

public class CommandWords
{
    private HashMap<String, Command> commands;

    public CommandWords()
    {
        commands = new HashMap<String, Command>();
        commands.put("back", new BackCommand());
        commands.put("drop", new DropCommand());
        commands.put("eat", new EatCommand());
        commands.put("go", new GoCommand());
        commands.put("help", new HelpCommand(this));
        commands.put("items", new ItemsCommand());
        commands.put("look", new LookCommand());
        commands.put("quit", new QuitCommand());
        commands.put("talk", new TalkCommand());
        commands.put("take", new TakeCommand());
    }

    public Command get(String word)
    {
        return (Command)commands.get(word);
    }

    public void showAll()
    {
        for(Iterator i = commands.keySet().iterator(); i.hasNext(); ) {
            System.out.print(i.next() + "  ");
        }
        System.out.println();
    }
}
