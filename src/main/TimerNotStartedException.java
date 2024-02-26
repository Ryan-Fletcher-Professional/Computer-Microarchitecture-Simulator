package main;

public class TimerNotStartedException extends Exception
{
    public TimerNotStartedException()
    {
        super("MemoryRequest timer not started. Call memoryRequest.start(<int>)");
    }

    public TimerNotStartedException(String errorMessage)
    {
        super(errorMessage);
    }
}
