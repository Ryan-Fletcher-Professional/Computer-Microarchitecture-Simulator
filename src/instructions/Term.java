package instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Term implements Cloneable
{
    public static final boolean NO_PAD = false;
    public static final boolean YES_PAD = true;

    private final int[] bits;

    /**
     * Removes leading 0s.
     * @param term
     */
    public Term(int term)
    {
        this(Integer.toUnsignedString(term, 2));
    }


    /**
     *
     * @param term
     * @param trim Whether to remove leading 0s
     */
    public Term(int term, boolean trim)
    {
        this(Integer.toUnsignedString(term, 2), trim);
    }

    public Term(int term, boolean trim, int sizeToPadTo)
    {
        this("0".repeat(sizeToPadTo - Integer.toUnsignedString(term, 2).length()) + Integer.toUnsignedString(term, 2), false);
    }

    /**
     * Removes leading 0s.
     * @param term
     */
    public Term(long term)
    {
        this(Long.toUnsignedString(term, 2));
    }

    /**
     *
     * @param term
     * @param trim Whether to remove leading 0s
     */
    public Term(long term, boolean trim)
    {
        this(Long.toUnsignedString(term, 2), trim);
    }

    public Term(long term, boolean trim, int sizeToPadTo)
    {
        this("0".repeat(sizeToPadTo - Long.toUnsignedString(term, 2).length()) + Long.toUnsignedString(term, 2), false);
    }

    /**
     * Removes leading 0s.
     * @param term
     */
    public Term(String term)
    {
        this(term.chars().map(c -> (c == '0') ? 0 : 1).toArray());
    }

    /**
     *
     * @param term
     * @param trim Whether to remove leading 0s
     */
    public Term(String term, boolean trim)
    {
        this(term.chars().map(c -> (c == '0') ? 0 : 1).toArray(), trim);
    }

    public Term(String term, boolean trim, int sizeToPadTo)
    {
        this("0".repeat(sizeToPadTo - term.length()) + term, false);
    }

    /**
     * Removes leading 0s.
     * @param bits
     */
    public Term(int[] bits)
    {
        this(bits, true);
    }

    /**
     *
     * @param bits
     * @param trim Whether to remove leading 0s
     */
    public Term(int[] bits, boolean trim)
    {
        if(bits.length < 1) { throw new IllegalArgumentException("Term must have at least one bit"); }
        List<Integer> bitsList = new ArrayList<>();
        boolean hitSigFig = !trim;
        for(int i = 0; i < bits.length; i++)
        {
            if(hitSigFig || (bits[i] == 1))
            {
                hitSigFig = true;
                bitsList.add(bits[i]);
            }
        }
        if(bitsList.isEmpty()) { bitsList.add(0); }
        this.bits = bitsList.stream().mapToInt(i->i).toArray();
    }

    public int length()
    {
        return bits.length;
    }

    @Override
    public String toString()
    {
        return Arrays.stream(bits).mapToObj(String::valueOf).collect(Collectors.joining());
    }

    public int toInt()
    {
        if(bits.length > Integer.SIZE) { throw new UnsupportedOperationException("More than " + Integer.SIZE + " bits"); }
        return Integer.parseUnsignedInt(toString(), 2);
    }

    public long toLong()
    {
        if(bits.length > Long.SIZE) { throw new UnsupportedOperationException("More than " + Long.SIZE + " bits"); }
        return Long.parseUnsignedLong(toString(), 2);
    }

    public int[] getBits()
    {
        return bits.clone();
    }

    private static int[][] FIX_LENGTH(int[] aBits, int[] bBits)
    {
        int[] smaller = (aBits.length < bBits.length) ? aBits : bBits;
        int[] larger = (bBits.length > aBits.length) ? bBits : aBits;
        int[] filler = new int[larger.length - smaller.length];
        int[] filled = new int[filler.length + smaller.length];
        System.arraycopy(filler, 0, filled, 0, filler.length);
        System.arraycopy(smaller, 0, filled, filler.length, smaller.length);
        return new int[][] {filled, larger};
    }

    public Term NOT()
    {
        return NOT(this);
    }

    public Term AND(Term other)
    {
        return AND(this, other);
    }

    public Term OR(Term other)
    {
        return OR(this, other);
    }

    public Term XOR(Term other)
    {
        return XOR(this, other);
    }

    public static Term NOT(Term term)
    {
        int[] ones = new int[term.length()];
        Arrays.fill(ones, 1);
        return XOR(term, new Term(ones, false));
    }

    public static Term AND(Term a, Term b)
    {
        int[][] fixedBits = FIX_LENGTH(a.getBits(), b.getBits());
        int[] aBits = fixedBits[0];
        int[] bBits = fixedBits[1];
        for(int i = 0; i < aBits.length; i++)
        {
            aBits[i] = (aBits[i] + bBits[i]) / 2;
        }
        return new Term(aBits, false);
    }

    public static Term OR(Term a, Term b)
    {
        int[][] fixedBits = FIX_LENGTH(a.getBits(), b.getBits());
        int[] aBits = fixedBits[0];
        int[] bBits = fixedBits[1];
        for(int i = 0; i < aBits.length; i++)
        {
            aBits[i] = Math.min(1, aBits[i] + bBits[i]);
        }
        return new Term(aBits, false);
    }

    public static Term XOR(Term a, Term b)
    {
        int[][] fixedBits = FIX_LENGTH(a.getBits(), b.getBits());
        int[] aBits = fixedBits[0];
        int[] bBits = fixedBits[1];
        for(int i = 0; i < aBits.length; i++)
        {
            aBits[i] = (aBits[i] + bBits[i]) % 2;
        }
        return new Term(aBits, false);
    }

    @Override
    public Term clone()
    {
        return new Term(getBits(), false);
    }
}
