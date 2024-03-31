package pipeline;

import instructions.Instruction;
import memory.RegisterFileModule;
import static instructions.Instructions.*;

public class MemoryWritebackStage extends PipelineStage
{
    private final RegisterFileModule indexableRegisters;
    private final RegisterFileModule internalRegisters;
    private final RegisterFileModule callStack;
    private final RegisterFileModule reversalStack;
    private final boolean[][] pendingRegisters;

    public MemoryWritebackStage(int wordSize, String name,
                                RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                                RegisterFileModule callStack, RegisterFileModule reversalStack,
                                boolean[][] pendingRegisters)
    {
        super(wordSize, name);
        this.indexableRegisters = indexableRegisters;
        this.internalRegisters = internalRegisters;
        this.callStack = callStack;
        this.reversalStack = reversalStack;
        this.pendingRegisters = pendingRegisters;
    }

    @Override
    public Instruction execute(boolean nextStatus) throws MRAException
    {
        if(heldInstruction.getAuxBits(AUX_RESULT) != null)
        {
            // TODO : Write to registers
        }
        else if(heldInstruction.getAuxBits(AUX_BRANCH) != null)
        {
            // TODO : Update PC
        }
        else if(heldInstruction.getAuxBits(AUX_JSR) != null)
        {
            // TODO : Handle jump to subroutine
        }
        else if(heldInstruction.getHeader() == HEADER.NOOP)
        {
            // TODO : Record NOOP
        }
        else if(heldInstruction.getHeader() == HEADER.STALL)
        {
            // TODO : Record stall
        }
        return pass(nextStatus);
    }
}
