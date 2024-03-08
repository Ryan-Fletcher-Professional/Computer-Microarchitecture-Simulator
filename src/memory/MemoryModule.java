package memory;

import java.util.*;
import static main.GLOBALS.*;

public class MemoryModule
{
    private final int id;                       // ID of this MemoryModule
    private final MEMORY_KIND kind;             // CACHE/RAM    TODO : Make register file module
    private final MEMORY_TYPE type;             // DATA/INSTRUCTION
    private final WORD_LENGTH wordLength;       // SHORT/LONG
    private final MemoryModule next;            // Pointer to memory one level down
    private static final int lineSize = 8;      // Number of words per line
    private final int numOffsetBits;            // Number of bits needed to distinguish between all words in a line
    private final int offsetMask;               // AND mask to retrieve offset of a given virtual address
    private final int columnSize;               // Number of lines
    private final int accessDelay;              // For timing simulation; how long to wait until clearing requests
    private WRITE_MODE writeMode;               // BACK/THROUGH_NO_ALLOCATE/THROUGH_ALLOCATE (dynamic)

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
     * @param id For simulator handling
     * @param kind CACHE/RAM
     * @param type DATA/INSTRUCTION
     * @param wordLength SHORT/LONG (should never be LONG when type=DATA)
     * @param next Pointer to the next MemoryModule down the line
     * @param columnSize Number of lines in this MemoryModule
     * @param accessDelay Minimum access time penalty for storing to or loading from this MemoryModule
     */
    public MemoryModule(int id, MEMORY_KIND kind, MEMORY_TYPE type, WORD_LENGTH wordLength, WRITE_MODE writeMode,
                        MemoryModule next, int columnSize, int accessDelay)
    {
        if(columnSize < 1) { throw new IllegalArgumentException("Column size cannot be below 0"); }

        this.id = id;
        this.kind = kind;
        this.type = type;
        this.wordLength = wordLength;
        this.writeMode = writeMode;
        this.next = next;
        numOffsetBits = (int)(Math.log(lineSize) / Math.log(2));
        offsetMask = numOffsetBits > 0 ? lineSize - 1 : 0;
        this.columnSize = columnSize;
        this.accessDelay = accessDelay;

        accesses = new LinkedList<>();
        blocks = new ArrayList<>();

        initMemory();
    }

    public String toString()
    {
        return Integer.toString(id);
    }

    public int getLineSize()
    {
        return lineSize;
    }

    public String getMemoryDisplay()
    {
        StringBuilder ret = new StringBuilder();
        int greatestAddressLength = Integer.toString(MAX_ADDRESS >>> numOffsetBits, 2).length();
        String addressLabel = "Line Address";
        int addressFillTotal = Math.max(0, greatestAddressLength - addressLabel.length());
        ret.append(" Dirty | Valid | ").append(" ".repeat((addressFillTotal + 1) / 2)).append(addressLabel).append(" ".repeat(addressFillTotal / 2)).append(" |                000               |                001               |                010               |                011               |                100               |                101               |                110               |                111              \n");
        ret.append("-".repeat(ret.toString().length() - 1)).append('\n');
        for(int[] line : memory)
        {
            ret.append("   ").append(isDirty(line) ? 1 : 0).append("   |   ").append(isValid(line) ? 1 : 0).append("   | ");
            String address = Integer.toString(getFirstAddress(line) >>> numOffsetBits, 2);
            ret.append(ADDRESS_FILLER.repeat(greatestAddressLength - address.length()));
            ret.append(address);
            for(int i = FIRST_WORD_INDEX; i < line.length; i++)
            {
                ret.append(" | ");
                String value = Integer.toString(line[i], 2);
                ret.append("0".repeat(32 - value.length()));
                ret.append(value);
            }
            ret.append('\n');
        }
        return ret.toString();
    }

    /**
     * Centralized method for checking whether a memory line is valid.
     * @param line The line to be checked.
     * @return true iff the line is valid, else false
     */
    private boolean isValid(int[] line)
    {
        return line[VALID_INDEX] == 1;
    }

    /**
     * Centralized method for setting the valid bit of a memory line to true.
     * @param line The line to be edited
     */
    private void setValid(int[] line)
    {
        setValid(line, true);
    }

    /**
     * Centralized method for setting the valid bit of a memory line.
     * @param line The line to be edited
     * @param valid Whether the valid bit should be set to 1 (as opposed to 0)
     */
    private void setValid(int[] line, boolean valid)
    {
        line[VALID_INDEX] = valid ? 1 : 0;
    }

