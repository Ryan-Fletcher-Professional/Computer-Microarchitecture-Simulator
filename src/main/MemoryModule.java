package main;

import java.util.ArrayList;
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

    MEMORY_TYPE type;
    MemoryModule next;
    int addressLength;
    int accessDelay;
    MODE returnMode;

    int[] memory;

    List<MemoryModule> accessors = new ArrayList<>();

    public MemoryModule(GLOBALS.MEMORY_TYPE type, MemoryModule next, int addressLength, int accessDelay, MODE returnMode)
    {
        this.type = type;
        this.next = next;
        this.addressLength = addressLength;
        this.accessDelay = accessDelay;
        this.returnMode = returnMode;

        memory = new int[Math.min(GET_ACTUAL_MAX_ADDRESS_LENGTH(this.type), this.addressLength)];
    }
}
