package poct.device.app.pdf;

import java.util.List;
import java.util.Map;

/**
 * PDF模板填充数据接口
 *
 */
public interface PdfTemplateData {

    /**
     * 获取关键字和值的映射表
     *
     */
    Map<String, String> findKeywordMap();

    /**
     * 封装为关键字，格式：${属性}
     *
     */
    List<String> findKeywords();

    /**
     * 获取需要填充的属性
     *
     */
    List<String> findFields();
}
