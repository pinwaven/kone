package poct.device.app.pdf;

import org.apache.commons.lang.ArrayUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import info.szyh.common4.lang.ExceptionUtils;
import info.szyh.common4.reflect.ReflectUtils;

/**
 */
public abstract class AbstractPdfTemplateData implements PdfTemplateData {
    /**
     * 获取关键字和值的映射表
     */
    @Override
    public Map<String, String> findKeywordMap() {
        Map<String, String> describe = describe(this);
        Set<Map.Entry<String, String>> entries = describe.entrySet();
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : entries) {
            result.put(wrapFieldAsKeyword(entry.getKey()), entry.getValue());
        }
        return result;
    }

    /**
     * 封装为关键字，格式：${属性}
     */
    @Override
    public List<String> findKeywords() {
        return findFields().stream().map(this::wrapFieldAsKeyword).collect(Collectors.toList());
    }

    private String wrapFieldAsKeyword(String field) {
        return String.format("${%s}", field);
    }

    /**
     * 获取需要填充的属性
     */
    @Override
    public List<String> findFields() {
        return getFieldNames(getClass());
    }

    /**
     * 把属性和值作为字符串列出来
     */
    public static Map<String, String> describe(Object obj) {
        try {
            Map<String, String> result = new HashMap<>();
            List<String> fieldNames = getFieldNames(obj.getClass());
            for (String fieldName : fieldNames) {
                result.put(fieldName, ReflectUtils.get(obj, fieldName));
            }
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.unchecked(e);

        }
    }

    /**
     * 获取定义的字段列表，不论该属性有没有getter方法，不包括静态字段
     */
    public static List<String> getFieldNames(Class clazz) {
        return getFieldNames(clazz, false);
    }

    /**
     * @param includeStatic 是否包含静态字段
     */
    public static List<String> getFieldNames(Class clazz, boolean includeStatic) {
        Field[] declaredFields = clazz.getDeclaredFields();
        if (ArrayUtils.isEmpty(declaredFields)) {
            return Collections.emptyList();
        }

        return Arrays.asList(declaredFields).stream()
                .filter(declaredField -> {
                    if (Modifier.isStatic(declaredField.getModifiers())) {
                        return includeStatic;
                    } else {
                        return true;
                    }
                }).map(Field::getName).collect(Collectors.toList());
    }
}
