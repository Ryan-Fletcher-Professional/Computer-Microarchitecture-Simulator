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

    public enum REQUEST_TYPE
    {
        LOAD,
        STORE
    }

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
