package cn.garymb.ygomobile.fragment.setting;

import cn.garymb.ygomobile.widget.BaseDialog;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.MenuItem;

public abstract class EventDialogPreferenceFragment extends PreferenceFragment
		implements OnDismissListener, Handler.Callback {
	/**
	 * @author mabin
	 * 
	 */
	public static class DataHandler extends Handler {
		/**
		 * 
		 */
		public DataHandler(Looper looper, Callback callback) {
			super(looper, callback);
		}
	}

	private BaseDialog mDialog;

	private boolean mShowsDialog = false;

	private int mDialogType = INVALID_DIALOG_TYPE;

	protected DataHandler mHandler;

	public static final int INVALID_DIALOG_TYPE = 0xffff;

	private static final String SAVED_DIALOG_TYPE = "dialog_type";
	private static final String SAVED_DIALOG_STATE = "dialog_state";
	private static final String SAVED_SHOWS_DIALOG = "dialog_showsdialog";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mDialogType = savedInstanceState.getInt(SAVED_DIALOG_TYPE,
					INVALID_DIALOG_TYPE);
			mShowsDialog = savedInstanceState.getBoolean(SAVED_SHOWS_DIALOG,
					false);
		}
		setHasOptionsMenu(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		 switch (item.getItemId()) {
	        // Respond to the action bar's Up/Home button
	        case android.R.id.home:
	            getFragmentManager().popBackStack(null, 0);
	            return true;
	    }
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mHandler = new DataHandler(activity.getMainLooper(), this);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (mDialog != null && mShowsDialog) {
			mDialog.show();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mDialog != null) {
			mDialog.hide();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.i("wtf", "onSaveInstanceState");
		if (mDialog != null) {
			Log.i("wtf", "onSaveInstanceState save dialog");
			Bundle dialogState = mDialog.onSaveInstanceState();
			if (dialogState != null) {
				outState.putBundle(SAVED_DIALOG_STATE, dialogState);
			}
			outState.putBoolean(SAVED_SHOWS_DIALOG, mShowsDialog);
			outState.putInt(SAVED_DIALOG_TYPE, mDialogType);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (!mShowsDialog) {
			return;
		}
		mDialog = onCreateDialog(mDialogType, null);	
		mDialog.setOnDismissListener(this);
		Log.i("wtf", "onActivityCreated, shows dialog");
		if (savedInstanceState != null) {
			Bundle dialogState = savedInstanceState
					.getBundle(SAVED_DIALOG_STATE);
			if (dialogState != null) {
				mDialog.onRestoreInstanceState(dialogState);
			}
		}
	}

	public abstract BaseDialog onCreateDialog(int type, Bundle param);

	protected BaseDialog getDialog() {
		return mDialog;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		mShowsDialog = false;
	}

	protected void showDialog(int type, Bundle param) {
		mDialog = onCreateDialog(type, param);
		mDialog.setOnDismissListener(this);
		mDialog.show();
		mShowsDialog = true;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog.setOnDismissListener(null);
			mDialog = null;
			mShowsDialog = false;
		}
	}

	protected void dismissDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog.setOnDismissListener(null);
			mShowsDialog = false;
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		if (isDetached()) {
			return true;
		}
		return false;
	}

}