    /**
     * Centralized method for checking whether a line is dirty.
     * @param line The line to be checked
     * @return true iff the line is dirty else false
     */
    private boolean isDirty(int[] line)
    {
        return line[DIRTY_INDEX] == 1;
    }

    /**
     * Centralized method for setting the dirty bit of a line to 1.
     * @param line The line to be edited
     */
    private void setDirty(int[] line)
    {
        setDirty(line, true);
    }

    /**
     * Centralized method for setting the dirty bit of a memory line.
     * @param line The line to be edited
     * @param dirty Whether the dirty bit should be set to 1 (as opposed to 0)
     */
    private void setDirty(int[] line, boolean dirty)
    {
        line[DIRTY_INDEX] = dirty ? 1 : 0;
    }

    /**
     * Centralized method for getting the starting virtual address of a memory line.
     * @param line The line to be read
     * @return The virtual address of the first word in the line
     */
    private int getFirstAddress(int[] line)
    {
        return line[ADDRESS_INDEX];
    }

    /**
     * Centralized method for setting the starting virtual address of a memory line.
     * @param line The line to be edited
     * @param virtualAddress The virtual address of the first word in the line
     */
    private void setFirstAddress(int[] line, int virtualAddress)
    {
        line[ADDRESS_INDEX] = virtualAddress;
    }

    /**
     * Flushes memory. If this is the lowest level of memory, all lines are set as valid so they can all be read. (They
     *  are initialized as all-0 data with correct addresses.)
     */
    private void initMemory()
    {
        memory = new int[Math.min(GET_ACTUAL_MAX_SIZE(type), columnSize * lineSize) / lineSize]
                        [WORD_INDECES.length - 1 + lineSize];

        // TODO : Remove this? (Would need to change accessNext())
        if(next == null)
        {
            for(int i = 0; i < memory.length; i++)
            {
                int[] line = memory[i];
                setValid(line);
                setFirstAddress(line, i * lineSize);
            }
        }
    }

    /**
     * Centralized method for checking whether two virtual addresses are in the same virtual memory line.
     * @param virtualAddress1 A word address
     * @param virtualAddress2 A word address
     * @return true iff The addresses are in the same line, false otherwise
     */
    private boolean sameLine(int virtualAddress1, int virtualAddress2)
    {
        return virtualAddress1 >>> numOffsetBits == virtualAddress2 >>> numOffsetBits;
    }

    /**
     * Switches the write mode of this memory module.
     * If switching out of write-back allocate mode, creates store requests to next level of memory for all dirty lines
     *  and marks them clean here.
     * @param mode WRITE_MODE to switch to.
     */
    public void setWriteMode(WRITE_MODE mode)
    {
        if(writeMode.equals(WRITE_MODE.BACK) && !mode.equals(WRITE_MODE.BACK) && next != null)
        {
            for(int[] line : memory)
            {
                if(isDirty(line) && isValid(line))
                {
                    if(next != null)
                    {
                        accessNext(REQUEST_TYPE.STORE, generateStoreArgsFromFullLine(line));
                    }
                    else
                    {
                        System.out.println("WARNING!\tMemoryModule.load() encountered unexpected behavior: " +
                                           "Lowest level of memory had dirty data in storage.");
                    }
                    setDirty(line, false);
                }
            }
        }
        writeMode = mode;
    }

    /**
     * Direct-maps virtual address to local address. Does so as follows:
     *  virtualWordAddress is checked to make sure it is within the correct maximum range. Throws
     *      IllegalArgumentException if not.
     *  virtualWordAddress is shifted right to retrieve the virtual line address.
     *  If this MemoryModule is the lowest level of unified RAM, the virtual line address is modulated to stay within
     *      the simulated address range.
     *  To map the virtual line address to its local line address, it is modulated by the number of lines in this
     *      MemoryModule.
     * @param virtualWordAddress Address in the lowest level of memory
     * @return The local line address
     */
    public int map(int virtualWordAddress)
    {
        if(virtualWordAddress > MAX_ADDRESS)
            { throw new IllegalArgumentException("Virtual address too high"); }
        if(virtualWordAddress < 0) { throw new IllegalArgumentException("Virtual address below 0"); }

        // Rightshift virtual word address to turn it into a virtual line address
        int lineAddress = virtualWordAddress >>> numOffsetBits;
                 // Modulate virtual line address into simulated address space if this is lowest level of memory and RAM
        return lineAddress % (next == null && kind.equals(MEMORY_KIND.RAM) ? memory.length : lineAddress + 1)
               % columnSize;  // Modulate line address by the number of lines in this MemoryModule for direct-map
    }

