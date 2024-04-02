package pipeline;

import instructions.Instruction;
import memory.MemoryModule;

import java.util.Objects;

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
    public Instruction execute(boolean nextIsBlocked) throws MRAException
    {
        if(MEMORY_INSTRUCTIONS.contains(heldInstruction.getHeader()))  // TODO : Instruction.execute() should be able to properly handle being called after AUX_FINISHED is marked as TRUE
        {
            heldInstruction.execute(this);
        }
        else
        {
            heldInstruction.addAuxBits(AUX_FINISHED_MEMORY_ACCESS_STAGE, AUX_TRUE);
        }

        Instruction ret = pass(nextIsBlocked);
        if(!nextIsBlocked && (heldInstruction.isFinished() || AUX_TRUE(Objects.requireNonNullElse(heldInstruction.getAuxBits(AUX_FINISHED_MEMORY_ACCESS_STAGE), AUX_FALSE()))))  // TODO : Memory instructions should not mark their results until memory access requests are done cycling
        {
            heldInstruction = previousStage.execute(nextIsBlocked);
        }
        else
        {
            previousStage.execute(true);
        }
        return ret;
    }
}
