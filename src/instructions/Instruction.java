package instructions;

import memory.MemoryModule;
import memory.MemoryRequest;
import pipeline.*;

import java.util.*;

import static instructions.Instructions.*;
import static main.GLOBALS.*;

public class Instruction
{
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
        if(HEADERS.get(word.toString().substring(0, TYPECODE_SIZE + OPCODE_SIZE)) == null)
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
        return HEADERS.get(word.toString().substring(0, TYPECODE_SIZE + OPCODE_SIZE));
    }

    public static final int MAX_REG_ARGS = 16;  // Should never be more than (3? 4?)

    public String[] getSourceRegs()
    {
        // IN SIGNED BASE 10
        List<String> retList = new ArrayList<>();
        for(int i = 0; i < MAX_REG_ARGS; i++)
        {
            Term sourceTerm = getAuxBits(AUX_SOURCE(i));
            if(sourceTerm == null) { break; }

            String prefix = AUX_EQUALS(getAuxBits(AUX_SOURCE_TYPE(i)), AUX_SOURCE_TYPE_REGISTER) ? prefixes.get(getAuxBits(AUX_SOURCE_BANK(i)).toInt()) : "";
            if(AUX_EQUALS(getAuxBits(AUX_SOURCE(i) + DecodeStage.READ), AUX_TRUE)) { prefix = ""; }
            retList.add(prefix.isEmpty() ? " -1" : (prefix + Integer.toString(sourceTerm.toInt())));
        }
        return retList.toArray(new String[] {});
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
        return ret;
    }

    public int[] getNegativeConditionChecks()
    {
        // bitmasks
        int[] ret = new int[3]; // TODO : Add others
        if(getHeader().equals(HEADER.BRANCH_IF_NEGATIVE))
        {
            ret[0] = CC_NEGATIVE_POSITIVE_MASK;
        }
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

            case HEADER.BRANCH_IF_NEGATIVE -> executeBranchIfNegative((ExecuteStage)invoker);  // Should never be called

            case HEADER.INT_ADD -> executeIntAdd((ExecuteStage)invoker);

            case HEADER.COMPARE -> executeCompare((ExecuteStage)invoker);

            case HEADER.LOAD_PC -> executeLoadPC((FetchStage)invoker);
            case HEADER.EXECUTION_ERR -> executeError(invoker);
        }


        // TODO : NOTE: All memory-accessing instructions must have execute() called when they're received into the
        //              appropriate PipelineStage AND again when that stage begins its next execution!
        //              This is performed in the PipelineStages. Make sure to account for it here!
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
            addAuxBits(KEY, new Term(cache.load(activeRequest)[0], false, 32));
        }
        if(activeRequest.isEmpty() && !isFinished())
        {
            addAuxBits(AUX_RESULT, getAuxBits(KEY));
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
        //TODO: make more modular in line with ISA (currently only accepting register sources)
        //      Register values read in DecodeStage
        addAuxBits(AUX_RESULT(0), new Term(getAuxBits(AUX_SOURCE(0)).toInt() + getAuxBits(AUX_SOURCE(1)).toInt()));
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

    public void executeBranchIfNegative(ExecuteStage stage)
    {
        return;  // Should never be called
    }

    public void executeCompare(ExecuteStage stage)
    {
        int src0 = getAuxBits(AUX_SOURCE(0)).toInt();
        int src1 = getAuxBits(AUX_SOURCE(1)).toInt();

        int result = src0 - src1;

        int newCC = (int)stage.internalRegisters.load(CC_INDEX);
        newCC = (result == 0) ? NEW_CC_ZERO(newCC) : NEW_CC_NONZERO(newCC);
        newCC = (result < 0) ? NEW_CC_NEGATIVE(newCC) : NEW_CC_POSITIVE(newCC);

        setResult(0, new Term(newCC, false));
    }

    public void executeError(PipelineStage ignored)
    {
        System.out.println("EXECUTION ERROR: " + Long.parseLong(word.toString().substring(HEADER_SIZE)));
    }
}
