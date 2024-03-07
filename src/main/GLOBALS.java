package main;

public class GLOBALS
{
    static int currentId = 0;

    public enum MEMORY_KIND
    {
        CACHE,
        RAM
    }

    public enum MEMORY_TYPE
    {
        DATA,
        INSTRUCTION
    }

    final static int MAX_ADDRESS = (int)Math.pow(2, 25) - 1;
    final static int DATA_MEMORY_ACTUAL_MAX_SIZE = (int)Math.pow(2, 20);            // 4MB
    final static int INSTRUCTION_MEMORY_ACTUAL_MAX_SIZE = (int)Math.pow(2, 20);     // 4MB

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

    public static int VALID_INDEX = 0;
    public static int DIRTY_INDEX = 1;
    public static int ADDRESS_INDEX = 2;
    public static int FIRST_WORD_INDEX = 3;
    public static int[] WORD_INDECES = new int[] { VALID_INDEX, DIRTY_INDEX, ADDRESS_INDEX, FIRST_WORD_INDEX };

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
}
