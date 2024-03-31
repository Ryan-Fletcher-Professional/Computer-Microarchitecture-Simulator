package instructions;

import java.util.Map;

public class Instruction
{
    private final Term word;
    private Map<String, Term> auxBits;

    public Instruction(int word)
    {
        this(new Term(word, false));
    }

    public Instruction(long word)
    {
        this(new Term(word, false));
    }

    public Instruction(String word)
    {
        this(new Term(word, false));
    }

    public Instruction(Term word)
    {
        int size = word.length();
        if((size != 32) && (size != 64)) {throw new IllegalArgumentException("Instruction word must be 32 or 64 bits long"); }
        this.word = word.clone();
    }

    public int wordLength()
    {
        return word.length();
    }

    public long wordNum()
    {
        return word.toLong();
    }

    public void addAuxBits(String identifier, int term)
    {
        addAuxBits(identifier, new Term(term));
    }

    public void addAuxBits(String identifier, long term)
    {
        addAuxBits(identifier, new Term(term));
    }

    public void addAuxBits(String identifier, int[] bits)
    {
        addAuxBits(identifier, new Term(bits));
    }

    public void addAuxBits(String identifier, String term)
    {
        addAuxBits(identifier, new Term(term));
    }

    public void addAuxBits(String identifier, Term term)
    {
        if(auxBits.get(identifier) != null) { throw new IllegalArgumentException("Identifier already in use"); }
        auxBits.put(identifier, term);
    }

    // TODO : Add methods to get ISA-specific info about instruction
}
