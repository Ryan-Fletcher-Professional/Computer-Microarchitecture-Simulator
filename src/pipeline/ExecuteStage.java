package pipeline;

import instructions.Instruction;
import instructions.Term;
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
    public Instruction execute(boolean nextStatus) throws MRAException
    {
        if(ALU_INSTRUCTIONS.contains(heldInstruction.getHeader()))
        {
            heldInstruction.execute();
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
        return pass(nextStatus);
    }
}
