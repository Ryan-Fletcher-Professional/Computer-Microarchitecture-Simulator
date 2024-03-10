package main;

import memory.RegisterBankForTesting;
import simulator.BasicMemoryManipulator;

import static main.GLOBALS.*;

public class Main
{
    public static void main(String[] args)
    {
        RegisterBankForTesting[] registerBanks = new RegisterBankForTesting[REGISTER_BANK_INDECES.length];
        int[] indexableLengths = new int[16];
        String[] indexableNames = new String[16];
        for(int i = 0; i < indexableLengths.length; i++)
        {
            indexableLengths[i] = 32;
            indexableNames[i] = "R" + i;
        }
        int[] internalLengths = new int[] { 1, 25, 16, 16, 16, 10, 10 };
        String[] internalNames = INTERNAL_REGISTER_NAMES;
        registerBanks[INDEXABLE_BANK_INDEX] = new RegisterBankForTesting(GET_ID(), indexableLengths, indexableNames);
        registerBanks[INTERNAL_BANK_INDEX] = new RegisterBankForTesting(GET_ID(), internalLengths, internalNames);
        new BasicMemoryManipulator(GET_ID(), registerBanks);
    }
}