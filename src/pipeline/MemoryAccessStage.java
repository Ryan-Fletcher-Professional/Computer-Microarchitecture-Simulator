package pipeline;

import instructions.Instruction;
import static instructions.Instructions.*;
import memory.MemoryModule;
import memory.RegisterFileModule;

import java.util.Objects;

import static instructions.Instructions.*;

public class MemoryAccessStage extends PipelineStage
{
    public RegisterFileModule indexableRegisters;
    public RegisterFileModule internalRegisters;
    public MemoryModule nearestDataCache;
    private int oldID = -1;

    public MemoryAccessStage(int wordSize, String name, RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters, MemoryModule nearestDataCache, int numSpecialInstructions)
    {
        super(wordSize, name, numSpecialInstructions);
        this.indexableRegisters = indexableRegisters;
        this.internalRegisters = internalRegisters;
        this.nearestDataCache = nearestDataCache;
    }

    @Override
    public void setNearestDataCache(MemoryModule module)
    {
        this.nearestDataCache = module;
        super.setNearestDataCache(module);
    }

    public void preExecute()
    {
        if((heldInstruction.id != oldID) && MEMORY_INSTRUCTIONS.contains(heldInstruction.getHeader()))
            { heldInstruction.execute(this); }
    }

    @Override
    public Instruction execute(boolean nextIsBlocked, boolean activePipeline) throws MRAException
    {
        if(MEMORY_INSTRUCTIONS.contains(heldInstruction.getHeader()))
        {
            heldInstruction.execute(this);
        }
        else
        {
            heldInstruction.addAuxBits(AUX_FINISHED_MEMORY_ACCESS_STAGE, AUX_TRUE);
        }

        Instruction ret = pass(nextIsBlocked);
        if(!nextIsBlocked && (heldInstruction.isFinished() || AUX_EQUALS(heldInstruction.getAuxBits(AUX_FINISHED_MEMORY_ACCESS_STAGE), AUX_TRUE)))
        {
            heldInstruction = previousStage.execute(nextIsBlocked, activePipeline);
            oldID = ret.id;
        }
        else
        {
            ret = nextIsBlocked ? ret : passBlocking();
            previousStage.execute(true, activePipeline);
        }
        return ret;
    }
}
