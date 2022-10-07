package com.k12nt.k12netframe.utils.helper;

import android.content.Context;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

import androidx.annotation.NonNull;

public class AtlasWebView extends WebView {
    public AtlasWebView(@NonNull Context context) {
        super(context);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        // This line fixes some issues but introduces others, YMMV.
        // super.onCreateInputConnection(outAttrs);

        return new BaseInputConnection(this, false);
    }
}
