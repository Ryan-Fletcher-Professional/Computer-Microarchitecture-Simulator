package pipeline;

import instructions.Instruction;
import memory.MemoryModule;
import memory.RegisterFileModule;
import static main.GLOBALS.*;

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
        internalRegisters.store(PC_INDEX, internalRegisters.load(PC_INDEX) + ((wordSize == 32) ? 1 : 2));
        return super.passUnblocked();
    }

    @Override
    protected Instruction pass(boolean nextStatus) throws MRAException
    {
        if(heldInstruction.getAuxBits(AUX_FINISHED) == null)
        {
            return passBlocking();
        }
        if((heldInstruction.getAuxBits(AUX_FINISHED) != null) && !nextStatus)
        {
            heldInstruction = new Instruction(heldInstruction.getAuxBits(AUX_RESULT));
            return passUnblocked();
        }
        return passBlocked();
    }

    @Override
    public Instruction execute(boolean nextStatus) throws MRAException
    {
        // Previous stage should always be dummy LOAD_PC
        // LOAD_PC should read the value in PC *AND* send out to the cache to get the instruction at that address
        if(heldInstruction == null) { heldInstruction = previousStage.execute(false); }

        if(heldInstruction.getAuxBits(AUX_FINISHED) == null)
        {
            if(!heldInstruction.getHeader().equals(HEADER.LOAD_PC)) { throw new MRAException("Fetch was given an instruction besides LOAD_PC: " + HEADER_STRINGS.get(heldInstruction.getHeader())); }
            heldInstruction.execute(this);
        }
        return pass(nextStatus);
    }
}
