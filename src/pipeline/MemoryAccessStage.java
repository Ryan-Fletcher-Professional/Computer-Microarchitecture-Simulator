package pipeline;

import memory.MemoryModule;

public class MemoryAccessStage extends PipelineStage
{
    private final MemoryModule nearestCache;

    public MemoryAccessStage(int wordSize, MemoryModule nearestCache)
    {
        super(wordSize);
        this.nearestCache = nearestCache;
    }
}
