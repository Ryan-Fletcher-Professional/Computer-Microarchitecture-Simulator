package instructions;

import memory.MemoryModule;
import memory.MemoryRequest;
import memory.RegisterFileModule;
import pipeline.FetchStage;
import pipeline.MRAException;
import pipeline.MemoryAccessStage;
import pipeline.PipelineStage;

import java.util.*;

import static instructions.Instructions.*;
import static main.GLOBALS.*;

public class Instruction
{
    private int id;
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

    // TODO : Add methods to get ISA-specific info about instruction

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
        HEADER header = HEADERS.get(word.toString().substring(0, TYPECODE_SIZE + OPCODE_SIZE));
        return header;
    }

    public String[] getSourceRegs()
    {
        return new String[0];  // TODO : Instruction-specific code
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

            case HEADER.EXECUTION_ERR -> executeError(invoker);
        }






        // TODO Others
        // TODO : NOTE: All memory-accessing instructions must have execute() called when they're received into the
        //              appropriate PipelineStage AND again when that stage begins its next execution!
        if(header.equals(HEADER.LOAD_PC))
        {
            MemoryModule cache = ((FetchStage)invoker).nearestInstructionCache;
            if(activeRequest == null)
            {
                activeRequest = new LinkedList<>(List.of(
                    new MemoryRequest(id, cache.getID(),
                                      MEMORY_TYPE.INSTRUCTION, REQUEST_TYPE.LOAD,
                                      new Object[]{(int)(((FetchStage)invoker).internalRegisters.load(PC_INDEX)), false})));
                addAuxBits("load_pc_holding", new Term(cache.load(activeRequest)[0], false, 32));
            }
            if(activeRequest.isEmpty() && !isFinished())
            {
                addAuxBits(AUX_RESULT, getAuxBits("load_pc_holding"));
                addAuxBits(AUX_FINISHED, AUX_TRUE);
            }
        }
    }

    public void executeLoad(MemoryAccessStage stage) {
        String KEY = "load_w_holding";

        MemoryModule cache = stage.nearestDataCache;
        if(activeRequest == null)
        {
            activeRequest = new LinkedList<>(List.of(
                    new MemoryRequest(id, cache.getID(),
                            MEMORY_TYPE.DATA, REQUEST_TYPE.LOAD,
                            new Object[] { getAuxBits(AUX_SOURCE(0)).toInt(), false })));
            addAuxBits(KEY, new Term(cache.load(activeRequest)[0], true));
        }
        if(activeRequest.isEmpty() && !isFinished())
        {
            addAuxBits(AUX_RESULT, getAuxBits(KEY));
            addAuxBits(AUX_FINISHED, AUX_TRUE);
            stage.indexableRegisters.store(getAuxBits(AUX_DEST(0)).toInt(), getAuxBits(KEY).toInt());
        }
    }

    public void executeError(PipelineStage ignored)
    {
        System.out.println("EXECUTION ERROR: " + Long.parseLong(word.toString().substring(HEADER_SIZE)));
    }
}
