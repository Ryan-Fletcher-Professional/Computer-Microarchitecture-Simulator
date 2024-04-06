package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

import static main.GLOBALS.*;
import static instructions.Instructions.*;

public class Assembler
{
    public static final char LABEL_DEFINE = '=';
    public static final char LABEL_USE = '%';
    public static final String MEM_LABEL = "MEM";
    public static final char COMMENT = '@';
    public static final char BASE_10 = '#';
    public static final char BASE_16 = 'x';
    public static final char REGISTER_PREFIX = 'R';
    public static final char PLUS = '+';
    public static final char MINUS = '-';

    public static void main(String[] args) throws AssemblyError
    {
        String assemblyPath = System.getProperty("user.dir") + "/src/files/assembly/";
        String binaryPath = System.getProperty("user.dir") + "/src/files/bin/";
        subMain(new String[] { "-s", assemblyPath + "testInstr.txt", "--data", assemblyPath + "testData.txt", "-d", binaryPath });
    }

    public static void subMain(String[] args) throws AssemblyError
    {
        System.out.println("\n!!!!!!!!    Running assembler    !!!!!!!!\n");

        if(args.length < 4) { throw new IllegalArgumentException("Assembler arguments format: -s <sourceFilePath>{ <sourceFilePath2> <sourceFilePath3> ...} -d destinationFileDirectory{(repeat all previous 0 or more times)}"); }
        if(!List.of(args).contains("-s") || (List.of(args).lastIndexOf("-s") > (args.length - 4))) { throw new IllegalArgumentException("Assembler must have -s argument followed by one or more source file paths followed by -d and a destination directory"); }
        if(!List.of(args).contains("-d") || (List.of(args).indexOf("-d") < List.of(args).indexOf("-s")) || (List.of(args).lastIndexOf("-d") != (args.length - 2))) { throw new IllegalArgumentException("Assembler must have -d argument preceded by -s and one or more source file paths and followed by a destiation directory"); }

        List<String> arguments = List.of(args);
        Map<String, Integer> labels = new HashMap<>();

        boolean dataMode = false;
        int data = arguments.indexOf("--data");
        int d = arguments.indexOf("-d");
        for(int i = 1; i < d; i++)
        {
            if(i == data)
            {
                dataMode = true;
                continue;
            }
            File source = new File(arguments.get(i));
            if(!source.isFile())
            {
                System.out.printf("The specified source file \"%s\" is not a file or doesn't exist.%n", arguments.get(i));
                continue;
            }

            File destination = new File(arguments.get(d + 1));
            if(!destination.exists())
            {
                System.out.printf("The specified destination directory \"%s\" does not exist.%n", arguments.get(d + 1));
            }

            try
            {
                ASSEMBLE(Files.readString(source.toPath()), destination, i, dataMode, labels);
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static void ASSEMBLE(String text, File destinationDirectory, int num, boolean dataMode, Map<String, Integer> labels) throws AssemblyError
    {
        String[] lines = text.split("\\r?\\n|\\r", -1);  // Preserves empty lines for line indexing purposes
        for(int i = 0; i < lines.length; i++)
        {
            int start = 0;
            while((start < lines[i].length()) && ((lines[i].charAt(start) == ' ') || (lines[i].charAt(start) == '\t')))
            {
                start++;
            }
            lines[i] = lines[i].substring(start);
        }

        List<Integer> words = new ArrayList<>();

        int wordSize = WORD_SIZE_SHORT;

        for(int i = 1; i <= lines.length; i++)
        {
            String line = lines[i - 1];

            if(line.isEmpty() || line.charAt(0) != LABEL_DEFINE) { continue; }
            if(CONTAINS_WHITESPACE(line) || (line.length() < 2))
                { throw new AssemblyError("Invalid label at line " + i); }
            if(labels.get(line.substring(1)) != null)
                { throw new AssemblyError("Duplicate label \"" + line.substring(1) + "\" at line " + i); }
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

                if(CONTAINS_WHITESPACE(line))
                    { throw new AssemblyError("Whitespace in data mode in line " + i); }

                words.add((int)PARSE_WORD_FOR_DATA(line, i, labels));
            }
        }
        else
        {
            int stackSize = 0b1000000000;
            int bufferSize = 0b1000000000;

            int index = 0;
            if(lines[index].equals("SET WORD SIZE 32"))
            {
                index++;
            }
            else if(lines[index].equals("SET WORD SIZE 64"))
            {
                wordSize = WORD_SIZE_LONG;
                index++;
            }
            if((index < lines.length) && lines[index].startsWith("SET STACK SIZE "))
            {
                int value;
                try
                {
                    value = Integer.parseUnsignedInt(lines[index].substring("SET STACK SIZE ".length()), 2);
                }
                catch(NumberFormatException e)
                {
                    throw new AssemblyError("Invalid SET STACK SIZE argument");
                }

                for(int i = 0; i < 10; i++)
                {
                    if((value & (0b1000000000 >> i)) != 0b0000000000)
                    {
                        stackSize = value & (0b1000000000 >> i);
                        break;
                    }
                }
                index++;
            }
            if((index < lines.length) && lines[index].startsWith("SET BUFFER SIZE "))
            {
                int value;
                try
                {
                    value = Integer.parseUnsignedInt(lines[index].substring("SET BUFFER SIZE ".length()), 2);
                }
                catch(NumberFormatException e)
                {
                    throw new AssemblyError("Invalid SET BUFFER SIZE argument");
                }

                for(int i = 0; i < 10; i++)
                {
                    if((value & (0b1000000000 >> i)) != 0b0000000000)
                    {
                        bufferSize = value & (0b1000000000 >> i);
                        break;
                    }
                }
                index++;
            }

            if(labels.get(MEM_LABEL) == null)
                { labels.put(MEM_LABEL, (int)((wordSize == WORD_SIZE_SHORT)
                                                ? SHORT_INSTRUCTION_ADDRESS_FIX(lines.length + 1, index)
                                                : LONG_INSTRUCTION_ADDRESS_FIX(lines.length + 1, index))); }

            words.add((((wordSize == WORD_SIZE_SHORT) ? 0 : 1) << 31) | (stackSize << 21) | (bufferSize << 11));
            words.add(0);

            for(int i = index + 1; i <= lines.length; i++)  // i is the line number the coder sees
            {
                String line = lines[i - 1];

                int firstNonWhitespace = 0;
                while(!line.isEmpty() && ((line.charAt(firstNonWhitespace) == ' ') || (line.charAt(firstNonWhitespace) == '\t')))
                    { firstNonWhitespace++; }
                if(line.isEmpty() || (line.charAt(firstNonWhitespace) == LABEL_DEFINE) || (line.charAt(firstNonWhitespace) == COMMENT))
                {
                    long instruction = NOOP(wordSize).wordNum();
                    if(wordSize == WORD_SIZE_LONG) { words.add((int)(instruction >>> (Long.SIZE - Integer.SIZE))); }
                    words.add((int)instruction);
                    continue;
                }

                List<String> tokensAll = new ArrayList<>();
                for(String unit : line.split(COMMENT + "")[0].split("\t"))
                {
                    for(String atom : unit.split(" "))
                    {
                        if(!atom.isEmpty()) { tokensAll.add(atom); }
                    }
                }
                List<String> tokensList = new ArrayList<>();
                for(String token : tokensAll)
                {
                    if(!token.startsWith(COMMENT + "")) { tokensList.add(token); }
                    else { break; }
                }
                long instruction = PARSE_INSTRUCTION(tokensList.toArray(new String[] {}), wordSize, i, index, labels);
                if(wordSize == WORD_SIZE_LONG) { words.add((int)(instruction >>> (Long.SIZE - Integer.SIZE))); }
                words.add((int)instruction);
            }
        }

        try
        {
            File file = new File(destinationDirectory.getAbsolutePath() + "/" + num + ".txt");

            if(file.exists())
            {
                file.delete();
            }

            file.getParentFile().mkdirs();

            boolean isFileCreated = file.createNewFile();

            if(isFileCreated)
            {
                try(FileOutputStream writer = new FileOutputStream(file))
                {
                    for(int word : words)
                    {
                        byte[] data = new byte[4];
                        data[0] = (byte)(word >>> 24);
                        data[1] = (byte)(word >>> 16);
                        data[2] = (byte)(word >>> 8);
                        data[3] = (byte)(word >>> 0);
                        writer.write(data);
                    }
                    System.out.println("Bin file created and text written successfully.");
                }
                catch(IOException e)
                {
                    System.out.println("An error occurred while writing to the bin file.");
                }
            }
            else
            {
                System.out.println("Bin file could not be overwritten or could not be created.");
            }
        }
        catch(IOException e)
        {
            System.out.println("An error occurred while writing to bin file");
            e.printStackTrace();
        }
    }

    /**
     * Translates the instruction line number the coder sees into the absolute memory address when instructions are short
     *  and there are numStartCommands of the three possible special commands at the start of the file.
     * @param lineNum
     * @return
     */
    public static long SHORT_INSTRUCTION_ADDRESS_FIX(long lineNum, int numStartInstructions)
    {
        return lineNum - 1 - (numStartInstructions - 2);
    }

    /**
     * Translates the instruction line number the coder sees into the absolute memory address when instructions are long
     *  and there are numStartCommands of the three possible special commands at the start of the file.
     * @param lineNum
     * @return
     */
    public static long LONG_INSTRUCTION_ADDRESS_FIX(long lineNum, int numStartInstructions)
    {
        return ((lineNum - 1) * 2) - ((numStartInstructions - 1) * 2L);
    }

    private static boolean CONTAINS_WHITESPACE(String str)
    {
        for(int i = 0; i < str.length(); i++)
        {
            if(Character.isWhitespace(str.charAt(i))) { return true; }
        }
        return false;
    }

    private static int BASE(String term, int lineNum)
    {
        if(term.isEmpty()) { throw new NumberFormatException("Empty term at line " + lineNum); }

        int base = 2;
        if((term.charAt(0) == BASE_10) || (term.charAt(0) == REGISTER_PREFIX))
        {
            base = 10;
        }
        if(term.charAt(0) == BASE_16)
        {
            base = 16;
        }

        if((base != 2) && (term.length() < 2)) { throw new NumberFormatException("Value-less term at line " + lineNum); }
        return base;
    }

    private static long PARSE_WORD_FOR_DATA(String text, int lineNum, Map<String, Integer> labels) throws AssemblyError
    {
        if(text.charAt(0) == LABEL_USE)
        {
            if(text.length() < 2)
                { throw new AssemblyError("Invalid label in line " + lineNum); }
            Integer labeledWord = labels.get(text.substring(1));
            if(labeledWord == null)
                { throw new AssemblyError("Undefined label " + text.substring(1) + " at line " + lineNum); }
            return(labeledWord);
        }

        return PARSE_TOKEN(text, lineNum, labels);
    }

    private static long PARSE_TOKEN(String token, int lineNum, Map<String, Integer> labels) throws AssemblyError
    {
        String[] terms = new String[] { token };
        int multiplier = 1;
        if(token.length() > 1)
        {
            String phrase = token.substring(1);
            if(phrase.indexOf(PLUS) > -1)
            {
                terms = (token.charAt(0) + phrase).split(Pattern.quote(PLUS + ""));
            }
            else if(phrase.indexOf(MINUS) > -1)
            {
                terms = (token.charAt(0) + phrase).split(Pattern.quote(MINUS + ""));
                multiplier *= -1;
            }
        }

        if(terms.length > 1)
        {
            for(int i = 0; i < terms.length; i++)
            {
                if(terms[i].charAt(0) == REGISTER_PREFIX)
                    { throw new AssemblyError("Register in multi-term expression in line " + lineNum); }
            }
        }

        long value = PARSE_TERM(terms[0], lineNum, labels);
        for(int i = 1; i < terms.length; i++)
        {
            value += multiplier * PARSE_TERM(terms[i], lineNum, labels);
        }
        return value;
    }

    private static long PARSE_TERM(String term, int lineNum, Map<String, Integer> labels) throws AssemblyError
    {
        if(term.isEmpty() || CONTAINS_WHITESPACE(term))
            { throw new AssemblyError("Empty or whitespace term at line " + lineNum); }

        if(term.charAt(0) == LABEL_USE)
        {
            Integer labelVal = labels.get(term.substring(1));
            if(labelVal == null)
                { throw new AssemblyError("Invalid label use at line " + lineNum); }
            return labelVal;
        }

        String kind = "value";
        int start = 0;
        if(term.charAt(start) == REGISTER_PREFIX)
        {
            kind = "register index";
            start = 1;
        }

        try
        {
            int base = BASE(term, lineNum);
            if(base != 2) { start = 1; }
            int multiplier = 1;
            if((term.charAt(start) == PLUS) || (term.charAt(start) == MINUS))
            {
                start += 1;
            }
            if(term.charAt(start) == MINUS)
            {
                if(term.charAt(0) == REGISTER_PREFIX) { throw new AssemblyError(); }
                multiplier = -1;
            }
            return multiplier * Long.parseUnsignedLong(term.substring(start), base);
        }
        catch(StringIndexOutOfBoundsException | NumberFormatException | AssemblyError e)
            { throw new AssemblyError("Invalid " + kind + " at line " + lineNum); }
    }

    private static long PARSE_INSTRUCTION(String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        HEADER header = HEADERS_FROM_MNEMONICS.get(tokens[0]);
        if(header == null)
            { throw new AssemblyError("Unrecognized mnemonic \"" + tokens[0] + "\" at line " + lineNum); }
        long word = Long.parseUnsignedLong(HEADER_STRINGS.get(header), 2) << (wordSize - HEADER_SIZE);
        switch(header)
        {  // TODO : Make sure all instruction addresses are LONG_INSTRUCTION_ADDRESS_FIX'd if necessary
            case HEADER.LOAD -> word = parseLoad(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.STORE -> word = parseStore(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.BRANCH_IF_NEGATIVE -> word = parseBranchIfNegative(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.INT_ADD -> word = parseIntAdd(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.COMPARE -> word = parseCompare(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.COPY -> word = parseCopy(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.HALT -> word = parseHalt(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
        }
        return word;
    }

    private static long SAFE(long arg, int length)
    {
        return (arg << (Long.SIZE - length)) >>> (Long.SIZE - length);
    }

    private static long parseLoad(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 3)
            { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <src adrs>, <dest reg>"); }
        if(!tokens[1].endsWith(","))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }
        if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <src adrs> is not a register"); }

            long src = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);

            int srcShift = 4;
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 4);

            return word | (src << srcShift) | dest;
        }
        else
        {
            long flag = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;

            long src = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag == 1) ? 25 : 4);

            int srcShift = 4;
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 4);

            return word | (flag << (wordSize - 7)) | (src << srcShift) | dest;
        }
    }

    private static long parseStore(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 3)
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <src>, <dest adrs>"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <src adrs> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <dest adrs> is not a register"); }

