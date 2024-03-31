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
    public static final int OPCODE_SIZE = 3;
    public enum OPCODE
    {
        NOOP,
        STALL
    }
    public static Map<OPCODE, String> OPCODE_STRINGS = new HashMap<>() {{
        put(OPCODE.NOOP, "000");
        put(OPCODE.STALL, "001");
    }};
    public static final int HEADER_SIZE = TYPECODE_SIZE + OPCODE_SIZE;
    public enum HEADER  // TODO : EACH HEADER MUST BE NAMED AND PUT() VERY CAREFULLY
    {
        NOOP,
        STALL
    }
    public static Map<HEADER, String> HEADER_STRINGS = new HashMap<>() {{
        put(HEADER.NOOP, TYPECODE_STRINGS.get(TYPECODE.MISC) + OPCODE_STRINGS.get(OPCODE.NOOP));
        put(HEADER.STALL, TYPECODE_STRINGS.get(TYPECODE.MISC) + OPCODE_STRINGS.get(OPCODE.STALL));
    }};

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

    public static Instruction NOOP(int size)
    {
        return GET_INSTRUCTION(size, TYPECODE.MISC, OPCODE.NOOP, "", "");
    }
    public static Instruction STALL(int size)
    {
        return GET_INSTRUCTION(size, TYPECODE.MISC, OPCODE.STALL, "", "");
    }
}
