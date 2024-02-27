package main;

import java.util.*;
import static main.GLOBALS.*;

public class MemoryModule
{
    private final int id;                   // ID of this MemoryModule
    private final MEMORY_TYPE type;         // STORAGE/INSTRUCTION
    private final MemoryModule next;        // Pointer to memory one level down
    private final int size;                 // In words
    private final int accessDelay;          // For timing simulation; how long to wait until clearing requests
    private final RETURN_MODE returnMode;   // Word or line (TODO)
    private WRITE_MODE writeMode;           // BACK/THROUGH (dynamic)

    // The underlying memory array. Remember to use >>> instead of >> for unsigned right shift
    private int[][] memory;

    Queue<MemoryRequest> accesses;  // Requests from higher-level memory
    List<MemoryRequest> blocks;     // Requests to lower-level memory

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

    /**
     * Flushes memory.
     */
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

    /**
     * Direct-maps virtual address to local address.
     * @param virtualAddress
     * @return Local address
     */
    // TODO : Finish implementing. Add mapping mode/scale to this and constructor
    public int map(int virtualAddress)
    {
        if(virtualAddress > size) { throw new IllegalArgumentException("Virtual address too high"); }
        if(virtualAddress < 0) { throw new IllegalArgumentException("Virtual address below 0"); }
        
        double scale = 4.0;  // This memory * scale = full virtual memory
        return (int)((double)virtualAddress / scale);
    }

    /**
     * Performs requested store operation immediately, then queues the access delay.
     * REQUESTS MUST BE MADE IN INTENDED ORDER OF EXECUTION!
     * @param request Must be instantiated. Should not be started.
     */
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

    /**
     * Simulates one clock cycle internally.
     * Checks to see if any requests to lower levels of memory are blocking this level.
     * If not, decrements timer of oldest request to this level.
     */
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

    /**
     * Performs write operation into this level of memory. CURRENTLY ONLY SUPPORTS SINGLE-WORD WRITE! (TODO)
     * If currently in write-back mode and a word was overwritten, creates a request to the next level of memory.
     * @param localAddress Local address to be written.
     * @param virtualAddress Virtual address to be associated with written data.
     * @param data Word to be written.
     */
    // MAKE SURE TO WRITE ALL CACHE DATA TO LOWEST MEMORY AND THEN FLUSH CACHE WHEN SWITCHING WRITE-BACK & WRITE-THROUGH
    private void write(int localAddress, int virtualAddress, int data)
    {
        int[] overWritten = memory[localAddress];
        memory[localAddress] = new int[] { virtualAddress, data };

        if((writeMode.equals(WRITE_MODE.BACK)) && (overWritten[0] > -1))
        {
            accessNext(REQUEST_TYPE.STORE, new Object[] { virtualAddress, data, false });
        }
    }

    /**
     * Creates access request to next level of memory. If there is no next level, this method simply no-ops.
     * @param requestType STORE/LOAD
     * @param args Appropriate arguments for request type.
     */
    private void accessNext(REQUEST_TYPE requestType, Object[] args)
    {
        if(next == null) {return;}

        MemoryRequest nextRequest = new MemoryRequest(id, requestType, args);
        blocks.add(nextRequest);
        next.store(nextRequest);
    }
}
