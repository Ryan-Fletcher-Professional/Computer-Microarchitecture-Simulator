package pipeline;

import memory.MemoryModule;
import memory.RegisterFileModule;

public class FetchStage extends PipelineStage
{
    private final RegisterFileModule internalRegisters;
    private final MemoryModule nearestCache;

    public FetchStage(int wordSize,
                      RegisterFileModule internalRegisters, MemoryModule nearestCache)
    {
        super(wordSize);
        this.internalRegisters = internalRegisters;
        this.nearestCache = nearestCache;
    }
}
