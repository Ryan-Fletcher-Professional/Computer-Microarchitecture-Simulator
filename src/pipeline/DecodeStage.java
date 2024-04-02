package pipeline;

import instructions.Instruction;
import memory.RegisterFileModule;
import static instructions.Instructions.*;
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
        /*
            Splitting instruction into fields is not simulated explicity.
            It's simply handled by the Instruction class and the relevant
                methods will not be called inappropriately by FetchStage.
         */

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
                    pendingRegisters[INDEXABLE_BANK_INDEX][idx] = true;
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
                    pendingRegisters[INTERNAL_BANK_INDEX][idx] = true;
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
                    // heldInstruction.addAuxBits(AUX_SOURCE(i), indexableRegisters.load(idx));  // TODO
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_TRUE);
                    pendingRegisters[CALL_STACK_INDEX][idx] = true;
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
                    // heldInstruction.addAuxBits(AUX_SOURCE(i), indexableRegisters.load(idx));  // TODO
                    heldInstruction.addAuxBits(AUX_SOURCE(i) + READ, AUX_TRUE);
                    pendingRegisters[REVERSAL_STACK_INDEX][idx] = true;
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
        Instruction ret = passUnblocked();
        heldInstruction = previousStage.execute(false);
        return ret;
    }
}
