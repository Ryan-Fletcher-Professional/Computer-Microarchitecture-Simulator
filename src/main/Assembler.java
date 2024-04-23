package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import static main.GLOBALS.*;
import static instructions.Instructions.*;

public class Assembler
{
    public static final char LABEL_DEFINE = '=';
    public static final char LABEL_USE = '%';
//    public static final String MEM_LABEL = "MEM";
    public static final char COMMENT = '@';
    public static final char BASE_10 = '#';
    public static final char BASE_16 = 'x';
    public static final char REGISTER_PREFIX = 'R';
    public static final char PLUS = '+';
    public static final char MINUS = '-';

    /**
     * Run this as its own program, separate from Main.main()
     * Will read files/assembly/ instructionAssembly.txt and dataAssembly.txt and, in that order, assemble them into
     *  binaries in files/bin
     */
    public static void main(String[] args) throws AssemblyError
    {
        String assemblyPath = System.getProperty("user.dir") + "/src/files/assembly/";
        String binaryPath = System.getProperty("user.dir") + "/src/files/bin/";
        subMain(new String[] { "-s", assemblyPath + "instructionAssembly.txt", "--data", assemblyPath + "dataAssembly.txt", "-d", binaryPath });
    }

    /**
     * Called by main()
     * Performs the assembly process.
     */
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
            if(i == data)  // If we've reached the data arg, go to the next arg and parse in data mode
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

    /**
     * @param num The 1-index of this file in the call args. Used to order data binary after instruction binary in
     *             destination directory.
     * @param labels Pre-gathered line labels mapped to line numbers
     */
    private static void ASSEMBLE(String text, File destinationDirectory, int num, boolean dataMode, Map<String, Integer> labels) throws AssemblyError
    {
        String[] lines = text.split("\\r?\\n|\\r", -1);  // Preserves empty lines for line indexing purposes
        for(int i = 0; i < lines.length; i++)  // Remove leading whitespace
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

        for(int i = 1; i <= lines.length; i++)  // Check for invalid label definitions
        {
            String line = lines[i - 1];

            if(line.isEmpty() || line.charAt(0) != LABEL_DEFINE) { continue; }
            if(CONTAINS_WHITESPACE(line) || (line.length() < 2))
                { throw new AssemblyError("Invalid label at line " + i); }
            if(labels.get(line.substring(1)) != null)
                { throw new AssemblyError("Duplicate label \"" + line.substring(1) + "\" at line " + i); }
            labels.put(line.substring(1), i);
        }

        if(dataMode)  // Read preloaded data memory into usable binary
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
        else  // Read instructions into usable instruction binary
        {
            int stackSize = 0b1000000000;
            int bufferSize = 0b1000000000;
            int set = 0b000;

            int index = 0;
            if(lines[index].equals("SET WORD SIZE 32"))
            {
                index++;
                set |= 0b001;
            }
            else if(lines[index].equals("SET WORD SIZE 64"))
            {
                wordSize = WORD_SIZE_LONG;
                index++;
                set |= 0b001;
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
                        set |= 0b010;
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
                        set |= 0b100;
                        break;
                    }
                }
                index++;
            }

            List<String> newLines = new ArrayList<>(List.of(lines));
            while(((newLines.size() - index) % (8 / (wordSize / WORD_SIZE_SHORT))) != 0)  // This is to pad out the final line in instruction memory
            {
                newLines.add("HALT #0");
            }
            newLines.add("HALT #0");
            while(((newLines.size() - index) % (8 / (wordSize / WORD_SIZE_SHORT))) != 0)  // This is to add an extra line in instruction memory as a buffer to prevent data from being read as instructions before HALT is registered
            {
                newLines.add("HALT #0");
            }
            lines = newLines.toArray(new String[] {});

