package pipeline;

import instructions.Instruction;
import memory.MemoryModule;

import java.util.Objects;

import static instructions.Instructions.*;
import static main.GLOBALS.SMART_TO_STRING;

public class PipelineStage
{
    private final String name;
    protected int wordSize;
    PipelineStage previousStage;
    Instruction heldInstruction;
    private PipelineStage nextStage;
    private boolean blocking = false;

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

    protected Instruction getDefaultInstruction(int wordSize)
    {
        return NOOP(wordSize);
    }

    public boolean isBlocking()
    {
        return blocking;
    }

    protected Instruction passUnblocked() throws MRAException
    {
        blocking = false;
        return heldInstruction;
    }

    protected Instruction passBlocked()
    {
        blocking = false;
        return STALL(wordSize);
    }

    protected Instruction passBlocking()
    {
        blocking = true;
        return STALL(wordSize);
    }

    protected Instruction pass(boolean obstructed) throws MRAException
    {
        if(!obstructed)
        {
            return passUnblocked();
        }
        else
        {
            return passBlocked();
        }
    }

    public Instruction quashFromBranch()
    {
        if(previousStage != null)
        {
            previousStage.quashFromBranch();
            heldInstruction = QUASH_BRANCH(wordSize);
        }
        else
        {
            heldInstruction = null;
        }
        return QUASH_BRANCH(wordSize);
    }

    /**
     * SHOULD BE EXTENDED (and sometimes called at start of child.execute()) BY CHILD CLASSES.
     * Should not be used as model behavior.
     * @param nextIsBlocked Blocked status of following stage in pipeline.
     * @return STALL if this stage is blocked. If not, previousStage.execute() (default NOOP).
     */
    protected Instruction execute(boolean nextIsBlocked) throws MRAException
    {
        if(heldInstruction == null) { heldInstruction = getDefaultInstruction(wordSize); }
        Instruction ret = heldInstruction;
        heldInstruction = (previousStage != null) ? previousStage.execute(nextIsBlocked) : null;
        return ret;
    }

    public String getDisplayText(int radix)
    {
        StringBuilder ret = new StringBuilder();

        String nameString = name + "  :  " + (isBlocking() ? "BLOCKING" : "AVAILABLE");
        String wordString = (heldInstruction != null) ? SMART_TO_STRING(heldInstruction.wordNum(), radix) : "NONE";
        if(!wordString.equals("NONE"))
        {
            int sizeDif = heldInstruction.wordLength() - wordString.length();
            if(sizeDif > 0) { wordString = "0".repeat(sizeDif) + wordString; }
            if(radix == 2)
            {
                String mnemonic = Objects.requireNonNullElse(MNEMONICS.get(heldInstruction.getHeader()),
                                          Objects.requireNonNullElse(INTERNAL_MNEMONICS.get(heldInstruction.getHeader()),
                                                  "UNRECOGNIZED"));
                wordString = mnemonic + " " +
                        wordString.substring(TYPECODE_SIZE + OPCODE_SIZE);
            }
        }
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

        ret.append("\n").append((previousStage == null) ? "" : previousStage.getDisplayText(radix));
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
