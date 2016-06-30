package com.exiu.camerademo;

import android.app.Activity;
import android.widget.Toast;

public class MyToast {

	/**
	 * 方法：主线程和子线程弹吐�?
	 * @param act
	 * @param msg
	 */
	public static void showToast(final Activity act,final String msg){
		
		if("main".equals(Thread.currentThread().getName())){
			// 主线程弹吐司
			Toast.makeText(act, msg, 0).show();
		}else{
			// 子线程弹吐司
			act.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(act, msg, 0).show();
				}
			});
		}
	}
	
}