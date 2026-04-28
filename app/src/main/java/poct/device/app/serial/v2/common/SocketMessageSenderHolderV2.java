package poct.device.app.serial.v2.common;

import org.apache.commons.collections4.QueueUtils;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import timber.log.Timber;

public class SocketMessageSenderHolderV2<T extends SocketMessageV2> {
    private final BlockingQueue<SocketMessageSenderV2<T>> senderQ = new LinkedBlockingQueue<>();

    public Queue<SocketMessageSenderV2<T>> getSenderQ() {
        return QueueUtils.unmodifiableQueue(this.senderQ);
    }

    public SocketMessageSenderHolderV2() {
    }

    public int size() {
        return this.senderQ.size();
    }

    public void add(SocketMessageSenderV2<T> sender) {
        try {
            this.senderQ.put(sender);
        } catch (InterruptedException e) {
            Timber.w("sender busy");
        }
    }

    public SocketMessageSenderV2<T> getSender() {
        return this.senderQ.poll();
    }

    public void resolve(SocketMessageSenderV2<T> sender) {
        if (sender != null) {
            sender.setExpiredTime(LocalDateTime.now());
        }
    }
}