    /**
     * Central method for creating arg arrays for STORE MemoryRequests.
     * @param line Line to be written
     * @return Object array fit for passing to accessNext()
     */
    private Object[] generateStoreArgsFromFullLine(int[] line)
    {
        int[] words = new int[lineSize];
        System.arraycopy(line, FIRST_WORD_INDEX, words, 0, lineSize);
        return new Object[] { getFirstAddress(line), words };
    }

    /**
     * Central method for creating arg arrays for STORE MemoryRequests.
     * @param virtualAddress Address of target word
     * @param words Line of words to be written
     * @return Object array fit for passing to accessNext()
     */
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
     * @param request Must be instantiated. Should not be started. If request arg words has length less than line size,
     *                request arg virtualAddress must be the exact word address of the first word in that array.
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
            System.out.println(args[0]);
            System.out.println(args[1]);
            throw new IllegalArgumentException("Store request had an invalid argument");
        }

        int localAddress = map(virtualAddress);
        int[] line = memory[localAddress];

        if(writeMode.equals(WRITE_MODE.THROUGH_NO_ALLOCATE))
        {
            if(sameLine(getFirstAddress(line), virtualAddress))
                { setValid(line, false); }
            if(next != null)
            {
                accessNext(REQUEST_TYPE.STORE, args);
            }
            else
            {
                writeData(false, virtualAddress, words);
            }
        }
        else if(writeMode.equals(WRITE_MODE.BACK))  // TODO : Currently, write-back mode will allow instruction memory to be written in a DATA cache, and thus outdated in unified memory, but not marked as dirty there. Prevent writing to instruction memory through data memory.
        {
            if(!sameLine(getFirstAddress(line), virtualAddress) && isDirty(line) && isValid(line))
            {
                if(next != null)
                {
                    accessNext(REQUEST_TYPE.STORE, generateStoreArgsFromFullLine(line));
                }
                else
                {
                    System.out.println("WARNING!\tMemoryModule.load() encountered unexpected behavior: " +
                                       "Lowest level of memory had dirty data or did not have requested address.");
                }
                int[] newWords = words;
                if(words.length < lineSize)
                {
                    int[] oldWords = (next != null) ? accessNext(REQUEST_TYPE.LOAD,
                                                                 generateLoadArgsFromValues(virtualAddress, true))
                                                    : readData(line, getFirstAddress(line), true);
                    System.arraycopy(words, 0, oldWords, virtualAddress & offsetMask, words.length);
                    newWords = oldWords;
                }
                writeData(true, virtualAddress, newWords);
            }
            else
            {
                int[] oldWords = readData(line, getFirstAddress(line), true);
                System.arraycopy(words, 0, oldWords, virtualAddress & offsetMask, words.length);
                writeData(true, virtualAddress, oldWords);
            }
        }
        else if(writeMode.equals(WRITE_MODE.THROUGH_ALLOCATE))
        {
            int[] newWords = words;
            if(words.length < lineSize)
            {
                int[] oldWords = ((next != null) && !sameLine(getFirstAddress(line), virtualAddress))
                                 ? accessNext(REQUEST_TYPE.LOAD, generateLoadArgsFromValues(virtualAddress, true))
                                 : readData(line, getFirstAddress(line), true);
                System.arraycopy(words, 0, oldWords, virtualAddress & offsetMask, words.length);
                newWords = oldWords;
            }
            writeData(false, virtualAddress, newWords);
            if(next != null) { accessNext(REQUEST_TYPE.STORE, generateStoreArgsFromValues(virtualAddress, newWords)); }
        }
    }

    /**
     * Writes into this level of memory's underlying array.
     * @param virtualAddress Virtual address of a word being written.
     * @param words Line of words to be written.
     */
    private void writeData(boolean dirty, int virtualAddress, int[] words)
    {
        int[] line = memory[map(virtualAddress)];
        setValid(line);
        setDirty(line, dirty);
        setFirstAddress(line, virtualAddress >> numOffsetBits << numOffsetBits);
        System.arraycopy(words, 0, line, FIRST_WORD_INDEX, words.length);
    }

    /**
     * Central method for creating arg arrays for LOAD MemoryRequests.
     * @param virtualAddress Address of target word
     * @param wholeLine Whether the load should return the whole line (as opposed to just the target word)
     * @return Object array fit for passing to accessNext()
     */
    private Object[] generateLoadArgsFromValues(int virtualAddress, boolean wholeLine)
    {
        return new Object[] { virtualAddress, wholeLine };
    }

    /**
     * Performs requested load operation immediately, then queues access delay.
     * If currently in write-back mode and this operation has to replace a dirty line, it first creates a new access
     *  request to the next level of memory to write the dirty line.
     * TODO: MAKE SURE THAT CODE FROM PIPELINE FOR INSTRUCTION MEMORY ACCESSES DOUBLES THE ADDRESSES DURING 64-BIT MODE!
     *       MemoryModule does not translate such addresses!
     * @param request LOAD request from the previous MemoryModule
     * @return Length 1 (wordLength SHORT) or 2 (wordLength LONG) int array if request arg wholeLine is false.
     *         Length lineSize array otherwise.
     */
    public int[] load(MemoryRequest request)
    {
        accesses.add(request);

        int virtualAddress;
        boolean wholeLine;  // Should only ever be false in the very highest-level cache

        Object[] args = request.getArgs();  // [ int virtualAddress, boolean wholeLine ]
        try
        {
            virtualAddress = (int)args[0];
            wholeLine = (boolean)args[1];
        }
        catch(ClassCastException e)
        {
            throw new IllegalArgumentException("Load request had an invalid argument");
        }

        int localAddress = map(virtualAddress);
        int[] line = memory[localAddress];
        if(isValid(line))
        {
            if(sameLine(getFirstAddress(line), virtualAddress))
            {
                return readData(line, virtualAddress, wholeLine);
            }
            else
            {
                int[] newLine = new int[lineSize];
                if(next != null)
                {
                    newLine = accessNext(REQUEST_TYPE.LOAD, generateLoadArgsFromValues(virtualAddress, true));
                }
                else
                {
                    System.out.println("WARNING!\tMemoryModule.load() encountered unexpected behavior: " +
                                       "Lowest level of memory did not have requested virtual address in storage.");
                }
                if(writeMode.equals(WRITE_MODE.BACK) && isDirty(line))
                {
                    if(next != null)
                    {
                        accessNext(REQUEST_TYPE.STORE, generateStoreArgsFromFullLine(line));
                    }
                    else
                    {
                        System.out.println("WARNING!\tMemoryModule.load() encountered unexpected behavior: " +
                                           "Lowest level of memory had dirty data in storage.");
                    }
                }
                writeData(false, virtualAddress, newLine);
                return readData(newLine, virtualAddress, wholeLine);
            }
        }
        else
        {
            int[] newLine = new int[lineSize];

            if(next != null)
            {
                accessNext(REQUEST_TYPE.LOAD, generateLoadArgsFromValues(virtualAddress, true));
            }
            else
            {
                System.out.println("WARNING!\tMemoryModule.load() encountered unexpected behavior: " +
                                   "Lowest level of memory did not have requested virtual address in storage.");
            }
            writeData(false, virtualAddress, newLine);
            return readData(newLine, virtualAddress, wholeLine);
        }
    }

    /**
     * Reads and returns either the entire given line or the given word, depending on wholeLine.
     * @param line Full internal line
     * @param virtualAddress Target word address to be read
     * @param wholeLine Whether to return the whole line (as opposed to just the target word)
     * @return Length 1 (wordLength SHORT) or 2 (wordLength LONG) int array if wholeLine is false.
     *         Length lineSize array otherwise.
     */
    private int[] readData(int[] line, int virtualAddress, boolean wholeLine)
    {
        int[] ret;
        if(wholeLine)
        {
            ret = new int[lineSize];
            System.arraycopy(line, FIRST_WORD_INDEX, ret, 0, lineSize);
        }
        else
        {
            int offset = virtualAddress & offsetMask;
            if(wordLength.equals(WORD_LENGTH.SHORT))
            {
                ret = new int[] { line[FIRST_WORD_INDEX + offset] };
            }
            else
            {
                ret = new int[] { line[FIRST_WORD_INDEX + offset], line[FIRST_WORD_INDEX + offset + 1] };
            }
        }
        return ret;
    }

    /**
     * Creates access request to next level of memory. If there is no next level, this method throws an error.
     * @param requestType STORE/LOAD
     * @param args Appropriate arguments for request type. (See comments near top of store() and load())
     */
    private int[] accessNext(REQUEST_TYPE requestType, Object[] args)
    {
        if(next == null) { throw new UnsupportedOperationException("The lowest level of memory cannot accessNext()"); }

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
        return new int[8];
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
        try{ currentAccess.tick(); } catch(MemoryRequestTimerNotStartedException _ignored_){}
    }
}
