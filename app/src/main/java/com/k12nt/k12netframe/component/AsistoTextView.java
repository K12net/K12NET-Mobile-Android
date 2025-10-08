package com.k12nt.k12netframe.component;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class AsistoTextView extends TextView {

	public AsistoTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	    init();

	}
	
	public AsistoTextView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init();
	}


	public AsistoTextView(Context context) {
	    super(context);
	    init();
	}


	public AsistoTextView(Context context, int defStyle) {
		super(context, null, defStyle);
	    init();
	}

	private void init() {
		if(isInEditMode() == false) {
			setTypeface(Typeface.DEFAULT);
		}
	}

}
