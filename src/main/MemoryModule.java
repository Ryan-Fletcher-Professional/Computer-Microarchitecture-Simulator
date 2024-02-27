package main;

import java.util.*;
import static main.GLOBALS.*;

public class MemoryModule
{
    private final int id;                   // ID of this MemoryModule
    private final MEMORY_TYPE type;         // STORAGE/INSTRUCTION
    private final MemoryModule next;        // Pointer to memory one level down
    private final int columnSize;           // Number of lines
    private final int lineSize;             // Number of words per line
    private final int accessDelay;          // For timing simulation; how long to wait until clearing requests
    private final RETURN_MODE returnMode;   // Word or line (TODO)
    private WRITE_MODE writeMode;           // BACK/THROUGH (dynamic)

    // The underlying memory array. Remember to use >>> instead of >> for unsigned right shift
    private int[][][] memory;

    Queue<MemoryRequest> accesses;  // Requests from higher-level memory
    List<MemoryRequest> blocks;     // Requests to lower-level memory

    public MemoryModule(int id, GLOBALS.MEMORY_TYPE type, MemoryModule next, int columnSize, int lineSize, int accessDelay, RETURN_MODE returnMode)
    {
        this.id = id;
        this.type = type;
        this.next = next;
        this.columnSize = columnSize;
        this.lineSize = lineSize;
        this.accessDelay = accessDelay;
        this.returnMode = returnMode;

        initMemory();
    }

    /**
     * Flushes memory.
     */
    private void initMemory()
    {
        memory = new int[Math.min(GET_ACTUAL_MAX_SIZE(type), columnSize * lineSize) / lineSize]
                        [lineSize]
                        [WORD_INDECES.length];
        for(int[][] line : memory)
        {
            for(int[] word : line)
            {
                word[ADDRESS_INDEX] = -1;
                word[LINE_FREQUENCY_INDEX] = 0;
                word[WORD_FREQUENCY_INDEX] = 0;
                word[DATA_INDEX] = 0;
            }
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
        if(virtualAddress > columnSize) { throw new IllegalArgumentException("Virtual address too high"); }
        if(virtualAddress < 0) { throw new IllegalArgumentException("Virtual address below 0"); }
        
        double scale = 4.0;  // This memory * scale = full virtual memory. Current 4.0 is PLACEHOLDER
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

    public int load(MemoryRequest request)
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
        try{ currentAccess.tick(); }catch(TimerNotStartedException ignored){}  // Should never take exception
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
        //INSERT LINE FREQUENCY MODULATION
        int[][] line = memory[localAddress];
        boolean placed = false;
        for(int[] word : line)
        {
            if(word[ADDRESS_INDEX] == -1)
            {
                insertData(word, virtualAddress, data);
                placed = true;
                break;
            }
        }
        int[] overWritten = new int[] { -1, 0, 0 };  // Should never be passed down memory.
        if(!placed)
        {
            int leastFrequented = 0;
            for(int i = 1; i < line.length; i++)
            {
                if(line[i][WORD_FREQUENCY_INDEX] < line[leastFrequented][WORD_FREQUENCY_INDEX])
                {
                    leastFrequented = i;
                }
            }
            overWritten = line[leastFrequented].clone();
            insertData(line[leastFrequented], virtualAddress, data);
        }

        if((writeMode.equals(WRITE_MODE.BACK)) && !placed)
        {
            accessNext(REQUEST_TYPE.STORE, new Object[] { overWritten[ADDRESS_INDEX], overWritten[DATA_INDEX], false });
        }
    }

    private static void insertData(int[] word, int virtualAddress, int data)
    {
        word[ADDRESS_INDEX] = virtualAddress;
        word[WORD_FREQUENCY_INDEX] = 0;
        word[DATA_INDEX] = data;
    }

    /**
     * Performs read operation in this level of memory. If data is not found, accesses from the next level of memory,
     * writes that data to this level, and returns it.
     * If data is found, increments the access frequency and returns it.
     * CURRENTLY ONLY SUPPORTS SINGLE-WORD READ! (TODO)
     * @param virtualAddress Virtual memory address from which the data should be read.
     * @return int Data contained in the given virtual address.
     */
    private int read(int virtualAddress)
    {
        int[][] line = memory[map(virtualAddress)];
        for(int[] word : line)
        {
            if(word[ADDRESS_INDEX] == virtualAddress)
            {
                word[WORD_FREQUENCY_INDEX] = Math.min(MAX_ACCESS_FREQUENCY, ++word[WORD_FREQUENCY_INDEX]);
                return word[DATA_INDEX];
            }
        }
        int data = accessNext(REQUEST_TYPE.LOAD, new Object[] { virtualAddress });
        write(map(virtualAddress), virtualAddress, data);
        return data;
    }

    /**
     * Creates access request to next level of memory. If there is no next level, this method simply no-ops.
     * @param requestType STORE/LOAD
     * @param args Appropriate arguments for request type.
     */
    private int accessNext(REQUEST_TYPE requestType, Object[] args)
    {
        if(next == null) {return -1;}

        MemoryRequest nextRequest = new MemoryRequest(id, requestType, args);
        blocks.add(nextRequest);
        if(requestType.equals(REQUEST_TYPE.STORE))
        {
            next.store(nextRequest);
        }
        else if(requestType.equals(REQUEST_TYPE.LOAD))
        {
            return next.load(nextRequest);
        }
        else
        {
            blocks.remove(nextRequest);
            throw new IllegalArgumentException("Invalid request type.");
        }
        return -1;
    }
}
