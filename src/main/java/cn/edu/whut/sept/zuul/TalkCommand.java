package cn.edu.whut.sept.zuul;

public class TalkCommand extends Command
{
    public boolean execute(Game game)
    {
        if(!hasSecondWord()) {
            System.out.println("Talk to whom?");
            return false;
        }

        String dialogue = game.talkTo(getSecondWord());
        if(dialogue == null) {
            System.out.println("There is no " + getSecondWord() + " here.");
        } else {
            System.out.println(dialogue);
        }
        return false;
    }
}
