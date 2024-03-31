package pipeline;

import memory.MemoryModule;
import memory.RegisterFileModule;

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
}