            // Set first line of memory
            words.add((((wordSize == WORD_SIZE_SHORT) ? 0 : 1) << 31) | (stackSize << 21) | (bufferSize << 11) | (set << 8));
            words.add((int)((wordSize == WORD_SIZE_SHORT)
                                ? SHORT_INSTRUCTION_ADDRESS_FIX(lines.length + 1, index)
                                : LONG_INSTRUCTION_ADDRESS_FIX(lines.length + 1, index)));
            for(int i = 0; i < (8 - 2); i++)
            {
                words.add(0);
            }

            for(int i = index + 1; i <= lines.length; i++)  // i is the line number the coder sees
            {
                String line = lines[i - 1];

                int firstNonWhitespace = 0;
                while(!line.isEmpty() && ((line.charAt(firstNonWhitespace) == ' ') || (line.charAt(firstNonWhitespace) == '\t')))
                    { firstNonWhitespace++; }
                if(line.isEmpty() || (line.charAt(firstNonWhitespace) == LABEL_DEFINE) || (line.charAt(firstNonWhitespace) == COMMENT))
                {  // Ignorable lines become noops. TODO : Auto-adjust memory start and instruction addresses to get rid of these noops.
                    long instruction = NOOP(wordSize).wordNum();
                    if(wordSize == WORD_SIZE_LONG) { words.add((int)(instruction >>> (Long.SIZE - Integer.SIZE))); }
                    words.add((int)instruction);
                    continue;
                }

                List<String> tokensAll = new ArrayList<>();
                for(String unit : line.split(COMMENT + "")[0].split("\t"))  // Split each line into instruction and comment sections, ignore comment section
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
        {  // Write destination binary
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
        return lineNum - 1 - (numStartInstructions - 8);
    }

    public static long SHORT_INSTRUCTION_ADDRESS_UNFIX(long address, int numStartInstructions)
    {
        return address + (numStartInstructions - 8) + 1;
    }

    /**
     * Translates the instruction line number the coder sees into the absolute memory address when instructions are long
     *  and there are numStartCommands of the three possible special commands at the start of the file.
     * @param lineNum
     * @return
     */
    public static long LONG_INSTRUCTION_ADDRESS_FIX(long lineNum, int numStartInstructions)
    {
        return ((lineNum - 1) * 2) - ((numStartInstructions - 4) * 2L);
    }

    public static long LONG_INSTRUCTION_ADDRESS_UNFIX(long address, int numStartInstructions)
    {
        return ((address + ((numStartInstructions - 4) * 2L)) / 2) + 1;
    }

    private static boolean CONTAINS_WHITESPACE(String str)
    {
        for(int i = 0; i < str.length(); i++)
        {
            if(Character.isWhitespace(str.charAt(i))) { return true; }
        }
        return false;
    }

    /**
     * Determines numerical base of a term (not Term) based on prefix (or lack thereof)
     */
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

    // term is not a Term
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

    /**
     * Takes header from line mnemonic, puts it at the beginning of a long (rest of bits are 0) and passes that to the
     *  appropriate instruction parse method.
     * @param tokens Array of Strings, which are the pre-comment sections of the line separated by the spaces. Some
     *               tokens may correctly include following commas. Allow for those, while throwing errors at incorrect
     *               commas.
     * @param wordSize 32 or 64
     * @param lineNum Assembly line number of the given instruction. Use for address fixing and error throwing
     * @param numStartInstructions [0,3] Number of configuration instructions explicated at start of file
     * @param labels Map of line label Strings to their corresponding line number
     * @return long with the correct bits to write to the binary file to represent this instruction
     * @throws AssemblyError if the assembly code is written incorrectly
     */
    private static long PARSE_INSTRUCTION(String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        HEADER header = HEADERS_FROM_MNEMONICS.get(tokens[0]);
        if(header == null)
            { throw new AssemblyError("Unrecognized mnemonic \"" + tokens[0] + "\" at line " + lineNum); }
        long word = Long.parseUnsignedLong(HEADER_STRINGS.get(header), 2) << (wordSize - HEADER_SIZE);
        switch(header)  // TODO : Add new instructions here
        {  // TODO : Make sure all instruction addresses are <SHORT/LONG>_INSTRUCTION_ADDRESS_FIX'd if necessary
            case HEADER.LOAD -> word = parseLoad(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.LOAD_LINE -> word = parseLoadLine(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.STORE -> word = parseStore(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.STORE_LINE -> word = parseStoreLine(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);

            case HEADER.BRANCH_IF_ZERO -> word = parseBranchIfZero(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.BRANCH_IF_NEGATIVE -> word = parseBranchIfNegative(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.JUMP -> word = parseJump(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.CALL -> word = parseCall(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.RETURN -> {}  // RETURN has no args

            case HEADER.INT_ADD -> word = parseIntAdd(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.INT_SUB -> word = parseIntSubtract(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.INT_MUL -> word = parseIntMultiply(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.INT_DIV -> word = parseIntDivide(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.INT_MOD -> word = parseIntModulo(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);

            case HEADER.AND -> word = parseAND(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.OR -> word = parseOR(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.XOR -> word = parseXOR(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.NOT -> word = parseNOT(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.COMPARE -> word = parseCompare(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);

            case HEADER.COPY -> word = parseCopy(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.SWAP -> word = parseSwap(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);

            case HEADER.UNDO -> word = parseUndo(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);
            case HEADER.HALT -> word = parseHalt(MNEMONICS.get(header), word, tokens, wordSize, lineNum, numStartInstructions, labels);

            default -> throw new AssemblyError("UNPARSED INSTRUCTION at line " + lineNum);
        }
        return word;
    }

    /**
     * TODO : IMPORTANT! Use this to restrict the bits which each argument can have to the bits it's supposed to have.
     * Argument should be situated all the way to the right and will be returned in that same position.
     * (See parseLoad long word section for good examples.)
     */
    public static long SAFE(long arg, int length)
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

    private static long parseLoadLine(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 3)
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <src adrs>, <dest reg start>"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }
        if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg start> is not a register"); }

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

            int srcShift = (flag_a == 0L) ? 25 : 4;
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), (flag_b == 1) ? 25 : 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (src << srcShift) | dest;
        }
    }

    private static long parseStoreLine(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 3)
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <src reg start>, <dest adrs>"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }
        if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <src reg start> is not a register"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <dest adrs> is not a register"); }

            long src = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);

            int srcShift = 4;
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 4);

            return word | (src << srcShift) | dest;
        }
        else
        {
            long flag = (!tokens[2].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;

            long src = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);

            int srcShift = 25;
            long dest = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), (flag == 1) ? 25 : 4);

            return word | (flag << (wordSize - 7)) | (src << srcShift) | dest;
        }
    }

    private static long parseBranchIfZero(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
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
            long flag = !tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L;

            long dest = LONG_INSTRUCTION_ADDRESS_FIX(SAFE(PARSE_TOKEN(tokens[1], lineNum, labels), (flag == 1) ? 25 : 4), numStartInstructions);

            return word | (flag << (wordSize - 7)) | dest;
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
            long flag = !tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L;

            long dest = LONG_INSTRUCTION_ADDRESS_FIX(SAFE(PARSE_TOKEN(tokens[1], lineNum, labels), (flag == 1) ? 25 : 4), numStartInstructions);

            return word | (flag << (wordSize - 7)) | dest;
        }
    }

    private static long parseJump(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
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
            long flag = !tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L;

            long dest = LONG_INSTRUCTION_ADDRESS_FIX(SAFE(PARSE_TOKEN(tokens[1], lineNum, labels), (flag == 1) ? 25 : 4), numStartInstructions);

            return word | (flag << (wordSize - 7)) | dest;
        }
    }

    private static long parseCall(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if((tokens.length < 2) || (tokens.length > 3))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <dest adrs>{, <sign><return shift>}"); }

        boolean defaulted = false;
        if(tokens.length == 2)
        {
            tokens = new String[] { tokens[0], tokens[1] + ",", "+1" };
            defaulted = true;
        }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <dest reg> is not a register"); }
            if(!((tokens[2].charAt(0) == '+') || (tokens[2].charAt(0) == '-')))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <sign> is not a + or -"); }
            if(!(defaulted || tokens[2].substring(1).startsWith(REGISTER_PREFIX + "")))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <return shift> is not a register"); }

            long flag_b = !tokens[2].substring(1).startsWith(REGISTER_PREFIX + "") ? 0L : 1L;
            long flag_c = tokens[2].charAt(0) == '+' ? 0L : 1L;

            long dest = SHORT_INSTRUCTION_ADDRESS_FIX(SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4), numStartInstructions);
            long shift = SAFE(PARSE_TOKEN(tokens[2].substring(1), lineNum, labels), 4);

            return word | (flag_b << (wordSize - 7)) | (flag_c << (wordSize - 8)) | (dest << 4) | shift;
        }
        else
        {
            if(!((tokens[2].charAt(0) == '+') || (tokens[2].charAt(0) == '-')))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <sign> is not a + or -"); }

            long flag_a = !tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L;
            long flag_b = !tokens[2].substring(1).startsWith(REGISTER_PREFIX + "") ? 0L : 1L;
            long flag_c = tokens[2].charAt(0) == '+' ? 0L : 1L;

            long dest = LONG_INSTRUCTION_ADDRESS_FIX(SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 0) ? 4 : 25), numStartInstructions);
//            System.out.println(dest);
            long shift = SAFE(PARSE_TOKEN(tokens[2].substring(1), lineNum, labels) * 2, (flag_b == 0) ? 25 : 4);
            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (flag_c << (wordSize - 9)) | (dest << ((flag_b == 0) ? 24 : 4)) | shift;
        }
    }

