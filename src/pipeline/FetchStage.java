package pipeline;

import memory.MemoryModule;
import memory.RegisterFileModule;
import static main.GLOBALS.*;
import instructions.Instruction;
import static instructions.Instructions.*;

public class FetchStage extends PipelineStage
{
    public final RegisterFileModule internalRegisters;
    public MemoryModule nearestInstructionCache;

    public FetchStage(int wordSize, String name,
                      RegisterFileModule internalRegisters, MemoryModule nearestInstructionCache)
    {
        super(wordSize, name);
        this.internalRegisters = internalRegisters;
        this.nearestInstructionCache = nearestInstructionCache;
        heldInstruction = LOAD_PC(wordSize);
    }

    @Override
    public void setNearestInstructionCache(MemoryModule module)
    {
        this.nearestInstructionCache = module;
        super.setNearestInstructionCache(module);
    }

    @Override
    protected Instruction passUnblocked() throws MRAException
    {
        // Increment PC then return unblocked
        internalRegisters.store(PC_INDEX, internalRegisters.load(PC_INDEX) + ((wordSize == 32) ? 1 : 2));
        Instruction ret = super.passUnblocked();
        return ret;
    }

    @Override
    protected Instruction getDefaultInstruction(int wordSize)
    {
        return LOAD_PC(wordSize);
    }

    @Override
    protected Instruction pass(boolean nextStatus) throws MRAException
    {
        if(!heldInstruction.isFinished())
        {
            return passBlocking();
        }
        if(heldInstruction.isFinished() && !nextStatus)
        {
            heldInstruction = new Instruction(heldInstruction.getAuxBits(AUX_RESULT));
            Instruction ret = passUnblocked();
            heldInstruction = null;
            return ret;
        }
        return passBlocked();
    }

    public void preExecute() throws MRAException
    {
        heldInstruction = super.execute(false);
        heldInstruction.execute(this);
    }

    @Override
    public Instruction execute(boolean nextIsBlocked) throws MRAException
    {
        // Default execute gives LOAD_PC
        // LOAD_PC should read the value in PC *AND* send out to the cache to get the instruction at that address
        heldInstruction = super.execute(nextIsBlocked);
        heldInstruction.execute(this);
        Instruction ret = pass(nextIsBlocked);
        heldInstruction = super.execute(nextIsBlocked);
        if(!heldInstruction.getHeader().equals(HEADER.LOAD_PC)) { throw new MRAException("Fetch was given an instruction besides LOAD_PC: " + HEADER_STRINGS.get(heldInstruction.getHeader())); }
        return ret;
    }
}
