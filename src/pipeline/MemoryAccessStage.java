package pipeline;

import instructions.Instruction;
import memory.MemoryModule;
import static instructions.Instructions.*;

public class MemoryAccessStage extends PipelineStage
{
    private MemoryModule nearestInstructionCache;
    private MemoryModule nearestDataCache;

    public MemoryAccessStage(int wordSize, String name, MemoryModule nearestInstructionCache, MemoryModule nearestDataCache)
    {
        super(wordSize, name);
        this.nearestInstructionCache = nearestInstructionCache;
        this.nearestDataCache = nearestDataCache;
    }

    @Override
    public void setNearestInstructionCache(MemoryModule module)
    {
        this.nearestInstructionCache = module;
        super.setNearestInstructionCache(module);
    }

    @Override
    public void setNearestDataCache(MemoryModule module)
    {
        this.nearestDataCache = module;
        super.setNearestDataCache(module);
    }

    @Override
    public Instruction execute(boolean nextStatus) throws MRAException
    {
        if(MEMORY_INSTRUCTIONS.contains(heldInstruction.getHeader()))  // TODO : Instruction.execute() should be able to properly handle being called after AUX_FINISHED is marked as TRUE
        {
            heldInstruction.execute(this);
        }
        else
        {
            heldInstruction.addAuxBits(AUX_FINISHED, AUX_TRUE);
        }

        if(AUX_TRUE(heldInstruction.getAuxBits(AUX_FINISHED)))  // TODO : Memory instructions should not mark their results until memory access requests are done cycling
        {
            return pass(nextStatus);
        }
        return passBlocking();
    }
}
