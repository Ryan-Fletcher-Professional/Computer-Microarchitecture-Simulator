package memory;

import static main.GLOBALS.*;

public class MemoryRequest
{
    private final int callerID;                 // ID of MemoryModule making the request
    private final REQUEST_TYPE requestType;     // LOAD/STORE
    private final Object[] args;                // Differs between LOAD and STORE
    private int timer;                          // Initial value controlled by callee
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
        return timer <= 0;
    }

    public Object[] getArgs()
    {
        return args;
    }

    // Decrements timer. Returns true iff timer ran out this tick.
    public boolean tick() throws MemoryRequestTimerNotStartedException
    {
        if(!started) { throw new MemoryRequestTimerNotStartedException(); }

        timer--;
        return timer == 0;
    }
}
