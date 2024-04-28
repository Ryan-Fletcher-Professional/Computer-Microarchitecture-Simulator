package instructions;

import memory.MemoryModule;
import memory.MemoryRequest;
import pipeline.*;

import java.util.*;

import static instructions.Instructions.*;
import static main.GLOBALS.*;

public class Instruction
{
    private static final long BYTE_MASK = -1L >>> (Long.SIZE - Integer.SIZE);

    public int id;
    public final Term word;
    private Map<String, Term> auxBits;  // Labeled auxiliary bitstrings to track information associated with the Instruction
    private LinkedList<MemoryRequest> activeRequest;

    public Instruction(int word)
    {
        this(new Term(word, false));
    }

    public Instruction(long word)
    {
        this(new Term(word, false));
    }

    public Instruction(String word)
    {
        this(new Term(word, false));
    }

    public Instruction(Term word)
    {
        id = GET_ID();
        int size = word.length();
        if((size != 32) && (size != 64)) {throw new IllegalArgumentException("Instruction word must be 32 or 64 bits long, not " + size); }
        if(HEADERS_FROM_BITSTRINGS.get(word.toString().substring(0, TYPECODE_SIZE + OPCODE_SIZE)) == null)
        {
            System.out.println("Null header");
            this.word = ERR(size, ERR_TYPE_NOT_IMPLEMENTED).word;
        }
        else
        {
            this.word = word.clone();
        }
        auxBits = new HashMap<>();
    }

    public int wordLength()
    {
        return word.length();
    }

    public long wordNum()
    {
        return word.toLong();
    }

    public void addAuxBits(String identifier, int term)
    {
        addAuxBits(identifier, new Term(term, false));
    }

    public void addAuxBits(String identifier, long term)
    {
        addAuxBits(identifier, new Term(term, false));
    }

    public void addAuxBits(String identifier, int[] bits)
    {
        addAuxBits(identifier, new Term(bits));
    }

    public void addAuxBits(String identifier, String term)
    {
        addAuxBits(identifier, new Term(term, false));
    }

    public void addAuxBits(String identifier, Term term)
    {
        auxBits.put(identifier, term);
    }

    public Term getAuxBits(String identifier)
    {
        return auxBits.get(identifier);
    }

    public TYPECODE getTypecode()
    {
        return TYPECODES.get(word.toString().substring(0, TYPECODE_SIZE));
    }

    public OPCODE getOpcode()
    {
        return OPCODES.get(word.toString().substring(TYPECODE_SIZE, TYPECODE_SIZE + OPCODE_SIZE));
    }

    public HEADER getHeader()
    {
        return HEADERS_FROM_BITSTRINGS.get(word.toString().substring(0, TYPECODE_SIZE + OPCODE_SIZE));
    }

    /**
     * Adds given number of flags using bits immediately after header.
     * @param numFlags
     */
    public void addFlags(int numFlags)
    {
        for(int i = 0; i < numFlags; i++)
        {
            addAuxBits(FLAG(i), new Term((wordLength() == WORD_SIZE_SHORT) ?
                                             MASK((int)wordNum(), 6 + i) :
                                             MASK_LONG(wordNum(), 6 + i)));
        }
    }

    /**
     * Adds labeled source register or value at source index idx. If register, will be automatically replaced with
     *  actual value during execution of the Decode pipeline stage.
     * @param idx Source index. Can be accessed with getAuxBits(AUX_SOURCE(idx))
     * @param start Start index (inclusive) of the instruction word bits to be included in the source's reg id or value
     * @param end End index (exclusive) of the instruction word bits for this source
     * @param sourceType AUX_SD_TYPE_REGISTER or AUX_SD_TYPE_IMMEDIATE
     * @param registerBank RegisterFileModule that the source register is in. Ignored if type is immediate
     */
    public void addSource(int idx, int start, int end, int sourceType, int registerBank)
    {
        addSD(true, idx, start, end, sourceType, registerBank);
    }

