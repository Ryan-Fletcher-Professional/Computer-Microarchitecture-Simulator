package pipeline;

import instructions.Instruction;
import memory.MemoryModule;
import memory.RegisterFileModule;

public class Pipeline
{
    private final RegisterFileModule indexableRegisters;
    private final RegisterFileModule internalRegisters;
    private final RegisterFileModule callStack;
    private final RegisterFileModule reversalStack;
    private final MemoryModule nearestCache;
    private final RegisterFileModule pendingRegisters;
    private final int wordSize;
    private final PipelineStage dummyEndStage;  // This and dummyStartStage exist to prevent erroneous consecutation of actual pipeline stages

    public Pipeline(RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                    RegisterFileModule callStack, RegisterFileModule reversalStack,
                    MemoryModule nearestCache, RegisterFileModule pendingRegisters,
                    int wordSize)
    {
        this.indexableRegisters = indexableRegisters;
        this.internalRegisters = internalRegisters;
        this.callStack = callStack;
        this.reversalStack = reversalStack;
        this.nearestCache = nearestCache;
        this.pendingRegisters = pendingRegisters;
        this.wordSize = wordSize;
        PipelineStage dummyStartStage = new PipelineStage(wordSize);
        FetchStage fetch = new FetchStage(wordSize, internalRegisters, nearestCache);
        DecodeStage decode = new DecodeStage(wordSize, indexableRegisters, internalRegisters, callStack, reversalStack, pendingRegisters);
        ExecuteStage execute = new ExecuteStage(wordSize);
        MemoryAccessStage access = new MemoryAccessStage(wordSize,
                                                         nearestCache);
        MemoryWritebackStage write = new MemoryWritebackStage(wordSize,
                                                              indexableRegisters, internalRegisters, callStack, reversalStack, pendingRegisters);
        this.dummyEndStage = new PipelineStage(wordSize);
        PipelineStage.CONSECUTE(new PipelineStage[] { dummyStartStage, fetch, decode, execute, access, write, this.dummyEndStage });
    }

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
}
