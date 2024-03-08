package memory;

public class MemoryRequestTimerNotStartedException extends Exception
{
    public MemoryRequestTimerNotStartedException()
    {
        super("MemoryRequest timer not started. Call memoryRequest.start(<int>)");
    }

    public MemoryRequestTimerNotStartedException(String errorMessage)
    {
        super(errorMessage);
    }
}
