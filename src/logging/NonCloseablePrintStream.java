package logging;

import java.io.PrintStream;

public class NonCloseablePrintStream extends PrintStream {

    public NonCloseablePrintStream(PrintStream stream) {
        super(stream);
    }

    @Override
    public void close() {
        // this stream cannot be closed
    }

}
