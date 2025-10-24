package com.pdftron.pdftronflutter.nativeviews;

import static com.pdftron.pdftronflutter.helpers.PluginUtils.REFLOW_ORIENTATION_HORIZONTAL;
import static com.pdftron.pdftronflutter.helpers.PluginUtils.REFLOW_ORIENTATION_VERTICAL;

import androidx.annotation.NonNull;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment2;
import com.pdftron.pdftronflutter.helpers.ViewerComponent;

import org.json.JSONException;
import org.json.JSONObject;

import io.flutter.plugin.common.EventChannel;

public class FlutterPdfViewCtrlTabFragment extends PdfViewCtrlTabFragment2 {

    private ViewerComponent mViewerComponent;
    private double mLastZoom = -1;

    public void setViewerComponent(@NonNull ViewerComponent component) {
        mViewerComponent = component;
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(REFLOW_ORIENTATION_HORIZONTAL,  l);
            jsonObject.put(REFLOW_ORIENTATION_VERTICAL, t);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mViewerComponent != null) {
            EventChannel.EventSink eventSink = mViewerComponent.getScrollChangedEventEmitter();
            if (eventSink != null) {
                eventSink.success(jsonObject.toString());
            }
        }

        // Check for zoom change
        checkZoomChanged();
    }

    private void checkZoomChanged() {
        PDFViewCtrl pdfViewCtrl = getPDFViewCtrl();
        if (pdfViewCtrl != null && mViewerComponent != null) {
            double currentZoom = pdfViewCtrl.getZoom();
            if (mLastZoom != currentZoom) {
                mLastZoom = currentZoom;
                sendZoomChanged(currentZoom);
            }
        }
    }

    private void sendZoomChanged(double zoom) {
        EventChannel.EventSink eventSink = mViewerComponent.getZoomChangedEventEmitter();
        if (eventSink == null) {
            return;
        }

        PDFViewCtrl pdfViewCtrl = getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }

        try {
            // Get the scrollable content dimensions (canvas size)
            int canvasWidth = pdfViewCtrl.getCanvasWidth();
            int canvasHeight = pdfViewCtrl.getCanvasHeight();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("zoom", zoom);
            jsonObject.put("width", canvasWidth);
            jsonObject.put("height", canvasHeight);

            eventSink.success(jsonObject.toString());
        } catch (Exception e) {
            // Fallback to old format
            try {
                eventSink.success(zoom);
            } catch (Exception ignored) {
            }
        } finally {
            try {
                if (pdfViewCtrl != null) {
                    pdfViewCtrl.docUnlock();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onPageChange(int old_page, int cur_page, PDFViewCtrl.PageChangeState state) {
        super.onPageChange(old_page, cur_page, state);
        sendDocumentSize(cur_page);
    }

    @Override
    public void onDocumentLoaded() {
        super.onDocumentLoaded();
        PDFViewCtrl pdfViewCtrl = getPDFViewCtrl();
        if (pdfViewCtrl != null) {
            sendDocumentSize(pdfViewCtrl.getCurrentPage());
        }
    }

    private void sendDocumentSize(int pageNumber) {
        android.util.Log.d("FlutterPdfFragment", "sendDocumentSize called for page: " + pageNumber);

        if (mViewerComponent == null) {
            android.util.Log.e("FlutterPdfFragment", "mViewerComponent is null");
            return;
        }

        EventChannel.EventSink eventSink = mViewerComponent.getDocumentSizeChangedEventEmitter();
        if (eventSink == null) {
            android.util.Log.e("FlutterPdfFragment", "eventSink is null - listener not registered");
            return;
        }

        PDFViewCtrl pdfViewCtrl = getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            android.util.Log.e("FlutterPdfFragment", "pdfViewCtrl is null");
            return;
        }

        try {
            pdfViewCtrl.docLock(true);
            Page page = pdfViewCtrl.getDoc().getPage(pageNumber);
            Rect pageRect = page.getCropBox();

            double width = pageRect.getWidth();
            double height = pageRect.getHeight();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("width", width);
            jsonObject.put("height", height);

            android.util.Log.d("FlutterPdfFragment", "Sending document size: " + width + " x " + height);
            eventSink.success(jsonObject.toString());
            android.util.Log.d("FlutterPdfFragment", "Document size sent successfully");
        } catch (Exception e) {
            android.util.Log.e("FlutterPdfFragment", "Error sending document size", e);
            e.printStackTrace();
        } finally {
            try {
                if (pdfViewCtrl != null) {
                    pdfViewCtrl.docUnlock();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
