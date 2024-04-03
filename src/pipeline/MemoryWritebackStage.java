package pipeline;

import instructions.Instruction;
import memory.RegisterFileModule;

import static instructions.Instructions.*;
import static main.GLOBALS.*;
import static main.GLOBALS.REVERSAL_STACK_INDEX;

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
    public Instruction execute(boolean nextIsBlocked) throws MRAException
    {
        if(AUX_TRUE(heldInstruction.getAuxBits(AUX_JSR)))
        {
            // TODO : Handle jump to subroutine
        }
        else if(heldInstruction.getAuxBits(AUX_RESULT) != null)
        {  // TODO : For instructions with results to multiple registers, have AUX_RESULTS_/AUX_RESULTS(idx) setup and
           //        use getDestRegs() (and make getResults() for good measure
            String dest = heldInstruction.getDestRegs()[0];
            int idx = Integer.parseInt(dest.substring(1));
            if(dest.startsWith(RegisterFileModule.INDEXABLE_PREFIX))
            {
                indexableRegisters.store(idx, heldInstruction.getAuxBits(AUX_RESULT).toInt());
            }
            else if(dest.startsWith(RegisterFileModule.INTERNAL_PREFIX))
            {
                internalRegisters.store(idx, heldInstruction.getAuxBits(AUX_RESULT).toInt());
            }
            else if(dest.startsWith(RegisterFileModule.CALL_PREFIX))
            {
                callStack.store(idx, heldInstruction.getAuxBits(AUX_RESULT).toInt());
            }
            else if(dest.startsWith(RegisterFileModule.REVERSAL_PREFIX)) {
                reversalStack.store(idx, heldInstruction.getAuxBits(AUX_RESULT).toInt());
            }

            Instruction ret = heldInstruction;
            heldInstruction = previousStage.execute(nextIsBlocked);
            return ret;
        }
        else if(heldInstruction.getHeader().equals(HEADER.NOOP))
        {
            // TODO : Record NOOP
        }
        else if(heldInstruction.getHeader().equals(HEADER.STALL))
        {
            // TODO : Record stall
        }
        Instruction ret = heldInstruction;
        Instruction gotten = previousStage.execute(nextIsBlocked);
        if(!nextIsBlocked) { heldInstruction = gotten; }
        return ret;
    }
}