    /**
     * Does the same thing as addSource() but instead of reading from the instruction word, the source id/value is taken
     *  from the given Term directly
     */
    public void addSourceManual(int idx, Term term, int sourceType, int registerBank)
    {
        addSDManual(true, idx, term, sourceType, registerBank);
    }

    /**
     * Adds labeled destination register at dest index idx. Corresponding (through idx) result will be written during
     *  execution of the Writeback pipeline stage.
     * @param idx Destination index. Can be accessed with getAuxBits(AUX_DEST(idx))
     * @param start Start index (inclusive) of the instruction word bits to be included in the destination's reg id
     * @param end End index (exclusive) of the instruction word bits for this destination
     * @param registerBank RegisterFileModule that the destination register is in
     */
    public void addDest(int idx, int start, int end, int registerBank)
    {
        addSD(false, idx, start, end, AUX_SD_TYPE_REGISTER, registerBank);
    }

    /**
     * Does the same thing as addDest() but instead of reading from the instruction word, the dest id is taken
     *  from the given Term directly
     */
    public void addDestManual(int idx, Term term, int registerBank)
    {
        addSDManual(false, idx, term, AUX_SD_TYPE_REGISTER, registerBank);
    }

    /**
     * Backend for adding source or destination aux bits. Should not be used except by existing methods above.
     */
    public void addSD(boolean source, int idx, int start, int end, int type, int registerBank)
    {
        addSDManual(source, idx,
                    new Term((wordLength() == WORD_SIZE_SHORT) ?
                                 MASK((int)wordNum(), start, end) :
                                 MASK_LONG(wordNum(), start, end)),
                    type, registerBank);
    }

    /**
     * Backend for adding source or destination aux bits. Should not be used except by existing methods above.
     */
    public void addSDManual(boolean source, int idx, Term term, int type, int registerBank)
    {
        addAuxBits(source ? AUX_SOURCE(idx) : AUX_DEST(idx), term);
        addAuxBits(source ? AUX_SOURCE_TYPE(idx) : AUX_DEST_TYPE(idx), new Term(type));
        if(type == AUX_SD_TYPE_REGISTER) { addAuxBits(source ? AUX_SOURCE_BANK(idx) : AUX_DEST_BANK(idx), new Term(registerBank)); }
    }

    public static final int MAX_REG_ARGS = 16;  // Should never actually be more than (3? 4?)

    /**
     * Returns array of source register IDs for use in DecodeStage. Should not be used except there; return is not
     *  consistent compilation of actual source registers, only of remaining ones in a specific format.
     */
    public String[] getSourceRegs()
    {
        try
        {// IN SIGNED BASE 10
            List<String> retList = new ArrayList<>();
            for(int i = 0; i < MAX_REG_ARGS; i++)
            {
                Term sourceTerm = getAuxBits(AUX_SOURCE(i));
                if(sourceTerm == null)
                {
                    break;
                }

                String prefix = AUX_EQUALS(getAuxBits(AUX_SOURCE_TYPE(i)), AUX_SD_TYPE_REGISTER) ? prefixes.get(getAuxBits(AUX_SOURCE_BANK(i)).toInt()) : "";
                if(AUX_EQUALS(getAuxBits(AUX_SOURCE(i) + DecodeStage.READ), AUX_TRUE))
                {
                    prefix = "";
                }
                retList.add(prefix.isEmpty() ? " -1" : (prefix + Integer.toString(sourceTerm.toInt())));
            }
            return retList.toArray(new String[]{});
        }
        catch(NullPointerException e)
        {
            System.out.println(getHeader());
            System.out.println(wordNum());
            throw e;
        }
    }

