package pipeline;

import instructions.Instruction;
import memory.RegisterFileModule;
import java.util.List;
import static main.GLOBALS.*;

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
        if(AUX_TRUE(heldInstruction.getAuxBits(AUX_JSR)))
        {
            int address = computeCurrentPC();
            heldInstruction.addAuxBits(AUX_CURRENT_PC, address);
        }
        if(BRANCH_INSTRUCTIONS.contains(heldInstruction.getHeader()))
        {
            boolean branch = true;

            // TODO : getPositiveConditionChecks() returns three (ordered) ints s.t. for each bit in the int that =1,
            //  that bit in the corresponding register of CC, PRED_1, or PRED_2 must be set to 1 in order for branch to
            //  occur.
            int[] checks = heldInstruction.getPositiveConditionChecks();
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(CC)) & checks[0]) == 0) { branch = false; }
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(PRED_1)) & checks[1]) == 0) { branch = false; }
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(PRED_1)) & checks[2]) == 0) { branch = false; }

            // TODO : getNegativeConditionChecks() returns three (ordered) ints s.t. for each bit in the int that =1,
            //  that bit in the corresponding register of CC, PRED_1, or PRED_2 must be set to 0 in order for branch to
            //  occur.
            checks = heldInstruction.getNegativeConditionChecks();
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(CC)) & checks[0]) != 0) { branch = false; }
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(PRED_1)) & checks[1]) != 0) { branch = false; }
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(PRED_1)) & checks[2]) != 0) { branch = false; }

            // TODO : Implement and use computeTargetAddress() when appropriate
            //  DecodeStage will add aux bits corresponding to branch destinations in AUX_RESULT

            heldInstruction.addAuxBits(AUX_BRANCH, branch ? AUX_TRUE() : AUX_FALSE());
        }
        Instruction ret = pass(nextIsBlocked);
        Instruction next = previousStage.execute(nextIsBlocked);
        if(!nextIsBlocked) { heldInstruction = next; }
        return ret;
    }
}
