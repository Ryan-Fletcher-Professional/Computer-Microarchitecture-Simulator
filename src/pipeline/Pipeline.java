package pipeline;

import instructions.Instruction;
import memory.MemoryModule;
import memory.RegisterFileModule;
import static main.GLOBALS.*;
import static instructions.Instructions.*;

public class Pipeline
{
    private RegisterFileModule indexableRegisters;
    private RegisterFileModule internalRegisters;
    private RegisterFileModule callStack;
    private RegisterFileModule reversalStack;
    private MemoryModule nearestInstructionCache;
    private MemoryModule nearestDataCache;
    private boolean[][] pendingRegisters;
    private int wordSize;
    private FetchStage fetch;
    private ExecuteStage execute;
    private MemoryAccessStage access;
    private MemoryWritebackStage write;
    private PipelineStage endStage;  // This and dummyStartStage exist to prevent erroneous consecutation of actual pipeline stages

    public Pipeline(RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                    RegisterFileModule callStack, RegisterFileModule reversalStack,
                    MemoryModule nearestInstructionCache, MemoryModule nearestDataCache,
                    boolean[][] pendingRegisters, int wordSize)
    {
        initialize(indexableRegisters, internalRegisters, callStack, reversalStack,
                   nearestInstructionCache, nearestDataCache, pendingRegisters, wordSize);
    }

    // TODO : Cycling with a different word size from before will call this.
    /**
     * NOTE: WILL FLUSH THE PIPELINE IF WORD SIZE CHANGES
     * @param size
     */
    public void setWordSize(int size)
    {
        wordSize = size;
        PipelineStage currentStage = endStage;
        while(currentStage != null)
        {
            currentStage.setWordSize(size);
            currentStage = currentStage.previousStage;
        }
    }

    public int getWordSize()
    {
        return wordSize;
    }

    public void openWrite()
    {
        write.heldInstruction = NOOP(wordSize);
    }

    public boolean preExecute()
    {
        Object[] writeRet = write.preExecute();
        boolean branching = (boolean)writeRet[0];
        Instruction aboutToWrite = (Instruction)writeRet[1];
        boolean aboutToWriteHalt = aboutToWrite.getHeader().equals(HEADER.HALT);
        Instruction aboutToAccess = access.heldInstruction;
        Instruction aboutToExecute = execute.heldInstruction;
        boolean waitingOnHalt = aboutToWriteHalt || aboutToAccess.getHeader().equals(HEADER.HALT) || aboutToExecute.getHeader().equals(HEADER.HALT);
        if(!branching && !aboutToWriteHalt)
        {
            try
            {
                if(!waitingOnHalt)
                {
                    fetch.preExecute();
                }
            }
            catch(MRAException e)
            {
                e.printStackTrace();
            }
            access.preExecute();
        }
        return aboutToWriteHalt;
    }

    public Instruction execute()
    {
        Instruction ret = null;
        try
        {
            ret = this.endStage.execute(false);
        }
        catch(MRAException e)
        {
            e.printStackTrace();
        }
        return ret;
    }


    private void initialize(RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                            RegisterFileModule callStack, RegisterFileModule reversalStack,
                            MemoryModule nearestInstructionCache, MemoryModule nearestDataCache, boolean[][] pendingRegisters,
                            int wordSize)
    {
        this.indexableRegisters = indexableRegisters;
        this.internalRegisters = internalRegisters;
        this.callStack = callStack;
        this.reversalStack = reversalStack;
        this.nearestInstructionCache = nearestInstructionCache;
        this.wordSize = wordSize;
        fetch = new FetchStage(wordSize, "Fetch", internalRegisters, nearestInstructionCache);
        DecodeStage decode = new DecodeStage(wordSize, "Decode", indexableRegisters, internalRegisters, callStack, reversalStack, pendingRegisters);
        execute = new ExecuteStage(wordSize, "Execute", internalRegisters);
        access = new MemoryAccessStage(wordSize, "Access", indexableRegisters, internalRegisters, nearestDataCache);
        write = new MemoryWritebackStage(wordSize, "Write",
                                                              indexableRegisters, internalRegisters, callStack, reversalStack, pendingRegisters);
        this.endStage = write;
        PipelineStage.CONSECUTE(new PipelineStage[] { fetch, decode, execute, access, write });
    }

    public void setNearestInstructionCache(MemoryModule module)
    {
        this.nearestInstructionCache = module;
        endStage.setNearestInstructionCache(module);
    }
    public void setNearestDataCache(MemoryModule module)
    {
        this.nearestDataCache = module;
        endStage.setNearestDataCache(module);
    }


    public String getDisplayText(int radix)
    {
        return (endStage.previousStage == null) ? "No pipeline stages" : endStage.getDisplayText(radix);
    }

    public void reset()
    {
        initialize(indexableRegisters, internalRegisters, callStack, reversalStack, nearestInstructionCache, nearestDataCache, NEW_PENDING_REGISTERS(new RegisterFileModule[] { indexableRegisters, internalRegisters, callStack, reversalStack }), wordSize);
    }
}
