package poct.device.app.utils.common;

import org.joor.Reflect;

public class ReflectUtils {
    public ReflectUtils() {
    }

    public static <E> E get(Object obj, String field) {
        if (null == obj) {
            throw new NullPointerException();
        } else if (StringUtils.isEmpty(field)) {
            throw new RuntimeException("Empty field");
        } else {
            return Reflect.on(obj).get(field);
        }
    }

    public static void set(Object obj, String field, Object value) {
        if (null == obj) {
            throw new NullPointerException();
        } else if (StringUtils.isEmpty(field)) {
            throw new RuntimeException("Empty field");
        } else {
            Reflect.on(obj).set(field, value);
        }
    }

    public static <E> E newObject(Class<E> clazz, Object... args) {
        return Reflect.on(clazz).create(args).get();
    }
}