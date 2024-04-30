package pipeline;

import instructions.Instruction;
import memory.RegisterFileModule;
import main.Simulator;

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
    public Simulator simulator;

    public MemoryWritebackStage(int wordSize, String name,
                                RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                                RegisterFileModule callStack, RegisterFileModule reversalStack,
                                int[][] pendingRegisters, int numSpecialInstructions)
    {
        super(wordSize, name, numSpecialInstructions);
        this.indexableRegisters = indexableRegisters;
        this.internalRegisters = internalRegisters;
        this.callStack = callStack;
        this.reversalStack = reversalStack;
        this.pendingRegisters = pendingRegisters;
    }

    public void fakeUndo(int quantity, int skip)
    {
        // Undo back to desired frame
        for(int r = 0; r < indexableRegisters.getNumRegisters(); r++)
        {
            indexableRegisters.store(indexableRegisters.getNumRegisters() - 1 - r, reversalStack.peek(((skip + quantity) * (indexableRegisters.getNumRegisters() + 1)) + r));
        }

        // Redo skipped undos (compare each frame to previous one to see actual changes)
        for(int i = skip - 1; i >= 0; i--)
        {
            int mask = (int)reversalStack.peek((i * (indexableRegisters.getNumRegisters() + 1)) + indexableRegisters.getNumRegisters());
            for(int r = 0; r < indexableRegisters.getNumRegisters(); r++)
            {
                if((mask & (1 << r)) != 0)
                {
                    indexableRegisters.store(indexableRegisters.getNumRegisters() - 1 - r, reversalStack.peek((i * (indexableRegisters.getNumRegisters() + 1)) + r));
                }
            }
        }

        // Clear out all looked-at frames (including skipped ones)
        for(int i = 0; i < (skip + quantity) * (indexableRegisters.getNumRegisters() + 1); i++)
        {
            reversalStack.load();
        }
    }

    public Object[] preExecute()
    {
        // Return whether will branch this cycle
        boolean branching = false;
        if(heldInstruction.getHeader().equals(HEADER.RETURN) ||
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

//        System.out.println(heldInstruction.getHeader() + " " + heldInstruction.getAuxBits(AUX_RESULT(0)));

        if(heldInstruction.getHeader().equals(HEADER.UNDO))
        {
            int quantity = heldInstruction.getAuxBits(AUX_SOURCE(0)).toInt();
            int skip = heldInstruction.getAuxBits(AUX_SOURCE(1)).toInt();

            // Undo back to desired frame
            for(int r = 0; r < indexableRegisters.getNumRegisters(); r++)
            {
                indexableRegisters.store(indexableRegisters.getNumRegisters() - 1 - r, reversalStack.peek(((skip + quantity) * (indexableRegisters.getNumRegisters() + 1)) + r));
            }

            // Redo skipped undos (compare each frame to previous one to see actual changes)
            for(int i = skip - 1; i >= 0; i--)
            {
                int mask = (int)reversalStack.peek((i * (indexableRegisters.getNumRegisters() + 1)) + indexableRegisters.getNumRegisters());
                for(int r = 0; r < indexableRegisters.getNumRegisters(); r++)
                {
                    if((mask & (1 << r)) != 0)
                    {
                        indexableRegisters.store(indexableRegisters.getNumRegisters() - 1 - r, reversalStack.peek((i * (indexableRegisters.getNumRegisters() + 1)) + r));
                    }
                }
            }

            // Clear out all looked-at frames (including skipped ones)
            for(int i = 0; i < (skip + quantity) * (indexableRegisters.getNumRegisters() + 1); i++)
            {
                reversalStack.load();
            }

            // Drop pendings
            for(int r = 0; r < indexableRegisters.getNumRegisters(); r++)
            {
                pendingRegisters[INDEXABLE_BANK_INDEX][r]--;
            }
        }
        else if(heldInstruction.getHeader().equals(HEADER.CALL))
        {
            // ExecuteStage switches this from return shift to actual return address
            callStack.store(heldInstruction.getAuxBits(AUX_SOURCE(1)).toLong());
            for(int r = 1; r < indexableRegisters.getNumRegisters(); r++)
            {
                callStack.store(indexableRegisters.load(r));
            }
            internalRegisters.store(PC_INDEX, heldInstruction.getResult(0).toLong());
            pendingRegisters[INTERNAL_BANK_INDEX][PC_INDEX]--;
            Instruction ret = heldInstruction;
            heldInstruction = previousStage.quashFromBranch();
            for(int[] bank : pendingRegisters)
            {
                Arrays.fill(bank, 0);
            }
            return ret;
        }
        else if(heldInstruction.getHeader().equals(HEADER.RETURN))
        {
            for(int r = 0; r < indexableRegisters.getNumRegisters() - 1; r++)
            {
                indexableRegisters.store(indexableRegisters.getNumRegisters() - 1 - r, callStack.load());
            }
            internalRegisters.store(PC_INDEX, callStack.load());
            pendingRegisters[INTERNAL_BANK_INDEX][PC_INDEX]--;
            Instruction ret = heldInstruction;
            heldInstruction = previousStage.quashFromBranch();
            for(int[] bank : pendingRegisters)
            {
                Arrays.fill(bank, 0);
            }
            return ret;
        }
        else if(heldInstruction.getResult(0) != null)
        {
            String[] destRegs = heldInstruction.getDestRegs();
//            System.out.println(header + Arrays.toString(destRegs));
            if(BRANCH_INSTRUCTIONS.contains(header))
            {
                branched = true;
            }

            int wroteToIndexable = 0;

            for(int i = 0; heldInstruction.getAuxBits(AUX_RESULT(i)) != null; i++)
            {
                //System.out.println(header + " " + i);
                String dest = destRegs[i];
                int idx = Integer.parseInt(dest.substring(1));
                if(dest.startsWith(RegisterFileModule.INDEXABLE_PREFIX))
                {
                    indexableRegisters.store(idx, heldInstruction.getResult(i).toInt());
                    pendingRegisters[INDEXABLE_BANK_INDEX][idx]--;
                    wroteToIndexable |= (1 << (indexableRegisters.getNumRegisters() - 1)) >>> idx;
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

            if(wroteToIndexable != 0)
            {
                reversalStack.store(wroteToIndexable);
                for(int r = 0; r < indexableRegisters.getNumRegisters(); r++)
                {
                    reversalStack.store(indexableRegisters.load(r));
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
        else if(BRANCH_INSTRUCTIONS.contains(heldInstruction.getHeader()))  // Unsuccessful branch
        {
            pendingRegisters[INTERNAL_BANK_INDEX][PC_INDEX]--;
        }
        else if(header.equals(HEADER.NOOP))
        {
            simulator.noops++;
        }
        else if(header.equals(HEADER.STALL))
        {
            simulator.stalls++;
        }
        Instruction ret = heldInstruction;
        Instruction gotten = previousStage.execute(nextIsBlocked, activePipeline);
        if(!nextIsBlocked) { heldInstruction = gotten; }
        return ret;
    }
}
