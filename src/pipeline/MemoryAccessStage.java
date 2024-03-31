package pipeline;

import memory.MemoryModule;

public class MemoryAccessStage extends PipelineStage
{
    private final MemoryModule nearestCache;

    public MemoryAccessStage(int wordSize, String name, MemoryModule nearestCache)
    {
        super(wordSize, name);
        this.nearestCache = nearestCache;
    }
}
