package poct.device.app.utils.common;

import java.io.PrintWriter;
import java.io.StringWriter;

import poct.device.app.utils.exception.UncheckedException;

public class ExceptionUtils {
    public ExceptionUtils() {
    }

    public static String getProtectedMessage(Throwable t, int length) {
        String message = t.getMessage();
        if (StringUtils.isEmpty(message)) {
            return "NPE";
        } else {
            String expKey = "Exception: ";
            int realMessageIndex = message.lastIndexOf(expKey);
            if (realMessageIndex >= 0) {
                message = message.substring(realMessageIndex + expKey.length());
            }

            return message.length() < length ? message : message.substring(0, length - 4) + " ...";
        }
    }

    public static String getStackTraceAsString(Throwable t) {
        if (t == null) {
            return "";
        } else {
            StringWriter stringWriter = new StringWriter();
            t.printStackTrace(new PrintWriter(stringWriter));
            return stringWriter.toString();
        }
    }

    public static boolean isCausedBy(Throwable ex, Class<? extends Throwable> causeExceptionClasses) {
        return null != getCausedException(ex, causeExceptionClasses);
    }

    public static <E extends Throwable> E getCausedException(Throwable ex, Class<E> causeExceptionClasses) {
        if (causeExceptionClasses.equals(ex.getClass())) {
            return (E) ex;
        } else {
            for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
                if (causeExceptionClasses.isInstance(cause)) {
                    return (E) cause;
                }
            }

            return null;
        }
    }

    public static RuntimeException unchecked(Exception e) {
        return e instanceof RuntimeException ? (RuntimeException) e : new UncheckedException(e);
    }
}
