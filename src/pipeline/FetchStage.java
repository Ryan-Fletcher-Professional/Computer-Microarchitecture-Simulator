package pipeline;

public class FetchStage extends PipelineStage
{
    public FetchStage(int wordSize, PipelineStage previous, PipelineStage next)
    {
        super(wordSize, previous, next);
    }
}
