package pipeline;

import instructions.Instruction;
import memory.MemoryModule;
import memory.RegisterFileModule;
import static main.GLOBALS.*;

public class Pipeline
{
    private RegisterFileModule indexableRegisters;
    private RegisterFileModule internalRegisters;
    private RegisterFileModule callStack;
    private RegisterFileModule reversalStack;
    private MemoryModule nearestCache;
    private boolean[][] pendingRegisters;
    private int wordSize;
    private PipelineStage dummyEndStage;  // This and dummyStartStage exist to prevent erroneous consecutation of actual pipeline stages

    public Pipeline(RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                    RegisterFileModule callStack, RegisterFileModule reversalStack,
                    MemoryModule nearestCache, boolean[][] pendingRegisters, int wordSize)
    {
        initialize(indexableRegisters, internalRegisters, callStack, reversalStack,
                   nearestCache, pendingRegisters, wordSize);
    }

    // TODO : Cycling with a different word size from before will call this.
    /**
     * NOTE: WILL FLUSH THE PIPELINE IF WORD SIZE CHANGES
     * @param size
     */
    public void setWordSize(int size)
    {
        wordSize = size;
        PipelineStage currentStage = dummyEndStage;
        while(currentStage != null)
        {
            currentStage.setWordSize(size);
            currentStage = currentStage.previousStage;
        }
    }

    // TODO : Call when cycling from the simulator
    public Instruction execute()
    {
        Instruction ret = null;
        try
        {
            ret = this.dummyEndStage.execute(false);
        }
        catch(MRAException e)
        {
            // TODO
        }
        return ret;
    }

    private void initialize(RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                            RegisterFileModule callStack, RegisterFileModule reversalStack,
                            MemoryModule nearestCache, boolean[][] pendingRegisters,
                            int wordSize)
    {
        this.indexableRegisters = indexableRegisters;
        this.internalRegisters = internalRegisters;
        this.callStack = callStack;
        this.reversalStack = reversalStack;
        this.nearestCache = nearestCache;
        this.wordSize = wordSize;
        PipelineStage dummyStartStage = new PipelineStage(wordSize, "dummy start");
        FetchStage fetch = new FetchStage(wordSize, "Fetch", internalRegisters, nearestCache);
        DecodeStage decode = new DecodeStage(wordSize, "Decode", indexableRegisters, internalRegisters, callStack, reversalStack, pendingRegisters);
        ExecuteStage execute = new ExecuteStage(wordSize, "Execute", internalRegisters);
        MemoryAccessStage access = new MemoryAccessStage(wordSize, "Access",
                                                         nearestCache);
        MemoryWritebackStage write = new MemoryWritebackStage(wordSize, "Write",
                                                              indexableRegisters, internalRegisters, callStack, reversalStack, pendingRegisters);
        this.dummyEndStage = new PipelineStage(wordSize, "dummy end");
        PipelineStage.CONSECUTE(new PipelineStage[] { dummyStartStage, fetch, decode, execute, access, write, this.dummyEndStage });
    }

    public String getDisplayText(int radix)
    {
        return (dummyEndStage.previousStage == null) ? "No pipeline stages" : dummyEndStage.previousStage.getDisplayText(radix);
    }

    public void reset()
    {
        initialize(indexableRegisters, internalRegisters, callStack, reversalStack, nearestCache, NEW_PENDING_REGISTERS(new RegisterFileModule[] { indexableRegisters, internalRegisters, callStack, reversalStack }), wordSize);
    }
}
