package pipeline;

public class MemoryWritebackStage extends PipelineStage
{
    public MemoryWritebackStage(int wordSize, PipelineStage previous, PipelineStage next)
    {
        super(wordSize, previous, next);
    }
}
