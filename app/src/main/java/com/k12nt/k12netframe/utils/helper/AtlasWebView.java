package com.k12nt.k12netframe.utils.helper;

import android.content.Context;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;

public class AtlasWebView extends WebView {
    public AtlasWebView(@NonNull Context context) {
        super(context);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        try {
            String language = K12NetUserReferences.getLanguageCode();
            if (language != null && language.split("-")[0].equals("ar")) {
                return super.onCreateInputConnection(outAttrs);
            }
            return new BaseInputConnection(this, false); //this is needed for #dispatchKeyEvent() to be notified.
        } catch(Exception e) {
            e.printStackTrace();
        }
        return super.onCreateInputConnection(outAttrs);
    }

    /*@Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        try {
            outAttrs.actionLabel = null;
            outAttrs.inputType = InputType.TYPE_NULL;
            final InputConnection con = new BaseInputConnection(this,false);
            InputConnectionWrapper public_con = new InputConnectionWrapper(
                    super.onCreateInputConnection(outAttrs), true) {
                @Override
                public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                    if (beforeLength == 1 && afterLength == 0) {
                        return this.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                                && this.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
                    }
                    return super.deleteSurroundingText(beforeLength, afterLength);
                }

                @Override
                public boolean sendKeyEvent(KeyEvent event) {
                    if(event.getKeyCode() == KeyEvent.KEYCODE_DEL){
                        return con.sendKeyEvent(event);
                    }else {
                        return super.sendKeyEvent(event);
                    }
                }
            };

            return public_con ;
        }catch (Exception e){
            return super.onCreateInputConnection(outAttrs) ;
        }
    }*/
}
