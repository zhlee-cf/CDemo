package com.exiu.camerademo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

	private SurfaceView sv;
	private boolean isPreviewing;
	private Camera mCamera;
	// private int mCurrentCameraIndex;
	private Button btn_preview, btn_takepicture, btn_savepicture;
	private Bitmap bitmap;
	private String imagePath;
	private MainActivity act;
	private SurfaceHolder surfaceHolder;
	private PowerManager.WakeLock wakeLock;
	private PowerManager powerManager;
    private AudioManager manager; //声音管理
    private int volumn; //声音值

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initCameraFirst();
		
		// 设置去掉title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// 设置填充整个屏幕
		int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		Window myWindow = this.getWindow();
		myWindow.setFlags(flag, flag);

		setContentView(R.layout.activity_main);
		act = this;
		// 让屏幕保持唤醒状态
		acquireWakeLock();

		sv = (SurfaceView) findViewById(R.id.sv);
		sv.setZOrderOnTop(true);
		// 通过surfaceView获取holder
		surfaceHolder = sv.getHolder();
		surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

		btn_preview = (Button) findViewById(R.id.btn_preview);
		btn_takepicture = (Button) findViewById(R.id.btn_takepicture);
		btn_savepicture = (Button) findViewById(R.id.btn_savepicture);

		btn_preview.setOnClickListener(this);
		btn_takepicture.setOnClickListener(this);
		btn_savepicture.setOnClickListener(this);
		// 给holder添加回调
		surfaceHolder.addCallback(new Callback() {

			@Override
			/**
			 * surfaceView创建时执行此方法
			 */
			public void surfaceCreated(SurfaceHolder holder) {
				if (mCamera == null) {
					// 开启摄像头
					mCamera = openCamera();
					isPreviewing = true;
				}
			}

			@Override
			/**
			 * surfaceView内容改变时执行此方法
			 */
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				try {
					mCamera.setPreviewDisplay(holder);
					mCamera.startPreview();
					isPreviewing = true;
					// setCameraDisplayOrientation(MainActivity.this,
					// mCurrentCameraIndex, mCamera);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			/**
			 * surfaceView销毁时执行此方法
			 */
			public void surfaceDestroyed(SurfaceHolder holder) {
				mCamera.stopPreview();
				isPreviewing = false;
				mCamera.release();
				mCamera = null;
			}
		});

		// mAutoFocusCallback = new AutoFocusCallback() {
		//
		// @Override
		// public void onAutoFocus(boolean success, Camera camera) {
		// if (success) {
		// mCamera.setOneShotPreviewCallback(null);
		// }
		// }
		// };
	}

	private void initCameraFirst() {
		manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        manager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        volumn = manager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        manager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        if (volumn != 0) {
            // 如果需要静音并且当前未静音（muteMode的设置可以放在Preference中）
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
	}

	/**
	 * 这俩不着啥时候回调的 现在都可以写为null
	 */
	private ShutterCallback shutterCallback = new ShutterCallback() {

		@Override
		public void onShutter() {

		}
	};
	/**
	 * 这俩不着啥时候回调的 现在都可以写为null
	 */
	private PictureCallback rawPictureCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

		}
	};
	/**
	 * 拍照时会回调到此方法
	 */
	private PictureCallback jpegPictureCallback = new PictureCallback() {

		@Override
		/**
		 * data就是图片的字节数组
		 */
		public void onPictureTaken(byte[] data, Camera camera) {
			 // 重置声音
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, volumn,
                    AudioManager.FLAG_ALLOW_RINGER_MODES);
			// 拍完之后图片是显示不动的，但是这时并没有保存
			// 点击拍照后 把图片存成bitmap 保存的时候用
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			// 拍照时给图片指定个路径 其实就是路径加文件名
			imagePath = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DCIM).toString()
					+ File.separator
					+ "PicTest_"
					+ System.currentTimeMillis()
					+ ".jpg";
		}
	};

	/**
	 * 方法 打开摄像头 不是系统相机
	 * 
	 * @return
	 */
	private Camera openCamera() {
		// 手机上的摄像头的个数
		int cameraCount = 0;
		Camera camera = null;
		// 创建摄像头信息对象 这个对象目前是没内容的 在这句填充内容Camera.getCameraInfo(cameraIndex,
		// cameraInfo);
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		// 获取总共有多少个摄像头
		cameraCount = Camera.getNumberOfCameras();
		for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
			// 获取摄像头的信息 C语言模式
			Camera.getCameraInfo(cameraIndex, cameraInfo);
			// 如果是后摄像头则打开摄像头
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				// 打开指定的摄像头
				camera = Camera.open(cameraIndex);
				// 为摄像头设置参数 参数配置完以后记得set回去 不然无效
				Parameters params = camera.getParameters();
				// 设置自动对焦 要先判断下是否支持自动对焦
				if (params.getSupportedFocusModes().contains(
						Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
					params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				}
				camera.cancelAutoFocus(); // 这句必须加上 不然无效
				// params.setPreviewFormat(ImageFormat.JPEG);
				params.setPictureFormat(PixelFormat.JPEG);
				// 设置闪光灯自动 要加权限  android:name="android.permission.FLASHLIGHT"
				params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				// 这儿填的是手机分辨率 不能随意填
				// 分辨率要设置 不设置的话拍的照片很模糊
//				params.setPictureSize(1280, 720);
				params.setPictureSize(4208, 2368);
				// 设置屏幕旋转
				params.set("rotation", 90);
				
				camera.setParameters(params);
				// 设置摄像头显示的方向 这个设置为零 方向是歪的 不知道为嘛 这个根据上面那个params.set("rotation",
				// 90);要一起设置
				// 这两句如果都不设置的话，保存的图片是横向的，这两句设置后，保存的都是纵向的
				camera.setDisplayOrientation(90);
				// mCurrentCameraIndex = cameraIndex;
			}
		}

		return camera;
	}

	/**
	 * 根据横竖屏自动调节preview方向 这方法 暂时没用了
	 * 
	 * @param activity
	 * @param cameraId
	 * @param camera
	 */
	// private void setCameraDisplayOrientation(Activity activity, int cameraId,
	// Camera camera) {
	// // C语言模式获取camerainfo
	// Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	// Camera.getCameraInfo(cameraId, cameraInfo);
	//
	// int rotation = activity.getWindowManager().getDefaultDisplay()
	// .getRotation();
	// int degrees = 0;
	// switch (rotation) {
	// case Surface.ROTATION_0:
	// degrees = 0;
	// break;
	// case Surface.ROTATION_90:
	// degrees = 90;
	// break;
	// case Surface.ROTATION_180:
	// degrees = 180;
	// break;
	// case Surface.ROTATION_270:
	// degrees = 270;
	// break;
	// default:
	// break;
	// }
	// int result = 0;
	// if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
	// result = (cameraInfo.orientation + degrees) % 360;
	// result = (360 - result) % 360;
	// } else {
	// result = (cameraInfo.orientation - degrees + 360) % 360;
	// }
	// camera.setDisplayOrientation(result);
	// }

	/**
	 * 让保存的图片能在相册中找到
	 * 
	 * @param path
	 */
	private void scanFileToPhotoAlbum(String path) {
		// 媒体扫描服务
		MediaScannerConnection.scanFile(this, new String[] { path }, null,
				new OnScanCompletedListener() {

					@Override
					public void onScanCompleted(String path, Uri uri) {
						Log.i("lzh", "Finished scanning " + path);
					}
				});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_preview:
			bitmap = null;
			// 如果点击预览的时候，重新设置预览画面
			try {
				mCamera.setPreviewDisplay(surfaceHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mCamera.startPreview();
			isPreviewing = true;
			// setCameraDisplayOrientation(MainActivity.this,
			// mCurrentCameraIndex, mCamera);
			// mCamera.autoFocus(mAutoFocusCallback);
			break;
		case R.id.btn_takepicture:
			if (isPreviewing) {
				// mCamera.autoFocus(mAutoFocusCallback);
				// 拍照 但是拍完并没有保存 如果不使用前面两个参数的话，可以都写null 目前是会回调到第三个参数的方法里
				// 第一个参数设为null 则静音拍照
				mCamera.takePicture(null, rawPictureCallback,
						jpegPictureCallback);
			}
			break;
		case R.id.btn_savepicture:
			if (bitmap == null) {
				MyToast.showToast(act, "请先点击拍照");
				return;
			}
			// 文件保存 保存的是bitmap到图片文件
			File imageFile = new File(imagePath);
			if (!imageFile.getParentFile().exists()) {
				imageFile.getParentFile().mkdirs();
			}
			try {
				BufferedOutputStream bos = new BufferedOutputStream(
						new FileOutputStream(imagePath));
				// bos.write(data);
				// 保存为jpeg格式，并且不压缩
				bitmap.compress(CompressFormat.JPEG, 100, bos);
				bos.flush();
				bos.close();
				// 不加这个在图册里面是不显示照片的，加上之后图册可以显示
				scanFileToPhotoAlbum(imagePath);
				MyToast.showToast(MainActivity.this, "图片保存成功" + imagePath);
				bitmap = null;
				try {
					// 保存成功后，继续到照片预览，可以点击拍照
					mCamera.setPreviewDisplay(surfaceHolder);
				} catch (IOException e) {
					e.printStackTrace();
				}
				mCamera.startPreview();
				isPreviewing = true;
				// setCameraDisplayOrientation(MainActivity.this,
				// mCurrentCameraIndex, mCamera);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}

	/**
	 * 方法 屏幕保持唤醒状态 要加权限 android.permission.WAKE_LOCK 摄像头 存储卡都要权限
	 */
	private void acquireWakeLock() {
		if (wakeLock == null) {
			powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			// wakeLock = powerManager.newWakeLock(PowerManager., tag)
			wakeLock = powerManager.newWakeLock(
					PowerManager.SCREEN_DIM_WAKE_LOCK, "lzh");
			wakeLock.acquire();
		}
	}

	/**
	 * 方法 取消屏幕唤醒状态
	 */
	private void releaseWakeLock() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			wakeLock = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 界面销毁时取消屏幕唤醒
		releaseWakeLock();
	}
}
