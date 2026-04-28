package poct.device.app.serial.v2.common;

public interface SocketMessageCallbackV2<T extends SocketMessageV2> {
    void beforeTry(SocketMessageSenderV2<T> sender);

    void afterTry(SocketMessageSenderV2<T> sender);

    void delay(T feedback, SocketMessageSenderV2<T> sender);

    void success(T feedback, SocketMessageSenderV2<T> sender);

    void error(SocketMessageSenderV2<T> sender, Exception ex);

    void complete(SocketMessageSenderV2<T> sender);
}
