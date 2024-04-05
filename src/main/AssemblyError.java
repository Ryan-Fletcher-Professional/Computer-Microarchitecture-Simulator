package main;

public class AssemblyError extends Exception
{
    public AssemblyError() { super(); }

    public AssemblyError(String message)
    {
        super(message);
    }
}
