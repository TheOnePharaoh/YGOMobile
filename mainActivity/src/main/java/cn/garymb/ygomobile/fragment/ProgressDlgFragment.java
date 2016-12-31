package cn.garymb.ygomobile.fragment;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import com.avast.android.dialogs.core.BaseDialogFragment;
import com.avast.android.dialogs.iface.INegativeButtonDialogListener;
import com.avast.android.dialogs.iface.IPositiveButtonDialogListener;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.garymb.ygomobile.R;
import cn.garymb.ygomobile.controller.Controller;


public class ProgressDlgFragment extends CustomDialogFragment implements
		Observer, OnClickListener {

	private static final String TAG = "ImageDLStatusDlgFragment";

	private ImageView mDLStopButton;

	private TextView mProgressPercentView;

	private TextView mProgressView;

	private ProgressBar mProgressBar;

	private long mTotalCount;
	private long mCurrentCount;

	public static ProgressDlgFragment newInstance(Bundle bundle,
			int requestCode) {
		ProgressDlgFragment f = new ProgressDlgFragment();
		f.setArguments(bundle);
		f.mRequestCode = requestCode;
		return f;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dlg = super.onCreateDialog(savedInstanceState);
		dlg.setCancelable(false);
		return dlg;
	}

	@Override
	public void onPause() {
		super.onPause();
		Controller.peekInstance().unregisterForImageDownload(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		Controller.peekInstance().registerForImageDownload(this);
	}

	@SuppressLint("InflateParams")
	@Override
	public BaseDialogFragment.Builder build(BaseDialogFragment.Builder builder) {
		View view = LayoutInflater.from(getActivity()).inflate(
				R.layout.image_dl_dialog, null);
		mDLStopButton = (ImageView) view
				.findViewById(R.id.download_stop_button);
		mProgressPercentView = (TextView) view
				.findViewById(R.id.dl_progress_percent_text);
		mProgressView = (TextView) view.findViewById(R.id.dl_progress_text);
		mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar2);
		mDLStopButton.setOnClickListener(this);

		builder.setTitle(R.string.image_download_label);
		builder.setView(view);
		builder.setPositiveButton(R.string.button_hide,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						List<IPositiveButtonDialogListener> listeners = getPositiveButtonDialogListeners();
						if (listeners != null) {
							for (IPositiveButtonDialogListener listener: listeners) {
								listener.onPositiveButtonClicked(mRequestCode);
							}
						}
						dismiss();
					}
				});
		builder.setNegativeButton(R.string.button_stop,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						List<INegativeButtonDialogListener> listeners = getNegativeButtonDialogListeners();
						if (listeners != null) {
							for (INegativeButtonDialogListener listener: listeners) {
								listener.onNegativeButtonClicked(mRequestCode);
							}
						}
						dismiss();
					}
				});
		return builder;
	}

	@Override
	public void update(Observable observable, Object data) {
		if (data instanceof Message) {
			if (isAdded()) {
				Message msg = (Message) data;
				mTotalCount = msg.arg2;
				mCurrentCount = msg.arg1;
				if (mTotalCount == mCurrentCount) {
					dismissAllowingStateLoss();
					return;
				}
				setProgress();
			}
		}
	}

	private void setProgress() {
		float progress = (float) ((mCurrentCount * 100.0f) / (mTotalCount * 1.0));
		mProgressBar.setProgress((int) progress);
		mProgressPercentView.setText(String.format("%2.1f%%", progress));
		mProgressView.setText(getResources().getString(
				R.string.image_count_progress, mCurrentCount, mTotalCount));
	}

	@Override
	public void onSaveInstanceState(Bundle arg0) {
		Log.i(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(arg0);
		arg0.putLong("total_count", mTotalCount);
		arg0.putLong("current_count", mCurrentCount);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			Log.i(TAG, "load from saved instance");
			mTotalCount = savedInstanceState.getInt("total_count", 0);
			mCurrentCount = savedInstanceState.getInt("current_count", 0);
		}
	}

	@Override
	public void onClick(View v) {
		List<INegativeButtonDialogListener> listeners = getNegativeButtonDialogListeners();
		if (listeners != null) {
			for (INegativeButtonDialogListener listener: listeners) {
				listener.onNegativeButtonClicked(0);
			}
		}
		dismiss();
	}
}
