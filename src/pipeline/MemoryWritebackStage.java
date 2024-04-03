package pipeline;

import instructions.Instruction;
import memory.RegisterFileModule;
import static main.GLOBALS.*;

import java.util.Arrays;

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
    public Instruction execute(boolean nextIsBlocked) throws MRAException
    {
        HEADER header = heldInstruction.getHeader();

        if(AUX_EQUALS(heldInstruction.getAuxBits(AUX_JSR), AUX_TRUE))
        {
            // TODO : Handle jump to subroutine
        }
        else if(heldInstruction.getResult(0) != null)
        {
            String[] destRegs = heldInstruction.getDestRegs();
//            System.out.println(Arrays.toString(destRegs));
            for(int i = 0; heldInstruction.getAuxBits(AUX_RESULT(i)) != null; i++)
            {
                System.out.println(i);
                String dest = destRegs[i];
                int idx = Integer.parseInt(dest.substring(1));
                if(dest.startsWith(RegisterFileModule.INDEXABLE_PREFIX))
                {
                    indexableRegisters.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[INDEXABLE_BANK_INDEX][idx] = false;
                }
                else if(dest.startsWith(RegisterFileModule.INTERNAL_PREFIX))
                {
                    internalRegisters.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[INTERNAL_BANK_INDEX][idx] = false;
                }
                else if(dest.startsWith(RegisterFileModule.CALL_PREFIX))
                {
                    callStack.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[CALL_STACK_INDEX][idx] = false;
                }
                else if(dest.startsWith(RegisterFileModule.REVERSAL_PREFIX))
                {
                    reversalStack.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[REVERSAL_STACK_INDEX][idx] = false;
                }
            }

            Instruction ret = heldInstruction;
            heldInstruction = previousStage.execute(nextIsBlocked);
            return ret;
        }
        else if(header.equals(HEADER.NOOP))
        {
            // TODO : Record NOOP
        }
        else if(header.equals(HEADER.STALL))
        {
            // TODO : Record stall
        }
        Instruction ret = heldInstruction;
        Instruction gotten = previousStage.execute(nextIsBlocked);
        if(!nextIsBlocked) { heldInstruction = gotten; }
        return ret;
    }
}
