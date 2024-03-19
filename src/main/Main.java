package main;

import memory.RegisterFileModule;
import simulator.BasicMemoryManipulator;

import static main.GLOBALS.*;

public class Main
{
    public static void main(String[] args)
    {
        RegisterFileModule[] registerBanks = new RegisterFileModule[REGISTER_BANK_INDECES.length];
        int[] indexableLengths = new int[16];
        String[] indexableNames = new String[16];
        for(int i = 0; i < indexableLengths.length; i++)
        {
            indexableLengths[i] = 32;
            indexableNames[i] = "R" + i;
        }
        int[] internalLengths = new int[] { 1, 25, 16, 16, 16, 10, 10 };
        String[] internalNames = INTERNAL_REGISTER_NAMES;
        int[] callStackLengths = new int[(int)Math.pow(2, internalLengths[5]) * (indexableLengths.length + 1)];  // Will be pushed/popped in groups of 17; 1 for return pointer and 16 for snapshot of indexable register file
        String[] callStackNames = new String[(int)Math.pow(2, internalLengths[5]) * (indexableLengths.length + 1)];
        callStackLengths[0] = 25;
        callStackNames[0] = "0";
        for(int i = 1; i < callStackLengths.length; i++)
        {
            callStackLengths[i] = 32;
            callStackNames[i] = Integer.toString(i);
        }
        int[] reversalStackLengths = new int[(int)Math.pow(2, internalLengths[6])];
        String[] reversalStackNames = new String[(int)Math.pow(2, internalLengths[6])];
        for(int i = 0; i < reversalStackLengths.length; i++)
        {
            reversalStackLengths[i] = 64;
            reversalStackNames[i] = Integer.toString(i);
        }
        registerBanks[INDEXABLE_BANK_INDEX] = new RegisterFileModule(GET_ID(), REGISTER_FILE_MODE.ADDRESSED, indexableLengths, indexableNames);
        registerBanks[INTERNAL_BANK_INDEX] = new RegisterFileModule(GET_ID(), REGISTER_FILE_MODE.ADDRESSED, internalLengths, internalNames);
        registerBanks[CALL_STACK_INDEX] = new RegisterFileModule(GET_ID(), REGISTER_FILE_MODE.STACK, callStackLengths, callStackNames);
        registerBanks[REVERSAL_STACK_INDEX] = new RegisterFileModule(GET_ID(), REGISTER_FILE_MODE.STACK_CIRCULAR, reversalStackLengths, reversalStackNames);
        new BasicMemoryManipulator(GET_ID(), registerBanks);
    }
}