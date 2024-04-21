package pipeline;

import instructions.Instruction;
import instructions.Term;
import memory.MemoryModule;
import memory.RegisterFileModule;

import static instructions.Instructions.*;
import static instructions.Instructions.AUX_SD_TYPE_REGISTER;
import static main.GLOBALS.*;

public class DecodeStage extends PipelineStage
{
    public static final String READ = "READ";

    private MemoryModule nearestDataCache;
    private final RegisterFileModule indexableRegisters;
    private final RegisterFileModule internalRegisters;
    private final RegisterFileModule callStack;
    private final RegisterFileModule reversalStack;
    private final int[][] pendingRegisters;

    public DecodeStage(int wordSize, String name,
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

    @Override
    public void setNearestDataCache(MemoryModule module)
    {
        this.nearestDataCache = module;
        super.setNearestDataCache(module);
    }

    @Override
    public Instruction execute(boolean nextIsBlocked, boolean activePipeline) throws MRAException
    {
        // Split flags and argument according to header and add as aux bits
        if(!AUX_EQUALS(heldInstruction.getAuxBits(AUX_DECODED), AUX_TRUE))
        {  // TODO : Add new instructions here
            switch(heldInstruction.getHeader())
            {
                case HEADER.LOAD -> decodeLoad();
                case HEADER.LOAD_LINE -> decodeLoadLine();
                case HEADER.STORE -> decodeStore();
                case HEADER.STORE_LINE -> decodeStoreLine();

                case HEADER.BRANCH_IF_ZERO -> decodeBranchIfZero();
                case HEADER.BRANCH_IF_NEGATIVE -> decodeBranchIfNegative();
                case HEADER.JUMP -> decodeJump();
                case HEADER.CALL -> decodeCall();
                case HEADER.RETURN -> decodeReturn();

                case HEADER.INT_ADD -> decodeIntAdd();
                case HEADER.INT_SUB -> decodeIntSubtract();
                case HEADER.INT_MUL -> decodeIntMultiply();
                case HEADER.INT_DIV -> decodeIntDivide();
                case HEADER.INT_MOD -> decodeIntModulo();

                case HEADER.AND -> decodeAND();
                case HEADER.OR -> decodeOR();
                case HEADER.XOR -> decodeXOR();
                case HEADER.NOT -> decodeNOT();
                case HEADER.COMPARE -> decodeCompare();

                case HEADER.COPY -> decodeCopy();
                case HEADER.SWAP -> decodeSwap();

                case HEADER.UNDO -> decodeUndo();
                case HEADER.HALT -> decodeHalt();
            }
            heldInstruction.addAuxBits(AUX_DECODED, AUX_TRUE);
        }

//        System.out.println(Arrays.toString(heldInstruction.getDestRegs()));

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
                if(pendingRegisters[INDEXABLE_BANK_INDEX][idx] == 0)
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
                if(pendingRegisters[INTERNAL_BANK_INDEX][idx] == 0)
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
                if(pendingRegisters[CALL_STACK_INDEX][idx] == 0)
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
                if(pendingRegisters[REVERSAL_STACK_INDEX][idx] == 0)
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
            if(AUX_EQUALS(heldInstruction.getAuxBits(AUX_SOURCE(i) + READ), AUX_FALSE))
            {
                if(activePipeline) { previousStage.execute(true, true); }
                return passBlocking();
            }
        }

        Instruction next = activePipeline ? previousStage.execute(nextIsBlocked && !DISPOSABLE_INSTRUCTIONS.contains(heldInstruction.getHeader()), true) : NOOP(wordSize);

        if(!(nextIsBlocked && !DISPOSABLE_INSTRUCTIONS.contains(heldInstruction.getHeader())))
        {
            // TODO : IMPORTANT: DESTINATION ARGS SHOULD BE MARKED AS SOURCE ARGS IF THEY'RE NOT REGISTERS!
            String[] destRegs = heldInstruction.getDestRegs();
            for(int i = 0; i < destRegs.length; i++)
            {
                int idx = Integer.parseInt(destRegs[i].substring(1));
                if((idx >= 0) && destRegs[i].startsWith(RegisterFileModule.INDEXABLE_PREFIX))
                {
                    pendingRegisters[INDEXABLE_BANK_INDEX][idx]++;
                }
                else if((idx >= 0) && destRegs[i].startsWith(RegisterFileModule.INTERNAL_PREFIX))
                {
                    pendingRegisters[INTERNAL_BANK_INDEX][idx]++;
                }
                else if((idx >= 0) && destRegs[i].startsWith(RegisterFileModule.CALL_PREFIX))
                {
                    pendingRegisters[CALL_STACK_INDEX][idx]++;
                }
                else if((idx >= 0) && destRegs[i].startsWith(RegisterFileModule.REVERSAL_PREFIX))
                {
                    pendingRegisters[REVERSAL_STACK_INDEX][idx]++;
                }
            }
            Instruction ret = passUnblocked();
            heldInstruction = next;
            return ret;
        }
        return passBlocked();
    }

