package pipeline;

public class ExecuteStage extends PipelineStage
{
    public ExecuteStage(int wordSize, PipelineStage previous, PipelineStage next)
    {
        super(wordSize, previous, next);
    }
}
