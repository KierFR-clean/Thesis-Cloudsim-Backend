package CloudSimTestRR;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class SimulationLogger {
    private ByteArrayOutputStream logStream;
    private PrintStream originalOut;

    public void startLogging() {
        logStream = new ByteArrayOutputStream();
        PrintStream logPrintStream = new PrintStream(logStream);
        originalOut = System.out;
        System.setOut(logPrintStream); // Redirect System.out to the log stream
    }

    public void stopLogging() {
        System.setOut(originalOut); // Restore the original System.out
    }

    public String getLogs() {
        return logStream.toString(); // Retrieve the captured logs
    }
}