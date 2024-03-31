package pipeline;

import static main.GLOBALS.*;

import instructions.Instruction;
import static instructions.Instructions.*;

import javax.naming.OperationNotSupportedException;

public class PipelineStage
{
    private final int wordSize;
    private final PipelineStage previousStage;
    private final PipelineStage nextStage;
    private boolean blocked = false;

    public PipelineStage(int wordSize, PipelineStage previous, PipelineStage next)
    {
        this.wordSize = wordSize;
        previousStage = previous;
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
    public Instruction execute(boolean nextStatus)
    {
        Instruction ret = NOOP(wordSize);
        if(previousStage != null) { ret = previousStage.execute(isBlocked()); }
        return ret;
    }
}