    private void decodeLoad()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            int start = 24;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
        }
        else  // LONG word
        {
            heldInstruction.addFlags(1);


            int start = 56;
            if(AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0))
            {
                heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            }
            else
            {
                start -= (25 - 4);
                heldInstruction.addSource(0, start, start + 25, AUX_SD_TYPE_IMMEDIATE, -1);
            }
            start = 60;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
        }
    }

    private void decodeLoadLine()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            int start = 24;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            for(int i = 1; i < nearestDataCache.getLineSize(); i++)
            {
                int index = heldInstruction.getAuxBits(AUX_DEST(0)).toInt() + i;
                if(index < indexableRegisters.getNumRegisters())
                {
                    heldInstruction.addDestManual(i, new Term(index), AUX_REG_BANK_INDEXABLES);
                }
                else
                {
                    break;
                }
            }
        }
        else  // LONG word
        {
            heldInstruction.addFlags(1);


            int start = 56;
            if(AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0))
            {
                heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            }
            else
            {
                start -= (25 - 4);
                heldInstruction.addSource(0, start, start + 25, AUX_SD_TYPE_IMMEDIATE, -1);
            }
            start = 60;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            for(int i = 1; i < nearestDataCache.getLineSize(); i++)
            {
                int index = heldInstruction.getAuxBits(AUX_DEST(0)).toInt() + i;
                if(index < indexableRegisters.getNumRegisters())
                {
                    heldInstruction.addDestManual(i, new Term(index), AUX_REG_BANK_INDEXABLES);
                }
                else
                {
                    break;
                }
            }
        }
    }

    private void decodeStore()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            int start = 24;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        }
        else  // LONG word
        {
            heldInstruction.addFlags(2);
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

                    heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                    start += 32;

                    heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                }
                else
                {
                    int start = 35;

                    heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    start += 4;

                    if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
                    {
                        start += (25 - 4);
                        heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    }
                    else
                    {
                        heldInstruction.addSource(1, start, start + 25, AUX_SD_TYPE_IMMEDIATE, -1);
                    }
                }
            }
        }
    }

    private void decodeStoreLine()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            int start = 24;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            for(int i = 1; i < nearestDataCache.getLineSize(); i++)
            {
                int index = heldInstruction.getAuxBits(AUX_SOURCE(0)).toInt() + i;
                if(index < indexableRegisters.getNumRegisters())
                {
                    heldInstruction.addSourceManual(i, new Term(index), AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                }
                else
                {
                    heldInstruction.addSourceManual(i, new Term(0), AUX_SD_TYPE_IMMEDIATE, -1);
                }
            }
            start += 4;

            heldInstruction.addSource(nearestDataCache.getLineSize(), start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        }
        else  // LONG word
        {
            heldInstruction.addFlags(1);

            int start = 35;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            for(int i = 1; i < nearestDataCache.getLineSize(); i++)
            {
                int index = heldInstruction.getAuxBits(AUX_SOURCE(0)).toInt() + i;
                if(index < indexableRegisters.getNumRegisters())
                {
                    heldInstruction.addSourceManual(i, new Term(index), AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                }
                else
                {
                    heldInstruction.addSourceManual(i, new Term(0), AUX_SD_TYPE_IMMEDIATE, -1);
                }
            }
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                start += (25 - 4);
                heldInstruction.addSource(nearestDataCache.getLineSize(), start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            }
            else
            {
                heldInstruction.addSource(nearestDataCache.getLineSize(), start, start + 25, AUX_SD_TYPE_IMMEDIATE, -1);
            }
        }
    }

    private void decodeBranchIfZero()
    {
        heldInstruction.addFlags(1);


        int start = heldInstruction.wordLength() - 4;
        int type = AUX_SD_TYPE_REGISTER;
        if(heldInstruction.getAuxBits(FLAG(0)).toInt() != 0)
        {
            start -= (25 - 4);
            type = AUX_SD_TYPE_IMMEDIATE;
        }
        heldInstruction.addSource(0, start, heldInstruction.wordLength(), type, AUX_REG_BANK_INDEXABLES);

        heldInstruction.addSourceManual(1, new Term(CC_INDEX), AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INTERNALS);

        heldInstruction.addDestManual(0, new Term(PC_INDEX), AUX_REG_BANK_INTERNALS);
    }

    private void decodeBranchIfNegative()
    {
        heldInstruction.addFlags(1);


        int start = heldInstruction.wordLength() - 4;
        int type = AUX_SD_TYPE_REGISTER;
        if(heldInstruction.getAuxBits(FLAG(0)).toInt() != 0)
        {
            start -= (25 - 4);
            type = AUX_SD_TYPE_IMMEDIATE;
        }
        heldInstruction.addSource(0, start, heldInstruction.wordLength(), type, AUX_REG_BANK_INDEXABLES);

        heldInstruction.addSourceManual(1, new Term(CC_INDEX), AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INTERNALS);

        heldInstruction.addDestManual(0, new Term(PC_INDEX), AUX_REG_BANK_INTERNALS);
    }

    private void decodeJump()
    {
        heldInstruction.addFlags(1);


        int start = heldInstruction.wordLength() - 4;
        int type = AUX_SD_TYPE_REGISTER;
        if(heldInstruction.getAuxBits(FLAG(0)).toInt() != 0)
        {
            start -= (25 - 4);
            type = AUX_SD_TYPE_IMMEDIATE;
        }
        heldInstruction.addSource(0, start, heldInstruction.wordLength(), type, AUX_REG_BANK_INDEXABLES);

        heldInstruction.addDestManual(0, new Term(PC_INDEX), AUX_REG_BANK_INTERNALS);
    }

    private void decodeCall()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            heldInstruction.addFlags(2);


            int start = heldInstruction.wordLength() - 8;
            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;
            heldInstruction.addSource(1, start, start + 4, (heldInstruction.getAuxBits(FLAG(1)).toInt() == 0) ? AUX_SD_TYPE_IMMEDIATE : AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);

            heldInstruction.addDestManual(0, new Term(PC_INDEX), AUX_REG_BANK_INTERNALS);
        }
        else  // LONG instruction word
        {
            heldInstruction.addFlags(3);


            int start = heldInstruction.wordLength() - 24 - ((heldInstruction.getAuxBits(FLAG(0)).toInt() == 0) ? 4 : 25);
            heldInstruction.addSource(0, start, heldInstruction.wordLength() - 24, (heldInstruction.getAuxBits(FLAG(0)).toInt() == 0) ? AUX_SD_TYPE_REGISTER : AUX_SD_TYPE_IMMEDIATE, AUX_REG_BANK_INDEXABLES);
            start = heldInstruction.wordLength() - ((heldInstruction.getAuxBits(FLAG(1)).toInt() == 0) ? 24 : 4);
            heldInstruction.addSource(1, start, heldInstruction.wordLength(), (heldInstruction.getAuxBits(FLAG(1)).toInt() == 0) ? AUX_SD_TYPE_IMMEDIATE : AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);

            heldInstruction.addDestManual(0, new Term(PC_INDEX), AUX_REG_BANK_INTERNALS);
        }
    }

    private void decodeReturn()
    {
        heldInstruction.addDestManual(0, new Term(PC_INDEX), AUX_REG_BANK_INTERNALS);
    }

    private void decodeIntAdd()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            heldInstruction.addFlags(1);


            int start = 16;
            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addSource(2, start, start + 4, AUX_SD_TYPE_IMMEDIATE, -1);
            }
            else
            {
                heldInstruction.addSource(2, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            }

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 1)
            {
                heldInstruction.addDestManual(1, new Term(CC_INDEX), AUX_REG_BANK_INTERNALS);
            }
        }
        else  // LONG word
        {
            heldInstruction.addFlags(3);

            int start = (heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)
                ? 20 : 48;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
            {
                heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(2, start, start + 4,
                                      (heldInstruction.getAuxBits(FLAG(2)).toInt() == 0) ?
                                          AUX_SD_TYPE_IMMEDIATE :
                                          AUX_SD_TYPE_REGISTER,
                                       AUX_REG_BANK_INDEXABLES);

            if(heldInstruction.getAuxBits(FLAG(2)).toInt() == 1)
            {
                heldInstruction.addDestManual(1, new Term(CC_INDEX), AUX_REG_BANK_INTERNALS);
            }
        }
    }

    private void decodeIntSubtract()
    {
        // Should be same as decodeIntAdd()
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            heldInstruction.addFlags(1);


            int start = 16;
            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addSource(2, start, start + 4, AUX_SD_TYPE_IMMEDIATE, -1);
            }
            else
            {
                heldInstruction.addSource(2, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            }

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 1)
            {
                heldInstruction.addDestManual(1, new Term(CC_INDEX), AUX_REG_BANK_INTERNALS);
            }
        }
        else  // LONG word
        {
            heldInstruction.addFlags(3);

            int start = (heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)
                ? 20 : 48;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
            {
                heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(2, start, start + 4,
                                      (heldInstruction.getAuxBits(FLAG(2)).toInt() == 0) ?
                                          AUX_SD_TYPE_IMMEDIATE :
                                          AUX_SD_TYPE_REGISTER,
                                      AUX_REG_BANK_INDEXABLES);

            if(heldInstruction.getAuxBits(FLAG(2)).toInt() == 1)
            {
                heldInstruction.addDestManual(1, new Term(CC_INDEX), AUX_REG_BANK_INTERNALS);
            }
        }
    }

    private void decodeIntMultiply()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            heldInstruction.addFlags(1);


            int start = 16;
            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 1)
            {
                heldInstruction.addDest(1, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
        else  // LONG word
        {
            heldInstruction.addFlags(3);

            int start = (heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)
                ? 20 : 48;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
            {
                heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(2)).toInt() == 1)
            {
                heldInstruction.addDest(1, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
    }

    private void decodeIntDivide()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            heldInstruction.addFlags(1);


            int start = 16;
            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 1)
            {
                heldInstruction.addDest(1, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
        else  // LONG word
        {
            heldInstruction.addFlags(3);

            int start = (heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)
                ? 20 : 48;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
            {
                heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(2)).toInt() == 1)
            {
                heldInstruction.addDest(1, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
    }

    private void decodeIntModulo()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            heldInstruction.addFlags(1);


            int start = 16;
            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 1)
            {
                heldInstruction.addDest(1, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
        else  // LONG word
        {
            heldInstruction.addFlags(3);

            int start = (heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)
                ? 20 : 48;

            if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
            {
                heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
            {
                heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                start += 4;
            }
            else
            {
                heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                start += 32;
            }

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            start += 4;

            if(heldInstruction.getAuxBits(FLAG(2)).toInt() == 1)
            {
                heldInstruction.addDest(1, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
    }

    private void decodeAND()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            int start = 20;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
        }
        else
        {
            heldInstruction.addFlags(2);
            if(((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) &&
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)))
            {
                (new MRAException("Long " + MNEMONICS.get(heldInstruction.getHeader()) + " has 2x 1-flags")).printStackTrace();
                heldInstruction = ERR(WORD_SIZE_LONG, ERR_TYPE_INVALID_FLAGS);
                heldInstruction.addAuxBits(new Term(ERR_TYPE_INVALID_FLAGS, false, WORD_SIZE_LONG - HEADER_SIZE).toString(),
                                           new Term(HEADER_STRINGS.get(heldInstruction.getHeader()), false));
            }
            else
            {
                int start = ((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                    (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1))
                    ? 24 : 52;

                if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
                {
                    heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    start += 4;
                }
                else
                {
                    heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                    start += 32;
                }

                if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
                {
                    heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    start += 4;
                }
                else
                {
                    heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                    start += 32;
                }

                heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
    }

    private void decodeOR()
    {  // Should be the same as decodeAND
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            int start = 20;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
        }
        else
        {
            heldInstruction.addFlags(2);
            if(((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) &&
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)))
            {
                (new MRAException("Long " + MNEMONICS.get(heldInstruction.getHeader()) + " has 2x 1-flags")).printStackTrace();
                heldInstruction = ERR(WORD_SIZE_LONG, ERR_TYPE_INVALID_FLAGS);
                heldInstruction.addAuxBits(new Term(ERR_TYPE_INVALID_FLAGS, false, WORD_SIZE_LONG - HEADER_SIZE).toString(),
                                           new Term(HEADER_STRINGS.get(heldInstruction.getHeader()), false));
            }
            else
            {
                int start = ((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                    (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1))
                    ? 24 : 52;

                if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
                {
                    heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    start += 4;
                }
                else
                {
                    heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                    start += 32;
                }

                if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
                {
                    heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    start += 4;
                }
                else
                {
                    heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                    start += 32;
                }

                heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
    }

    private void decodeXOR()
    {  // Should be the same as decodeAND
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            int start = 20;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
        }
        else
        {
            heldInstruction.addFlags(2);
            if(((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) &&
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)))
            {
                (new MRAException("Long " + MNEMONICS.get(heldInstruction.getHeader()) + " has 2x 1-flags")).printStackTrace();
                heldInstruction = ERR(WORD_SIZE_LONG, ERR_TYPE_INVALID_FLAGS);
                heldInstruction.addAuxBits(new Term(ERR_TYPE_INVALID_FLAGS, false, WORD_SIZE_LONG - HEADER_SIZE).toString(),
                                           new Term(HEADER_STRINGS.get(heldInstruction.getHeader()), false));
            }
            else
            {
                int start = ((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) ||
                    (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1))
                    ? 24 : 52;

                if(heldInstruction.getAuxBits(FLAG(0)).toInt() == 0)
                {
                    heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    start += 4;
                }
                else
                {
                    heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                    start += 32;
                }

                if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
                {
                    heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    start += 4;
                }
                else
                {
                    heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                    start += 32;
                }

                heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
            }
        }
    }

    private void decodeNOT()
    {
        int start = (heldInstruction.wordLength() == WORD_SIZE_SHORT) ? 24 : 56;

        heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        start += 4;

        heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
    }

    private void decodeCompare()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            int start = 24;

            heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
            start += 4;

            heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        }
        else
        {
            heldInstruction.addFlags(2);
            if(((heldInstruction.getAuxBits(FLAG(0)).toInt() == 1) &&
                (heldInstruction.getAuxBits(FLAG(1)).toInt() == 1)))
            {
                (new MRAException("Long " + MNEMONICS.get(heldInstruction.getHeader()) + " has 2x 1-flags")).printStackTrace();
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
                    heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                    start += 4;
                }
                else
                {
                    heldInstruction.addSource(0, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                    start += 32;
                }

                if(heldInstruction.getAuxBits(FLAG(1)).toInt() == 0)
                {
                    heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
                }
                else
                {
                    heldInstruction.addSource(1, start, start + 32, AUX_SD_TYPE_IMMEDIATE, -1);
                }
            }
        }

        heldInstruction.addDestManual(0, new Term(CC_INDEX), AUX_REG_BANK_INTERNALS);
    }

    public void decodeCopy()
    {
        int length = heldInstruction.wordLength();

        if(length == WORD_SIZE_SHORT)
        {
            heldInstruction.addFlags(1);


            int start = 7;
            int end = start + 21;

            int type = AUX_SD_TYPE_IMMEDIATE;
            if(AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0))
            {
                start += (21 - 4);
                type = AUX_SD_TYPE_REGISTER;
            }

            heldInstruction.addSource(0, start, end, type, AUX_REG_BANK_INDEXABLES);
            start = end;

            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
        }
        else
        {
            heldInstruction.addFlags(1);


            int start = 28;
            int end = start + 32;

            int type = AUX_SD_TYPE_IMMEDIATE;
            if(AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0))
            {
                start += (32 - 4);
                type = AUX_SD_TYPE_REGISTER;
            }
            heldInstruction.addSource(0, start, end, type, AUX_REG_BANK_INDEXABLES);
            start = end;

            type = AUX_SD_TYPE_REGISTER;
            heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
        }

        if(((length == WORD_SIZE_SHORT) || AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0)) &&
            (AUX_EQUALS(heldInstruction.getAuxBits(AUX_SOURCE(0)), heldInstruction.getAuxBits(AUX_DEST(0)).toInt())))
        {
            (new MRAException("COPY writes to same register it reads")).printStackTrace();
            heldInstruction = ERR(length, ERR_TYPE_INVALID_ARGS);
            heldInstruction.addAuxBits(new Term(ERR_TYPE_INVALID_ARGS, false, length - HEADER_SIZE).toString(),
                                       new Term(HEADER_STRINGS.get(heldInstruction.getHeader()), false));
        }
    }

    public void decodeSwap()
    {
        int start = heldInstruction.wordLength() - 8;

        heldInstruction.addSource(0, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        start += 4;
        heldInstruction.addSource(1, start, start + 4, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        // Swap is done in execute, not here
        heldInstruction.addDest(1, start, start + 4, AUX_REG_BANK_INDEXABLES);
        start -= 4;
        heldInstruction.addDest(0, start, start + 4, AUX_REG_BANK_INDEXABLES);
    }

    private void decodeHalt()
    {
        if(heldInstruction.wordLength() == WORD_SIZE_SHORT)
        {
            heldInstruction.addFlags(1);

            int start = 28;

            int type = AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0) ? AUX_SD_TYPE_REGISTER : AUX_SD_TYPE_IMMEDIATE;

            heldInstruction.addSource(0, start, start + 4, type, AUX_REG_BANK_INDEXABLES);
        }
        else
        {
            heldInstruction.addFlags(1);

            int start = 60;

            int type = AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0) ? AUX_SD_TYPE_REGISTER : AUX_SD_TYPE_IMMEDIATE;

            heldInstruction.addSource(0, start, start + 4, type, AUX_REG_BANK_INDEXABLES);
        }

        heldInstruction.addDestManual(0, new Term(CC_INDEX), AUX_REG_BANK_INTERNALS);
    }

    public void decodeUndo()
    {
        /*
         * This decodes a bit weirdly. UNDO can have wildly varied behavior depending on which instructions precede it,
         *  which registers those instructions write to, etc. So to avoid any forwarding or backwarding shenanigans,
         *  UNDO simply will act as though it writes to all 16 indexable registers.
         *  That means UNDO will block for at most 2 extra cycles while it approaches the writeback stage.
         */

        for(int i = 0; i < indexableRegisters.getNumRegisters(); i++)
        {
            heldInstruction.addSDManual(false, i, new Term(i), AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        }

        heldInstruction.addFlags(2);

        int start = heldInstruction.wordLength() - 20;

        if(AUX_EQUALS(heldInstruction.getAuxBits(FLAG(0)), 0))
        {
            heldInstruction.addSource(0, start, start + 10, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        }
        else
        {
            heldInstruction.addSource(0, start, start + 10, AUX_SD_TYPE_IMMEDIATE, -1);
        }
        start += 10;

        if(AUX_EQUALS(heldInstruction.getAuxBits(FLAG(1)), 0))
        {
            heldInstruction.addSource(1, start, start + 10, AUX_SD_TYPE_IMMEDIATE, -1);
        }
        else
        {
            heldInstruction.addSource(1, start, start + 10, AUX_SD_TYPE_REGISTER, AUX_REG_BANK_INDEXABLES);
        }
    }
}
