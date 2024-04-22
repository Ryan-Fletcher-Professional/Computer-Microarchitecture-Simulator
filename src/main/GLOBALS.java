package main;

import memory.RegisterFileModule;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GLOBALS
{
    /*
        Clerical
     */

    public static final int DEFAULT_LINE_SIZE = 8;
    public static final int INDEXABLE_BANK_SIZE = 16;
    public static int WORD_SIZE_SHORT = 32;
    public static int WORD_SIZE_LONG = 64;

    public static final int[][][] START_MEM_5RAM32 = new int[][][] {  // TODO : Use this for Monday
        new int[][] {
            new int[] {5, 32, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
        },
        new int[][] {
        },
        new int[][] {
        }
    };
    public static final int[][][] START_MEM_5RAM32_1INST4LONG = new int[][][] {
        new int[][] {
            new int[] {5, 32, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
        },
        new int[][] {
        },
        new int[][] {
            new int[] {1, 4, DEFAULT_LINE_SIZE, WORD_SIZE_LONG}
        }
    };
    public static final int[][][] START_MEM_5RAM32_2DATA8_1INST4LONG = new int[][][] {  // TODO : Use this for Monday
            new int[][] {
                    new int[] {5, 32, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
            },
            new int[][] {
                    new int[] {2, 8, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
            },
            new int[][] {
                    new int[] {1, 8, DEFAULT_LINE_SIZE, WORD_SIZE_LONG}
            }
    };
    public static final int[][][] START_MEM_100RAM1024_10DATA256_2DATA128_1INST32LONG = new int[][][] {
        new int[][] {
            new int[] {100, 1024, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
        },
        new int[][] {
            new int[] {10, 256, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT},
            new int[] {2, 128, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
        },
        new int[][] {
            new int[] {1, 32, DEFAULT_LINE_SIZE, WORD_SIZE_LONG}
        }
    };
    public static final int[][][] START_MEM_100RAM1024_10DATA256_2DATA128_1INST32SHORT = new int[][][] {
        new int[][] {
            new int[] {100, 1024, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
        },
        new int[][] {
            new int[] {10, 256, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT},
            new int[] {2, 128, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
        },
        new int[][] {
            new int[] {1, 32, DEFAULT_LINE_SIZE, WORD_SIZE_SHORT}
        }
    };

    public static int currentId = 0;
    public static int GET_ID()
    {
        return currentId++;
    }

    public static int CURRENT_TICK = 0;

    public static final int INDEXABLE_BANK_INDEX = 0;
    public static final int INTERNAL_BANK_INDEX = 1;
    public static final int CALL_STACK_INDEX = 2;
    public static final int REVERSAL_STACK_INDEX = 3;
    public static final int[] REGISTER_BANK_INDECES = new int[] { INDEXABLE_BANK_INDEX, INTERNAL_BANK_INDEX, CALL_STACK_INDEX, REVERSAL_STACK_INDEX };
    public static final String CM = "CM";
    public static final String PC = "PC";
    public static final String CC = "CC";
    public static final String PRED_1 = "PRED 1";
    public static final String PRED_2 = "PRED 2";
    public static final String CALL = "CALL";
    public static final String REV = "REV";
    public static final String[] INTERNAL_REGISTER_NAMES = new String[] { "C0", CM, PC, CC, PRED_1, PRED_2, CALL, REV };
    public static final int CC_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(CC);
    public static final int CM_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(CM);
    public static final int PC_INDEX = List.of(INTERNAL_REGISTER_NAMES).indexOf(PC);
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
    public enum WORD_LENGTH
    {
        SHORT,
        LONG
    }
    public enum WRITE_MODE
    {
        BACK,
        THROUGH_NO_ALLOCATE,
        THROUGH_ALLOCATE
    }
    public enum REQUEST_TYPE
    {
        LOAD,
        STORE
    }

    public static final int ADDRESS_SIZE = 25;
    public static final int MAX_ADDRESS = (int)Math.pow(2, ADDRESS_SIZE) - 1;
    public static final int DATA_MEMORY_ACTUAL_MAX_SIZE = (int)Math.pow(2, 20);            // 4MB
    public static final int INSTRUCTION_MEMORY_ACTUAL_MAX_SIZE = (int)Math.pow(2, 20);     // 4MB
    public static final WRITE_MODE DEFAULT_CACHE_WRITE_MODE = WRITE_MODE.BACK;
    public static final WRITE_MODE DEFAULT_RAM_WRITE_MODE = WRITE_MODE.THROUGH_ALLOCATE;
    public static final int DEFAULT_CACHE_ACCESS_DELAY = 10;
    public static final int DEFAULT_RAM_ACCESS_DELAY = 100;
    public static final int VALID_INDEX = 0;
    public static final int DIRTY_INDEX = 1;
    public static final int ADDRESS_INDEX = 2;
    public static final int FIRST_WORD_INDEX = 3;
    public static int[] WORD_INDECES = new int[] { VALID_INDEX, DIRTY_INDEX, ADDRESS_INDEX, FIRST_WORD_INDEX };
    public static final String ADDRESS_FILLER = " ";
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

    public static final String PATH_TO_FILES = "src/files/";
    public static final String PATH_TO_BINARIES = PATH_TO_FILES + "bin/";
    public static final  String PENDING_INDICATOR = "*";

    /*
        Arrangement
     */

    public static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int DEFAULT_UI_WIDTH  = screenSize.width - 20;
    public static final int DEFAULT_UI_HEIGHT = screenSize.height - 100;

    public static final int CC_NEGATIVE_MASK = 0b1 << 0;  // The bit in CC corresponding to the negative comparison
    public static boolean CC_NEGATIVE(int cc)  // Checks whether given CC value has the negative bit set to 1
        { return (cc & CC_NEGATIVE_MASK) == CC_NEGATIVE_MASK; }
    public static int NEW_CC_NEGATIVE(int cc)  // Copies given CC value and sets negative bit to 1.
        { return cc | CC_NEGATIVE_MASK; }      //   Does not set positive bit either way.
    public static int NEW_CC_NONNEGATIVE(int cc)  // Copies given CC value and sets negative bit to 0.
    { return cc & (~CC_NEGATIVE_MASK); }      //   Does not set positive bit either way.
    // etc.
    public static final int CC_POSITIVE_MASK = 0b1 << 1;
    public static boolean CC_POSITIVE(int cc) { return (cc & CC_POSITIVE_MASK) == CC_POSITIVE_MASK; }
    public static int NEW_CC_POSITIVE(int cc) { return cc | CC_POSITIVE_MASK; }
    public static int NEW_CC_NONPOSITIVE(int cc) { return cc & (~CC_POSITIVE_MASK); }
    //
    public static final int CC_ZERO_MASK = 0b1 << 2;
    public static boolean CC_ZERO(int cc) { return (cc & CC_ZERO_MASK) == CC_ZERO_MASK; }
    public static int NEW_CC_ZERO(int cc) { return cc | CC_ZERO_MASK; }
    public static int NEW_CC_NONZERO(int cc) { return cc & (~CC_ZERO_MASK); }
    //
    public static final int CC_CARRY_MASK = 0b1 << 3;
    public static boolean CC_CARRY(int cc) { return (cc & CC_CARRY_MASK) == CC_CARRY_MASK; }
    public static int NEW_CC_CARRY(int cc) { return cc | CC_CARRY_MASK; }
    public static int NEW_CC_NOCARRY(int cc) { return cc & (~CC_CARRY_MASK); }
    //
    public static final int CC_HALT_MASK = 0b1 << 4;
    public static int NEW_CC_HALT(int cc) { return cc | CC_HALT_MASK; }

    /**
     * For memory/register display arrangement. Do not change.
     */
    public static String SMART_TO_STRING(long i, int radix)
    {
        return radix == 10 ? Long.toString(i, 10) : Long.toUnsignedString(i, radix);
    }

    /**
     * For memory/register display arrangement. Do not change.
     */
    public static String SMART_INT_TO_STRING(int i, int radix)
    {
        return radix == 10 ? Integer.toString(i, 10) : Integer.toUnsignedString(i, radix);
    }

    /**
     * Copies given value, sets all bits to 0 except given index from left, then rightshifts the new value so that bit
     *  is at the far right.
     */
    public static int MASK(int subject, int idxFromLeft)
    {
        return (subject & (0b1 << (Integer.SIZE - 1 - idxFromLeft))) >>> (Integer.SIZE - 1 - idxFromLeft);
    }

    /**
     * Copies given value, sets all bits to 0 except given index from left, then rightshifts the new value so that bit
     *  is at the far right.
     */
    public static long MASK_LONG(long subject, int idxFromLeft)
    {
        return (subject & (0b1L << (Long.SIZE - 1 - idxFromLeft))) >>> (Long.SIZE - 1 - idxFromLeft);
    }

    /**
     * Copies given value, sets all bits to 0 except given range, then rightshifts the new value so those bits
     *  are at the far right.
     */
    public static int MASK(int subject, int startIdxFromLeft, int endIdxFromLeft)
    {
        int right = Integer.SIZE - endIdxFromLeft;
        return (subject >>> right) & (((~0) << startIdxFromLeft) >>> (startIdxFromLeft + right));
    }

    /**
     * Copies given value, sets all bits to 0 except given range, then rightshifts the new value so those bits
     *  are at the far right.
     */
    public static long MASK_LONG(long subject, int startIdxFromLeft, int endIdxFromLeft)
    {
        int right = Long.SIZE - endIdxFromLeft;
        return (subject >>> right) & (((~0L) << startIdxFromLeft) >>> (startIdxFromLeft + right));
    }

    /**
     * Creates new smooth-shaped 2D array to track pending registers in all four banks.
     * TODO : Change to int[][] and implement "scoreboarding"
     */
    public static int[][] NEW_PENDING_REGISTERS(RegisterFileModule[] banks)
    {
        int longest = 0;
        for(RegisterFileModule bank : banks)
        {
            longest = Math.max(longest, bank.getNumRegisters());
        }
        return new int[banks.length][longest];
    }

    /*
        Logging
     */

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
