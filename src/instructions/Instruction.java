package instructions;

import memory.MemoryModule;
import memory.MemoryRequest;
import pipeline.*;

import java.util.*;

import static instructions.Instructions.*;
import static main.GLOBALS.*;

public class Instruction
{
    private static final long BYTE_MASK = 0b0000000000000000000000000000000011111111111111111111111111111111L;

    public int id;
    public final Term word;
    private Map<String, Term> auxBits;
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

    public void addFlags(int numFlags)
    {
        for(int i = 0; i < numFlags; i++)
        {
            addAuxBits(FLAG(i), new Term((wordLength() == WORD_SIZE_SHORT) ?
                                             MASK((int)wordNum(), 6 + i) :
                                             MASK_LONG(wordNum(), 6 + i)));
        }
    }

    public void addSource(int idx, int start, int end, int sourceType, int registerBank)
    {
        addSD(true, idx, start, end, sourceType, registerBank);
    }

    public void addSourceManual(int idx, Term term, int sourceType, int registerBank)
    {
        addSDManual(true, idx, term, sourceType, registerBank);
    }

    public void addDest(int idx, int start, int end, int destType, int registerBank)
    {
        addSD(false, idx, start, end, destType, registerBank);
    }

    public void addDestManual(int idx, Term term, int destType, int registerBank)
    {
        addSDManual(false, idx, term, destType, registerBank);
    }

    public void addSD(boolean source, int idx, int start, int end, int type, int registerBank)
    {
        addSDManual(source, idx,
                    new Term((wordLength() == WORD_SIZE_SHORT) ?
                                   MASK((int)wordNum(), start, end) :
                                   MASK_LONG(wordNum(), start, end)),
                    type, registerBank);
    }

    public void addSDManual(boolean source, int idx, Term term, int type, int registerBank)
    {
        addAuxBits(source ? AUX_SOURCE(idx) : AUX_DEST(idx), term);
        addAuxBits(source ? AUX_SOURCE_TYPE(idx) : AUX_DEST_TYPE(idx), new Term(type));
        if(type == AUX_SD_TYPE_REGISTER) { addAuxBits(source ? AUX_SOURCE_BANK(idx) : AUX_DEST_BANK(idx), new Term(registerBank)); }
    }

    public static final int MAX_REG_ARGS = 16;  // Should never actually be more than (3? 4?)

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

    public void setResult(Term term)
    {
        addAuxBits(AUX_RESULT, term);
    }

    public void setResult(int idx, Term term)
    {
        addAuxBits(AUX_RESULT(idx), term);
    }

    public Term getResult()
    {
        return getAuxBits(AUX_RESULT);
    }

    public Term getResult(int idx)
    {
        Term ret = getAuxBits(AUX_RESULT(idx));
        if((ret == null) && (idx == 0)) { ret = getResult(); }
        return ret;
    }

    public int[] getPositiveConditionChecks()
    {
        // bitmasks
        int[] ret = new int[3]; // TODO : Add others
        if(getHeader().equals(HEADER.BRANCH_IF_NEGATIVE))
        {
            ret[0] = CC_NEGATIVE_MASK;
        }
        return ret;
    }

    public int[] getNegativeConditionChecks()
    {
        // bitmasks
        int[] ret = new int[3]; // TODO : Add others
        return ret;
    }

    public void setFinished()
    {
        setFinished(true);
    }

    public void setFinished(boolean status)
    {
        addAuxBits(AUX_FINISHED, status ? AUX_TRUE : AUX_FALSE);
    }

    public boolean isFinished()
    {
        return AUX_EQUALS(getAuxBits(AUX_FINISHED), AUX_TRUE);
    }

    public void execute(PipelineStage invoker)
    {
        HEADER header = getHeader();

        switch(header)
        {
            case HEADER.LOAD -> executeLoad((MemoryAccessStage)invoker);
            case HEADER.STORE -> executeStore((MemoryAccessStage)invoker);

            case HEADER.BRANCH_IF_NEGATIVE -> {}  // Nothing to execute; logic in getNegativeConditionChecks()

            case HEADER.INT_ADD -> executeIntAdd((ExecuteStage)invoker);

            case HEADER.COMPARE -> executeCompare((ExecuteStage)invoker);

            case HEADER.COPY -> executeCopy((ExecuteStage)invoker);

            case HEADER.HALT -> executeHalt((ExecuteStage)invoker);

            case HEADER.LOAD_PC -> executeLoadPC((FetchStage)invoker);
            case HEADER.EXECUTION_ERR -> executeError(invoker);
        }
    }

