package main;

public class GLOBALS
{
    static int currentId = 0;

    public enum MEMORY_TYPE
    {
        STORAGE,
        INSTRUCTION
    }

    static int STORAGE_MEMORY_ACTUAL_MAX_SIZE = (int)Math.pow(2, 20);      // 4MB
    static int INSTRUCTION_MEMORY_ACTUAL_MAX_SIZE = (int)Math.pow(2, 20);  // 4MB

    public enum WORD_LENGTH
    {
        SHORT,
        LONG
    }

    public enum RETURN_MODE
    {
        WORD,
        LINE
    }

    public enum WRITE_MODE
    {
        BACK,
        THROUGH
    }

    public enum REQUEST_TYPE
    {
        LOAD,
        STORE
    }

    public static int ADDRESS_INDEX = 0;
    public static int LINE_FREQUENCY_INDEX = 1;
    public static int WORD_FREQUENCY_INDEX = 2;
    public static int DATA_INDEX = 3;
    public static int[] WORD_INDECES = new int[] { ADDRESS_INDEX, LINE_FREQUENCY_INDEX, WORD_FREQUENCY_INDEX, DATA_INDEX };

    public static int MAX_ACCESS_FREQUENCY = 7;  // 3 bits

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
            case MEMORY_TYPE.STORAGE:
                return STORAGE_MEMORY_ACTUAL_MAX_SIZE;
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