            long src = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 4);

            return word | (src << 4) | dest;
        }
        else
        {
            long flag_a = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;
            long flag_b = (!tokens[2].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;
            if((flag_a == 1L) && (flag_b == 1L))
                { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                        lineNum + ": both <src> and <dest adrs> are immediates"); }

            long src = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 1) ? 32 : 4);

            int srcShift = (flag_b == 1L) ? 25 : 4;
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), (flag_b == 1) ? 25 : 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (src << srcShift) | dest;
        }
    }

    private static long parseBranchIfNegative(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 2)
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <dest adrs>"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <dest adrs> is not a register"); }

            long flag = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;

            long dest = SHORT_INSTRUCTION_ADDRESS_FIX(SAFE(PARSE_TOKEN(tokens[1], lineNum, labels), (flag == 1) ? 25 : 4), numStartInstructions);

            return word | dest;
        }
        else
        {
            long flag = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;

            long dest = LONG_INSTRUCTION_ADDRESS_FIX(SAFE(PARSE_TOKEN(tokens[1], lineNum, labels), (flag == 1) ? 25 : 4), numStartInstructions);

            return word | (flag << (wordSize - 7)) | dest;
        }
    }

    private static long parseIntAdd(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if((tokens.length != 3) && (tokens.length != 5))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <operand 1>, <operand 2>{, <dest reg>, <set carry>}"); }
        for(int i = 1; i < tokens.length - 1; i++)
        {
            if(!tokens[i].endsWith(","))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": Missing comma separator"); }
        }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_c = (tokens.length == 3) ? 0L : (!tokens[4].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);

        if(tokens.length == 3)
            { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
                                      ((flag_a == 0) ? tokens[1] : (tokens[2] + ",")),
                                      "0" }; }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand 1> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand 2> is not a register"); }
            if(!tokens[3].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <dest reg> is not a register"); }

            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long operand2 = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[2].length() - 1), lineNum, labels), 4);
            long dest = SAFE(PARSE_TOKEN(tokens[3].substring(0, tokens[3].length() - 1), lineNum, labels), 4);
            long carry = SAFE(PARSE_TOKEN(tokens[4], lineNum, labels), 4);

            return word | (flag_c << (wordSize - 7)) | (operand1 << 12) | (operand2 << 8) | (dest << 4) | carry;
        }
        else
        {
            if(!tokens[3].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <dest reg> is not a register"); }

            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 1) ? 32 : 4);

            int operand1Shift = (flag_b == 1L) ? 40 : 12;

            long operand2 = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[2].length() - 1), lineNum, labels), (flag_b == 1) ? 32 : 4);

            long dest = SAFE(PARSE_TOKEN(tokens[3].substring(0, tokens[3].length() - 1), lineNum, labels), 4);
            long carry = SAFE(PARSE_TOKEN(tokens[4], lineNum, labels), 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (flag_c << (wordSize - 9))
                        | (operand1 << operand1Shift) | (operand2 << 8) | (dest << 4) | carry;
        }
    }

    private static long parseCompare(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 3)
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <operand1>, <operand2>"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand1> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand2> is not a register"); }

            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long operand2 = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 4);

            return word | (operand1 << 4) | operand2;
        }
        else
        {
            long flag_a = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;
            long flag_b = (!tokens[2].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;
            if((flag_a == 1L) && (flag_b == 1L))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": both <operand1> and <operand2> are immediates"); }

            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 1) ? 32 : 4);

            int operand1Shift = (flag_b == 1L) ? 32 : 4;
            long operand2 = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), (flag_b == 1) ? 32 : 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8))
                        | (operand1 << operand1Shift) | operand2;
        }
    }

    private static long parseCopy(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 3)
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <src>, <dest reg>"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }
        if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            long flag = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;

            long src = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag == 1) ? 21 : 4);

            int srcShift = 4;
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 4);

            return word | (flag << (wordSize - 7)) | (src << srcShift) | dest;
        }
        else
        {
            long flag = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;

            long src = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag == 1) ? 32 : 4);

            int srcShift = 4;
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 4);

            return word | (flag << (wordSize - 7)) | (src << srcShift) | dest;
        }
    }

    private static long parseHalt(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 2)
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + "<condition>"); }

        long flag = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;

        long cond = SAFE(PARSE_TOKEN(tokens[1], lineNum, labels), (flag == 1) ? 3 : 4);

        return word | (flag << (wordSize - 7)) | cond;
    }
}
