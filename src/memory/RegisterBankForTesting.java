package memory;

public class RegisterBankForTesting
{
    private long[] memory;
    private final long[] masks;
    private final int totalLength;
    private final String[] names;
    private final int id;

    public RegisterBankForTesting(int id, int[] registerLengths)
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
        names = new String[registerLengths.length];
        for(int i = 0; i < registerLengths.length; i++)
        {
            names[i] = "R" + i;
        }
    }

    public RegisterBankForTesting(int id, int[] registerLengths, String[] names)
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
        memory[index] = value & masks[index];
    }

    public long load(int index)
    {
        return memory[index];
    }

    public String getDisplayText()
    {
        return getDisplayText(8);
    }

    public String getDisplayText(int maxRowSize)
    {
        StringBuilder ret = new StringBuilder();
        ret.append("  ");
        for(int i = 0; i < names.length; i++)
        {
            int valueLength = Math.max(names[i].length(), Long.toBinaryString(masks[i]).length());
            StringBuilder currentName = new StringBuilder();
            int startPos = (valueLength + 1) / 2;
            currentName.append(" ".repeat((valueLength - names[i].length()) / 2))
                       .append(names[i])
                       .append(" ".repeat(valueLength - currentName.length()));
            ret.append(currentName)
               .append("  |  ");
        }
        ret.setCharAt(ret.length() - "|  ".length(), '\n');
        for(int i = 0; i < memory.length; i++)
        {
            int valueLength = Long.toBinaryString(masks[i]).length();
            String binaryValue = Long.toBinaryString(memory[i] & masks[i]);
            StringBuilder currentValue = new StringBuilder();
            currentValue.append("0".repeat(valueLength - binaryValue.length()))
                        .append(binaryValue)
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
        }

        return ret.toString();
    }

    public void reset()
    {
        memory = new long[memory.length];
    }
}
