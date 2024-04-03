package main;

import memory.RegisterFileModule;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GLOBALS
{
    public static int currentId = 0;

    public static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int DEFAULT_UI_WIDTH  = screenSize.width - 20;
    public static final int DEFAULT_UI_HEIGHT = screenSize.height - 100;

    public static final String PATH_TO_FILES = "src/files/";
    public static final String PATH_TO_DATA_FILES = PATH_TO_FILES + "data/";
    public static final int DATA_STARTING_ADDRESS = 0b100000000000000000000;  // 1MB
    public static final String PATH_TO_INSTRUCTION_BINS = PATH_TO_FILES + "instructionBins/";

    public static final int INDEXABLE_BANK_SIZE = 16;
    public static final int INDEXABLE_BANK_INDEX = 0;
    public static final int INTERNAL_BANK_INDEX = 1;
    public static final int CALL_STACK_INDEX = 2;
    public static final int REVERSAL_STACK_INDEX = 3;
    public static final int[] REGISTER_BANK_INDECES = new int[] { INDEXABLE_BANK_INDEX, INTERNAL_BANK_INDEX, CALL_STACK_INDEX, REVERSAL_STACK_INDEX };
    public static final String PC = "PC";
    public static final String CC = "CC";
    public static final int CC_NEGATIVE_POSITIVE_MASK = MASK(15);
    public static boolean CC_NEGATIVE(int cc) { return !CC_POSITIVE(cc); }
    public static int NEW_CC_NEGATIVE(int cc) { return cc & (~CC_NEGATIVE_POSITIVE_MASK); }
    public static boolean CC_POSITIVE(int cc) { return (cc & CC_NEGATIVE_POSITIVE_MASK) == CC_NEGATIVE_POSITIVE_MASK; }
    public static int NEW_CC_POSITIVE(int cc) { return cc | CC_NEGATIVE_POSITIVE_MASK; }
    public static final int CC_ZERO_MASK = MASK(14);
    public static boolean CC_ZERO(int cc) { return (cc & CC_ZERO_MASK) == CC_ZERO_MASK; }
    public static int NEW_CC_ZERO(int cc) { return cc | CC_ZERO_MASK; }
    public static int NEW_CC_NONZERO(int cc) { return cc & (~CC_ZERO_MASK); }
    public static final String PRED_1 = "PRED 1";
    public static final String PRED_2 = "PRED 2";
    public static final String CALL = "CALL";
    public static final String REV = "REV";
    public static final String[] INTERNAL_REGISTER_NAMES = new String[] { "C0", PC, CC, PRED_1, PRED_2, CALL, REV };
    public static final int PC_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(PC);
    public static final int CC_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(CC);
    public static final int PRED_1_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(PRED_1);
    public static final int PRED_2_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(PRED_2);
    public static final int CALL_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(CALL);
    public static final int REV_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(REV);
    public enum MEMORY_KIND
    {
        CACHE,
        RAM
    }

    public enum REGISTER_FILE_MODE
    {
        ADDRESSED,
        STACK,
        STACK_CIRCULAR
    }

    public enum MEMORY_TYPE
    {
        DATA,
        INSTRUCTION
    }
    public static int CURRENT_TICK = 0;

    public static final int MAX_ADDRESS = (int)Math.pow(2, 25) - 1;
    public static final int DATA_MEMORY_ACTUAL_MAX_SIZE = (int)Math.pow(2, 20);            // 4MB
    public static final int INSTRUCTION_MEMORY_ACTUAL_MAX_SIZE = (int)Math.pow(2, 20);     // 4MB

    public enum WORD_LENGTH
    {
        SHORT,
        LONG
    }
    public static int WORD_SIZE_SHORT = 32;
    public static int WORD_SIZE_LONG = 64;

    public enum WRITE_MODE
    {
        BACK,
        THROUGH_NO_ALLOCATE,
        THROUGH_ALLOCATE
    }

    public static final WRITE_MODE DEFAULT_CACHE_WRITE_MODE = WRITE_MODE.BACK;
    public static final WRITE_MODE DEFAULT_RAM_WRITE_MODE = WRITE_MODE.THROUGH_ALLOCATE;
    public static final int DEFAULT_CACHE_ACCESS_DELAY = 10;
    public static final int DEFAULT_RAM_ACCESS_DELAY = 100;

    public enum REQUEST_TYPE
    {
        LOAD,
        STORE
    }

    public static final int VALID_INDEX = 0;
    public static final int DIRTY_INDEX = 1;
    public static final int ADDRESS_INDEX = 2;
    public static final int FIRST_WORD_INDEX = 3;
    public static int[] WORD_INDECES = new int[] { VALID_INDEX, DIRTY_INDEX, ADDRESS_INDEX, FIRST_WORD_INDEX };

    public static final String ADDRESS_FILLER = " ";  //"0"

    public static int GET_ID()
    {
        return currentId++;
    }

    public static int GET_ACTUAL_MAX_SIZE(MEMORY_TYPE type)
    {
        return GET_ACTUAL_MAX_SIZE(type, WORD_LENGTH.SHORT);
    }

    public static int GET_ACTUAL_MAX_SIZE(MEMORY_TYPE type, WORD_LENGTH wordLength)
    {
        switch(type)
        {
            case MEMORY_TYPE.DATA:
                return DATA_MEMORY_ACTUAL_MAX_SIZE;
            case MEMORY_TYPE.INSTRUCTION:
                switch(wordLength)
                {
                    case WORD_LENGTH.SHORT:
                        return INSTRUCTION_MEMORY_ACTUAL_MAX_SIZE;
                    case WORD_LENGTH.LONG:
                        return INSTRUCTION_MEMORY_ACTUAL_MAX_SIZE / 2;
                    default:
                        throw new IllegalArgumentException("Invalid word length");
                }
            default:
                throw new IllegalArgumentException("Invalid memory type");
        }
    }

    public static String SMART_TO_STRING(long i, int radix)
    {
        return radix == 10 ? Long.toString(i, 10) : Long.toUnsignedString(i, radix);
    }

    /**
     * Returns an int with a single bit in the given index from the left
     * (i.e. MASK(2) = 0b00100000000000000000000000000000)
     * @param idxFromLeft
     * @return
     */
    public static int MASK(int idxFromLeft)
    {
        return 0b1 << (Integer.SIZE - 1 - idxFromLeft);
    }

    /**
     * Returns a long with a single bit in the given index from the left
     * (i.e. MASK_LONG(2) = 0b0010000000000000000000000000000000000000000000000000000000000000)
     * @param idxFromLeft
     * @return
     */
    public static long MASK_LONG(int idxFromLeft)
    {
        return 0b1L << (Long.SIZE - 1 - idxFromLeft);
    }

    /**
     * Returns an int with bits in the given range, indexed from the left
     * (i.e. MASK(2, 5) = 0b00111000000000000000000000000000)
     * @param startIdxFromLeft
     * @param endIdxFromLeft
     * @return
     */
    public static int MASK(int startIdxFromLeft, int endIdxFromLeft)
    {
        return (((~0) << (startIdxFromLeft - 1))
               >>> (startIdxFromLeft - 1 + (Integer.SIZE - endIdxFromLeft)))
               << (Integer.SIZE - endIdxFromLeft);
    }

    /**
     * Returns an int with bits in the given range, indexed from the left
     * (i.e. MASK_LONG(2, 5) = 0b0011100000000000000000000000000000000000000000000000000000000000)
     * @param startIdxFromLeft
     * @param endIdxFromLeft
     * @return
     */
    public static long MASK_LONG(int startIdxFromLeft, int endIdxFromLeft)
    {
        return (((~0L) << (startIdxFromLeft - 1))
               >>> (startIdxFromLeft - 1 + (Long.SIZE - endIdxFromLeft)))
               << (Long.SIZE - endIdxFromLeft);
    }

    public static boolean[][] NEW_PENDING_REGISTERS(RegisterFileModule[] banks)
    {
        int longest = 0;
        for(RegisterFileModule bank : banks)
        {
            longest = Math.max(longest, bank.getNumRegisters());
        }
        return new boolean[4][longest];
    }

    /**
     * @param invoker The name of the method containing the line in question.
     * @param offset N - 1, where N is the number of method calls between this and invoker.
     * @return {tab}at className.invoker(className.java:lineNumber)
     */
    public static String GET_TRACE_LINE(Logger logger, String invoker, int offset)
    {
        String className = logger.getName(); //MethodHandles.lookup().lookupClass().getName();
        return "\tat " + className + "." + invoker + "(" + className + ".java:" +
                Thread.currentThread().getStackTrace()[2 + offset].getLineNumber() + ")";
    }

    /**
     * Prints a red warning message to the console, like an Exception but it doesn't cause terminal and can't be caught.
     * {tab}at className.invoker(className.java:lineNumber) message
     * @param message The message to follow the logistical info in the warning.
     */
    public static void WARN(Logger logger, String message)
    {
        logger.log(Level.WARNING,
                GET_TRACE_LINE(logger, Thread.currentThread().getStackTrace()[2].getMethodName(), 1) +
                        " " + message);
    }
}
