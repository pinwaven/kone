package poct.device.app.utils.common;

import java.nio.charset.StandardCharsets;

public class StringUtils extends org.apache.commons.lang.StringUtils {
    private static final String CHARSET_NAME = "UTF-8";
    private static final String[] EMPTY_ARRAY = new String[0];

    public StringUtils() {
    }

    public static String firstUpperCase(String target) {
        StringBuilder result = new StringBuilder();
        char[] chars = target.toCharArray();

        for (int i = 0; i < chars.length; ++i) {
            if (i == 0) {
                result.append(Character.toUpperCase(chars[i]));
            } else {
                result.append(chars[i]);
            }
        }

        return result.toString();
    }

    public static String firstLowerCase(String target) {
        StringBuilder result = new StringBuilder();
        char[] chars = target.toCharArray();

        for (int i = 0; i < chars.length; ++i) {
            if (i == 0) {
                result.append(Character.toLowerCase(chars[i]));
            } else {
                result.append(chars[i]);
            }
        }

        return result.toString();
    }

    public static boolean isEmptyOrWhitespace(String target) {
        if (target == null) {
            return true;
        } else {
            int targetLen = target.length();
            if (targetLen == 0) {
                return true;
            } else {
                char c0 = target.charAt(0);
                if ((c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z')) {
                    for (int i = 0; i < targetLen; ++i) {
                        char c = target.charAt(i);
                        if (c != ' ' && !Character.isWhitespace(c)) {
                            return false;
                        }
                    }

                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static String camelCaseToUnderscore(String camelCaseName) {
        StringBuilder result = new StringBuilder();
        if (camelCaseName != null && !camelCaseName.isEmpty()) {
            result.append(camelCaseName.substring(0, 1).toLowerCase());

            for (int i = 1; i < camelCaseName.length(); ++i) {
                char ch = camelCaseName.charAt(i);
                if (Character.isUpperCase(ch)) {
                    result.append("_");
                    result.append(Character.toLowerCase(ch));
                } else {
                    result.append(ch);
                }
            }
        }

        return result.toString();
    }

    public static String underscoreToCamelCase(String underscoreName) {
        StringBuilder result = new StringBuilder();
        if (underscoreName != null && !underscoreName.isEmpty()) {
            boolean flag = false;

            for (int i = 0; i < underscoreName.length(); ++i) {
                char ch = underscoreName.charAt(i);
                if ('_' == ch) {
                    flag = true;
                } else if (flag) {
                    result.append(Character.toUpperCase(ch));
                    flag = false;
                } else {
                    result.append(ch);
                }
            }
        }

        return result.toString();
    }

    public static byte[] getBytes(String str) {
        if (str != null) {
            return str.getBytes(StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    public static String replaceAll(String hexString, String s, String s1) {
        return isEmpty(hexString) ? hexString : hexString.replaceAll(s, s1);
    }
}
