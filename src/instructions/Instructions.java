package instructions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        QUASH_SIZE_ERR,
        LOAD_PC
    }
    public static Map<OPCODE, String> OPCODE_STRINGS = new HashMap<>() {{
        put(OPCODE.NOOP, "000");
        put(OPCODE.STALL, "001");
        put(OPCODE.QUASH_SIZE_ERR, "010");
        put(OPCODE.LOAD_PC, "011");
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
        QUASH_SIZE_ERR,
        LOAD_PC
    }
    private static String MAKE_HEADER_STRING(TYPECODE type, OPCODE op) { return TYPECODE_STRINGS.get(type) + OPCODE_STRINGS.get(op); }
    public static Map<HEADER, String> HEADER_STRINGS = new HashMap<>() {{
        put(HEADER.NOOP, MAKE_HEADER_STRING(TYPECODE.MISC, OPCODE.NOOP));
        put(HEADER.STALL, MAKE_HEADER_STRING(TYPECODE.MISC, OPCODE.STALL));
        put(HEADER.QUASH_SIZE_ERR, MAKE_HEADER_STRING(TYPECODE.MISC, OPCODE.QUASH_SIZE_ERR));
        put(HEADER.LOAD_PC, MAKE_HEADER_STRING(TYPECODE.MISC, OPCODE.LOAD_PC));
    }};
    public static Map<String, HEADER> HEADERS = new HashMap<>() {{
        for(HEADER code : HEADER_STRINGS.keySet())
        {
            put(HEADER_STRINGS.get(code), code);
        }
    }};

    public static final List<HEADER> MEMORY_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {

    }));
    public static final List<HEADER> ALU_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {

    }));
    public static final List<HEADER> JUMP_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {
        // SHOULD INCLUDE ALL BRANCH INSTRUCTIONS
    }));
    public static final List<HEADER> BRANCH_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {

    }));

    public static final int AUX_FALSE = 0;
    public static Term AUX_FALSE() { return new Term(AUX_FALSE); }
    public static boolean AUX_FALSE(Term term) { return term.toInt() == AUX_FALSE; }
    public static final int AUX_TRUE = 1;
    public static Term AUX_TRUE() { return new Term(AUX_TRUE); }
    public static boolean AUX_TRUE(Term term) { return term.toInt() == AUX_TRUE; }
    public static final String AUX_FINISHED = "final result has just been written or instruction has been handled manually by pipeline";
    public static final String AUX_RESULT = "final result of execution";
    public static final String AUX_BRANCH = "branch";
    public static final String AUX_JSR = "jump to subroutine";
    public static final String AUX_JUMP_ADDRESS = "address to jump to";
    public static final String AUX_CURRENT_PC = "return address for JSR";
    private static final String AUX_SOURCE_ = "source value ";
    public static String AUX_SOURCE(int idx)
    {
        return AUX_SOURCE_ + Integer.toString(idx);
    }

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
    public static Instruction LOAD_PC(int size) { return GET_INSTRUCTION(size, HEADER.LOAD_PC, "", ""); }
}
