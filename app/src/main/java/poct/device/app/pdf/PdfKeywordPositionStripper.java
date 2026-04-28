package poct.device.app.pdf;



import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;
import com.tom_roush.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


class PdfKeywordPositionStripper extends PDFTextStripper {

    /**
     * 查找的关键字集合
     */
    private final List<String> keywordList;
    /**
     * 查找成功的关键字实体对象集合
     */
    private final List<PdfKeyword> pdfKeywords = new ArrayList<>();

    public List<PdfKeyword> getPdfKeywords() {
        return pdfKeywords;
    }

    public PdfKeywordPositionStripper(List<String> keywordList) throws IOException {
        this.keywordList = keywordList;
    }

    @Override
    protected void writeString(String text, List<TextPosition> positions) {
        int size = positions.size();
        int num = 1;
        for (String keyWord : keywordList) {
            char[] chars = keyWord.toCharArray();
            for (int i = 0; i < size; i++) {
                // 获取当前读取的字符
                String currentChar = positions.get(i).getUnicode();
                // 当前字符 和 keyWord 关键字进行匹配
                if (!Objects.equals(currentChar, String.valueOf(chars[0]))) {
                    continue;
                }
                int count = 1;
                int j;
                for (j = 1; j < chars.length && i + j < size; j++) {
                    currentChar = positions.get(i + j).getUnicode();
                    if (!Objects.equals(currentChar, String.valueOf(chars[j]))) {
                        break;
                    }
                    count++;
                }
                // 匹配成功,记录文本的坐标位置
                if (!Objects.equals(count, chars.length)) {
                    continue;
                }
                TextPosition startPosition = positions.get(i);
                TextPosition endPosition = positions.get(i + j < size ? i + j : i + j - 1);
                // 创建实体对象
                PdfKeyword entity = new PdfKeyword();
                entity.setKeyWord(keyWord);
                // 获取起始字符坐标
                Matrix matrix = startPosition.getTextMatrix();
                float x = matrix.getTranslateX();
                float y = matrix.getTranslateY();
                // 获取结束字符坐标
                Matrix endMatrix = endPosition.getTextMatrix();
                float x2 = endMatrix.getTranslateX();
                // 获取字体大小
                float fontSizeInPt = startPosition.getFontSizeInPt();
                entity.setX(x);
                entity.setY(y - fontSizeInPt / 5);
                float width = i + j < size ? x2 - x : x2 - x + fontSizeInPt;
                entity.setWidth(width);
                entity.setHeight(fontSizeInPt);
                entity.setSerialNo(num++);
                entity.setPageNo(getCurrentPageNo());
                pdfKeywords.add(entity);
            }
        }
    }
}