    /**
     * Returns array of source register IDs for use in DecodeStage and MemoryWritebackStage.
     */
    public String[] getDestRegs()
    {
        // IN SIGNED BASE 10
        List<String> retList = new ArrayList<>();
        for(int i = 0; i < MAX_REG_ARGS; i++)
        {
            Term destTerm = getAuxBits(AUX_DEST(i));
            if(destTerm == null) { break; }

            String prefix = prefixes.get(getAuxBits(AUX_DEST_BANK(i)).toInt());
            retList.add(prefix + Integer.toString(destTerm.toInt()));
        }
        return retList.toArray(new String[] {});
    }

    /**
     * Sets unindexed result aux bits. Should not be used except for LOAD_PC.
     */
    public void setResult(Term term)
    {
        addAuxBits(AUX_RESULT, term);
    }

    /**
     * Sets indexed result auxiliary bits. Use this to denote which value should be written to the destination register
     *  with the same idx. Will be automatically written in Writeback pipeline stage.
     * @param term Term with the exact value to write.
     */
    public void setResult(int idx, Term term)
    {
        addAuxBits(AUX_RESULT(idx), term);
    }

    /**
     * Returns unindexed result value from auxiliary bits. Should not be used except for LOAD_PC.
     */
    public Term getResult()
    {
        return getAuxBits(AUX_RESULT);
    }

    /**
     * Returns indexed result auxiliary bitstring value. Use this to see which value will be written to the destination
     *  register with the same idx.
     */
    public Term getResult(int idx)
    {
        Term ret = getAuxBits(AUX_RESULT(idx));
        if((ret == null) && (idx == 0)) { ret = getResult(); }
        return ret;
    }

    /**
     * Returns an array of three bitmasks, corresponding to the condition code, predicate 1, and predicate 2 registers,
     *  in that order. In order for the instruction to execute, each bit in a given mask that's set to 1 must also be
     *  set to 1 in the corresponding register. Currently this check is only implemented for branch instructions. (TODO)
     *  Should be modified appropriately as additional conditional instructions are implemented.
     */
    public int[] getPositiveConditionChecks()
    {
        // bitmasks
        int[] ret = new int[3];
        if(getHeader().equals(HEADER.BRANCH_IF_ZERO))
        {
            ret[0] = CC_ZERO_MASK;
        }
        else if(getHeader().equals(HEADER.BRANCH_IF_NEGATIVE))
        {
            ret[0] = CC_NEGATIVE_MASK;
        }
        return ret;
    }

    /**
     * Returns an array of three bitmasks, corresponding to the condition code, predicate 1, and predicate 2 registers,
     *  in that order. In order for the instruction to execute, each bit in a given mask that's set to 1 must be
     *  set to 0 in the corresponding register. Currently this check is only implemented for branch instructions. (TODO)
     *  Should be modified appropriately as additional conditional instructions are implemented.
     */
    public int[] getNegativeConditionChecks()
    {
        // bitmasks
        int[] ret = new int[3];
        return ret;
    }

    /**
     * setFinished(true);
     */
    public void setFinished()
    {
        setFinished(true);
    }

    /**
     * Sets the aux bitstring denoting whether this instruction's results are ready to be written to its destination(s).
     * Can be checked with isFinished()
     * @param status true if finished should be set to true, false if it should be set to false
     */
    public void setFinished(boolean status)
    {
        addAuxBits(AUX_FINISHED, status ? AUX_TRUE : AUX_FALSE);
    }

    /**
     * Whether this instruction's results are ready to be written to its destination(s)
     */
    public boolean isFinished()
    {
        return AUX_EQUALS(getAuxBits(AUX_FINISHED), AUX_TRUE);
    }

