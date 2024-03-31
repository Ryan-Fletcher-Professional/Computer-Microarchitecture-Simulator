package pipeline;

import memory.RegisterFileModule;

public class DecodeStage extends PipelineStage
{
    private final RegisterFileModule indexableRegisters;
    private final RegisterFileModule internalRegisters;
    private final RegisterFileModule callStack;
    private final RegisterFileModule reversalStack;
    private final RegisterFileModule pendingRegisters;

    public DecodeStage(int wordSize,
                       RegisterFileModule indexableRegisters, RegisterFileModule internalRegisters,
                       RegisterFileModule callStack, RegisterFileModule reversalStack,
                       RegisterFileModule pendingRegisters)
    {
        super(wordSize);
        this.indexableRegisters = indexableRegisters;
        this.internalRegisters = internalRegisters;
        this.callStack = callStack;
        this.reversalStack = reversalStack;
        this.pendingRegisters = pendingRegisters;
    }
}
