package poct.device.app.utils.exception;

public class SocketMaxTriedException extends RuntimeException {
    public static final SocketMaxTriedException INSTANCE = new SocketMaxTriedException();

    public SocketMaxTriedException() {
    }
}