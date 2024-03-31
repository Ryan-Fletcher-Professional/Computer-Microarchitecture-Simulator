package pipeline;

public class DecodeStage extends PipelineStage
{
    public DecodeStage(int wordSize, PipelineStage previous, PipelineStage next)
    {
        super(wordSize, previous, next);
    }
}
