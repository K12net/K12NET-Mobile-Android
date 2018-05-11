package com.k12nt.k12netframe;


import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


public abstract class K12NetDailogView extends Dialog {

    LayoutInflater inflater = null;
    LinearLayout mainDialogLayout = null;
    View ownDialogView = null;

    public K12NetDailogView(Context context) {
		super(context, R.style.Asisto_PopupDialog);
	}
	
	public void createContextView(Object objView){
		
		inflater = LayoutInflater.from(getContext());
        ownDialogView = inflater.inflate(R.layout.k12net_dialog_layout, null, false);
        mainDialogLayout = (LinearLayout) ownDialogView.findViewById(R.id.lyt_activity);
		
		TextView txt_title1 = (TextView)ownDialogView.findViewById(R.id.txt_toolbar_title);
		//TextView toolbarSubtitle = (TextView)ownDialogView.findViewById(R.id.txt_toolbar_subtitle);
		txt_title1.setText(getToolbarTitle());
       /* if(toolbarSubtitle != null){
            toolbarSubtitle.setText(getToolbarSubtitle());
        }*/

		/*ImageView icon = (ImageView)ownDialogView.findViewById(R.id.img_toolbar);
        if(icon != null) {
            icon.setImageResource(getToolbarIcon());
        }*/
		
		View dialogView = getDialogView(objView);

		mainDialogLayout.addView(dialogView);
		
		View back_button = (View) ownDialogView.findViewById(R.id.lyt_back);
		back_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 onBackPressed();
			}
		});
		
		setContentView(ownDialogView);
		
	}

	public abstract View getDialogView(Object objView);
	
	protected CharSequence getToolbarSubtitle() {
       /* String toolbarSubtitle = MainActivity.getSelectedPersonelInfo().getName().getFullName();
        if(MainActivity.getSelectedEnrollment() != null) {
           toolbarSubtitle += " - " + MainActivity.getSelectedEnrollment().getSchoolYearID();
        }*/
        return "";
	}

	protected abstract int getToolbarIcon();

	protected abstract int getToolbarTitle();
}
