package poct.device.app.serial.v2.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import timber.log.Timber;

public class SocketMessageSenderV2<T extends SocketMessageV2> {
    private static final String TAG = "IOTPlugin-" + SocketMessageSenderV2.class.getSimpleName();

    private SocketMessageSenderV2.SenderConfig config;

    private String devUid;

    private T socketMessage;

    private SocketMessageCallbackV2<T> callback;

    private LocalDateTime latestTime;

    private LocalDateTime sentTime;

    private LocalDateTime expiredTime;

    private int tried;

    private List<LocalDateTime> times;

    public SocketMessageSenderV2(String devUid, T sockMsg) {
        this(devUid, sockMsg, null, SocketMessageSenderV2.SenderConfig.DEFAULT);
    }

    public SocketMessageSenderV2(String devUid, T sockMsg, SocketMessageCallbackV2<T> callback) {
        this(devUid, sockMsg, callback, SocketMessageSenderV2.SenderConfig.DEFAULT);
    }

    public SocketMessageSenderV2(String devUid, T sockMsg, SocketMessageSenderV2.SenderConfig config) {
        this(devUid, sockMsg, null, config);
    }

    public SocketMessageSenderV2(String devUid, T sockMsg, SocketMessageCallbackV2<T> callback, SocketMessageSenderV2.SenderConfig config) {
        this.devUid = devUid;
        this.socketMessage = sockMsg;
        this.callback = callback;
        this.sentTime = LocalDateTime.now();
        this.times = new ArrayList<>(10);
        this.config = config;
    }

    private void doSend(SocketContextHolderV2 socketContextHolder) {
        ChannelHandlerContext context = socketContextHolder.getContext(this.devUid);
        if (context == null) {
            Timber.tag(TAG).w("The device [" + this.devUid + "] is not online");
        } else {
            this.doSend(context);
        }
    }

    public void trySend(ChannelHandlerContext ctx) {
        this.trySend(ctx, null);
    }

    public void trySend(ChannelHandlerContext ctx, SocketMessageSenderHolderV2 senderHolder) {
        this.triggerBeforeTry();
        this.latestTime = LocalDateTime.now();
        this.times.add(this.latestTime);
        ++this.tried;
        if (senderHolder != null) {
            senderHolder.add(this);
        }

        this.setExpiredTime(this.latestTime.plusSeconds(this.config.getTriedInterval()));
        this.doSend(ctx);
        this.triggerAfterTry();
    }

    public void trySend(SocketContextHolderV2 ctxHolder, SocketMessageSenderHolderV2 senderHolder) {
        this.triggerBeforeTry();
        this.latestTime = LocalDateTime.now();
        this.times.add(this.latestTime);
        ++this.tried;
        if (senderHolder != null) {
            senderHolder.add(this);
        }

        this.setExpiredTime(this.latestTime.plusSeconds(this.config.getTriedInterval()));
        this.doSend(ctxHolder);
        this.triggerAfterTry();
    }

    private void doSend(ChannelHandlerContext ctx) {
        try {
            for (int i = 0; i < this.config.getSendOnce(); ++i) {
                if (i > 0) {
                    Thread.sleep(this.config.getSendOnceDelay());
                }

                ctx.writeAndFlush(this.socketMessage);
            }
        } catch (Exception e) {
            Timber.tag(TAG).w(e);
            this.triggerError(e);
        }
    }

    public void triggerBeforeTry() {
        if (this.callback != null) {
            try {
                this.callback.beforeTry(this);
            } catch (Exception e) {
                this.triggerError(e);
            }
        }
    }

    public void triggerAfterTry() {
        if (this.callback != null) {
            try {
                this.callback.afterTry(this);
            } catch (Exception e) {
                this.triggerError(e);
            }
        }
    }

    public void triggerSuccess(T result) {
        if (this.callback != null) {
            try {
                this.callback.success(result, this);
            } catch (Exception e) {
                this.triggerError(e);
            }
        }
    }

    public void triggerComplete() {
        if (this.callback != null) {
            try {
                this.callback.complete(this);
            } catch (Exception e) {
                this.triggerError(e);
            }
        }
    }

    public void triggerError(Exception e) {
        if (this.callback != null) {
            try {
                this.callback.error(this, e);
                this.triggerComplete();
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    public void triggerDelay(T result, SocketMessageSenderV2<T> sender) {
        if (this.callback != null) {
            try {
                this.callback.delay(result, sender);
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    public SocketMessageSenderV2.SenderConfig getConfig() {
        return this.config;
    }

    public String getDevUid() {
        return this.devUid;
    }

    public T getSocketMessage() {
        return this.socketMessage;
    }

    public SocketMessageCallbackV2<T> getCallback() {
        return this.callback;
    }

    public LocalDateTime getLatestTime() {
        return this.latestTime;
    }

    public LocalDateTime getSentTime() {
        return this.sentTime;
    }

    public LocalDateTime getExpiredTime() {
        return this.expiredTime;
    }

    public int getTried() {
        return this.tried;
    }

    public List<LocalDateTime> getTimes() {
        return this.times;
    }

    public void setConfig(SocketMessageSenderV2.SenderConfig config) {
        this.config = config;
    }

    public void setDevUid(String devUid) {
        this.devUid = devUid;
    }

    public void setSocketMessage(T socketMessage) {
        this.socketMessage = socketMessage;
    }

    public void setCallback(SocketMessageCallbackV2<T> callback) {
        this.callback = callback;
    }

    public void setLatestTime(LocalDateTime latestTime) {
        this.latestTime = latestTime;
    }

    public void setSentTime(LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }

    public void setExpiredTime(LocalDateTime expiredTime) {
        this.expiredTime = expiredTime;
    }

    public void setTried(int tried) {
        this.tried = tried;
    }

    public void setTimes(List<LocalDateTime> times) {
        this.times = times;
    }

    public static class SenderConfig {
        public static final SocketMessageSenderV2.SenderConfig DEFAULT = new SocketMessageSenderV2.SenderConfig();

        private int sendOnce = 1;

        private int sendOnceDelay = 20;

        private int triedInterval = 3;

        public SenderConfig() {
        }

        public SenderConfig(int sendOnce, int sendOnceDelay, int triedInterval) {
            this.sendOnce = sendOnce;
            this.sendOnceDelay = sendOnceDelay;
            this.triedInterval = triedInterval;
        }

        public int getSendOnce() {
            return this.sendOnce;
        }

        public void setSendOnce(int sendOnce) {
            this.sendOnce = sendOnce;
        }

        public int getSendOnceDelay() {
            return this.sendOnceDelay;
        }

        public void setSendOnceDelay(int sendOnceDelay) {
            this.sendOnceDelay = sendOnceDelay;
        }

        public int getTriedInterval() {
            return this.triedInterval;
        }

        public void setTriedInterval(int triedInterval) {
            this.triedInterval = triedInterval;
        }
    }
}
