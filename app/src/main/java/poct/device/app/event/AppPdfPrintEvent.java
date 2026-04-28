package poct.device.app.event;

public class AppPdfPrintEvent {
    private final String pdfPath;

    public AppPdfPrintEvent(String pdfPath) {
        this.pdfPath = pdfPath;
    }


    public String getPdfPath() {
        return pdfPath;
    }
}
