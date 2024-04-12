package main;

import memory.RegisterFileModule;
import pipeline.Pipeline;
import simulator.Simulator;

import javax.swing.*;
import java.io.*;
import java.util.List;

import static main.GLOBALS.*;

public class Main
{
    public static void main(String[] args)
    {
        System.out.println("\n!!!!!!!!    Running simulator    !!!!!!!!\n");

        int[] startingParams = FIND_START_PARAMS(PATH_TO_BINARIES);

        RegisterFileModule[] registerBanks = new RegisterFileModule[REGISTER_BANK_INDECES.length];
        int[] indexableLengths = new int[INDEXABLE_BANK_SIZE];
        String[] indexableNames = new String[INDEXABLE_BANK_SIZE];
        for(int i = 0; i < indexableLengths.length; i++)
        {
            indexableLengths[i] = Integer.SIZE;
            indexableNames[i] = "R" + i;
        }
        int[] internalLengths = new int[] { 1, 25, 25, 16, 64, 64, startingParams[1], startingParams[2] };
        String[] internalNames = INTERNAL_REGISTER_NAMES;
        // TODO : Ensure external pushes to and pops from call stack perform 17 operations in correct order
        int[] callStackLengths = new int[internalLengths[6] * (indexableLengths.length + 1)];  // Will be pushed/popped in groups of 17; 1 for return pointer and 16 for snapshot of indexable register file
        String[] callStackNames = new String[internalLengths[6] * (indexableLengths.length + 1)];
        for(int i = 0; i < callStackLengths.length; i++)
        {
            int index = i % (indexableLengths.length + 1);
            callStackLengths[i] = (index == 0) ? ADDRESS_SIZE : indexableLengths[0];
            callStackNames[i] = i / (indexableLengths.length + 1) + " " + ((index == 0) ? "R" : index);
        }
        int[] reversalStackLengths = new int[internalLengths[7]];
        String[] reversalStackNames = new String[internalLengths[7]];
        for(int i = 0; i < reversalStackLengths.length; i++)
        {
            reversalStackLengths[i] = WORD_SIZE_LONG;
            reversalStackNames[i] = Integer.toString(i);
        }
        registerBanks[INDEXABLE_BANK_INDEX] = new RegisterFileModule(GET_ID(), REGISTER_FILE_MODE.ADDRESSED, indexableLengths, indexableNames);
        registerBanks[INTERNAL_BANK_INDEX] = new RegisterFileModule(GET_ID(), REGISTER_FILE_MODE.ADDRESSED, internalLengths, internalNames);
        int startingPC = 8;
        registerBanks[INTERNAL_BANK_INDEX].store(List.of(INTERNAL_REGISTER_NAMES).indexOf(PC), startingPC);
        registerBanks[CALL_STACK_INDEX] = new RegisterFileModule(GET_ID(), REGISTER_FILE_MODE.STACK, callStackLengths, callStackNames);
        registerBanks[REVERSAL_STACK_INDEX] = new RegisterFileModule(GET_ID(), REGISTER_FILE_MODE.STACK_CIRCULAR, reversalStackLengths, reversalStackNames);
        int[][] pendingRegisters = NEW_PENDING_REGISTERS(registerBanks);
        registerBanks[INDEXABLE_BANK_INDEX].pendings = pendingRegisters[INDEXABLE_BANK_INDEX];
        registerBanks[INTERNAL_BANK_INDEX].pendings = pendingRegisters[INTERNAL_BANK_INDEX];
        registerBanks[CALL_STACK_INDEX].pendings = pendingRegisters[CALL_STACK_INDEX];
        registerBanks[REVERSAL_STACK_INDEX].pendings = pendingRegisters[REVERSAL_STACK_INDEX];
        new Simulator(GET_ID(), registerBanks, new Pipeline(registerBanks[INDEXABLE_BANK_INDEX], registerBanks[INTERNAL_BANK_INDEX], registerBanks[CALL_STACK_INDEX], registerBanks[REVERSAL_STACK_INDEX], null, null, pendingRegisters, startingParams[0]), JFrame.MAXIMIZED_BOTH, startingPC);
    }

    private static int[] FIND_START_PARAMS(String path)
    {
        int[] ret = new int[] { 32, 0b1000000000, 0b1000000000 };

        File directory = new File(path);
        File[] files = directory.listFiles();
        try
        {
            FileInputStream fis = new FileInputStream(files[0]);
            byte[] buffer = new byte[4];
            if(fis.read(buffer) == 4)
            {
                int value = (buffer[0] << 24) | (buffer[1] << 16) | (buffer[2] << 8) | buffer[3];
                if((value & 0b10000000000000000000000000000000) == 0b10000000000000000000000000000000)
                {
                    ret[0] = 64;
                }
                for(int i = 0; i < 10; i++)
                {
                    if((value & (0b01000000000000000000000000000000 >> i)) != 0b00000000000000000000000000000000)
                    {
                        ret[1] = (value & (0b01000000000000000000000000000000 >> i)) >> 20;
                        break;
                    }
                }
                for(int i = 0; i < 10; i++)
                {
                    if((value & (0b00000000000100000000000000000000 >> i)) != 0b00000000000000000000000000000000)
                    {
                        ret[2] = (value & (0b00000000000100000000000000000000 >> i)) >> 10;
                        break;
                    }
                }
            }
            fis.close();
        }
        catch(ArrayIndexOutOfBoundsException ignored) {}
        catch(IOException e)
        {
            e.printStackTrace();
        }

        return ret;
    }
}