package poct.device.app.utils.common;

import android.annotation.SuppressLint;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class ByteUtils {
    private static final byte[] EMPTY = new byte[0];

    public ByteUtils() {
    }

    @SuppressLint({"DefaultLocale"})
    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < (long) unit) {
            return bytes + " B";
        } else {
            int exp = (int) (Math.log((double) bytes) / Math.log(unit));
            String pre = String.valueOf("KMGTPE".charAt(exp - 1));
            return String.format("%.1f %sB", (double) bytes / Math.pow(unit, exp), pre);
        }
    }

    public static int toPositiveIntFromByte(byte b) {
        return (b + 256) % 256;
    }

    public static byte toHexByteFromPositiveInt(int n) {
        return (byte) n;
    }

    public static byte[] fromShort(short s) {
        return fromShort(s, false);
    }

    public static byte[] fromShort(short s, boolean leFlag) {
        byte[] buf = new byte[2];
        if (leFlag) {
            for (int i = 0; i < buf.length; ++i) {
                buf[i] = (byte) (s & 255);
                s = (short) (s >> 8);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; --i) {
                buf[i] = (byte) (s & 255);
                s = (short) (s >> 8);
            }
        }

        return buf;
    }

    public static short toShort(byte[] bytes) {
        return toShort(bytes, false);
    }

    public static short toShort(byte[] bytes, boolean leFlag) {
        if (bytes == null) {
            throw new IllegalArgumentException("byte array is null!");
        } else if (bytes.length > 2) {
            throw new IllegalArgumentException("byte array size > 2 !");
        } else {
            short r = 0;
            if (leFlag) {
                for (int i = bytes.length - 1; i >= 0; --i) {
                    r = (short) (r << 8);
                    r = (short) (r | bytes[i] & 255);
                }
            } else {
                for (byte aByte : bytes) {
                    r = (short) (r << 8);
                    r = (short) (r | aByte & 255);
                }
            }

            return r;
        }
    }

    public static byte[] fromInt(int s) {
        return fromInt(s, false);
    }

    public static byte[] fromInt(int s, boolean leFlag) {
        byte[] buf = new byte[4];
        if (leFlag) {
            for (int i = 0; i < buf.length; ++i) {
                buf[i] = (byte) (s & 255);
                s >>= 8;
            }
        } else {
            for (int i = buf.length - 1; i >= 0; --i) {
                buf[i] = (byte) (s & 255);
                s >>= 8;
            }
        }

        return buf;
    }

    public static int toInt(byte[] buf) {
        return toInt(buf, false);
    }

    public static int toInt(byte[] buf, boolean leFlag) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        } else if (buf.length > 4) {
            throw new IllegalArgumentException("byte array size > 4 !");
        } else {
            int r = 0;
            if (leFlag) {
                for (int i = buf.length - 1; i >= 0; --i) {
                    r <<= 8;
                    r |= buf[i] & 255;
                }
            } else {
                for (byte b : buf) {
                    r <<= 8;
                    r |= b & 255;
                }
            }

            return r;
        }
    }

    public static byte[] fromLong(long s) {
        return fromLong(s, false);
    }

    public static byte[] fromLong(long s, boolean leFlag) {
        byte[] buf = new byte[8];
        if (leFlag) {
            for (int i = 0; i < buf.length; ++i) {
                buf[i] = (byte) ((int) (s & 255L));
                s >>= 8;
            }
        } else {
            for (int i = buf.length - 1; i >= 0; --i) {
                buf[i] = (byte) ((int) (s & 255L));
                s >>= 8;
            }
        }

        return buf;
    }

    public static long toLong(byte[] buf) {
        return toLong(buf, false);
    }

    public static long toLong(byte[] buf, boolean leFlag) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        } else if (buf.length > 8) {
            throw new IllegalArgumentException("byte array size > 8 !");
        } else {
            long r = 0L;
            if (leFlag) {
                for (int i = buf.length - 1; i >= 0; --i) {
                    r <<= 8;
                    r |= (buf[i] & 255);
                }
            } else {
                for (byte b : buf) {
                    r <<= 8;
                    r |= (b & 255);
                }
            }

            return r;
        }
    }

    public static String toBinaryString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        for (byte item : bytes) {
            String string = Integer.toBinaryString(Integer.parseInt(toHex(item), 16));
            builder.append(StringUtils.leftPad(string, 8, '0'));
        }

        return builder.toString();
    }

    public static String toHexStringFromBinary(String binaryString) {
        int mod = binaryString.length() % 8;
        int targetLength = binaryString.length();
        if (mod > 0) {
            targetLength += 8 - mod;
        }

        String binary = StringUtils.leftPad(binaryString, targetLength, '0');
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < binary.length() - 4 + 1; i += 4) {
            String byteStr = binary.substring(i, i + 4);
            builder.append(Integer.toHexString(Byte.parseByte(byteStr, 2)));
        }

        return builder.toString().toUpperCase();
    }

    public static byte[] toBytesFromBinary(String binaryString) {
        return fromHexString(toHexStringFromBinary(binaryString));
    }

    public static byte[] subBytes(byte[] srcBytes, int startIndex, int length) {
        byte[] result = new byte[length];
        int index = 0;
        int endIndex = startIndex + length;

        for (int i = startIndex; i < endIndex; ++i) {
            result[index++] = srcBytes[i];
        }

        return result;
    }

    public static int fromBCDBytes(byte[] bytes) {
        int n = 0;
        int length = bytes.length;

        for (int i = 0; i < length; ++i) {
            n = (int) ((double) n + (double) fromBCD(bytes[i]) * Math.pow(100.0F, (length - 1 - i)));
        }

        return n;
    }

    public static byte[] toBCDBytes(int n) {
        if (0 == n) {
            return EMPTY;
        } else {
            List<Integer> ints;
            for (ints = new ArrayList<>(); n != 0; n /= 100) {
                ints.add(0, n % 100);
            }

            byte[] bytes = new byte[ints.size()];

            for (int i = 0; i < bytes.length; ++i) {
                bytes[i] = toBCD(ints.get(i));
            }

            return bytes;
        }
    }

    public static byte[] toBCDBytes(int n, int targetLength) {
        if (0 == n) {
            return new byte[targetLength];
        } else {
            List<Integer> ints;
            for (ints = new ArrayList<>(); n != 0; n /= 100) {
                ints.add(0, n % 100);
            }

            int size = ints.size();
            if (size > targetLength) {
                throw new RuntimeException("the target length is not enough for " + size);
            } else {
                byte[] bytes = new byte[targetLength];
                int offset = targetLength - size;

                for (int i = offset; i < targetLength; ++i) {
                    bytes[i] = toBCD(ints.get(i - offset));
                }

                return bytes;
            }
        }
    }

    public static byte toBCD(int n) {
        Validate.isTrue(0 <= n && n < 100, "int to bcd should between 0(include) ~ 100(exclude)");
        int high = n / 10;
        int low = n % 10;
        return (byte) ((high << 4) + low);
    }

    public static int fromBCD(byte aByte) {
        int high = (aByte & 240) >> 4;
        int low = aByte & 15;
        if (high < 10 && low < 10) {
            return high * 10 + low;
        } else {
            throw new RuntimeException("[" + toHex(aByte) + "]不是有效的BCD码");
        }
    }

    public static int indexOfBytes(byte[] srcBytes, byte[] bytes) {
        return indexOfBytes(srcBytes, bytes, 0);
    }

    public static int indexOfBytes(byte[] srcBytes, byte[] bytes, int startIndex) {
        int length = srcBytes.length - bytes.length + 1;

        for (int i = startIndex; i < length; ++i) {
            boolean notMatch = false;

            for (int j = 0; j < bytes.length; ++j) {
                if (srcBytes[i + j] != bytes[j]) {
                    notMatch = true;
                    break;
                }
            }

            if (!notMatch) {
                return i;
            }
        }

        return -1;
    }

    public static int indexOfBytes(ByteBuf byteBuf, byte[] bytes) {
        return indexOfBytes(byteBuf, bytes, 0);
    }

    public static int indexOfBytes(ByteBuf byteBuf, byte[] bytes, int startIndex) {
        int readerStartIndex = byteBuf.readerIndex();
        int readerBytes = byteBuf.readableBytes();
        int lastIndex = readerBytes + readerStartIndex - bytes.length;
        int realStartIndex = Math.max(startIndex, readerStartIndex);

        for (int i = realStartIndex; i <= lastIndex; ++i) {
            boolean matched = true;

            for (int j = 0; j < bytes.length; ++j) {
                if (byteBuf.getByte(i + j) != bytes[j]) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                return i;
            }
        }

        return -1;
    }

    public static byte[] convert(int... ints) {
        if (ArrayUtils.isEmpty(ints)) {
            return EMPTY;
        } else {
            byte[] bytes = new byte[ints.length];

            for (int i = 0; i < bytes.length; ++i) {
                bytes[i] = (byte) ints[i];
            }

            return bytes;
        }
    }

    public static boolean equals(byte[] data1, byte[] data2) {
        if (!ArrayUtils.isEmpty(data1) && !ArrayUtils.isEmpty(data2) && data1.length == data2.length) {
            for (int i = 0; i < data1.length; ++i) {
                if (data1[i] != data2[i]) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static byte[] fromHexString(String hexString) {
        String hexForDecode = hexString;
        if (hexString.indexOf(32) >= 0) {
            hexForDecode = StringUtils.replaceAll(hexString, " ", "");
        }

        if (hexForDecode.length() % 2 == 1) {
            hexForDecode = "0" + hexForDecode;
        }

        try {
            return Hex.decodeHex(hexForDecode.toCharArray());
        } catch (DecoderException e) {
            throw ExceptionUtils.unchecked(e);
        }
    }

    public static String toPrettyHex(byte[] bytes) {
        char[] hexChars = Hex.encodeHex(bytes);
        StringBuilder builder = new StringBuilder();
        String sep = "";

        for (int i = 0; i < hexChars.length; ++i) {
            char it = hexChars[i];
            if (i % 2 == 0) {
                builder.append(sep);
                sep = " ";
            }

            builder.append(Character.toUpperCase(it));
        }

        return builder.toString();
    }

    public static String toPrettyHexWithoutSep(byte[] bytes) {
        char[] hexChars = Hex.encodeHex(bytes);
        StringBuilder builder = new StringBuilder();

        for (char it : hexChars) {
            builder.append(Character.toUpperCase(it));
        }

        return builder.toString();
    }

    public static String toPrettyHex(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.markReaderIndex();
        byteBuf.readBytes(bytes);
        byteBuf.resetReaderIndex();
        return toPrettyHex(bytes);
    }

    public static String toPrettyHexWithoutSep(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.markReaderIndex();
        byteBuf.readBytes(bytes);
        byteBuf.resetReaderIndex();
        return toPrettyHexWithoutSep(bytes);
    }

    public static String dumpPrettyHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

        String var3;
        try {
            ByteBufUtil.appendPrettyHexDump(builder, Unpooled.wrappedBuffer(bytes));
            var3 = builder.toString();
        } finally {
            byteBuf.release();
        }

        return var3;
    }

    public static String dumpPrettyHex(ByteBuf byteBuf) {
        StringBuilder builder = new StringBuilder();
        ByteBufUtil.appendPrettyHexDump(builder, byteBuf);
        return builder.toString();
    }

    public static String toHex(byte[] bytes) {
        return toHex(bytes, true);
    }

    public static String toHex(byte aByte) {
        return toHex(new byte[]{aByte});
    }

    public static String toHex(byte[] bytes, boolean toUpperCase) {
        return toUpperCase ? (new String(Hex.encodeHex(bytes))).toUpperCase() : new String(Hex.encodeHex(bytes));
    }

    public static String toHex(byte aByte, boolean toUpperCase) {
        return toHex(new byte[]{aByte}, toUpperCase);
    }
}