    public void executeLoadPC(FetchStage stage)
    {
        String KEY = "load_pc_holding";

        MemoryModule cache = stage.nearestInstructionCache;
        if(activeRequest == null)
        {
            activeRequest = new LinkedList<>(List.of(
                    new MemoryRequest(id, cache.getID(),
                            MEMORY_TYPE.INSTRUCTION, REQUEST_TYPE.LOAD,
                            new Object[]{(int)(stage.internalRegisters.load(PC_INDEX)), false})));
            int[] words = cache.load(activeRequest);
            for(int i = 0; i < words.length; i++)
            {
                addAuxBits(KEY + i, new Term(words[i], false, 32));
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

    public void executeLoad(MemoryAccessStage stage) {
        String KEY = "load_w_holding";
        MemoryModule cache = stage.nearestDataCache;

        if (activeRequest == null) {
            activeRequest = new LinkedList<>(List.of(
                new MemoryRequest(id, cache.getID(),
                                  MEMORY_TYPE.DATA, REQUEST_TYPE.LOAD,
                                  new Object[]{getAuxBits(AUX_SOURCE(0)).toInt(), false})));
            addAuxBits(KEY, new Term(cache.load(activeRequest)[0], true));
        }
        if (activeRequest.isEmpty() && !isFinished()) {
            //store the loaded value in aux_result
            addAuxBits(AUX_RESULT(0), getAuxBits(KEY));
            addAuxBits(AUX_FINISHED, AUX_TRUE);
        }
    }

    public void executeStore(MemoryAccessStage stage)
    {
        MemoryModule cache = stage.nearestDataCache;

        if (activeRequest == null) {
            activeRequest = new LinkedList<>(List.of(
                new MemoryRequest(id, cache.getID(),
                                  MEMORY_TYPE.DATA, REQUEST_TYPE.STORE,
                                  new Object[]{ getAuxBits(AUX_SOURCE(1)).toInt(),
                                                new int[] { getAuxBits(AUX_SOURCE(0)).toInt() } })));
            cache.store(activeRequest);
        }
        if(activeRequest.isEmpty() && !isFinished())
        {
            addAuxBits(AUX_FINISHED, AUX_TRUE);
        }
    }

    public void executeIntAdd(ExecuteStage stage)
    {
        String KEY = "add_w_holding";
        int operand1 = getAuxBits(AUX_SOURCE(0)).toInt();
        int operand2 = getAuxBits(AUX_SOURCE(1)).toInt();
        int result = operand1 + operand2;
        long longResult = ((long)result) & BYTE_MASK;
        long longOperand1 = ((long)operand1) & BYTE_MASK;
        long longOperand2 = ((long)operand2) & BYTE_MASK;
        if(AUX_EQUALS(getAuxBits(FLAG((wordLength() == WORD_SIZE_SHORT) ? 0 : 2)), 1))
        {
            int newCC = (int)stage.internalRegisters.load(CC_INDEX);
            newCC = (longResult != (longOperand1 + longOperand2)) ? NEW_CC_CARRY(newCC) : NEW_CC_NOCARRY(newCC);
            addAuxBits(AUX_RESULT(1), new Term(newCC, false));
        }
        addAuxBits(AUX_RESULT(0), new Term(result));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

//    public void executeSubtract(){
//        String KEY = "subtract_w_holding";
//        int srcReg = Integer.parseInt(getAuxBits(AUX_SOURCE(0)).toString().substring(1));
//        int destReg = Integer.parseInt(getAuxBits(AUX_DEST(0)).toString().substring(1));
//        //get the values from source registers
//        int operand1 = stage.indexableRegisters.load(srcReg);
//        int operand2;
//        //     operand2 = stage.indexableRegisters.load(Integer.parseInt(getAuxBits(AUX_SOURCE(1)).toString()));
//        //     operand2 = Integer.parseInt(getAuxBits(AUX_IMMEDIATE(0)).toString());
//        int result = operand1 - operand2;
//        addAuxBits(AUX_RESULT, new Term(result));
//        addAuxBits(AUX_FINISHED, AUX_TRUE);
//    }

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
    }

    public void executeCopy(ExecuteStage stage)
    {
        setResult(0, new Term(getAuxBits(AUX_SOURCE(0)).toInt(), false));
    }

    public void executeHalt(ExecuteStage stage)
    {
        // Just copies the result value. Does not cease
        setResult(0, new Term(getAuxBits(AUX_SOURCE(0)).toInt(), false));
    }

    public void executeError(PipelineStage ignored)
    {
        System.out.println("EXECUTION ERROR: " + Long.parseLong(word.toString().substring(HEADER_SIZE)));
    }
}
