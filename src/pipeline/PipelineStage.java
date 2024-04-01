package pipeline;

import instructions.Instruction;
import memory.MemoryModule;

import static instructions.Instructions.*;
import static main.GLOBALS.SMART_TO_STRING;

public class PipelineStage
{
    private final String name;
    protected int wordSize;
    PipelineStage previousStage;
    Instruction heldInstruction;
    private PipelineStage nextStage;
    private boolean blocked = false;

    public PipelineStage(int wordSize, String name)
    {
        this.wordSize = wordSize;
        this.name = name;
        this.heldInstruction = NOOP(wordSize);
    }

    public void setPrevious(PipelineStage previous)
    {
        if(previousStage != null) { throw new UnsupportedOperationException("Previous stage is already assigned"); }
        previousStage = previous;
    }

    public void setNext(PipelineStage next)
    {
        if(nextStage != null) { throw new UnsupportedOperationException("Following stage is already assigned"); }
        nextStage = next;
    }

    public void setWordSize(int size)
    {
        wordSize = size;
        if((heldInstruction != null) && (heldInstruction.wordLength() != size))
        {
            heldInstruction = QUASH_SIZE_ERR(size);
        }
    }

    public void setNearestInstructionCache(MemoryModule module)
    {
        if(previousStage != null)
        {
            previousStage.setNearestInstructionCache(module);
        }
    }

    public void setNearestDataCache(MemoryModule module)
    {
        if(previousStage != null)
        {
            previousStage.setNearestDataCache(module);
        }
    }

    public boolean isBlocked()
    {
        return blocked;
    }

    protected Instruction passUnblocked() throws MRAException
    {
        heldInstruction.addAuxBits(AUX_FINISHED, AUX_FALSE);
        Instruction ret = heldInstruction;
        heldInstruction = previousStage.execute(false);
        return ret;
    }

    protected Instruction passBlocked() throws MRAException
    {
        heldInstruction.addAuxBits(AUX_FINISHED, AUX_TRUE);
        previousStage.execute(true);
        return heldInstruction.getHeader() == HEADER.STALL ? STALL(wordSize) : NOOP(wordSize);
    }

    protected Instruction passBlocking() throws MRAException
    {
        previousStage.execute(true);
        return STALL(wordSize);
    }

    protected Instruction pass(boolean nextStatus) throws MRAException
    {
        if(!nextStatus)
        {
            return passUnblocked();
        }
        else
        {
            return passBlocked();
        }
    }

    /**
     * SHOULD BE EXTENDED BY CHILD CLASSES
     * @param nextStatus Blocked status of following stage in pipeline.
     * @return STALL if this stage is blocked. If not, previousStage.execute() (default NOOP).
     */
    protected Instruction execute(boolean nextStatus) throws MRAException
    {
        Instruction ret = this.isBlocked() ? STALL(wordSize) : LOAD_PC(wordSize);
        if(previousStage != null) { ret = previousStage.execute(this.isBlocked()); }
        return ret;
    }

    public String getDisplayText(int radix)
    {
        if(previousStage == null) { return ""; }
        StringBuilder ret = new StringBuilder();

        String nameString = name + "  :  " + (isBlocked() ? "BLOCKED" : "AVAILABLE");
        String wordString = (heldInstruction != null) ? SMART_TO_STRING(heldInstruction.wordNum(), radix) : "NONE";
        int valueLength = Math.max(nameString.length(), wordString.length());
        StringBuilder currentName = new StringBuilder();
        currentName.append("  ")
                   .append(" ".repeat((valueLength - nameString.length()) / 2))
                   .append(nameString)
                   .append(" ".repeat(valueLength + "  ".length() - currentName.length()))
                   .append("  ");
        ret.append(currentName).append("\n\n");

        StringBuilder currentValue = new StringBuilder();
        currentValue.append("  ").append(wordString).append("  ");
        ret.append(currentValue).append("\n\n");

        ret.append("  ").append("-".repeat(valueLength)).append("  ");

        ret.append("\n").append(previousStage.getDisplayText(radix));
        return ret.toString();
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
