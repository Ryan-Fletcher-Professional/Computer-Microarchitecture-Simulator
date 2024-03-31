package pipeline;

import instructions.Instruction;
import static instructions.Instructions.*;

public class PipelineStage
{
    private final int wordSize;
    private PipelineStage previousStage;
    private PipelineStage nextStage;
    private boolean blocked = false;

    public PipelineStage(int wordSize)
    {
        this.wordSize = wordSize;
    }

    public void setPrevious(PipelineStage previous)
    {
        if(previousStage != null) { throw new UnsupportedOperationException("Previous stage is already assigned"); }
        previousStage = previous;
    }

    public void setNext(PipelineStage next)
    {
        if(previousStage != null) { throw new UnsupportedOperationException("Following stage is already assigned"); }
        nextStage = next;
    }

    public boolean isBlocked()
    {
        return blocked;
    }

    /**
     * SHOULD BE EXTENDED BY CHILD CLASSES
     * @param nextStatus Blocked status of following stage in pipeline.
     * @return Whether this stage IS blocked.
     */
    public Instruction execute(boolean nextStatus) throws MRAException
    {
        Instruction ret = this.isBlocked() ? STALL(wordSize) : NOOP(wordSize);
        if(previousStage != null) { ret = previousStage.execute(this.isBlocked()); }
        return ret;
    }

    public static void CONSECUTE(PipelineStage[] stages)
    {
        for(int i = 0; i < stages.length - 1; i++)
        {
            stages[i].setNext(stages[i + 1]);
            stages[i + 1].setPrevious(stages[i]);
        }
    }
}
