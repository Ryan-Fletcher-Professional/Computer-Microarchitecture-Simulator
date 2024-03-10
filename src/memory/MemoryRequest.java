package memory;

import static main.GLOBALS.*;

public class MemoryRequest
{
    private final int callerID;                 // ID of MemoryModule making the request
    private final int targetID;                 // ID of MemoryModule handling this request
    private final MEMORY_TYPE type;             // Which memory hierarchy this request works through
    private final REQUEST_TYPE requestType;     // LOAD/STORE
    private final Object[] args;                // Differs between LOAD and STORE
    private int timer;                          // Initial value controlled by callee
    private boolean started = false;

    public MemoryRequest(int callerID, int targetID, MEMORY_TYPE type, REQUEST_TYPE requestType, Object[] args)
    {
        this.callerID = callerID;
        this.targetID = targetID;
        this.type = type;
        this.requestType = requestType;
        this.args = args;
    }

    public int getCallerID()
    {
        return callerID;
    }

    public int getTargetID() { return targetID; }

    public void start(int delay)
    {
        timer = delay;
        started = true;
    }

    public boolean isStarted()
    {
        return started;
    }

    public int getTimeRemaining() throws MemoryRequestTimerNotStartedException
    {
        if(!started) { throw new MemoryRequestTimerNotStartedException(); }
        return timer;
    }

    public boolean isFinished()
    {
        return timer <= 0;
    }

    public MEMORY_TYPE getType()
    {
        return type;
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
