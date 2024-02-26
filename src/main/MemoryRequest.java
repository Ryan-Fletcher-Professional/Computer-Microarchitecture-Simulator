package main;

import main.GLOBALS.*;
import static main.GLOBALS.*;

public class MemoryRequest
{
    private final int callerID;
    private int timer;

    public MemoryRequest(int callerID, int delay)
    {
        this.callerID = callerID;
        this.timer = delay;
    }

    public int getCallerID()
    {
        return callerID;
    }

    public boolean tick()
    {
        return tick(0);
    }

    public boolean tick(int amount)
    {
        timer -= amount;
        return timer == 0;
    }
}