    /**
     * Calls appropriate execution logic method. These methods should set their aux finished bits to true when they
     *  don't want to be called again.
     * @param invoker PipelineStage that called this method. Will be cast to the appropriate class as a correctness
     *                check
     */
    public void execute(PipelineStage invoker)
    {
        if(!isFinished())
        {
            HEADER header = getHeader();

            switch(header)
            {  // TODO : Add new instructions here
                case HEADER.LOAD -> executeLoad((MemoryAccessStage)invoker);
                case HEADER.LOAD_LINE -> executeLoadLine((MemoryAccessStage)invoker);
                case HEADER.STORE -> executeStore((MemoryAccessStage)invoker);
                case HEADER.STORE_LINE -> executeStoreLine((MemoryAccessStage)invoker);

                case HEADER.BRANCH_IF_ZERO -> {}  // Nothing to execute; logic in getPositiveConditionChecks()
                case HEADER.BRANCH_IF_NEGATIVE -> {}  // Nothing to execute; logic in getPositiveConditionChecks()
                case HEADER.JUMP -> {}  // Nothing to execute here
                case HEADER.CALL -> {}  // Nothing to execute; decentralized logic
                case HEADER.RETURN -> {}  // Nothing to execute; decentralized logic

                case HEADER.INT_ADD -> executeIntAdd((ExecuteStage)invoker);
                case HEADER.INT_SUB -> executeIntSubtract((ExecuteStage)invoker);
                case HEADER.INT_MUL -> executeIntMultiply((ExecuteStage)invoker);
                case HEADER.INT_DIV -> executeIntDivide((ExecuteStage)invoker);
                case HEADER.INT_MOD -> executeIntModulo((ExecuteStage)invoker);

                case HEADER.AND -> executeAND((ExecuteStage)invoker);
                case HEADER.OR -> executeOR((ExecuteStage)invoker);
                case HEADER.XOR -> executeXOR((ExecuteStage)invoker);
                case HEADER.NOT -> executeNOT((ExecuteStage)invoker);
                case HEADER.COMPARE -> executeCompare((ExecuteStage)invoker);

                case HEADER.SLL -> executeSLL((ExecuteStage)invoker);
                case HEADER.SLR -> executeSLR((ExecuteStage)invoker);
                case HEADER.SRL -> executeSRL((ExecuteStage)invoker);
                case HEADER.SRA -> executeSRA((ExecuteStage)invoker);
                case HEADER.COPY -> executeCopy((ExecuteStage)invoker);
                case HEADER.SWAP -> executeSwap((ExecuteStage)invoker);

                case HEADER.HALT -> executeHalt((ExecuteStage)invoker);

                case HEADER.LOAD_PC -> executeLoadPC((FetchStage)invoker);
                case HEADER.EXECUTION_ERR -> executeError(invoker);
            }
        }
    }

    public void executeLoadPC(FetchStage stage)
    {
        String KEY = "load_pc_holding";

        MemoryModule cache = stage.nearestInstructionCache;
        if(activeRequest == null)
        {
            long pc = stage.internalRegisters.load(PC_INDEX);
            activeRequest = new LinkedList<>(List.of(
                new MemoryRequest(id, cache.getID(),
                                  MEMORY_TYPE.INSTRUCTION, REQUEST_TYPE.LOAD,
                                  new Object[]{(int)(pc), true})));
            int[] words = cache.load(activeRequest);
            for(int i = 0; i < wordLength() / WORD_SIZE_SHORT; i++)
            {
                addAuxBits(KEY + i, new Term(words[(int)(pc % stage.nearestInstructionCache.getLineSize()) + i], false, 32));
            }
        }
        if(activeRequest.isEmpty() && !isFinished())
        {
            StringBuilder wordBuilder = new StringBuilder();
            for(int i = 0; getAuxBits(KEY + i) != null; i++)
            {
                wordBuilder.append(getAuxBits(KEY + i).toString());
            }
            // IMPORTANT: For other instructions, use AUX_RESULT(int), not AUX_RESULT
            addAuxBits(AUX_RESULT, new Term(wordBuilder.toString(), false, wordLength()));
            addAuxBits(AUX_FINISHED, AUX_TRUE);
        }
    }

