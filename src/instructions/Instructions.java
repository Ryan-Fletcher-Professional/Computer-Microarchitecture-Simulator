package instructions;

import java.util.HashMap;
import java.util.Map;

public class Instructions
{
    public static final int TYPECODE_SIZE = 3;
    public enum TYPECODE
    {
        MISC
    }
    public static Map<TYPECODE, String> TYPECODE_STRINGS = new HashMap<>() {{
       put(TYPECODE.MISC, "111");
    }};
    public static Map<String, TYPECODE> TYPECODES = new HashMap<>() {{
        for(TYPECODE code : TYPECODE_STRINGS.keySet())
        {
            put(TYPECODE_STRINGS.get(code), code);
        }
    }};
    public static final int OPCODE_SIZE = 3;
    public enum OPCODE
    {
        NOOP,
        STALL,
        QUASH_SIZE_ERR
    }
    public static Map<OPCODE, String> OPCODE_STRINGS = new HashMap<>() {{
        put(OPCODE.NOOP, "000");
        put(OPCODE.STALL, "001");
        put(OPCODE.QUASH_SIZE_ERR, "010");
    }};
    public static Map<String, OPCODE> OPCODES = new HashMap<>() {{
        for(OPCODE code : OPCODE_STRINGS.keySet())
        {
            put(OPCODE_STRINGS.get(code), code);
        }
    }};
    public static final int HEADER_SIZE = TYPECODE_SIZE + OPCODE_SIZE;
    public enum HEADER  // TODO : EACH HEADER MUST BE NAMED AND PUT() VERY CAREFULLY
    {
        NOOP,
        STALL,
        QUASH_SIZE_ERR
    }
    public static Map<HEADER, String> HEADER_STRINGS = new HashMap<>() {{
        put(HEADER.NOOP, TYPECODE_STRINGS.get(TYPECODE.MISC) + OPCODE_STRINGS.get(OPCODE.NOOP));
        put(HEADER.STALL, TYPECODE_STRINGS.get(TYPECODE.MISC) + OPCODE_STRINGS.get(OPCODE.STALL));
        put(HEADER.QUASH_SIZE_ERR, TYPECODE_STRINGS.get(TYPECODE.MISC) + OPCODE_STRINGS.get(OPCODE.QUASH_SIZE_ERR));
    }};
    public static Map<String, HEADER> HEADERS = new HashMap<>() {{
        for(HEADER code : HEADER_STRINGS.keySet())
        {
            put(HEADER_STRINGS.get(code), code);
        }
    }};

    public static String AUX_RESULT = "resultOfExecution";
    public static String AUX_BRANCH = "branch";
    public static String AUX_JSR = "jumpToSubroutine";

    private static String GET_FILLER(int size)
    {
        return new String(new char[size]);
    }

    public static String GET_INSTRUCTION_STRING(int size, TYPECODE type, OPCODE op, String flags, String args)
    {
        return GET_INSTRUCTION_STRING(size, TYPECODE_STRINGS.get(type) + OPCODE_STRINGS.get(op), flags, args);
    }

    public static String GET_INSTRUCTION_STRING(int size, HEADER header, String flags, String args)
    {
        return GET_INSTRUCTION_STRING(size, HEADER_STRINGS.get(header), flags, args);
    }

    public static String GET_INSTRUCTION_STRING(int size, String headerString, String flags, String args)
    {
        return headerString + flags +
               GET_FILLER(size - headerString.length() - flags.length() - args.length()) +
               args;
    }

    public static Term GET_INSTRUCTION_TERM(int size, TYPECODE type, OPCODE op, String flags, String args)
    {
        return new Term(GET_INSTRUCTION_STRING(size, type, op, flags, args), false);
    }

    public static Term GET_INSTRUCTION_TERM(int size, HEADER header, String flags, String args)
    {
        return new Term(GET_INSTRUCTION_STRING(size, header, flags, args), false);
    }

    public static Term GET_INSTRUCTION_TERM(int size, String headerString, String flags, String args)
    {
        return new Term(GET_INSTRUCTION_STRING(size, headerString, flags, args), false);
    }

    /**
     * Puts filler 0s between flags and args
     * @param size
     * @param type
     * @param op
     * @param flags
     * @param args
     * @return
     */
    public static Instruction GET_INSTRUCTION(int size, TYPECODE type, OPCODE op, String flags, String args)
    {
        return new Instruction(GET_INSTRUCTION_TERM(size, type, op, flags, args));
    }

    /**
     * Puts filler 0s between flags and args
     * @param size
     * @param header
     * @param flags
     * @param args
     * @return
     */
    public static Instruction GET_INSTRUCTION(int size, HEADER header, String flags, String args)
    {
        return new Instruction((GET_INSTRUCTION_TERM(size, header, flags, args)));
    }

    public static Instruction NOOP(int size)
    {
        return GET_INSTRUCTION(size, HEADER.NOOP, "", "");
    }
    public static Instruction STALL(int size)
    {
        return GET_INSTRUCTION(size, HEADER.STALL, "", "");
    }
    public static Instruction QUASH_SIZE_ERR(int size) { return GET_INSTRUCTION(size, HEADER.QUASH_SIZE_ERR, "", ""); }
}
