package main;

import main.GLOBALS.*;
import static main.GLOBALS.*;

public class MemoryRequest
{
    private final int callerID;
    private final REQUEST_TYPE requestType;
    private final Object[] args;
    private int timer;
    private boolean started = false;

    public MemoryRequest(int callerID, REQUEST_TYPE requestType, Object[] args)
    {
        this.callerID = callerID;
        this.requestType = requestType;
        this.args = args;
    }

    public int getCallerID()
    {
        return callerID;
    }

    public void start(int delay)
    {
        timer = delay;
        started = true;
    }

    public boolean isStarted()
    {
        return started;
    }

    public boolean finished()
    {
        return timer < 1;
    }

    public Object[] getArgs()
    {
        return args;
    }

    public boolean tick() throws TimerNotStartedException
    {
        if(!started) { throw new TimerNotStartedException(); }

        timer--;
        return timer == 0;
    }
}