    public void executeLoad(MemoryAccessStage stage)
    {
        String KEY = "load_w_holding";
        MemoryModule cache = stage.nearestDataCache;

        if(activeRequest == null)
        {
            activeRequest = new LinkedList<>(List.of(
                new MemoryRequest(id, cache.getID(),
                                  MEMORY_TYPE.DATA, REQUEST_TYPE.LOAD,
                                  new Object[]{getAuxBits(AUX_SOURCE(0)).toInt() + ((int)stage.internalRegisters.load(CM_INDEX)), false})));
            addAuxBits(KEY, new Term(cache.load(activeRequest)[0], false, stage.nearestDataCache.wordLength.equals(WORD_LENGTH.SHORT) ? WORD_SIZE_SHORT : WORD_SIZE_LONG));
        }
        if(activeRequest.isEmpty() && !isFinished()) {
            //store the loaded value in aux_result
            addAuxBits(AUX_RESULT(0), getAuxBits(KEY));
            addAuxBits(AUX_FINISHED, AUX_TRUE);
        }
    }

    public void executeLoadLine(MemoryAccessStage stage)
    {
        String KEY = "load_l_holding_";
        MemoryModule cache = stage.nearestDataCache;

        if(activeRequest == null)
        {
            activeRequest = new LinkedList<>(List.of(
                new MemoryRequest(id, cache.getID(),
                                  MEMORY_TYPE.DATA, REQUEST_TYPE.LOAD,
                                  new Object[]{getAuxBits(AUX_SOURCE(0)).toInt() + ((int)stage.internalRegisters.load(CM_INDEX)), true})));
            int[] words = cache.load(activeRequest);
            for(int i = 0; i < words.length; i++)
            {
                addAuxBits(KEY + i, new Term(words[i], false, stage.nearestDataCache.wordLength.equals(WORD_LENGTH.SHORT) ? WORD_SIZE_SHORT : WORD_SIZE_LONG));
            }
        }
        if(activeRequest.isEmpty() && !isFinished())
        {
            //store the loaded values in aux_results
            for(int i = 0; i < stage.nearestDataCache.getLineSize(); i++)
            {
                if(getAuxBits(AUX_DEST(i)) != null)  // If line goes past end of indexable registers
                {
                    addAuxBits(AUX_RESULT(i), getAuxBits(KEY + i));
                }
                else
                {
                    break;
                }
            }
            addAuxBits(AUX_FINISHED, AUX_TRUE);
        }
    }

    public void executeStore(MemoryAccessStage stage)
    {
        MemoryModule cache = stage.nearestDataCache;

        if(activeRequest == null)
        {
            activeRequest = new LinkedList<>(List.of(
                new MemoryRequest(id, cache.getID(),
                                  MEMORY_TYPE.DATA, REQUEST_TYPE.STORE,
                                  new Object[]{ getAuxBits(AUX_SOURCE(1)).toInt() + ((int)stage.internalRegisters.load(CM_INDEX)),
                                      new int[] { getAuxBits(AUX_SOURCE(0)).toInt() } })));
            cache.store(activeRequest);
        }
        addAuxBits(AUX_FINISHED, AUX_TRUE);//if(activeRequest.isEmpty() && !isFinished())
//        {
//            addAuxBits(AUX_FINISHED, AUX_TRUE);
//        }
    }

