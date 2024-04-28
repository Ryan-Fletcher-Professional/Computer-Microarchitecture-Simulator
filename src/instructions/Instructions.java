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
        FLOW_CONTROL,
        INT_ARITHMETIC,
        INT_LOGIC,
        INT_CONTROL,
        FP,
        MISC,
        INTERNAL
    }
    public static Map<TYPECODE, String> TYPECODE_STRINGS = new HashMap<>() {{

        put(    TYPECODE.LOAD_STORE,        "000");
        put(    TYPECODE.FLOW_CONTROL,      "001");
        put(    TYPECODE.INT_ARITHMETIC,    "010");
        put(    TYPECODE.INT_LOGIC,         "011");
        put(    TYPECODE.INT_CONTROL,       "100");
        put(    TYPECODE.FP,                "101");
        put(    TYPECODE.MISC,              "110");
        put(    TYPECODE.INTERNAL,          "111");

    }};
    public static Map<String, TYPECODE> TYPECODES = new HashMap<>() {{
        for(TYPECODE code : TYPECODE_STRINGS.keySet())
        {
            put(TYPECODE_STRINGS.get(code), code);
        }
    }};
    public static final int OPCODE_SIZE = 3;
    public enum OPCODE  // TODO : Add new instructions here
    {
        LOAD,
        LOAD_LINE,
        STORE,
        STORE_LINE,

        BRANCH_IF_ZERO,
        BRANCH_IF_NEGATIVE,
        JUMP,
        CALL,
        RETURN,

        INT_ADD,
        INT_SUB,
        INT_MUL,
        INT_DIV,
        INT_MOD,

        AND,
        OR,
        XOR,
        NOT,
        COMPARE,

        SLL,
        SLR,
        SRL,
        SRA,
        COPY,
        SWAP,

        // FP here if implement

        UNDO,
        HALT,

        NOOP,
        STALL,
        QUASH_SIZE,
        QUASH_BRANCH,
        LOAD_PC,
        EXECUTION_ERR
    }
    public static Map<OPCODE, String> OPCODE_STRINGS = new HashMap<>() {{  // TODO : Add new instructions here

        put(OPCODE.LOAD, "000");
        put(OPCODE.LOAD_LINE, "001");
        put(OPCODE.STORE, "010");
        put(OPCODE.STORE_LINE, "011");

        put(OPCODE.BRANCH_IF_ZERO, "000");
        put(OPCODE.BRANCH_IF_NEGATIVE, "010");
        put(OPCODE.JUMP, "100");
        put(OPCODE.CALL, "110");
        put(OPCODE.RETURN, "111");

        put(OPCODE.INT_ADD, "000");
        put(OPCODE.INT_SUB, "001");
        put(OPCODE.INT_MUL, "010");
        put(OPCODE.INT_DIV, "011");
        put(OPCODE.INT_MOD, "100");

        put(OPCODE.AND, "000");
        put(OPCODE.OR, "001");
        put(OPCODE.XOR, "010");
        put(OPCODE.NOT, "011");
        put(OPCODE.COMPARE, "100");

        put(OPCODE.SLL, "000");
        put(OPCODE.SLR, "001");
        put(OPCODE.SRL, "010");
        put(OPCODE.SRA, "011");
        put(OPCODE.COPY, "100");
        put(OPCODE.SWAP, "101");

        // FP here if implement

        put(OPCODE.UNDO, "100");
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
    public enum HEADER  // TODO : Add new instructions here
    {  // TODO : EACH HEADER MUST BE NAMED AND PUT() VERY CAREFULLY
        LOAD,
        LOAD_LINE,
        STORE,
        STORE_LINE,

        BRANCH_IF_ZERO,
        BRANCH_IF_NEGATIVE,
        JUMP,
        CALL,
        RETURN,

        INT_ADD,
        INT_SUB,
        INT_MUL,
        INT_DIV,
        INT_MOD,

        AND,
        OR,
        XOR,
        NOT,
        COMPARE,

        SLL,
        SLR,
        SRL,
        SRA,
        COPY,
        SWAP,

        // FP here if implement

        UNDO,
        HALT,

        NOOP,
        STALL,
        QUASH_SIZE,
        QUASH_BRANCH,
        LOAD_PC,
        EXECUTION_ERR
    }
    private static String MAKE_HEADER_STRING(TYPECODE type, OPCODE op) { return TYPECODE_STRINGS.get(type) + OPCODE_STRINGS.get(op); }
    public static Map<HEADER, String> HEADER_STRINGS = new HashMap<>() {{  // TODO : Add new instructions here

        put(HEADER.LOAD,                    MAKE_HEADER_STRING( TYPECODE.LOAD_STORE,        OPCODE.LOAD                 ));
        put(HEADER.LOAD_LINE,               MAKE_HEADER_STRING( TYPECODE.LOAD_STORE,        OPCODE.LOAD_LINE            ));
        put(HEADER.STORE,                   MAKE_HEADER_STRING( TYPECODE.LOAD_STORE,        OPCODE.STORE                ));
        put(HEADER.STORE_LINE,              MAKE_HEADER_STRING( TYPECODE.LOAD_STORE,        OPCODE.STORE_LINE           ));

        put(HEADER.BRANCH_IF_ZERO,          MAKE_HEADER_STRING( TYPECODE.FLOW_CONTROL,       OPCODE.BRANCH_IF_ZERO      ));
        put(HEADER.BRANCH_IF_NEGATIVE,      MAKE_HEADER_STRING( TYPECODE.FLOW_CONTROL,       OPCODE.BRANCH_IF_NEGATIVE  ));
        put(HEADER.JUMP,                    MAKE_HEADER_STRING( TYPECODE.FLOW_CONTROL,       OPCODE.JUMP                ));
        put(HEADER.CALL,                    MAKE_HEADER_STRING( TYPECODE.FLOW_CONTROL,       OPCODE.CALL                ));
        put(HEADER.RETURN,                  MAKE_HEADER_STRING( TYPECODE.FLOW_CONTROL,       OPCODE.RETURN              ));

        put(HEADER.INT_ADD,                 MAKE_HEADER_STRING( TYPECODE.INT_ARITHMETIC,    OPCODE.INT_ADD              ));
        put(HEADER.INT_SUB,                 MAKE_HEADER_STRING( TYPECODE.INT_ARITHMETIC,    OPCODE.INT_SUB              ));
        put(HEADER.INT_MUL,                 MAKE_HEADER_STRING( TYPECODE.INT_ARITHMETIC,    OPCODE.INT_MUL              ));
        put(HEADER.INT_DIV,                 MAKE_HEADER_STRING( TYPECODE.INT_ARITHMETIC,    OPCODE.INT_DIV              ));
        put(HEADER.INT_MOD,                 MAKE_HEADER_STRING( TYPECODE.INT_ARITHMETIC,    OPCODE.INT_MOD              ));

        put(HEADER.AND,                     MAKE_HEADER_STRING( TYPECODE.INT_LOGIC,         OPCODE.AND                  ));
        put(HEADER.OR,                      MAKE_HEADER_STRING( TYPECODE.INT_LOGIC,         OPCODE.OR                   ));
        put(HEADER.XOR,                     MAKE_HEADER_STRING( TYPECODE.INT_LOGIC,         OPCODE.XOR                  ));
        put(HEADER.NOT,                     MAKE_HEADER_STRING( TYPECODE.INT_LOGIC,         OPCODE.NOT                  ));
        put(HEADER.COMPARE,                 MAKE_HEADER_STRING( TYPECODE.INT_LOGIC,         OPCODE.COMPARE              ));

        put(HEADER.SLL,                     MAKE_HEADER_STRING( TYPECODE.INT_CONTROL,        OPCODE.SLL                 ));
        put(HEADER.SLR,                     MAKE_HEADER_STRING( TYPECODE.INT_CONTROL,        OPCODE.SLR                 ));
        put(HEADER.SRL,                     MAKE_HEADER_STRING( TYPECODE.INT_CONTROL,        OPCODE.SRL                 ));
        put(HEADER.SRA,                     MAKE_HEADER_STRING( TYPECODE.INT_CONTROL,        OPCODE.SRA                 ));
        put(HEADER.COPY,                    MAKE_HEADER_STRING( TYPECODE.INT_CONTROL,        OPCODE.COPY                ));
        put(HEADER.SWAP,                    MAKE_HEADER_STRING( TYPECODE.INT_CONTROL,        OPCODE.SWAP                ));

        // FP here if implement

        put(HEADER.UNDO,                    MAKE_HEADER_STRING( TYPECODE.MISC,              OPCODE.UNDO                 ));
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
    public static Map<HEADER, String> MNEMONICS = new HashMap<>() {{  // TODO : Add new instructions here
        put(HEADER.LOAD,                    "LOAD");
        put(HEADER.LOAD_LINE,               "LOADL");
        put(HEADER.STORE,                   "STR");
        put(HEADER.STORE_LINE,              "STRL");

        put(HEADER.BRANCH_IF_ZERO,          "BR0");
        put(HEADER.BRANCH_IF_NEGATIVE,      "BRN");
        put(HEADER.JUMP,                    "JUMP");
        put(HEADER.CALL,                    "CALL");
        put(HEADER.RETURN,                  "RETURN");

        put(HEADER.INT_ADD,                 "ADD");
        put(HEADER.INT_SUB,                 "SUB");
        put(HEADER.INT_MUL,                 "MUL");
        put(HEADER.INT_DIV,                 "DIV");
        put(HEADER.INT_MOD,                 "MOD");

        put(HEADER.AND,                     "AND");
        put(HEADER.OR,                      "OR");
        put(HEADER.XOR,                     "XOR");
        put(HEADER.NOT,                     "NOT");
        put(HEADER.COMPARE,                 "CMP");

        put(HEADER.SLL,                     "SLL");
        put(HEADER.SLR,                     "SLR");
        put(HEADER.SRL,                     "SRL");
        put(HEADER.SRA,                     "SRA");
        put(HEADER.COPY,                    "COPY");
        put(HEADER.SWAP,                    "SWAP");

        // FP here if implement

        put(HEADER.UNDO,                    "UNDO");
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
        put(HEADER.QUASH_SIZE,              "_QUASH_SIZE_");
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
    {  // TODO : Add new appropriate instructions here (executed in MemoryAccessStage)
        HEADER.LOAD,
        HEADER.LOAD_LINE,
        HEADER.STORE,
        HEADER.STORE_LINE

        // HEADER.LOAD_PC  // NOT THIS!
    }));
    public static final List<HEADER> ALU_EXECUTE_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {  // TODO : Add new appropriate instructions here (executed in ExecuteStage)
        HEADER.INT_ADD,
        HEADER.INT_SUB,
        HEADER.INT_MUL,
        HEADER.INT_DIV,
        HEADER.INT_MOD,
        HEADER.AND,
        HEADER.OR,
        HEADER.XOR,
        HEADER.NOT,
        HEADER.COMPARE,
        HEADER.SLL,
        HEADER.SLR,
        HEADER.SRL,
        HEADER.SRA,
        HEADER.COPY,
        HEADER.SWAP,
        HEADER.HALT
    }));
    public static final List<HEADER> BRANCH_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {  // TODO : Add new appropriate instructions here (executed in ExecuteStage using condition checks)
        HEADER.BRANCH_IF_ZERO,
        HEADER.BRANCH_IF_NEGATIVE,
        HEADER.JUMP,
        HEADER.CALL,
        HEADER.RETURN
        // Should include all Jump instructions as well (they should be treated as unconditional branches)
    }));
    public static final List<HEADER> QUASH_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {  // TODO : Add new appropriate instructions here
        HEADER.QUASH_SIZE,
        HEADER.QUASH_BRANCH
    }));
    public static final List<HEADER> ERROR_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {  // TODO : Add new appropriate instructions here
        HEADER.EXECUTION_ERR,       // Should have flag matching errType
    }));
    public static final List<HEADER> DISPOSABLE_INSTRUCTIONS = new ArrayList<>(List.of(new HEADER[]
    {  // TODO : Add new appropriate instructions here (these will be ignored for deciding pipeline blocking behavior)
        HEADER.NOOP,
        HEADER.STALL,
        HEADER.QUASH_SIZE,
        HEADER.QUASH_BRANCH
    }));

    /*
        The following are all for enumeration. Do not use for computation.
     */
    public static final int AUX_TRUE = 1;
    public static final int AUX_FALSE = 0;
    public static final String AUX_FETCHED = "fetched";
    public static final String AUX_FINISHED = "final result has just been written or instruction has been handled manually by pipeline";
    public static final String AUX_FINISHED_MEMORY_ACCESS_STAGE = "don't need to execute in memory access stage";
    public static final String AUX_DECODED = "don't need to decode again";
    public static final String AUX_JSR = "jump to subroutine";
    public static final String AUX_PC_AT_FETCH = "pc when this instruction was fetched";
    public static final String AUX_CURRENT_PC = "return address for JSR";
    public static final String AUX_SOURCE_ = "source value ";
    public static final String AUX_DEST_ = "destination address ";  // Do not use. Use AUX_DEST(int idx) instead.
    public static final String AUX_RESULT = "final result of execution";  // Do not use.
    public static final String AUX_RESULTS_ = "final results of execution ";  // Do not use. Use AUX_RESULT(int idx) instead.
    public static final String AUX_SOURCE_TYPE_ = "source type ";  // Do not use.
    public static final String AUX_DEST_TYPE_ = "destination type ";  // Do not use.
    // Use the following for identifying the type of a source/destination.
        public static final int AUX_SD_TYPE_REGISTER = 0;  // No such thing as a destination non-register; such descriptions in the ISA Spec are treated as sources
        public static final int AUX_SD_TYPE_IMMEDIATE = 1;
    public static final String AUX_SOURCE_BANK_ = "source bank ";  // Do not use.
    public static final String AUX_DEST_BANK_ = "destination bank ";  // Do not use.
    public static final List<String> prefixes = List.of(new String[] { RegisterFileModule.INDEXABLE_PREFIX, RegisterFileModule.INTERNAL_PREFIX, RegisterFileModule.CALL_PREFIX, RegisterFileModule.REVERSAL_PREFIX });
    // Use the following for identifying which register bank a source/destination should read to/write from.
    public static final int AUX_REG_BANK_INDEXABLES = prefixes.indexOf(RegisterFileModule.INDEXABLE_PREFIX);
    public static final int AUX_REG_BANK_INTERNALS = prefixes.indexOf(RegisterFileModule.INTERNAL_PREFIX);
    public static final int AUX_REG_BANK_CALL = prefixes.indexOf(RegisterFileModule.CALL_PREFIX);
    public static final int AUX_REG_BANK_REVERSAL = prefixes.indexOf(RegisterFileModule.REVERSAL_PREFIX);
    public static final String AUX_FLAG_ = "flag ";  // Do not use. Use FLAG(int idx) instead.
    public static final int ERR_TYPE_NOT_IMPLEMENTED =  0b00000000000000000000000001;  // For when trying to execute() an instruction that has been intentionally left unimplemented
    public static final int ERR_TYPE_INVALID_FLAGS =    0b00000000000000000000000010;  // Also do <err instruction>.addAuxBits(new Term(ERR_TYPE_INVALID_FLAGS, false, <word size> - HEADER_SIZE).toString(), new Term(HEADER_STRINGS.get(<err instruction>.getHeader()), false))
    public static final int ERR_TYPE_INVALID_ARGS =     0b00000000000000000000000011;  // Also do <err instruction>.addAuxBits(new Term(ERR_TYPE_INVALID_ARGS, false, <word size> - HEADER_SIZE).toString(), new Term(HEADER_STRINGS.get(<err instruction>.getHeader()), false))

    /**
     * Checks whether the given Term (presumably from a getAuxBits() call) is set (i.e. not null) and equal to the given
     * comparison value.
     * @param term getAuxBits() return
     * @param aux Comparison value
     * @return (term != null) && term.toString().equals(aux)
     */
    public static boolean AUX_EQUALS(Term term, String aux) { return (term != null) && term.toString().equals(aux); }

    /**
     * Checks whether the given Term (presumably from a getAuxBits() call) is set (i.e. not null) and equal to the given
     * comparison value.
     * @param term getAuxBits() return
     * @param aux Comparison value
     * @return (term != null) && (term.toInt() == aux)
     */
    public static boolean AUX_EQUALS(Term term, int aux) { return (term != null) && (term.toInt() == aux); }

    /**
     * Returns unique auxiliary source label for given index.
     */
    public static String AUX_SOURCE(int idx)
    {
        return AUX_SOURCE_ + Integer.toString(idx);
    }

    /**
     * Returns unique auxiliary source type label for given index.
     */
    public static String AUX_SOURCE_TYPE(int idx)
    {
        return AUX_SOURCE_TYPE_ + Integer.toString(idx);
    }

    /**
     * Returns unique auxiliary destination label for given index.
     */
    public static String AUX_DEST(int idx)
    {
        return AUX_DEST_ + Integer.toString(idx);
    }

    /**
     * Returns unique auxiliary destination type label for given index.
     */
    public static String AUX_DEST_TYPE(int idx)
    {
        return AUX_DEST_TYPE_ + Integer.toString(idx);
    }

    /**
     * Returns unique auxiliary source register bank label for given index.
     */
    public static String AUX_SOURCE_BANK(int idx)
    {
        return AUX_SOURCE_BANK_ + Integer.toString(idx);
    }

    /**
     * Returns unique auxiliary destination register bank label for given index.
     */
    public static String AUX_DEST_BANK(int idx)
    {
        return AUX_DEST_BANK_ + Integer.toString(idx);
    }

    /**
     * Returns unique auxiliary result value label for given index.
     */
    public static String AUX_RESULT(int idx)
    {
        return AUX_RESULTS_ + Integer.toString(idx);
    }

    /**
     * Returns unique auxiliary flag label for given index.
     */
    public static String FLAG(int idx)
    {
        return AUX_FLAG_ + Integer.toString(idx);
    }

    /**
     * Filler character array for string formatting. Do not change.
     */
    private static String GET_FILLER(int size)
    {
        return new String(new char[size]);
    }

    /*
        The following methods are methods for constructing Instruction objects. They're pretty self-explanatory.
     */

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
     */
    public static Instruction GET_INSTRUCTION(int size, TYPECODE type, OPCODE op, String flags, String args)
    {
        return new Instruction(GET_INSTRUCTION_TERM(size, type, op, flags, args));
    }

    /**
     * Note: Puts filler 0s between flags and args
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
