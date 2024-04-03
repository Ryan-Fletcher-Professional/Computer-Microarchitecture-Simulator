package instructions;

import memory.MemoryModule;
import memory.MemoryRequest;
import memory.RegisterFileModule;
import pipeline.FetchStage;
import pipeline.MRAException;
import pipeline.MemoryAccessStage;
import pipeline.PipelineStage;
import pipeline.ExecuteStage;

import java.util.*;

import static instructions.Instructions.*;
import static main.GLOBALS.*;

public class Instruction
{
    public int id;
    private final Term word;
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

    public String[] getSourceRegs()
    {
        // IN SIGNED BASE 10
        return new String[0]; // TODO
    }

    public String[] getDestRegs()
    {
        // IN SIGNED BASE 10
        return new String[0]; // TODO
    }

    public void setResult(Term term)
    {
        addAuxBits(AUX_RESULT, term);
    }

    public void setResult(Term term, int idx)
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
        return new int[3]; // TODO later
    }

    public int[] getNegativeConditionChecks()
    {
        // bitmasks
        return new int[3]; // TODO later
    }

    public boolean isFinished()
    {
        return AUX_TRUE(Objects.requireNonNullElse(getAuxBits(AUX_FINISHED), AUX_FALSE()));
    }

    public void execute(PipelineStage invoker)
    {
        HEADER header = getHeader();

        switch(header)
        {
            case HEADER.LOAD -> executeLoad((MemoryAccessStage)invoker);

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
            addAuxBits(AUX_RESULT, getAuxBits(KEY));
            addAuxBits(AUX_FINISHED, AUX_TRUE);
        }
    }

    public int[] executeStore(MemoryAccessStage stage) {
        String KEY = "store_w_holding";
        MemoryModule cache = stage.nearestDataCache;
        int[] result = new int[2];

        if (activeRequest == null) {
            //memory request with AUX_SOURCE and AUX_DEST values and store it
            activeRequest = new LinkedList<>(List.of(
                new MemoryRequest(id, cache.getID(),
                                  MEMORY_TYPE.DATA, REQUEST_TYPE.STORE,
                                  new Object[]{getAuxBits(AUX_SOURCE(0)).toInt(),
                                      getAuxBits(AUX_DEST(0)).toInt()})));
            cache.store(activeRequest);
        }
        if (activeRequest.isEmpty() && !isFinished()) {
            addAuxBits(AUX_FINISHED, AUX_TRUE);
        }
        result[0] = getAuxBits(AUX_SOURCE(0)).toInt();
        result[1] = getAuxBits(AUX_DEST(0)).toInt();

        return result;
    }

    // public void executeStore(MemoryAccessStage stage) {
    //     String KEY = "store_w_holding";
    //     MemoryModule cache = stage.nearestDataCache;

    //     if (activeRequest == null) {
    //         activeRequest = new LinkedList<>(List.of(
    //             //memory request
    //             //TODO: check MemoryModule switch source and dest and put the value in an array
    //             new MemoryRequest(id, cache.getID(),
    //                     MEMORY_TYPE.DATA, REQUEST_TYPE.STORE,
    //                     new Object[]{getAuxBits(AUX_DEST(0)).toInt(),
    //                             getAuxBits(AUX_SOURCE(0)).toInt()})));
    //     cache.store(activeRequest);
    //     }
    //     if (activeRequest.isEmpty() && !isFinished()) {
    //     addAuxBits(AUX_FINISHED, AUX_TRUE);
    //     }
    // }

    public void executeAdd() {
        String KEY = "add_w_holding";
        //TODO: make more modular in line with ISA (currently only accepting register sources)
        //get the source and destination registers
        int srcReg2 = Integer.parseInt(getAuxBits(AUX_SOURCE(1)).toString().subtring(1));
        int srcReg = Integer.parseInt(getAuxBits(AUX_SOURCE(0)).toString().subtring(1));
        int destReg = Integer.parseInt(getAuxBits(AUX_DEST(0)).toString().substring(1));
        //get values from source registers
        int operand1 = stage.indexableRegisters.load(srcReg);
        int operand2 = stage.indexableRegisters.load(srcReg2);
        //     operand2 = stage.indexableRegisters.load(Integer.parseInt(getAuxBits(AUX_SOURCE(1)).toString()));
        //     operand2 = Integer.parseInt(getAuxBits(AUX_IMMEDIATE(0)).toString());

        int result = operand1 + operand2;

        addAuxBits(AUX_RESULT, new Term(result));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }

    public void executeSubtract(){
        String KEY = "subtract_w_holding";
        int srcReg = Integer.parseInt(getAuxBits(AUX_SOURCE(0)).toString().substring(1));
        int destReg = Integer.parseInt(getAuxBits(AUX_DEST(0)).toString().substring(1));
        //get the values from source registers
        int operand1 = stage.indexableRegisters.load(srcReg);
        int operand2;
        //     operand2 = stage.indexableRegisters.load(Integer.parseInt(getAuxBits(AUX_SOURCE(1)).toString()));
        //     operand2 = Integer.parseInt(getAuxBits(AUX_IMMEDIATE(0)).toString());
        int result = operand1 - operand2;
        addAuxBits(AUX_RESULT, new Term(result));
        addAuxBits(AUX_FINISHED, AUX_TRUE);
    }
    // public void executeBranch() {
    //     //get memory address/offset
    //     int address = Integer.parseInt(getAuxBits(AUX_IMMEDIATE(0)).toString());

    //     //TODO:
    //     //determine branch condition based on flags or registers
    //     boolean condition =

    //     //if condition is true, update program counter
    //     if (condition) {
    //         stage.internalRegisters.store(PC_INDEX, stage.internalRegisters.load(PC_INDEX) + address);
    //     }
    //     addAuxBits(AUX_FINISHED, AUX_TRUE);
    // }

    //execute compare
    //define condition bit in execute compare
    //branch 0
    //branch is negative and compare have them look at the same condition bit
    //will return the correct condition code
    // will return an array of masks
    public void executeCompare(ExecuteStage stage)
    {
        //define the condition bit
        boolean condition = false;
        //set the condition bit in internal register bank
        stage.internalRegisters.setConditionBit(condition);
        //branch condition
        boolean branchCondition = condition && (stage.internalRegisters.getConditionBit() == 0);
        int conditionCode = getConditionCode(condition, branchCondition);

        // TODO: find negative condition helper method
        int[] masks = (stage.internalRegisters.getConditionBit());
        //update
        addAuxBits(AUX_CONDITION_CODE, new Term(conditionCode, false));
        addAuxBits(AUX_NEGATIVE_CODE_MASKS, new Term(masks));

        // could set the selected bit as 1 or 0?
        //int selectedBit = one of the bits in the condition register
        // stage.internalRegisters.setRegisterBit(selectedBit, condition ? 1 : 0);
    }

    public void executeError(PipelineStage ignored)
    {
        System.out.println("EXECUTION ERROR: " + Long.parseLong(word.toString().substring(HEADER_SIZE)));
    }
}
