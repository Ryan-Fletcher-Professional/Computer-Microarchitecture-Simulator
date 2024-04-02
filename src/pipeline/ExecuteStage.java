package pipeline;

import instructions.Instruction;
import memory.RegisterFileModule;

import static instructions.Instructions.*;

public class ExecuteStage extends PipelineStage
{
    private final RegisterFileModule internalRegisters;

    public ExecuteStage(int wordSize, String name, RegisterFileModule internalRegisters)
    {
        super(wordSize, name);
        this.internalRegisters = internalRegisters;
    }

    private int computeJumpAddress()
    {
        return -1;  // TODO
    }

    private int computeCurrentPC()
    {
        return -1;  // TODO
    }

    private int computeMemoryAddress()
    {
        return -1;  // TODO
    }

    @Override
    protected Instruction pass(boolean nextIsBlocked) throws MRAException
    {
        Instruction ret = super.pass(nextIsBlocked);
        heldInstruction = previousStage.execute(nextIsBlocked);
        return ret;
    }

    @Override
    public Instruction execute(boolean nextIsBlocked) throws MRAException
    {
        if(ALU_INSTRUCTIONS.contains(heldInstruction.getHeader()))
        {
            heldInstruction.execute(this);
        }
        if(JUMP_INSTRUCTIONS.contains(heldInstruction.getHeader()))
        {
            int address = computeJumpAddress();
            heldInstruction.addAuxBits(AUX_JUMP_ADDRESS, address);
        }
        if(heldInstruction.getAuxBits(AUX_JSR) != null)
        {
            int address = computeCurrentPC();
            heldInstruction.addAuxBits(AUX_CURRENT_PC, address);
        }
        if(BRANCH_INSTRUCTIONS.contains(heldInstruction.getHeader()))
        {
            // TODO : Check condition, set flag
        }
        return pass(nextIsBlocked);
    }
}
