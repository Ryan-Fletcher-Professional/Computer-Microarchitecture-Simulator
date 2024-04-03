package pipeline;

import instructions.Instruction;
import instructions.Term;
import memory.RegisterFileModule;
import static instructions.Instructions.*;
import static instructions.Instructions.AUX_SOURCE_TYPE_REG;
import static main.GLOBALS.*;

public class DecodeStage extends PipelineStage
{
    public static final String READ = "READ";

    private final RegisterFileModule indexableRegisters;
    private final RegisterFileModule internalRegisters;
    private final RegisterFileModule callStack;
    private final RegisterFileModule reversalStack;
    private final boolean[][] pendingRegisters;

    public DecodeStage(int wordSize, String name,
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
        // TODO : Split flags and argument according to header and add as aux bits
        int length = heldInstruction.wordLength();
        long instruction = heldInstruction.wordNum();

        switch(heldInstruction.getHeader())
        {
            case HEADER.LOAD -> {
                if(length == WORD_SIZE_LONG)
                    { heldInstruction.addAuxBits(FLAG(0), new Term(instruction & MASK(6))); }

                int start = (length == WORD_SIZE_LONG) ? 56 : 24;
                if((length == WORD_SIZE_SHORT) || AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0))
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(0), instruction & MASK_RANGE(start, start + 4));
                    heldInstruction.addAuxBits(AUX_SOURCE_DEST_TYPE(0), new Term(AUX_SOURCE_DEST_TYPE_REG));
                }
                else
                {
                    start -= (25 - 4);
                    heldInstruction.addAuxBits(AUX_SOURCE(0), instruction & MASK_RANGE(start, start + 25));
                    heldInstruction.addAuxBits(AUX_SOURCE_DEST_TYPE(0), new Term(AUX_SOURCE_DEST_TYPE_IMMEDIATE));
                }
                start += 4;
                heldInstruction.addAuxBits(AUX_DEST(0), instruction & MASK_RANGE(start, start + 4));
                heldInstruction.addAuxBits(AUX_SOURCE_DEST_TYPE(0), new Term(AUX_SOURCE_DEST_TYPE_REG));
            }

            case HEADER.BRANCH_IF_NEGATIVE -> {
                heldInstruction.addAuxBits(FLAG(0), new Term(instruction & MASK(6)));

                int start = length - 4;
                heldInstruction.addAuxBits(AUX_SOURCE_DEST_TYPE(0), new Term(AUX_SOURCE_DEST_TYPE_REG));
                if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 1)
                {
                    start -= (25 - 4);
                    heldInstruction.addAuxBits(AUX_SOURCE_DEST_TYPE(0), new Term(AUX_SOURCE_DEST_TYPE_IMMEDIATE));
                }
                heldInstruction.addAuxBits(AUX_SOURCE(0), instruction & MASK_RANGE(start, length));
            }

//            case HEADER.INT_ADD -> {
//                if(length == WORD_SIZE_SHORT)
//                {
//                    heldInstruction.addAuxBits(FLAG(0), new Term(instruction & MASK(6)));
//
//
//                }
//                else
//                {
//
//                }
//            }
        }

        // TODO : getSourceRegs() returns an index for each source in the instruction, REGISTER OR NOT!
        //  Non-register sources and already-read sources are set to index=-1 with no prefix
        String[] sourceRegs = heldInstruction.getSourceRegs();
        for(int i = 0; i < sourceRegs.length; i++)
        {
            int idx = Integer.parseInt(sourceRegs[i].substring(1));
            if((idx >= 0) && sourceRegs[i].startsWith(RegisterFileModule.INDEXABLE_PREFIX))
            {
                if(!pendingRegisters[INDEXABLE_BANK_INDEX][idx])
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(i), indexableRegisters.load(idx));
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_TRUE);
                }
                else
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_FALSE);
                }
            }
            else if((idx >= 0) && sourceRegs[i].startsWith(RegisterFileModule.INTERNAL_PREFIX))
            {
                if(!pendingRegisters[INTERNAL_BANK_INDEX][idx])
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(i), internalRegisters.load(idx));
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_TRUE);
                }
                else
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_FALSE);
                }
            }
            else if((idx >= 0) && sourceRegs[i].startsWith(RegisterFileModule.CALL_PREFIX))
            {
                if(!pendingRegisters[CALL_STACK_INDEX][idx])
                {
                    // Index callstack from top w/return address=-1, RR=0, R1=1, etc.
                    // Memory instructions will need to pop from stack later.
                    heldInstruction.addAuxBits(AUX_SOURCE(i), callStack.peek(idx + 1));
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_TRUE);
                }
                else
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_FALSE);
                }
            }
            else if((idx >= 0) && sourceRegs[i].startsWith(RegisterFileModule.REVERSAL_PREFIX))
            {
                if(!pendingRegisters[REVERSAL_STACK_INDEX][idx])
                {
                    // Index reversal stack from top. Each item is 64-bit word.
                    // Memory instructions will need to pop from stack later.
                    heldInstruction.addAuxBits(AUX_SOURCE(i), reversalStack.peek(idx));
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_TRUE);
                }
                else
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_FALSE);
                }
            }
            else
            {
                heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_TRUE);
            }
        }
        for(int i = 0; i < sourceRegs.length; i++)
        {
            if(AUX_FALSE(heldInstruction.getAuxBits(AUX_SOURCE(i) + READ)))
            {
                previousStage.execute(true);
                return passBlocking();
            }
        }

        Instruction next = previousStage.execute(nextIsBlocked);

        if(!nextIsBlocked)
        {
            String[] destRegs = heldInstruction.getDestRegs();
            for(int i = 0; i < destRegs.length; i++)
            {
                int idx = Integer.parseInt(destRegs[i].substring(1));
                if((idx >= 0) && destRegs[i].startsWith(RegisterFileModule.INDEXABLE_PREFIX))
                {
                    pendingRegisters[INDEXABLE_BANK_INDEX][idx] = true;
                }
                else if((idx >= 0) && destRegs[i].startsWith(RegisterFileModule.INTERNAL_PREFIX))
                {
                    pendingRegisters[INTERNAL_BANK_INDEX][idx] = true;
                }
                else if((idx >= 0) && destRegs[i].startsWith(RegisterFileModule.CALL_PREFIX))
                {
                    pendingRegisters[CALL_STACK_INDEX][idx] = true;
                }
                else if((idx >= 0) && destRegs[i].startsWith(RegisterFileModule.REVERSAL_PREFIX))
                {
                    pendingRegisters[REVERSAL_STACK_INDEX][idx] = true;
                }
            }
            Instruction ret = passUnblocked();
            heldInstruction = next;
            return ret;
        }
        return passBlocked();
    }
}
