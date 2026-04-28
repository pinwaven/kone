package poct.device.app.pdf;

import android.content.Context;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import info.szyh.common4.lang.StringUtils;
import poct.device.app.App;
import timber.log.Timber;

/**
 * @since 2024-09-02
 */
public class PdfUtils {
    /**
     * 默认使用宋体，支持中文，但不支持换行、制表符等转义字符
     *
     */
    public static void replaceText(PdfTemplateData data, File destFile) throws IOException {
        Map<String, String> keywordMap = data.findKeywordMap();

        // 1.加载文档
        try (PDDocument document = PDDocument.load(App.Companion.getContext().getAssets().open("pdfTemplate/card_temp.pdf"))) {
            Timber.w("加载文档");
            // 2.提取文字
            List<String> keys = data.findKeywords();
            PdfKeywordPositionStripper stripper = new PdfKeywordPositionStripper(keys);
            stripper.setSortByPosition(true);
            stripper.getText(document);
            Timber.w("提取文字");
            // 3.提取结果
            List<PdfKeyword> pdfKeywords = stripper.getPdfKeywords();
            Timber.w("提取结果");
            // 4.加载字体
            PDFont font = loadFont(document, App.Companion.getContext());
            Timber.w("加载字体");
            float fontSize = 12;
            // 5.替换
            int numberOfPages = document.getNumberOfPages();
            for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++) {
                Timber.w("得到页面");
                //1、得到页面
                try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(pageIndex), PDPageContentStream.AppendMode.APPEND, true)) {
                    //2、循环替换指定关键字文本内容
                    Timber.w("循环替换指定关键字文本内容");
                    for (PdfKeyword keyWordEntity : pdfKeywords) {
                        if (keyWordEntity.getPageNo() != (pageIndex + 1)) {
                            continue;
                        }
                        // 设置画笔颜色
                        stream.setNonStrokingColor(255, 255, 255);

                        // 划定覆盖区域, 修正起始点和宽高（额外往外扩10%）
                        float dw = keyWordEntity.getWidth() * 0.05f;
                        float dh = keyWordEntity.getHeight() * 0.1f;
                        stream.addRect(keyWordEntity.getX() - dw, keyWordEntity.getY() - dh, keyWordEntity.getWidth() + dw * 2, keyWordEntity.getHeight() + dh * 2);
                        stream.fill();
                        // 替换关键字文本内容
                        String text = keywordMap.get(keyWordEntity.getKeyWord());
                        if (StringUtils.isEmpty(text)) {
                            text = "-";
                        }
                        stream.setNonStrokingColor(0,0,0);
                        stream.beginText();
                        stream.setFont(font, fontSize);
                        stream.newLineAtOffset(keyWordEntity.getX(), keyWordEntity.getY() + dh);
                        stream.showText(text);
                        stream.endText();
                    }
                }
            }
            Timber.w("保存文件");
            // 6.保存文件
            document.save(destFile);
        }
    }

    /**
     * 加载默认宋体
     *
     */
    public static PDFont loadFont(PDDocument document, Context context) throws IOException {
        try (InputStream inputStream = context.getAssets().open("font/simsun.ttf")) {
            return PDType0Font.load(document, inputStream);
        }
    }
}
