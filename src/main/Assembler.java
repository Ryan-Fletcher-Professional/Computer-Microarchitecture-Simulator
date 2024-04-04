package main;

import instructions.Instructions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static main.GLOBALS.*;
import static instructions.Instructions.*;

public class Assembler
{
    public static final char LABEL_DEFINE = '=';
    public static final char LABEL_USE = '$';
    public static final char COMMENT = '@';
    public static final char BASE_10 = '#';
    public static final char BASE_16 = 'x';

    public static void main(String[] args)
    {
        System.out.println("\n!!!!!!!!    Running assembler    !!!!!!!!\n");

        if(args.length < 4) { throw new IllegalArgumentException("Assembler arguments format: -s <sourceFilePath>{ <sourceFilePath2> <sourceFilePath3> ...} -d destinationFileDirectory{(repeat all previous 0 or more times)}"); }
        if(!List.of(args).contains("-s") || (List.of(args).lastIndexOf("-s") > (args.length - 4))) { throw new IllegalArgumentException("Assembler must have -s argument followed by one or more source file paths followed by -d and a destination directory"); }
        if(!List.of(args).contains("-d") || (List.of(args).indexOf("-d") < List.of(args).indexOf("-s")) || (List.of(args).lastIndexOf("-d") != (args.length - 2))) { throw new IllegalArgumentException("Assembler must have -d argument preceded by -s and one or more source file paths and followed by a destiation directory"); }

        List<String> arguments = List.of(args);

        boolean dataMode = args[0].equals("--data");

        int nextS = arguments.indexOf("-s");
        while(nextS != -1)
        {
            int nextD = nextS;
            while(!args[nextD++].equals("-d")) {}

            while(nextS++ != nextD)
            {
                File source = new File(args[nextS]);
                if(!source.isFile())
                {
                    System.out.printf("The specified source file \"%s\" is not a file or doesn't exist.%n", args[nextS]);
                    continue;
                }

                File destination = new File(args[nextD]);
                if(!destination.exists())
                {
                    System.out.printf("The specified destination directory \"%s\" does not exist.%n", args[nextD]);
                }

                try
                {
                    ASSEMBLE(new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8), destination, dataMode);
                }
                catch(IOException e)
                {
                    throw new RuntimeException(e);
                }
            }

            while(!args[++nextS].equals("-s")) {}
        }
    }

    private static void ASSEMBLE(String text, File destinationDirectory, boolean dataMode)
    {
        String[] lines = new String[(int)(text.chars().filter(ch -> ch == '\n').count()) + 1];  // Preserves empty lines for line indexing purposes
        Map<String, Integer> labels = new HashMap<>();
        List<Integer> words = new ArrayList<>();

        int wordSize = 32;  // TODO

        for(int i = 1; i <= lines.length; i++)
        {
            String line = lines[i - 1];

            if(line.isEmpty() || line.charAt(0) != LABEL_DEFINE) { continue; }
            if(CONTAINS_WHITESPACE(line) || (line.length() < 2)) { throw new RuntimeException("Invalid label at line " + i); }
            labels.put(line.substring(1), i);
        }

        if(dataMode)
        {
            for(int i = 1; i <= lines.length; i++)
            {
                String line = lines[i - 1];

                if(line.isEmpty() || (line.charAt(0) == LABEL_DEFINE) || (line.charAt(0) == COMMENT))
                {
                    words.add(0);
                    continue;
                }

                if(CONTAINS_WHITESPACE(line)) { throw new RuntimeException("Whitespace in data mode in line " + i); }

                words.add(PARSE_WORD(line, i, labels));
            }
        }
        else
        {
            for(int i = 1; i <= lines.length; i++)
            {
                String line = lines[i - 1];

                if(line.isEmpty() || (line.charAt(0) == LABEL_DEFINE) || (line.charAt(0) == COMMENT))
                {
                    long instruction = NOOP(wordSize).wordNum();
                    words.add((int)instruction);
                    if(wordSize == WORD_SIZE_LONG) { words.add((int)(instruction >>> (Long.SIZE - Integer.SIZE))); }
                    continue;
                }

                String[] tokens = line.split(" ");
                words.add(PARSE_INSTRUCTION(tokens, wordSize, i, labels));
            }
        }
    }

    private static boolean CONTAINS_WHITESPACE(String str)
    {
        for(int i = 0; i < str.length(); i++)
        {
            if(Character.isWhitespace(str.charAt(i))) { return true; }
        }
        return false;
    }

    private static int PARSE_WORD(String text, int lineNum, Map<String, Integer> labels)
    {
        if(text.charAt(0) == LABEL_USE)
        {
            if(text.length() < 2) { throw new RuntimeException("Invalid label in line " + lineNum); }
            Integer labeledWord = labels.get(text.substring(1));
            if(labeledWord == null) { throw new RuntimeException("Undefined label " + text.substring(1) + " at line " + lineNum); }
            return(labeledWord);
        }

        int radix = 2;
        if(text.charAt(0) == BASE_10) { radix = 10; }
        if(text.charAt(0) == BASE_16) { radix = 16; }

        return Integer.parseInt((radix != 2) ? text.substring(1) : text, radix);
    }

    private static int PARSE_INSTRUCTION(String[] tokens, int wordSize, int lineNum, Map<String, Integer> labels)
    {
        HEADER header = HEADERS_FROM_MNEMONICS.get(tokens[0]);
        if(header == null) { throw new RuntimeException("Unrecognized mnemonic " + tokens[0] + " at line " + lineNum); }
        long word = (Long.parseUnsignedLong(HEADER_STRINGS.get(header), 2) << (wordSize - HEADER_SIZE));
        switch(header)
        {
            case HEADER.LOAD:
                word = parseLoad(word);
        }
        return (int)word;  // TODO
    }

    private static long parseLoad(long word)
    {
        return 0;
    }
}
