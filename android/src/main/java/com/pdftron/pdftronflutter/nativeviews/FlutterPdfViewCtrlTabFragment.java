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
        if (mViewerComponent == null) {
            return;
        }

        EventChannel.EventSink eventSink = mViewerComponent.getDocumentSizeChangedEventEmitter();
        if (eventSink == null) {
            return;
        }

        PDFViewCtrl pdfViewCtrl = getPDFViewCtrl();
        if (pdfViewCtrl == null) {
            return;
        }

        try {
            pdfViewCtrl.docLock(true);
            Page page = pdfViewCtrl.getDoc().getPage(pageNumber);
            Rect pageRect = page.getCropBox();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("width", pageRect.getWidth());
            jsonObject.put("height", pageRect.getHeight());

            eventSink.success(jsonObject.toString());
        } catch (Exception e) {
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
