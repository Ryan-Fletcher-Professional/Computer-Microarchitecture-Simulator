package main;

import java.util.*;
import static main.GLOBALS.*;

public class MemoryModule
{
    private final int id;
    private final MEMORY_TYPE type;
    private final MemoryModule next;
    private final int size;
    private final int accessDelay;
    private final RETURN_MODE returnMode;
    private WRITE_MODE writeMode;

    // Remember to use >>> instead of >> for unsigned right shift
    private int[][] memory;

    Queue<MemoryRequest> accesses;
    List<MemoryRequest> blocks;

    public MemoryModule(int id, GLOBALS.MEMORY_TYPE type, MemoryModule next, int size, int accessDelay, RETURN_MODE returnMode)
    {
        this.id = id;
        this.type = type;
        this.next = next;
        this.size = size;
        this.accessDelay = accessDelay;
        this.returnMode = returnMode;

        initMemory();
    }

    private void initMemory()
    {
        memory = new int[Math.min(GET_ACTUAL_MAX_SIZE(this.type), this.size)][2];
        for(int[] word : memory)
        {
            word[0] = -1;
            word[1] = 0;
        }
    }

    public void setWriteMode(WRITE_MODE mode)
    {
        writeMode = mode;
        // TODO : Implement writing (when switching from write-back)
        initMemory();
    }

    // TODO : Add mapping mode/scale to this and constructor
    public int map(int virtualAddress)
    {
        if(virtualAddress > size) { throw new IllegalArgumentException("Virtual address too high"); }
        if(virtualAddress < 0) { throw new IllegalArgumentException("Virtual address below 0"); }
        
        double scale = 4.0;  // This memory * scale = full virtual memory
        return (int)((double)virtualAddress / scale);
    }

    public void store(MemoryRequest request)
    {
        accesses.add(request);

        int virtualAddress;
        int word;

        Object[] args = request.getArgs();  // [ int virtualAddress, int word ]
        try
        {
            virtualAddress = (int)args[0];
            word = (int)args[1];
        }
        catch(ClassCastException e)
        {
            throw new IllegalArgumentException("Store request had an invalid argument");
        }

        int hereAddress = map(virtualAddress);
        write(hereAddress, virtualAddress, word);
        if(writeMode.equals(WRITE_MODE.THROUGH))
        {
            accessNext(REQUEST_TYPE.STORE, args);
        }
    }

    public void load(MemoryRequest request)
    {
        // TODO : Implement
    }

    public void tick()
    {
        for(int i = 0; i < blocks.size(); i++)
        {
            if(blocks.get(i).finished())
            {
                blocks.remove(i--);
            }
        }

        if(!blocks.isEmpty() || accesses.isEmpty()) {return;}

        MemoryRequest currentAccess = accesses.peek();
        if(!currentAccess.isStarted())
        {
            currentAccess.start(accessDelay);
        }
        try{ currentAccess.tick(); }catch(TimerNotStartedException ignored){}
    }

    // MAKE SURE TO WRITE ALL CACHE DATA TO LOWEST MEMORY AND THEN FLUSH CACHE WHEN SWITCHING WRITE-BACK & WRITE-THROUGH
    private void write(int hereAddress, int virtualAddress, int data)
    {
        int[] overWritten = memory[hereAddress];
        memory[hereAddress] = new int[] { virtualAddress, data };

        if((writeMode.equals(WRITE_MODE.BACK)) && (overWritten[0] > -1))
        {
            accessNext(REQUEST_TYPE.STORE, new Object[] { virtualAddress, data, false });
        }
    }

    private void accessNext(REQUEST_TYPE requestType, Object[] args)
    {
        if(next == null) {return;}

        MemoryRequest nextRequest = new MemoryRequest(id, requestType, args);
        blocks.add(nextRequest);
        next.store(nextRequest);
    }
}
