package main;

import java.util.*;
import static main.GLOBALS.*;

public class MemoryModule
{
    private final int id;                       // ID of this MemoryModule
    private final MEMORY_KIND kind;             // REGISTER_FILE/CACHE/RAM
    private final MEMORY_TYPE type;             // DATA/INSTRUCTION
    private final WORD_LENGTH wordLength;       // SHORT/LONG
    private final MemoryModule next;            // Pointer to memory one level down
    private static final int lineSize = 8;      // Number of words per line
    private static final int numOffsetBits = 3; // (int)(Math.log(lineSize) / Math.log(2))
    private final int columnSize;               // Number of lines
    private final int scale;                    // Ratio of size of lowest memory level to this one
    private final int accessDelay;              // For timing simulation; how long to wait until clearing requests
    private WRITE_MODE writeMode;               // BACK/THROUGH (dynamic)

    // The underlying memory array.
    // [ [valid0, dirty0, address00, data00, data01, ... , data07],
    //   [valid1, dirty1, address10, data10, data11, ... , data17],
    //   ..., ]
    // Remember to use >>> instead of >> for logical right shift
    private int[][] memory;

    Queue<MemoryRequest> accesses;  // Requests from higher-level memory
    List<MemoryRequest> blocks;     // Requests to lower-level memory

    /**
     *
     * @param id
     * @param type
     * @param wordLength
     * @param next
     * @param columnSize
     * @param accessDelay
     */
    public MemoryModule(int id, MEMORY_KIND kind, MEMORY_TYPE type, WORD_LENGTH wordLength, MemoryModule next,
                        int columnSize, int scale, int accessDelay)
    {
        if(columnSize < 1) { throw new IllegalArgumentException("Column size cannot be below 0"); }

        this.id = id;
        this.kind = kind;
        this.type = type;
        this.wordLength = wordLength;
        this.next = next;
        this.columnSize = columnSize;
        this.scale = scale;
        this.accessDelay = accessDelay;

        initMemory();
    }

    private boolean isValid(int[] line)
    {
        return line[VALID_INDEX] == 1;
    }

    private void setValid(int[] line)
    {
        setValid(line, true);
    }

    private void setValid(int[] line, boolean valid)
    {
        line[VALID_INDEX] = valid ? 1 : 0;
    }

    private boolean isDirty(int[] line)
    {
        return line[DIRTY_INDEX] == 1;
    }

    private void setDirty(int[] line)
    {
        setDirty(line, true);
    }

    private void setDirty(int[] line, boolean dirty)
    {
        line[DIRTY_INDEX] = dirty ? 1 : 0;
    }

    private int getFirstAddress(int[] line)
    {
        return line[ADDRESS_INDEX];
    }

    private void setFirstAddress(int[] line, int virtualAddress)
    {
        line[ADDRESS_INDEX] = virtualAddress;
    }

    /**
     * Flushes memory.
     */
    private void initMemory()
    {
        memory = new int[Math.min(GET_ACTUAL_MAX_SIZE(type), columnSize * lineSize) / lineSize]
                        [WORD_INDECES.length - 1 + lineSize];
    }

    private boolean sameLine(int virtualAddress1, int virtualAddress2)
    {
        return (virtualAddress1 >>> numOffsetBits) == (virtualAddress2 >>> numOffsetBits);
    }

    /**
     * Switches the write mode of this memory module.
     * If switching out of write-back allocate mode, creates store requests to next level of memory for all dirty lines
     *  and marks them clean here.
     * @param mode WRITE_MODE to switch to.
     */
    public void setWriteMode(WRITE_MODE mode)
    {
        if((writeMode.equals(WRITE_MODE.BACK)) && (!mode.equals(WRITE_MODE.BACK)) && (next != null))
        {
            for(int i = 0; i < memory.length; i++)
            {
                int[] line = memory[i];
                if(isDirty(line) && isValid(line))
                {
                    accessNext(REQUEST_TYPE.STORE, generateStoreArgsFromFullLine(line));
                    setDirty(line, false);
                }
            }
        }
        writeMode = mode;
    }

