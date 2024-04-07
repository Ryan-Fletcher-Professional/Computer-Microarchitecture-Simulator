package memory;

import java.util.logging.Logger;

import static main.GLOBALS.*;

public class RegisterFileModule
{
    private static final Logger logger = Logger.getLogger(RegisterFileModule.class.getName());

    public static final String INDEXABLE_PREFIX = "R";
    public static final String INTERNAL_PREFIX = "N";
    public static final String CALL_PREFIX = "C";
    public static final String REVERSAL_PREFIX = "U";

    private long[] memory;
    private final long[] masks;
    private final int totalLength;
    public final String[] names;
    private final int id;
    private int currentRegisterIndex = -1;
    private final REGISTER_FILE_MODE mode;
    public boolean[] pendings;

    public RegisterFileModule(int id, REGISTER_FILE_MODE mode, int[] registerLengths, String[] names)
    {
        this.id = id;
        memory = new long[registerLengths.length];
        masks = new long[registerLengths.length];
        int len = 0;
        for(int i = 0; i < registerLengths.length; i++)
        {
            masks[i] = (long)(Math.pow(2, registerLengths[i]) - 1);
            len += registerLengths[i];
        }
        totalLength = len;
        this.names = names.clone();
        this.mode = mode;
    }

    public int getID()
    {
        return id;
    }

    public int getNumRegisters()
    {
        return memory.length;
    }

    public void store(int index, long value)
    {
        if(mode.equals(REGISTER_FILE_MODE.ADDRESSED))
        {
            if(index < 0 || index >= getNumRegisters())
            {
                throw new IllegalArgumentException("Register address out of range.");
            }
            memory[index] = value & masks[index];
        }
        else if(mode.equals(REGISTER_FILE_MODE.STACK))
        {
            if(index != -1) { WARN(logger, "You are attempting to address a stack register file"); }
            if(currentRegisterIndex == getNumRegisters() - 1)
                { throw new StackOverflowError("Register file " + id + " overflowed"); }
            currentRegisterIndex += 1;
            memory[currentRegisterIndex] = value & masks[currentRegisterIndex];
        }
        else if(mode.equals(REGISTER_FILE_MODE.STACK_CIRCULAR))
        {
            if(index != -1) { WARN(logger, "You are attempting to address a stack register file"); }
            currentRegisterIndex = (currentRegisterIndex + 1) % getNumRegisters();
            memory[currentRegisterIndex] = value & masks[currentRegisterIndex];
        }
    }

    public long load()
    {
        return load(-1);
    }

    public long load(int index)
    {
        if(mode.equals(REGISTER_FILE_MODE.ADDRESSED))
        {
            if(index < 0 || index >= getNumRegisters())
            {
                System.out.println("Bad index: " + index);
                throw new IllegalArgumentException("Register address out of range.");
            }
            return memory[index];
        }
        else if(mode.equals(REGISTER_FILE_MODE.STACK))
        {
            if(index != -1) { WARN(logger, "You are attempting to address a stack register file"); }
            if(currentRegisterIndex == -1)
                { throw new RuntimeException("STACK UNDERFLOW: Register file " + id); }
            long value = memory[currentRegisterIndex];
            currentRegisterIndex -= 1;
            return value;
        }
        else if(mode.equals(REGISTER_FILE_MODE.STACK_CIRCULAR))
        {
            if(index != -1) { WARN(logger, "You are attempting to address a stack register file"); }
            long value = memory[currentRegisterIndex];
            currentRegisterIndex = (currentRegisterIndex + getNumRegisters() - 1) % getNumRegisters();
            return value;
        }
        else { throw new RuntimeException("Register file mode not assigned; should never occur"); }
    }

    public long peek(int index)
    {
        if(mode.equals(REGISTER_FILE_MODE.ADDRESSED))
        {
            return memory[index];
        }
        if(mode.equals(REGISTER_FILE_MODE.STACK))
        {
            return memory[currentRegisterIndex - index];
        }
        if(mode.equals(REGISTER_FILE_MODE.STACK_CIRCULAR))
        {
            return memory[(currentRegisterIndex + getNumRegisters() - index) % getNumRegisters()];
        }
        else { throw new RuntimeException("Register file mode not assigned; should never occur"); }
    }

    public String getDisplayText()
    {
        return getDisplayText(16);
    }

    public String getDisplayText(int radix)
    {
        return getDisplayText(mode.equals(REGISTER_FILE_MODE.ADDRESSED) ? 8 : 1, radix);
    }

    public String getDisplayText(int maxRowSize, int radix)
    {
        StringBuilder ret = new StringBuilder();
        ret.append("  ");
        int[] valueLengths = new int[names.length];
        for(int i = 0; i < names.length; i++)
        {
            valueLengths[i] = Math.max(names[i].length() + (PENDING_INDICATOR.length() * 2), SMART_TO_STRING(masks[i], radix).length());
            StringBuilder currentName = new StringBuilder();
            String pendingIndicator = pendings[i] ? PENDING_INDICATOR : "";
            currentName.append(" ".repeat((valueLengths[i] - names[i].length()) / 2))
                       .append(pendingIndicator).append(names[i]).append(pendingIndicator)
                       .append(" ".repeat(valueLengths[i] - currentName.length()));
            ret.append(currentName)
               .append("  |  ");
        }
        ret.setCharAt(ret.length() - "|  ".length(), '\n');
        for(int i = 0; i < getNumRegisters(); i++)
        {
            String value = (Long.toBinaryString(masks[i]).length() == Integer.SIZE) ? SMART_INT_TO_STRING((int)(memory[i] & masks[i]), radix) : SMART_TO_STRING(memory[i] & masks[i], radix);
            StringBuilder currentValue = new StringBuilder();
            currentValue.append(((radix == 10)? " " : "0").repeat(valueLengths[i] - value.length()))
                        .append(value)
                        .append("  |  ");
            ret.append(currentValue);
        }
        ret.delete(ret.length() - "|  ".length(), ret.length());

        if(names.length > maxRowSize)
        {
            String[] rows = ret.toString().split("\\n");
            ret = new StringBuilder();
            String[] nameUnits = rows[0].split("\\|");
            String[] valueUnits = rows[1].split("\\|");
            int lengthBeforeBefore = 0;
            int lengthBefore = 0;
            for(int i = 0; i < names.length / maxRowSize; i++)
            {
                for(int j = 0; j < maxRowSize; j++)
                {
                    ret.append(nameUnits[(i * maxRowSize) + j])
                       .append("|");
                }
                ret.setCharAt(ret.length() - 1, '\n');
                for(int j = 0; j < maxRowSize; j++)
                {
                    ret.append(valueUnits[(i * maxRowSize) + j])
                       .append("|");
                }
                ret.setCharAt(ret.length() - 1, '\n');
                lengthBeforeBefore = ret.length();
                ret.append("-".repeat((ret.length() - lengthBefore) / 2));
                ret.append("\n");
                lengthBefore = ret.length();
            }
            ret.delete(lengthBeforeBefore - 1, ret.length());
            if(maxRowSize == 1) { ret.append('\n'); }
        }

        return ret.toString().toUpperCase();
    }

    public void reset()
    {
        memory = new long[getNumRegisters()];
    }
}
