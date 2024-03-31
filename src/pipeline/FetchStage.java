package pipeline;

import instructions.Instruction;
import memory.MemoryModule;
import memory.RegisterFileModule;
import static instructions.Instructions.*;

public class FetchStage extends PipelineStage
{
    private final RegisterFileModule internalRegisters;
    private final MemoryModule nearestCache;

    public FetchStage(int wordSize, String name,
                      RegisterFileModule internalRegisters, MemoryModule nearestCache)
    {
        super(wordSize, name);
        this.internalRegisters = internalRegisters;
        this.nearestCache = nearestCache;
    }

    @Override
    public Instruction execute(boolean nextStatus) throws MRAException
    {
        // Previous stage should always be dummy LOAD_PC
        // LOAD_PC should read the value in PC *AND* send out to the cache to get the instruction at that address
        if(heldInstruction == null) { heldInstruction = previousStage.execute(false); }

        if(heldInstruction.getAuxBits(AUX_FINISHED) == null)
        {
            heldInstruction.execute();
            heldInstruction = new Instruction(heldInstruction.getAuxBits(AUX_RESULT));
        }

        return pass(nextStatus);
    }
}
