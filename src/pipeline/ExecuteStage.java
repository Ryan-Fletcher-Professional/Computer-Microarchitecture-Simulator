package pipeline;

import instructions.Instruction;
import instructions.Term;
import memory.RegisterFileModule;
import java.util.List;
import static main.GLOBALS.*;

import static instructions.Instructions.*;

public class ExecuteStage extends PipelineStage
{
    public final RegisterFileModule internalRegisters;

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
        return ret;
    }

    @Override
    public Instruction execute(boolean nextIsBlocked, boolean activePipeline) throws MRAException
    {
        if(ALU_EXECUTE_INSTRUCTIONS.contains(heldInstruction.getHeader()))
        {
            heldInstruction.execute(this);
        }
        if(heldInstruction.getHeader().equals(HEADER.CALL))
        {
            int sign = 1;
            if(((heldInstruction.wordLength() == WORD_SIZE_SHORT) && AUX_EQUALS(heldInstruction.getAuxBits(FLAG(2)), 1)) ||
                ((heldInstruction.wordLength() == WORD_SIZE_LONG) && AUX_EQUALS(heldInstruction.getAuxBits(FLAG(3)), 1)))
                { sign = -1; }
            heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(heldInstruction.getAuxBits(AUX_PC_AT_FETCH).toInt() + (sign * heldInstruction.getAuxBits(AUX_SOURCE(1)).toInt())));
        }
        if(BRANCH_INSTRUCTIONS.contains(heldInstruction.getHeader()))
        {
            boolean branch = true;

            // TODO : getPositiveConditionChecks() returns three (ordered) ints s.t. for each bit in the int that =1,
            //  that bit in the corresponding register of CC, PRED_1, or PRED_2 must be set to 1 in order for branch to
            //  occur.
            int[] checks = heldInstruction.getPositiveConditionChecks();
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(CC)) & checks[0]) != checks[0]) { branch = false; }
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(PRED_1)) & checks[1]) != checks[1]) { branch = false; }
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(PRED_1)) & checks[2]) != checks[2]) { branch = false; }

            // TODO : getNegativeConditionChecks() returns three (ordered) ints s.t. for each bit in the int that =1,
            //  that bit in the corresponding register of CC, PRED_1, or PRED_2 must be set to 0 in order for branch to
            //  occur.
            checks = heldInstruction.getNegativeConditionChecks();
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(CC)) & checks[0]) != 0) { branch = false; }
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(PRED_1)) & checks[1]) != 0) { branch = false; }
            if((internalRegisters.load(List.of(internalRegisters.names).indexOf(PRED_1)) & checks[2]) != 0) { branch = false; }

            Term destinationValue = heldInstruction.getAuxBits(AUX_SOURCE(0));
            // Some branching instructions have main dest adrs as first source
            // For others, this will be null until writeback

            if(branch)
            {
                //System.out.println(heldInstruction.getAuxBits(AUX_SOURCE(0)).toLong());
                heldInstruction.addAuxBits(AUX_RESULT(0), destinationValue);
            }
        }
        Instruction ret = pass(nextIsBlocked && !DISPOSABLE_INSTRUCTIONS.contains(heldInstruction.getHeader()));
        Instruction next = previousStage.execute(nextIsBlocked && !DISPOSABLE_INSTRUCTIONS.contains(heldInstruction.getHeader()), activePipeline);
        if(!(nextIsBlocked && !DISPOSABLE_INSTRUCTIONS.contains(heldInstruction.getHeader()))) { heldInstruction = next; }
        return ret;
    }
}
