package pipeline;

import instructions.Instruction;
import instructions.Term;
import memory.RegisterFileModule;

import java.util.Arrays;
import java.util.Objects;

import static instructions.Instructions.*;
import static instructions.Instructions.AUX_SOURCE_TYPE_REGISTER;
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
        // Split flags and argument according to header and add as aux bits
        int length = heldInstruction.wordLength();
        long instruction = heldInstruction.wordNum();

        if(!AUX_EQUALS(heldInstruction.getAuxBits(AUX_DECODED), AUX_TRUE))
        {
            switch(heldInstruction.getHeader())
            {
                case HEADER.LOAD -> decodeLoad(length, instruction);
                case HEADER.STORE -> decodeStore(length, instruction);

                case HEADER.BRANCH_IF_NEGATIVE -> decodeBranchIfNegative(length, instruction);

                case HEADER.INT_ADD -> decodeIntAdd(length, instruction);

                case HEADER.COMPARE -> decodeCompare(length, instruction);
            }
            heldInstruction.addAuxBits(AUX_DECODED, AUX_TRUE());
        }

        // TODO : getSourceRegs() returns an index for each source in the instruction, REGISTER OR NOT!
        //  Non-register sources and already-read sources are sent as " -1" with no prefix
        String[] sourceRegs = heldInstruction.getSourceRegs();
        //System.out.println(heldInstruction.getHeader() + " " + Arrays.toString(sourceRegs));
        for(int i = 0; i < sourceRegs.length; i++)
        {
            int idx = Integer.parseInt(sourceRegs[i].substring(1));
//            System.out.println(heldInstruction.getHeader() + " " + idx);
//            System.out.println(heldInstruction.getHeader() + " " + heldInstruction.getAuxBits(AUX_SOURCE(i)).toInt());
//            System.out.println(heldInstruction.getHeader() + " " + Objects.requireNonNullElse(heldInstruction.getAuxBits(AUX_SOURCE(i) + READ), new Term(2)).toInt());
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
            // TODO : IMPORTANT: DESTINATION ARGS SHOULD BE MARKED AS SOURCE ARGS IF THEY'RE NOT REGISTERS!
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

    private void decodeLoad(int length, long instruction)
    {
        if(length == WORD_SIZE_SHORT)
        {
            int start = 24;

            heldInstruction.addAuxBits(AUX_SOURCE(0), MASK((int)instruction, start, start + 4));
            heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            start += 4;

            heldInstruction.addAuxBits(AUX_DEST(0), MASK((int)instruction, start, start + 4));
            heldInstruction.addAuxBits(AUX_DEST_TYPE(0), new Term(AUX_DEST_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_DEST_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
        }
        else  // LONG word
        {
            heldInstruction.addAuxBits(FLAG(0), new Term(MASK_LONG(instruction, 6)));


            int start = 56;
            if(AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0))
            {
                heldInstruction.addAuxBits(AUX_SOURCE(0), MASK_LONG(instruction, start, start + 4));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
                heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            }
            else
            {
                start -= (25 - 4);
                heldInstruction.addAuxBits(AUX_SOURCE(0), MASK_LONG(instruction, start, start + 25));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
            }
            start += 4;

            heldInstruction.addAuxBits(AUX_DEST(0), MASK_LONG(instruction, start, start + 4));
            heldInstruction.addAuxBits(AUX_DEST_TYPE(0), new Term(AUX_DEST_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_DEST_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
        }
    }

    private void decodeStore(int length, long instruction)
    {
        if(length == WORD_SIZE_SHORT)
        {
            int start = 24;

            heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK((int)instruction, start, start + 4)));
            heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            start += 4;

            heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK((int)instruction, start, start + 4)));
            heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_SOURCE_BANK(1), new Term(AUX_REG_BANK_INDEXABLES));
        }
        else  // LONG word
        {
            heldInstruction.addAuxBits(FLAG(0), new Term(MASK_LONG(instruction, 6)));
            heldInstruction.addAuxBits(FLAG(1), new Term(MASK_LONG(instruction, 7)));
            if(((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) &&
                    (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)))
            {
                (new MRAException("Long STR has 2x 1-flags")).printStackTrace();
                heldInstruction = ERR(WORD_SIZE_LONG, ERR_TYPE_INVALID_FLAGS);
                heldInstruction.addAuxBits(new Term(ERR_TYPE_INVALID_FLAGS, false, WORD_SIZE_LONG - HEADER_SIZE).toString(),
                                           new Term(HEADER_STRINGS.get(heldInstruction.getHeader()), false));
            }
            else
            {
                if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 1)
                {
                    int start = 28;

                    heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK_LONG(instruction, start, start + 32)));
                    heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
                    start += 32;

                    heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK_LONG(instruction, start, start + 4)));
                    heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_REGISTER));
                    heldInstruction.addAuxBits(AUX_SOURCE_BANK(1), new Term(AUX_REG_BANK_INDEXABLES));
                }
                else
                {
                    int start = 35;

                    heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK_LONG(instruction, start, start + 4)));
                    heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
                    heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
                    start += 4;

                    if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
                    {
                        start += (25 - 4);
                        heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK_LONG(instruction, start, start + 4)));
                        heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_REGISTER));
                        heldInstruction.addAuxBits(AUX_SOURCE_BANK(1), new Term(AUX_REG_BANK_INDEXABLES));
                    }
                    else
                    {
                        heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK_LONG(instruction, start, start + 25)));
                        heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
                    }
                }
            }
        }
    }

    private void decodeBranchIfNegative(int length, long instruction)
    {
        if(length == WORD_SIZE_SHORT)
        {
            heldInstruction.addAuxBits(FLAG(0), new Term(MASK((int)instruction, 6)));


            int start = length - 4;
            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
                heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            }
            else
            {
                start -= (25 - 4);
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
            }
            heldInstruction.addAuxBits(AUX_SOURCE(0), MASK((int)instruction, start, length));
        }
        else  // LONG word
        {
            heldInstruction.addAuxBits(FLAG(0), new Term(MASK_LONG(instruction, 6)));


            int start = length - 4;
            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
                heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            }
            else
            {
                start -= (25 - 4);
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
            }
            heldInstruction.addAuxBits(AUX_SOURCE(0), MASK_LONG(instruction, start, length));
        }

        heldInstruction.addAuxBits(AUX_DEST(0), new Term(PC_INDEX));
        heldInstruction.addAuxBits(AUX_DEST_TYPE(0), new Term(AUX_DEST_TYPE_REGISTER));
        heldInstruction.addAuxBits(AUX_DEST_BANK(0), new Term(AUX_REG_BANK_INTERNALS));
    }

    private void decodeIntAdd(int length, long instruction)
    {
        if(length == WORD_SIZE_SHORT)
        {
            heldInstruction.addAuxBits(FLAG(0), new Term(MASK_LONG(instruction, 6)));


            int start = 16;
            heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK((int)instruction, start, start + 4)));
            heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            start += 4;

            heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK((int)instruction, start, start + 4)));
            heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_SOURCE_BANK(1), new Term(AUX_REG_BANK_INDEXABLES));
            start += 4;

            heldInstruction.addAuxBits(AUX_DEST(0), new Term(MASK((int)instruction, start, start + 4)));
            heldInstruction.addAuxBits(AUX_DEST_TYPE(0), new Term(AUX_DEST_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_DEST_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addAuxBits(AUX_SOURCE(2), new Term(MASK((int)instruction, start, start + 4)));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(2), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
            }
            else
            {
                heldInstruction.addAuxBits(AUX_SOURCE(2), new Term(MASK((int)instruction, start, start + 4)));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(2), new Term(AUX_SOURCE_TYPE_REGISTER));
                heldInstruction.addAuxBits(AUX_SOURCE_BANK(2), new Term(AUX_REG_BANK_INDEXABLES));
            }
        }
        else  // LONG word
        {
            heldInstruction.addAuxBits(FLAG(0), new Term(MASK_LONG(instruction, 6)));
            heldInstruction.addAuxBits(FLAG(1), new Term(MASK_LONG(instruction, 7)));
            heldInstruction.addAuxBits(FLAG(2), new Term(MASK_LONG(instruction, 8)));


            int start = (heldInstruction.getAuxBits(FLAG(0)).toInt() == 0) ||
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
                ? 20 : 48;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK_LONG(instruction, start, start + 4)));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
                heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
                start += 4;
            }
            else
            {
                heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK_LONG(instruction, start, start + 32)));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
                start += 32;
            }

            if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
            {
                heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK_LONG(instruction, start, start + 4)));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_REGISTER));
                heldInstruction.addAuxBits(AUX_SOURCE_BANK(1), new Term(AUX_REG_BANK_INDEXABLES));
                start += 4;
            }
            else
            {
                heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK_LONG(instruction, start, start + 32)));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
                start += 32;
            }

            heldInstruction.addAuxBits(AUX_DEST(0), new Term(MASK_LONG(instruction, start, start + 4)));
            heldInstruction.addAuxBits(AUX_DEST_TYPE(0), new Term(AUX_DEST_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_DEST_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(2)).toInt() == 0)
            {
                heldInstruction.addAuxBits(AUX_SOURCE(2), new Term(MASK_LONG(instruction, start, start + 4)));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(2), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
            }
            else
            {
                heldInstruction.addAuxBits(AUX_SOURCE(2), new Term(MASK_LONG(instruction, start, start + 4)));
                heldInstruction.addAuxBits(AUX_SOURCE_TYPE(2), new Term(AUX_SOURCE_TYPE_REGISTER));
                heldInstruction.addAuxBits(AUX_SOURCE_BANK(2), new Term(AUX_REG_BANK_INDEXABLES));
            }
        }
    }

    private void decodeCompare(int length, long instruction)
    {
        if(length == WORD_SIZE_SHORT)
        {
            int start = 24;

            heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK((int)instruction, start, start + 4)));
            heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_SOURCE_BANK(0), new Term(AUX_REG_BANK_INDEXABLES));
            start += 4;

            heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK((int)instruction, start, start + 4)));
            heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_REGISTER));
            heldInstruction.addAuxBits(AUX_SOURCE_BANK(1), new Term(AUX_REG_BANK_INDEXABLES));
        }
        else  // LONG word
        {
            heldInstruction.addAuxBits(FLAG(0), new Term(MASK_LONG(instruction, 6)));
            heldInstruction.addAuxBits(FLAG(1), new Term(MASK_LONG(instruction, 7)));
            if(((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) &&
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)))
            {
                (new MRAException("Long CMP has 2x 1-flags")).printStackTrace();
                heldInstruction = ERR(WORD_SIZE_LONG, ERR_TYPE_INVALID_FLAGS);
                heldInstruction.addAuxBits(new Term(ERR_TYPE_INVALID_FLAGS, false, WORD_SIZE_LONG - HEADER_SIZE).toString(),
                                           new Term(HEADER_STRINGS.get(heldInstruction.getHeader()), false));
            }
            else
            {
                int start = ((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                            (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1))
                            ? 28 : 56;

                if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK_LONG(instruction, start, start + 4)));
                    heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_REGISTER));
                    start += 4;
                }
                else
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(0), new Term(MASK_LONG(instruction, start, start + 32)));
                    heldInstruction.addAuxBits(AUX_SOURCE_TYPE(0), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
                    start += 32;
                }

                if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK_LONG(instruction, start, start + 4)));
                    heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_REGISTER));
                }
                else
                {
                    heldInstruction.addAuxBits(AUX_SOURCE(1), new Term(MASK_LONG(instruction, start, start + 32)));
                    heldInstruction.addAuxBits(AUX_SOURCE_TYPE(1), new Term(AUX_SOURCE_TYPE_IMMEDIATE));
                }
            }
        }


        heldInstruction.addAuxBits(AUX_DEST(0), new Term(CC_INDEX));
        heldInstruction.addAuxBits(AUX_DEST_TYPE(0), new Term(AUX_DEST_TYPE_REGISTER));
        heldInstruction.addAuxBits(AUX_DEST_BANK(0), new Term(AUX_REG_BANK_INTERNALS));
    }
}
