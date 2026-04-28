package poct.device.app.utils.common;

public class CrcUtils {
    private static final int CRC16_POLYNOMIAL = 4129;

    private static final int[] CRC16_TABLE = initTable16();

    public CrcUtils() {
    }

    public static byte[] crc8(byte[] data) {
        // CRC-8 参数配置
        final int poly = 0x07;     // 多项式 (移除了最高位的 1)
        final int init = 0x00;      // 初始值
        final int xorout = 0x00;    // 结果异或值
        final boolean refin = true; // 输入反转
        final boolean refout = false; // 输出反转

        int crc = init; // 初始化 CRC 寄存器

        for (byte b : data) {
            int currentByte = b & 0xFF; // 转换为无符号整数 (0-255)

            // 如果启用输入反转，反转字节的比特位
            if (refin) {
                currentByte = reverseBits(currentByte, 8);
            }

            crc ^= currentByte; // 与当前字节异或

            // 处理每个字节的 8 位
            for (int i = 0; i < 8; i++) {
                // 检查最高位 (MSB)
                boolean msbSet = (crc & 0x80) != 0;

                // 左移一位并确保 8 位长度
                crc = (crc << 1) & 0xFF;

                // 如果最高位为 1，则异或多项式
                if (msbSet) {
                    crc ^= poly;
                }
            }
        }

        // 如果启用输出反转，反转最终结果的比特位
        if (refout) {
            crc = reverseBits(crc, 8);
        }

        // 应用结果异或值并返回最终结果
        crc = (crc ^ xorout) & 0xFF;

        // 返回长度为 1 的 byte 数组
        return new byte[]{(byte) crc};
    }

    public static byte[] crc16(byte[] bytes) {
        int crc = 0;

        for (byte aByte : bytes) {
            crc = crc << 8 ^ CRC16_TABLE[(crc >> 8 ^ 255 & aByte) & 255];
        }

        return ByteUtils.fromShort((short) (crc & '\uffff'));
    }

    private static int[] initTable16() {
        int[] table = new int[256];
        int temp;
        int a;

        for (int i = 0; i < table.length; ++i) {
            temp = 0;
            a = i << 8;

            for (int j = 0; j < 8; ++j) {
                if (((temp ^ a) & '耀') != 0) {
                    temp = temp << 1 ^ 4129;
                } else {
                    temp <<= 1;
                }

                a <<= 1;
            }

            table[i] = temp;
        }

        return table;
    }

    // 反转指定位数的比特顺序
    private static int reverseBits(int value, int bitLength) {
        int reversed = 0;
        for (int i = 0; i < bitLength; i++) {
            reversed = (reversed << 1) | (value & 1);
            value >>= 1;
        }
        return reversed;
    }
}
