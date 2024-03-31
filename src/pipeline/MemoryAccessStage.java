package pipeline;

public class MemoryAccessStage extends PipelineStage
{
    public MemoryAccessStage(int wordSize, PipelineStage previous, PipelineStage next)
    {
        super(wordSize, previous, next);
    }
}
