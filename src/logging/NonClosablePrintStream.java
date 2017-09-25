package logging;

import java.io.PrintStream;

public class NonClosablePrintStream extends PrintStream {

    public NonClosablePrintStream(PrintStream stream) {
        super(stream);
    }

    @Override
    public void close() {
        // this stream cannot be closed
    }

}
