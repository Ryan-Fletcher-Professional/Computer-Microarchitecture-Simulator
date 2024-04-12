package pipeline;

import instructions.Instruction;
import memory.RegisterFileModule;

import java.util.Arrays;

import static main.GLOBALS.*;

import static instructions.Instructions.*;

public class MemoryWritebackStage extends PipelineStage
{
    private final RegisterFileModule indexableRegisters;
    private final RegisterFileModule internalRegisters;
    private final RegisterFileModule callStack;
    private final RegisterFileModule reversalStack;
    private final int[][] pendingRegisters;

    public MemoryWritebackStage(int wordSize, String name,
                                RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                                RegisterFileModule callStack, RegisterFileModule reversalStack,
                                int[][] pendingRegisters)
    {
        super(wordSize, name);
        this.indexableRegisters = indexableRegisters;
        this.internalRegisters = internalRegisters;
        this.callStack = callStack;
        this.reversalStack = reversalStack;
        this.pendingRegisters = pendingRegisters;
    }

    public Object[] preExecute()
    {
        // Return whether will branch this cycle
        boolean branching = false;
        if(AUX_EQUALS(heldInstruction.getAuxBits(AUX_JSR), AUX_TRUE) ||
            ((heldInstruction.getResult(0) != null) && BRANCH_INSTRUCTIONS.contains(heldInstruction.getHeader())))
        {
            branching = true;
        }
        return new Object[] { branching, heldInstruction };
    }

    @Override
    public Instruction execute(boolean nextIsBlocked, boolean activePipeline) throws MRAException
    {
        HEADER header = heldInstruction.getHeader();
        boolean branched = false;

        if(AUX_EQUALS(heldInstruction.getAuxBits(AUX_JSR), AUX_TRUE))
        {
            // TODO : Handle jump to subroutine
            branched = true;
        }
        else if(heldInstruction.getResult(0) != null)
        {
            String[] destRegs = heldInstruction.getDestRegs();
//            System.out.println(header + Arrays.toString(destRegs));
            if(BRANCH_INSTRUCTIONS.contains(header))
            {
                branched = true;
            }

            for(int i = 0; heldInstruction.getAuxBits(AUX_RESULT(i)) != null; i++)
            {
                //System.out.println(header + " " + i);
                String dest = destRegs[i];
                int idx = Integer.parseInt(dest.substring(1));
                if(dest.startsWith(RegisterFileModule.INDEXABLE_PREFIX))
                {
                    indexableRegisters.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[INDEXABLE_BANK_INDEX][idx]--;
                }
                else if(dest.startsWith(RegisterFileModule.INTERNAL_PREFIX))
                {
                    internalRegisters.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[INTERNAL_BANK_INDEX][idx]--;
                }
                else if(dest.startsWith(RegisterFileModule.CALL_PREFIX))
                {
                    callStack.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[CALL_STACK_INDEX][idx]--;
                }
                else if(dest.startsWith(RegisterFileModule.REVERSAL_PREFIX))
                {
                    reversalStack.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[REVERSAL_STACK_INDEX][idx]--;
                }
            }

            Instruction ret = heldInstruction;
            if(!branched)
            {
                heldInstruction = previousStage.execute(nextIsBlocked, activePipeline);
            }
            else
            {
                heldInstruction = previousStage.quashFromBranch();
                for(int[] bank : pendingRegisters)
                {
                    Arrays.fill(bank, 0);
                }
            }
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
        Instruction gotten = previousStage.execute(nextIsBlocked, activePipeline);
        if(!nextIsBlocked) { heldInstruction = gotten; }
        return ret;
    }
}