    /**
     * Direct-maps virtual address to local address.
     * @param virtualWordAddress
     * @return The local line address
     */
    public int map(int virtualWordAddress)
    {
        if(virtualWordAddress > (wordLength.equals(WORD_LENGTH.SHORT) ? SHORT_WORD_MAX_ADDRESS : LONG_WORD_MAX_ADDRESS))
            { throw new IllegalArgumentException("Virtual address too high"); }
        if(virtualWordAddress < 0) { throw new IllegalArgumentException("Virtual address below 0"); }


        return ((virtualWordAddress >>> numOffsetBits)
                // Rightshift virtual word address to turn it into a virtual line address
                % (((next == null) && (kind.equals(MEMORY_KIND.RAM))) ? memory.length : 1))
                // Modulate virtual line address into simulated address space if this is lowest level of memory and RAM
                % scale;
                // Modulate line address by size ratio of the lowest memory level to this one to direct-map address
    }

    private Object[] generateStoreArgsFromFullLine(int[] line)
    {
        int[] words = new int[lineSize];
        System.arraycopy(line, FIRST_WORD_INDEX, words, 0, lineSize);
        return new Object[] { getFirstAddress(line), words };
    }

    private Object[] generateStoreArgsFromValues(int virtualAddress, int[] words)
    {
        return new Object[] { virtualAddress, words };
    }

    /**
     * Performs requested store operation immediately, then queues the access delay.
     * If currently in write-through no-allocate mode and the line is valid here, creates a store request to the next
     *  level of memory and marks the line as invalid here.
     * If currently in write-back mode and the line overwrites a dirty and valid line here, creates a store request to
     *  the next level of memory and then overwrites here as valid and clean.
     * If currently in write-through allocate mode, writes the line here and creates a store request to the next level
     *  of memory.
     * REQUESTS MUST BE MADE IN INTENDED ORDER OF EXECUTION!
     * @param request Must be instantiated. Should not be started.
     */
    public void store(MemoryRequest request)
    {
        accesses.add(request);

        int virtualAddress;
        int[] words;

        Object[] args = request.getArgs();  // [ int virtualAddress, int[] words ]
        try
        {
            virtualAddress = (int)args[0];
            words = (int[])args[1];
        }
        catch(ClassCastException e)
        {
            throw new IllegalArgumentException("Store request had an invalid argument");
        }

        int localAddress = map(virtualAddress);
        int[] line = memory[localAddress];

        if(writeMode.equals(WRITE_MODE.THROUGH_NO_ALLOCATE))
        {
            if(sameLine(getFirstAddress(line), virtualAddress))
                { setValid(line, false); }
            accessNext(REQUEST_TYPE.STORE, args);
        }
        else if(writeMode.equals(WRITE_MODE.BACK))
        {
            if(isDirty(line) && isValid(line)) { accessNext(REQUEST_TYPE.STORE, generateStoreArgsFromFullLine(line)); }
            write(true, virtualAddress, words);
        }
        else if(writeMode.equals(WRITE_MODE.THROUGH_ALLOCATE))
        {
            write(false, virtualAddress, words);
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
        }                            // Should never take exception
        try{ currentAccess.tick(); } catch(TimerNotStartedException _ignored_){}
    }

    /**
     * Writes into this level of memory's underlying array.
     * @param virtualAddress Virtual address of a word being written.
     * @param words Line of words to be written.
     */
    private void write(boolean dirty, int virtualAddress, int[] words)
    {
        int[] line = memory[map(virtualAddress)];
        setValid(line);
        setDirty(line, dirty);
        setFirstAddress(line, (virtualAddress >> numOffsetBits) << numOffsetBits);
        System.arraycopy(words, 0, line, FIRST_WORD_INDEX, words.length);
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
