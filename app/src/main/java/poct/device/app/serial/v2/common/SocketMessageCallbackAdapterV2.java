package poct.device.app.serial.v2.common;

public abstract class SocketMessageCallbackAdapterV2<T extends SocketMessageV2> implements SocketMessageCallbackV2<T> {
    public SocketMessageCallbackAdapterV2() {
    }

    public void beforeTry(SocketMessageSenderV2<T> sender) {
    }

    public void afterTry(SocketMessageSenderV2<T> sender) {
    }

    public void delay(T feedback, SocketMessageSenderV2<T> sender) {
    }

    public void success(T feedback, SocketMessageSenderV2<T> sender) {
    }

    public void error(SocketMessageSenderV2<T> sender, Exception e) {
    }

    public void complete(SocketMessageSenderV2<T> sender) {
    }
}