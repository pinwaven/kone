package poct.device.app.pdf;

import java.io.Serializable;

/**
 */
public class PdfKeyword implements Serializable {
    /**
     * 关键词
     */
    private String keyWord;
    /**
     * 序号，从1开始
     */
    private int serialNo;
    /**
     * 所在页，从1开始
     */
    private int pageNo;
    /**
     * pdf页上x坐标
     */
    private float x;
    /**
     * pdf页上y坐标
     */
    private float y;
    /**
     * 关键词的长度
     */
    private float width;
    /**
     * 关键词的高度
     */
    private float height;

    public String getKeyWord() {
        return keyWord;
    }

    public void setKeyWord(String keyWord) {
        this.keyWord = keyWord;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}