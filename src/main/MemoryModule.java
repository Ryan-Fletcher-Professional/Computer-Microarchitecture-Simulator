package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import main.GLOBALS.*;
import static main.GLOBALS.*;

public class MemoryModule
{
    public enum MODE
    {
        WORD,
        LINE
    }

    int id;
    MEMORY_TYPE type;
    MemoryModule next;
    int size;
    int accessDelay;
    MODE returnMode;

    // Remember to use >>> instead of >> for unsigned right shift
    int[][] memory;

    List<MemoryRequest> accessors = new ArrayList<>();

    public MemoryModule(GLOBALS.MEMORY_TYPE type, MemoryModule next, int size, int accessDelay, MODE returnMode)
    {
        new MemoryModule(GET_ID(), type, next, size, accessDelay, returnMode);
    }

    public MemoryModule(int id, GLOBALS.MEMORY_TYPE type, MemoryModule next, int size, int accessDelay, MODE returnMode)
    {
        this.id = id;
        this.type = type;
        this.next = next;
        this.size = size;
        this.accessDelay = accessDelay;
        this.returnMode = returnMode;

        memory = new int[Math.min(GET_ACTUAL_MAX_SIZE(this.type), this.size)][2];
        for(int[] word : memory)
        {
            word = new int[] { -1, 0 };
        }
    }

    // TODO : Add mapping mode/scale to constructor
    public int map(int virtualAddress)
    {
        if(virtualAddress > size) { throw new IllegalArgumentException("Virtual address too high"); }
        if(virtualAddress < 0) { throw new IllegalArgumentException("Virtual address below 0"); }
        
        double scale = 4.0;  // This memory * scale = full virtual memory
        return (int)((double)virtualAddress / scale);
    }

    public void store(int callerID, int address, int[] data, Boolean finished)
    {
        store(callerID, address, data, false, finished);
    }

    public void store(int callerID, int address, int[] data, boolean writeThrough, Boolean finished)
    {
        // DO PROCESSING NOW
        accessors.add(new MemoryRequest(callerID, accessDelay));
    }

    public void load(int callerID, int addressStart, int addressEnd, int[] data, Boolean finished)
    {
        // DO PROCESSING NOW
        accessors.add(new MemoryRequest(callerID, accessDelay));
    }

    public void tick()
    {
        tick(0);
    }

    public void tick(int amount)
    {
        if(accessors.isEmpty()) { return; }

        if(accessors.getFirst().tick(amount))
        {
            accessors.removeFirst();
        }
    }
}