    public void executeStoreLine(MemoryAccessStage stage)
    {
        MemoryModule cache = stage.nearestDataCache;

        if(activeRequest == null)
        {
            int[] words = new int[stage.nearestDataCache.getLineSize()];
            for(int i = 0; i < words.length; i++)
            {
                words[i] = getAuxBits(AUX_SOURCE(i)).toInt();
            }
            activeRequest = new LinkedList<>(List.of(
                new MemoryRequest(id, cache.getID(),
                                  MEMORY_TYPE.DATA, REQUEST_TYPE.STORE,
                                  new Object[]{ getAuxBits(AUX_SOURCE(words.length)).toInt() + ((int)stage.internalRegisters.load(CM_INDEX)),
                                      words })));
            cache.store(activeRequest);
        }
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeIntAdd(ExecuteStage stage)
    {
        int operand1 = getAuxBits(AUX_SOURCE(0)).toInt();
        int operand2 = getAuxBits(AUX_SOURCE(1)).toInt();
        int result = operand1 + operand2;
        long longResult = ((long)result) & BYTE_MASK;
        long longOperand1 = ((long)operand1) & BYTE_MASK;
        long longOperand2 = ((long)operand2) & BYTE_MASK;
        addAuxBits(AUX_RESULT(0), new Term(result));
        if(AUX_EQUALS(getAuxBits(FLAG((wordLength() == WORD_SIZE_SHORT) ? 0 : 2)), 1))
        {
            int newCC = (int)stage.internalRegisters.load(CC_INDEX);
            newCC = (longResult != (longOperand1 + longOperand2)) ? NEW_CC_CARRY(newCC) : NEW_CC_NOCARRY(newCC);
            addAuxBits(AUX_RESULT(1), new Term(newCC, false));
        }
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeIntSubtract(ExecuteStage stage)
    {
        int operand1 = getAuxBits(AUX_SOURCE(0)).toInt();
        int operand2 = getAuxBits(AUX_SOURCE(1)).toInt();
        int result = operand1 - operand2;

        if(AUX_EQUALS(getAuxBits(FLAG((wordLength() == WORD_SIZE_SHORT) ? 0 : 2)), 1))
        {
            int newCC = (int)stage.internalRegisters.load(CC_INDEX);
            newCC = (result == 0) ? NEW_CC_ZERO(newCC) : NEW_CC_NONZERO(newCC);
            newCC = (result < 0) ? NEW_CC_NEGATIVE(newCC) : NEW_CC_NONNEGATIVE(newCC);
            newCC = (result > 0) ? NEW_CC_POSITIVE(newCC) : NEW_CC_NONPOSITIVE(newCC);
            addAuxBits(AUX_RESULT(1), new Term(newCC, false));
        }
        addAuxBits(AUX_RESULT(0), new Term(result));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeIntMultiply(ExecuteStage stage)
    {
        long operand1 = getAuxBits(AUX_SOURCE(0)).toLong();
        long operand2 = getAuxBits(AUX_SOURCE(1)).toLong();
        long result = operand1 * operand2;

        addAuxBits(AUX_RESULT(0), new Term((int)result));
        if(AUX_EQUALS(getAuxBits(FLAG((wordLength() == WORD_SIZE_SHORT) ? 0 : 2)), 1))
        {
            addAuxBits(AUX_RESULT(1), new Term((int)(result >>> Integer.SIZE), false));
        }
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeIntDivide(ExecuteStage stage)
    {
        int operand1 = getAuxBits(AUX_SOURCE(0)).toInt();
        int operand2 = getAuxBits(AUX_SOURCE(1)).toInt();
        int result = operand1 / operand2;
        int remainder = operand1 % operand2;

        addAuxBits(AUX_RESULT(0), new Term(result));
        if(AUX_EQUALS(getAuxBits(FLAG((wordLength() == WORD_SIZE_SHORT) ? 0 : 2)), 1))
        {
            addAuxBits(AUX_RESULT(1), new Term(remainder, false));
        }
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeIntModulo(ExecuteStage stage)
    {
        int operand1 = getAuxBits(AUX_SOURCE(0)).toInt();
        int operand2 = getAuxBits(AUX_SOURCE(1)).toInt();
        int result = operand1 % operand2;
        int divisor = operand1 / operand2;

        addAuxBits(AUX_RESULT(0), new Term(result));
        if(AUX_EQUALS(getAuxBits(FLAG((wordLength() == WORD_SIZE_SHORT) ? 0 : 2)), 1))
        {
            addAuxBits(AUX_RESULT(1), new Term(divisor, false));
        }
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeAND(ExecuteStage stage)
    {
        int x = getAuxBits(AUX_SOURCE(0)).toInt();
        int y = getAuxBits(AUX_SOURCE(1)).toInt();
        addAuxBits(AUX_RESULT(0), new Term(x & y, false, Integer.SIZE));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeOR(ExecuteStage stage)
    {
        int x = getAuxBits(AUX_SOURCE(0)).toInt();
        int y = getAuxBits(AUX_SOURCE(1)).toInt();
        addAuxBits(AUX_RESULT(0), new Term(x | y, false, Integer.SIZE));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeXOR(ExecuteStage stage)
    {
        int x = getAuxBits(AUX_SOURCE(0)).toInt();
        int y = getAuxBits(AUX_SOURCE(1)).toInt();
        addAuxBits(AUX_RESULT(0), new Term(x ^ y, false, Integer.SIZE));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeNOT(ExecuteStage stage)
    {
        int bits = getAuxBits(AUX_SOURCE(0)).toInt();
        addAuxBits(AUX_RESULT(0), new Term(~bits, false, Integer.SIZE));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeCompare(ExecuteStage stage)
    {
        int src0 = getAuxBits(AUX_SOURCE(0)).toInt();
        int src1 = getAuxBits(AUX_SOURCE(1)).toInt();

        int result = src0 - src1;

        int newCC = (int)stage.internalRegisters.load(CC_INDEX);
        newCC = (result == 0) ? NEW_CC_ZERO(newCC) : NEW_CC_NONZERO(newCC);
        newCC = (result < 0) ? NEW_CC_NEGATIVE(newCC) : NEW_CC_NONNEGATIVE(newCC);
        newCC = (result > 0) ? NEW_CC_POSITIVE(newCC) : NEW_CC_NONPOSITIVE(newCC);

        setResult(0, new Term(newCC, false));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeSLL(ExecuteStage stage) {
        int src = getAuxBits(AUX_SOURCE(0)).toInt();
        //shift amount
        int amount = getAuxBits(AUX_SOURCE(1)).toInt();

        // << logical left shift operation
        int result = src << amount;

        addAuxBits(AUX_RESULT(0), new Term(result, false, Integer.SIZE));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeSLR(ExecuteStage stage) {
        int src = getAuxBits(AUX_SOURCE(0)).toInt();
        int amount = getAuxBits(AUX_SOURCE(1)).toInt();

        //rotating left shift operation
        int result = Integer.rotateLeft(src, amount);

        addAuxBits(AUX_RESULT(0), new Term(result, false, Integer.SIZE));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeSRL(ExecuteStage stage) {
        int src = getAuxBits(AUX_SOURCE(0)).toInt();
        int amount = getAuxBits(AUX_SOURCE(1)).toInt();

        // >>> logical right shift operation
        int result = src >>> amount;
        addAuxBits(AUX_RESULT(0), new Term(result, false, Integer.SIZE));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeSRA(ExecuteStage stage) {
        int src = getAuxBits(AUX_SOURCE(0)).toInt();
        int amount = getAuxBits(AUX_SOURCE(1)).toInt();

        // >> arithmetic right shift operation
        int result = src >> amount;
        addAuxBits(AUX_RESULT(0), new Term(result, false, Integer.SIZE));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeCopy(ExecuteStage stage)
    {
        setResult(0, new Term(getAuxBits(AUX_SOURCE(0)).toInt(), false));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeSwap(ExecuteStage stage)
    {
        setResult(0, new Term(getAuxBits(AUX_SOURCE(1)).toInt(), false));
        setResult(1, new Term(getAuxBits(AUX_SOURCE(0)).toInt(), false));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeHalt(ExecuteStage stage)
    {
        // Just copies the result value. Does not cease
        setResult(0, new Term(getAuxBits(AUX_SOURCE(0)).toInt(), false));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeError(PipelineStage ignored)
    {
        System.out.println("EXECUTION ERROR: " + Long.parseLong(word.toString().substring(HEADER_SIZE)));
    }
}