    private static long parseIntAdd(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if((tokens.length < 3) || (tokens.length > 5))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <operand 1>, <operand 2>{, <dest reg>{, <set carry>}}"); }
        for(int i = 1; i < tokens.length - 1; i++)
        {
            if(!tokens[i].endsWith(","))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": Missing comma separator"); }
        }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_c = (tokens.length < 5) ? 0L : (!tokens[4].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);

        if(tokens.length == 3)
            { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
                                      ((flag_a == 0) ? tokens[1] : (tokens[2] + ",")),
                                      "0" }; }
        if(tokens.length == 4)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2], tokens[3] + ",",
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

    private static long parseIntSubtract(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        // Should be the same as parseIntAdd() except the format reminders
        if((tokens.length < 3) || (tokens.length > 5))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <operand 1>, <operand 2>{, <dest reg>{, <compare>}}"); }
        for(int i = 1; i < tokens.length - 1; i++)
        {
            if(!tokens[i].endsWith(","))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": Missing comma separator"); }
        }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_c = (tokens.length < 5) ? 0L : (!tokens[4].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);

        if(tokens.length == 3)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
            ((flag_a == 0) ? tokens[1] : (tokens[2] + ",")),
            "0" }; }
        if(tokens.length == 4)
            { tokens = new String[] { tokens[0], tokens[1], tokens[2], tokens[3] + ",",
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
                                          lineNum + ": <compare> is not a register"); }

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

    private static long parseIntMultiply(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        // Should be the same as parseIntAdd() except the format reminders
        if((tokens.length < 3) || (tokens.length > 5))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <operand 1>, <operand 2>{, <dest reg>{, <major>}}"); }
        for(int i = 1; i < tokens.length - 1; i++)
        {
            if(!tokens[i].endsWith(","))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": Missing comma separator"); }
        }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_c = (tokens.length < 5) ? 0L : 1L;

        if(tokens.length == 3)
            { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
                ((flag_a == 0) ? tokens[1] : (tokens[2] + ",")),
                "R0" }; }
        if(tokens.length == 4)
            { tokens = new String[] { tokens[0], tokens[1], tokens[2], tokens[3] + ",",
                "R0" }; }

        if(!tokens[3].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }
        if(!tokens[4].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <major> is not a register"); }
        if((flag_c == 1) && tokens[4].equals(tokens[3].substring(0, tokens[3].length() - 1)))
        { throw new AssemblyError("Invalid argument for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <major> is the same as <dest reg>"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand 1> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand 2> is not a register"); }

            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long operand2 = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[2].length() - 1), lineNum, labels), 4);
            long dest = SAFE(PARSE_TOKEN(tokens[3].substring(0, tokens[3].length() - 1), lineNum, labels), 4);
            long major = SAFE(PARSE_TOKEN(tokens[4], lineNum, labels), 4);

            return word | (flag_c << (wordSize - 7)) | (operand1 << 12) | (operand2 << 8) | (dest << 4) | major;
        }
        else
        {
            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 1) ? 32 : 4);

            int operand1Shift = (flag_b == 1L) ? 40 : 12;

            long operand2 = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[2].length() - 1), lineNum, labels), (flag_b == 1) ? 32 : 4);

            long dest = SAFE(PARSE_TOKEN(tokens[3].substring(0, tokens[3].length() - 1), lineNum, labels), 4);
            long major = SAFE(PARSE_TOKEN(tokens[4], lineNum, labels), 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (flag_c << (wordSize - 9))
                | (operand1 << operand1Shift) | (operand2 << 8) | (dest << 4) | major;
        }
    }

    private static long parseIntDivide(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        // Should be the same as parseIntAdd() except the format reminders
        if((tokens.length < 3) || (tokens.length > 5))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <operand 1>, <operand 2>{, <dest reg>{, <remainder>}}"); }
        for(int i = 1; i < tokens.length - 1; i++)
        {
            if(!tokens[i].endsWith(","))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": Missing comma separator"); }
        }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_c = (tokens.length < 5) ? 0L : 1L;

        if(tokens.length == 3)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
            ((flag_a == 0) ? tokens[1] : (tokens[2] + ",")),
            "R0" }; }
        if(tokens.length == 4)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2], tokens[3] + ",",
            "R0" }; }

        if(!tokens[3].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }
        if(!tokens[4].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <remainder> is not a register"); }
        if((flag_c == 1) && tokens[4].equals(tokens[3].substring(0, tokens[3].length() - 1)))
        { throw new AssemblyError("Invalid argument for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <remainder> is the same as <dest reg>"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand 1> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand 2> is not a register"); }

            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long operand2 = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[2].length() - 1), lineNum, labels), 4);
            long dest = SAFE(PARSE_TOKEN(tokens[3].substring(0, tokens[3].length() - 1), lineNum, labels), 4);
            long remainder = SAFE(PARSE_TOKEN(tokens[4], lineNum, labels), 4);

            return word | (flag_c << (wordSize - 7)) | (operand1 << 12) | (operand2 << 8) | (dest << 4) | remainder;
        }
        else
        {
            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 1) ? 32 : 4);

            int operand1Shift = (flag_b == 1L) ? 40 : 12;

            long operand2 = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[2].length() - 1), lineNum, labels), (flag_b == 1) ? 32 : 4);

            long dest = SAFE(PARSE_TOKEN(tokens[3].substring(0, tokens[3].length() - 1), lineNum, labels), 4);
            long remainder = SAFE(PARSE_TOKEN(tokens[4], lineNum, labels), 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (flag_c << (wordSize - 9))
                | (operand1 << operand1Shift) | (operand2 << 8) | (dest << 4) | remainder;
        }
    }

    private static long parseIntModulo(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        // Should be the same as parseIntAdd() except the format reminders
        if((tokens.length < 3) || (tokens.length > 5))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <operand 1>, <operand 2>{, <dest reg>{, <divisor>}}"); }
        for(int i = 1; i < tokens.length - 1; i++)
        {
            if(!tokens[i].endsWith(","))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": Missing comma separator"); }
        }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_c = (tokens.length < 5) ? 0L : 1L;

        if(tokens.length == 3)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
            ((flag_a == 0) ? tokens[1] : (tokens[2] + ",")),
            "R0" }; }
        if(tokens.length == 4)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2], tokens[3] + ",",
            "R0" }; }

        if(!tokens[3].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }
        if(!tokens[4].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <divisor> is not a register"); }
        if((flag_c == 1) && tokens[4].equals(tokens[3].substring(0, tokens[3].length() - 1)))
        { throw new AssemblyError("Invalid argument for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <divisor> is the same as <dest reg>"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand 1> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <operand 2> is not a register"); }

            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long operand2 = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[2].length() - 1), lineNum, labels), 4);
            long dest = SAFE(PARSE_TOKEN(tokens[3].substring(0, tokens[3].length() - 1), lineNum, labels), 4);
            long divisor = SAFE(PARSE_TOKEN(tokens[4], lineNum, labels), 4);

            return word | (flag_c << (wordSize - 7)) | (operand1 << 12) | (operand2 << 8) | (dest << 4) | divisor;
        }
        else
        {
            long operand1 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 1) ? 32 : 4);

            int operand1Shift = (flag_b == 1L) ? 40 : 12;

            long operand2 = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[2].length() - 1), lineNum, labels), (flag_b == 1) ? 32 : 4);

            long dest = SAFE(PARSE_TOKEN(tokens[3].substring(0, tokens[3].length() - 1), lineNum, labels), 4);
            long divisor = SAFE(PARSE_TOKEN(tokens[4], lineNum, labels), 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (flag_c << (wordSize - 9))
                | (operand1 << operand1Shift) | (operand2 << 8) | (dest << 4) | divisor;
        }
    }

    private static long parseAND(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if((tokens.length < 3) || (tokens.length > 4))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <x>, <y>{, <dest reg>}"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);

        if((flag_a == 1) && (flag_b == 1))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": both <x> and <y> are immediates"); }

        if(tokens.length == 3)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
            ((flag_a == 0) ? tokens[1].substring(0, tokens[1].length() - 1) : tokens[2]) }; }

        if(!tokens[3].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <x> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <y> is not a register"); }

            long x = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long y = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long dest = SAFE(PARSE_TOKEN(tokens[3], lineNum, labels), 4);

            return word | (x << 8) | (y << 4) | dest;
        }
        else
        {
            long x = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 0) ? 4 : 32);
            int xShift = (flag_b == 0) ? 4 : 32;
            long y = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_b == 0) ? 4 : 32);
            long dest = SAFE(PARSE_TOKEN(tokens[3], lineNum, labels), 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (x << xShift) | (y << 4) | dest;
        }
    }

    private static long parseOR(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {  // Should be the same as parseAND
        if((tokens.length < 3) || (tokens.length > 4))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <x>, <y>{, <dest reg>}"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);

        if((flag_a == 1) && (flag_b == 1))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": both <x> and <y> are immediates"); }

        if(tokens.length == 3)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
            ((flag_a == 0) ? tokens[1].substring(0, tokens[1].length() - 1) : tokens[2]) }; }

        if(!tokens[3].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <x> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <y> is not a register"); }

            long x = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long y = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long dest = SAFE(PARSE_TOKEN(tokens[3], lineNum, labels), 4);

            return word | (x << 8) | (y << 4) | dest;
        }
        else
        {
            long x = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 0) ? 4 : 32);
            int xShift = (flag_b == 0) ? 4 : 32;
            long y = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_b == 0) ? 4 : 32);
            long dest = SAFE(PARSE_TOKEN(tokens[3], lineNum, labels), 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (x << xShift) | (y << 4) | dest;
        }
    }

    private static long parseXOR(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {  // Should be the same as parseAND
        if((tokens.length < 3) || (tokens.length > 4))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <x>, <y>{, <dest reg>}"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }

        long flag_a = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[1].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);
        long flag_b = (wordSize == WORD_SIZE_SHORT) ? 0L : (!tokens[2].startsWith(REGISTER_PREFIX + "") ? 1L : 0L);

        if((flag_a == 1) && (flag_b == 1))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": both <x> and <y> are immediates"); }

        if(tokens.length == 3)
        { tokens = new String[] { tokens[0], tokens[1], tokens[2] + ",",
            ((flag_a == 0) ? tokens[1].substring(0, tokens[1].length() - 1) : tokens[2]) }; }

        if(!tokens[3].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }

        if(wordSize == WORD_SIZE_SHORT)
        {
            if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <x> is not a register"); }
            if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": <y> is not a register"); }

            long x = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long y = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
            long dest = SAFE(PARSE_TOKEN(tokens[3], lineNum, labels), 4);

            return word | (x << 8) | (y << 4) | dest;
        }
        else
        {
            long x = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_a == 0) ? 4 : 32);
            int xShift = (flag_b == 0) ? 4 : 32;
            long y = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[1].length() - 1), lineNum, labels), (flag_b == 0) ? 4 : 32);
            long dest = SAFE(PARSE_TOKEN(tokens[3], lineNum, labels), 4);

            return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (x << xShift) | (y << 4) | dest;
        }
    }

    private static long parseNOT(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if((tokens.length < 2) || (tokens.length > 3))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <bits reg>{, <dest reg>}"); }

        if(tokens.length == 2)
        { tokens = new String[] { tokens[0], tokens[1] + ",",
                                    tokens[1]}; }

        if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <bits reg> is not a register"); }
        if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <dest reg> is not a register"); }

        long bits = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
        long dest = SAFE(PARSE_TOKEN(tokens[2].substring(0, tokens[1].length() - 1), lineNum, labels), 4);

        return word | (bits << 4) | dest;
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

    private static long parseSwap(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if(tokens.length != 3)
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + " <src reg>, <dest reg>"); }
        if(!tokens[1].endsWith(","))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": Missing comma separator"); }
        if(!tokens[1].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <reg 1> is not a register"); }
        if(!tokens[2].startsWith(REGISTER_PREFIX + ""))
        { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                      lineNum + ": <reg 2> is not a register"); }

        long src0 = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 4);
        long src1 = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 4);

        if(src0 == src1)
        {
            throw new AssemblyError("Incorrect arguments for instruction \"" + mnemonic + "\" at line " + lineNum +
                                        ": <reg1> and <reg2> are the same");
        }

        return word | (src0 << 4) | src1;
    }

    private static long parseUndo(String mnemonic, long word, String[] tokens, int wordSize, int lineNum, int numStartInstructions, Map<String, Integer> labels) throws AssemblyError
    {
        if((tokens.length < 2) || (tokens.length > 3))
        { throw new AssemblyError("Incorrect number of arguments for instruction \"" + mnemonic +
                                      "\" in line " + lineNum + ". Instruction format is:\n\t" + mnemonic + "<quantity>{, <skip>}"); }

        for(int i = 1; i < tokens.length - 1; i++)
        {
            if(!tokens[i].endsWith(","))
            { throw new AssemblyError("Incorrect argument format for instruction \"" + mnemonic + "\" in line " +
                                          lineNum + ": Missing comma separator"); }
        }

        if(tokens.length == 2)
        {
            tokens = new String[] {tokens[0], tokens[1] + ",", "0"};
        }

        long flag_a = (!tokens[1].startsWith(REGISTER_PREFIX + "")) ? 1L : 0L;
        long flag_b = (!tokens[2].startsWith(REGISTER_PREFIX + "")) ? 0L : 1L;

        long quantity = SAFE(PARSE_TOKEN(tokens[1].substring(0, tokens[1].length() - 1), lineNum, labels), 10);
        long skip = SAFE(PARSE_TOKEN(tokens[2], lineNum, labels), 10);

        return word | (flag_a << (wordSize - 7)) | (flag_b << (wordSize - 8)) | (quantity << 10) | skip;
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
