package main;

import memory.RegisterFileModule;
import simulator.Simulator;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import static main.GLOBALS.*;

public class Main
{
    public static void main(String[] args)
    {
        // TODO : Ensure PC starts at 10 if 32 bit word mode or 01 if 64 bit word mode
        //createTestInstructionBinary("00");

        RegisterFileModule[] registerBanks = new RegisterFileModule[REGISTER_BANK_INDECES.length];
        int[] indexableLengths = new int[INDEXABLE_BANK_SIZE];
        String[] indexableNames = new String[INDEXABLE_BANK_SIZE];
        for(int i = 0; i < indexableLengths.length; i++)
        {
            indexableLengths[i] = 32;
            indexableNames[i] = "R" + i;
        }
        int[] internalLengths = new int[] { 1, 25, 16, 64, 64, 10, 10 };
        String[] internalNames = INTERNAL_REGISTER_NAMES;
        // TODO : Ensure external pushes to and pops from call stack perform 17 operations in correct order
        int[] callStackLengths = new int[(int)Math.pow(2, internalLengths[5]) * (indexableLengths.length + 1)];  // Will be pushed/popped in groups of 17; 1 for return pointer and 16 for snapshot of indexable register file
        String[] callStackNames = new String[(int)Math.pow(2, internalLengths[5]) * (indexableLengths.length + 1)];
        for(int i = 0; i < callStackLengths.length; i++)
        {
            int index = i % (indexableLengths.length + 1);
            callStackLengths[i] = (index == 0) ? 25 : 32;
            callStackNames[i] = i / (indexableLengths.length + 1) + " " + ((index == 0) ? "R" : index);
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
        new Simulator(GET_ID(), registerBanks, JFrame.MAXIMIZED_BOTH);
    }

    private static void createTestInstructionBinary(String name)
    {
        String filePath = PATH_TO_INSTRUCTION_BINS + name + ".txt";
        File file = new File(filePath);

        try
        {
            if(file.exists())
            {
                file.delete();
            }

            // Ensure the parent directories exist
            file.getParentFile().mkdirs();

            boolean isFileCreated = file.createNewFile();

            if(isFileCreated)
            {
                try(FileOutputStream writer = new FileOutputStream(file))
                {
                    byte[] words = new byte[] {
                            (byte)0b01000000, (byte)0b00010000, (byte)0b00000000, (byte)0b00000000,  // 32-bit word mode with size 1024 reversal and call stacks
                            (byte)0b00000000, (byte)0b00000000, (byte)0b00000000, (byte)0b00000000,
                            (byte)0b11110110, (byte)0b00000000, (byte)0b00000000, (byte)0b00000000  // HALT 0
                    };
                    writer.write(words);

                    System.out.println("Test binary created and text written successfully.");
                }
                catch(IOException e)
                {
                    System.out.println("An error occurred while writing to the test binary.");
                }
            }
            else
            {
                System.out.println("Test binary could not be overwritten or could not be created.");
            }
        }
        catch(IOException e)
        {
            System.out.println("An error occurred while checking or creating the test binary.");
            e.printStackTrace();
        }
    }
}