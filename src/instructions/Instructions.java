package instructions;

import memory.RegisterFileModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Instructions
{
    public static final int TYPECODE_SIZE = 3;
    public enum TYPECODE
    {
        LOAD_STORE,
        BRANCH,
        INT_ARITHMETIC,
        INT_LOGIC,
        CONTROL,
        MISC,
        INTERNAL
    }
    public static Map<TYPECODE, String> TYPECODE_STRINGS = new HashMap<>() {{

        put(TYPECODE.LOAD_STORE, "000");
        put(TYPECODE.BRANCH, "001");
        put(TYPECODE.INT_ARITHMETIC, "010");
        put(TYPECODE.INT_LOGIC, "011");
        put(TYPECODE.CONTROL, "100");
        //
        put(TYPECODE.MISC, "110");
        put(TYPECODE.INTERNAL, "111");

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
        LOAD,
        STORE,

        BRANCH_IF_NEGATIVE,

        INT_ADD,

        COMPARE,

        COPY,

        HALT,

        NOOP,
        STALL,
        QUASH_SIZE,
        QUASH_BRANCH,
        LOAD_PC,
        EXECUTION_ERR
    }
    public static Map<OPCODE, String> OPCODE_STRINGS = new HashMap<>() {{

        put(OPCODE.LOAD, "000");
        put(OPCODE.STORE, "010");

        put(OPCODE.BRANCH_IF_NEGATIVE, "010");

        put(OPCODE.INT_ADD, "000");

        put(OPCODE.COMPARE, "100");

        put(OPCODE.COPY, "100");

        put(OPCODE.HALT, "101");

        put(OPCODE.NOOP, "000");
        put(OPCODE.STALL, "001");
        put(OPCODE.QUASH_SIZE, "010");
        put(OPCODE.QUASH_BRANCH, "011");
        put(OPCODE.LOAD_PC, "100");
        //
        put(OPCODE.EXECUTION_ERR, "111");

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
        LOAD,
        STORE,

        BRANCH_IF_NEGATIVE,

        INT_ADD,

        COMPARE,

        COPY,

        HALT,

        NOOP,
        STALL,
        QUASH_SIZE,
        QUASH_BRANCH,
        LOAD_PC,
        EXECUTION_ERR
    }
    private static String MAKE_HEADER_STRING(TYPECODE type, OPCODE op) { return TYPECODE_STRINGS.get(type) + OPCODE_STRINGS.get(op); }
    public static Map<HEADER, String> HEADER_STRINGS = new HashMap<>() {{

        put(HEADER.LOAD,                    MAKE_HEADER_STRING( TYPECODE.LOAD_STORE,        OPCODE.LOAD                 ));
        put(HEADER.STORE,                   MAKE_HEADER_STRING( TYPECODE.LOAD_STORE,        OPCODE.STORE                ));

        put(HEADER.BRANCH_IF_NEGATIVE,      MAKE_HEADER_STRING( TYPECODE.BRANCH,            OPCODE.BRANCH_IF_NEGATIVE   ));

        put(HEADER.INT_ADD,                 MAKE_HEADER_STRING( TYPECODE.INT_ARITHMETIC,    OPCODE.INT_ADD              ));

        put(HEADER.COMPARE,                 MAKE_HEADER_STRING( TYPECODE.INT_LOGIC,         OPCODE.COMPARE              ));

        put(HEADER.COPY,                    MAKE_HEADER_STRING( TYPECODE.CONTROL,           OPCODE.COPY                 ));

        put(HEADER.HALT,                    MAKE_HEADER_STRING( TYPECODE.MISC,              OPCODE.HALT                 ));

        put(HEADER.NOOP,                    MAKE_HEADER_STRING( TYPECODE.INTERNAL,          OPCODE.NOOP                 ));
        put(HEADER.STALL,                   MAKE_HEADER_STRING( TYPECODE.INTERNAL,          OPCODE.STALL                ));
        put(HEADER.QUASH_SIZE,              MAKE_HEADER_STRING( TYPECODE.INTERNAL,          OPCODE.QUASH_SIZE           ));
        put(HEADER.QUASH_BRANCH,            MAKE_HEADER_STRING( TYPECODE.INTERNAL,          OPCODE.QUASH_BRANCH         ));
        put(HEADER.LOAD_PC,                 MAKE_HEADER_STRING( TYPECODE.INTERNAL,          OPCODE.LOAD_PC              ));
        put(HEADER.EXECUTION_ERR,           MAKE_HEADER_STRING( TYPECODE.INTERNAL,          OPCODE.EXECUTION_ERR        ));

    }};
    public static Map<String, HEADER> HEADERS_FROM_BITSTRINGS = new HashMap<>() {{
        for(HEADER code : HEADER_STRINGS.keySet())
        {
            put(HEADER_STRINGS.get(code), code);
        }
    }};
    public static Map<HEADER, String> MNEMONICS = new HashMap<>() {{
        put(HEADER.LOAD,                    "LOAD");
        put(HEADER.STORE,                   "STR");

        put(HEADER.BRANCH_IF_NEGATIVE,      "BRN");

        put(HEADER.INT_ADD,                 "ADD");

        put(HEADER.COMPARE,                 "CMP");

        put(HEADER.COPY,                    "COPY");

        put(HEADER.HALT,                    "HALT");
    }};
    public static Map<String, HEADER> HEADERS_FROM_MNEMONICS = new HashMap<>() {{
        for(HEADER code : MNEMONICS.keySet())
        {
            put(MNEMONICS.get(code), code);
        }
    }};
    public static Map<HEADER, String> INTERNAL_MNEMONICS = new HashMap<>() {{
        put(HEADER.NOOP,                    "_NOOP_");
        put(HEADER.STALL,                   "_STALL_");
        put(HEADER.QUASH_SIZE, "_QUASH_SIZE_");
        put(HEADER.QUASH_BRANCH,            "_QUASH_BR_");
        put(HEADER.LOAD_PC,                 "_LOAD_PC_");
        put(HEADER.EXECUTION_ERR,           "_EXEC_ERR_");
    }};
    public static Map<String, HEADER> HEADERS_FROM_INTERNAL_MNEMONICS = new HashMap<>() {{
        for(HEADER code : INTERNAL_MNEMONICS.keySet())
        {
            put(INTERNAL_MNEMONICS.get(code), code);
        }
    }};

    public static final List<HEADER> MEMORY_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {
        HEADER.LOAD,
        HEADER.STORE

        // HEADER.LOAD_PC  // NOT THIS!
    }));
    public static final List<HEADER> ALU_EXECUTE_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {
        HEADER.INT_ADD,
        HEADER.COMPARE
    }));
    public static final List<HEADER> BRANCH_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {
        HEADER.BRANCH_IF_NEGATIVE
        // Should include all Jump instructions as well (they should be treated as unconditional branches)
    }));
    public static final List<HEADER> QUASH_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {
        HEADER.QUASH_SIZE,
        HEADER.QUASH_BRANCH
    }));
    public static final List<HEADER> ERROR_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {
        HEADER.EXECUTION_ERR,       // Should have flag matching errType
    }));

    public static final int AUX_FALSE = 0;
    public static Term AUX_FALSE() { return new Term(AUX_FALSE, false); }
    public static boolean AUX_FALSE(Term term) { return term.toInt() == AUX_FALSE; }
    public static final int AUX_TRUE = 1;
    public static Term AUX_TRUE() { return new Term(AUX_TRUE, false); }
    public static boolean AUX_TRUE(Term term) { return term.toInt() == AUX_TRUE; }
    public static final String AUX_FINISHED = "final result has just been written or instruction has been handled manually by pipeline";
    public static final String AUX_FINISHED_MEMORY_ACCESS_STAGE = "don't need to execute in memory access stage";
    public static final String AUX_DECODED = "don't need to decode again";
    public static final String AUX_JSR = "jump to subroutine";
    public static final String AUX_JUMP_ADDRESS = "address to jump to";
    public static final String AUX_CURRENT_PC = "return address for JSR";
    public static final String AUX_SOURCE_ = "source value ";
    public static final String AUX_DEST_ = "destination address ";
    public static final String AUX_RESULT = "final result of execution";
    public static final String AUX_RESULTS_ = "final results of execution ";
    public static final String AUX_SOURCE_TYPE_ = "source type ";
    public static final String AUX_DEST_TYPE_ = "destination type ";
        public static final int AUX_SD_TYPE_REGISTER = 0;  // No such thing as a destination non-register; such descriptions in the ISA Spec are treated as sources
        public static final int AUX_SD_TYPE_IMMEDIATE = 1;
    public static final String AUX_SOURCE_BANK_ = "source bank ";
    public static final String AUX_DEST_BANK_ = "destination bank ";
    public static final List<String> prefixes = List.of(new String[] { RegisterFileModule.INDEXABLE_PREFIX, RegisterFileModule.INTERNAL_PREFIX, RegisterFileModule.CALL_PREFIX, RegisterFileModule.REVERSAL_PREFIX });
    public static final int AUX_REG_BANK_INDEXABLES = prefixes.indexOf(RegisterFileModule.INDEXABLE_PREFIX);
    public static final int AUX_REG_BANK_INTERNALS = prefixes.indexOf(RegisterFileModule.INTERNAL_PREFIX);
    public static final int AUX_REG_BANK_CALL = prefixes.indexOf(RegisterFileModule.CALL_PREFIX);
    public static final int AUX_REG_BANK_REVERSAL = prefixes.indexOf(RegisterFileModule.REVERSAL_PREFIX);
    public static final String AUX_FLAG_ = "flag ";
        public static final int ERR_TYPE_NOT_IMPLEMENTED =  0b00000000000000000000000001;  // For when trying to execute() an instruction that has been intentionally left unimplemented
        public static final int ERR_TYPE_INVALID_FLAGS =    0b00000000000000000000000010;  // Also do <err instruction>.addAuxBits(new Term(ERR_TYPE_INVALID_FLAGS, false, <word size> - HEADER_SIZE).toString(), new Term(HEADER_STRINGS.get(<err instruction>.getHeader()), false))
        public static final int ERR_TYPE_INVALID_ARGS =     0b00000000000000000000000011;  // Also do <err instruction>.addAuxBits(new Term(ERR_TYPE_INVALID_ARGS, false, <word size> - HEADER_SIZE).toString(), new Term(HEADER_STRINGS.get(<err instruction>.getHeader()), false))

    public static boolean AUX_EQUALS(Term term, String aux) { return (term != null) && term.toString().equals(aux); }

    public static boolean AUX_EQUALS(Term term, int aux) { return (term != null) && (term.toInt() == aux); }

    public static String AUX_SOURCE(int idx)
    {
        return AUX_SOURCE_ + Integer.toString(idx);
    }

    public static String AUX_SOURCE_TYPE(int idx)
    {
        return AUX_SOURCE_TYPE_ + Integer.toString(idx);
    }

    public static String AUX_DEST_TYPE(int idx)
    {
        return AUX_DEST_TYPE_ + Integer.toString(idx);
    }

    public static String AUX_DEST(int idx)
    {
        return AUX_DEST_ + Integer.toString(idx);
    }

    public static String AUX_SOURCE_BANK(int idx)
    {
        return AUX_SOURCE_BANK_ + Integer.toString(idx);
    }

    public static String AUX_DEST_BANK(int idx)
    {
        return AUX_DEST_BANK_ + Integer.toString(idx);
    }

    public static String AUX_RESULT(int idx)
    {
        return AUX_RESULTS_ + Integer.toString(idx);
    }

    public static String FLAG(int idx)
    {
        return AUX_FLAG_ + Integer.toString(idx);
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
     * Note: Puts filler 0s between flags and args
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
     * Note: Puts filler 0s between flags and args
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

    public static Instruction NOOP              (int size) { return GET_INSTRUCTION(size, HEADER.NOOP, "", ""); }
    public static Instruction STALL             (int size) { return GET_INSTRUCTION(size, HEADER.STALL, "", ""); }
    public static Instruction QUASH_SIZE_ERR    (int size) { return GET_INSTRUCTION(size, HEADER.QUASH_SIZE, "", ""); }
    public static Instruction QUASH_BRANCH      (int size) { return GET_INSTRUCTION(size, HEADER.QUASH_BRANCH,"", ""); }
    public static Instruction LOAD_PC           (int size) { return GET_INSTRUCTION(size, HEADER.LOAD_PC, "", ""); }
    public static Instruction HALT              (int size) { return GET_INSTRUCTION(size, HEADER.HALT, "", ""); }
    public static Instruction ERR               (int size, long errType)
    {
        Instruction err = GET_INSTRUCTION(size, HEADER.EXECUTION_ERR, "", Long.toUnsignedString(errType, 2));
        return err;
    }
}
