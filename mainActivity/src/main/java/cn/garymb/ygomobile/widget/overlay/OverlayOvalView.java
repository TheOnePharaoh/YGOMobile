/*
 Copyright 2011 jawsware international

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package cn.garymb.ygomobile.widget.overlay;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import cn.garymb.ygomobile.R;
import cn.garymb.ygomobile.common.Constants;

import com.jawsware.core.share.OverlayView;

public class OverlayOvalView extends OverlayView implements android.view.View.OnClickListener {

	/**
	 * @author mabin
	 * 
	 */
	public interface OnDuelOptionsSelectListener {
		void onDuelOptionsSelected(int mode, boolean action);
	}

	private TextView mInfo;

	private OnDuelOptionsSelectListener mListener;

	public OverlayOvalView(Context context) {
		super(context, R.layout.overlay_oval);
	}

	@Override
	protected void onInflateView() {
		mInfo = (TextView) this.findViewById(R.id.textview_info);
		mInfo.setOnClickListener(this);
	}

	public void setDuelOpsListener(OnDuelOptionsSelectListener listener) {
		mListener = listener;
	}

	@Override
	public void onClick(View v) {
		mListener.onDuelOptionsSelected(Constants.MODE_CANCEL_CHAIN_OPTIONS, true);
	}
}
