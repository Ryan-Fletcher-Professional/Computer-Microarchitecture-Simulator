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
        NOOP
    }
    public static Map<OPCODE, String> OPCODE_STRINGS = new HashMap<>() {{
        put(OPCODE.NOOP, "000");
    }};

    private static String GET_FILLER(int size)
    {
        return new String(new char[size]);
    }

    public static String GET_INSTRUCTION_STRING(int size, TYPECODE type, OPCODE op, String flags, String args)
    {
        return TYPECODE_STRINGS.get(type) + OPCODE_STRINGS.get(op) + flags +
               GET_FILLER(size - TYPECODE_SIZE - OPCODE_SIZE - flags.length() - args.length()) +
               args;
    }

    public static Term GET_INSTRUCTION_TERM(int size, TYPECODE type, OPCODE op, String flags, String args)
    {
        return new Term(GET_INSTRUCTION_STRING(size, type, op, flags, args), false);
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
}
