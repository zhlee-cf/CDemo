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
    private AudioManager manager; //��������
    private int volumn; //����ֵ

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initCameraFirst();
		
		// ����ȥ��title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// �������������Ļ
		int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		Window myWindow = this.getWindow();
		myWindow.setFlags(flag, flag);

		setContentView(R.layout.activity_main);
		act = this;
		// ����Ļ���ֻ���״̬
		acquireWakeLock();

		sv = (SurfaceView) findViewById(R.id.sv);
		sv.setZOrderOnTop(true);
		// ͨ��surfaceView��ȡholder
		surfaceHolder = sv.getHolder();
		surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

		btn_preview = (Button) findViewById(R.id.btn_preview);
		btn_takepicture = (Button) findViewById(R.id.btn_takepicture);
		btn_savepicture = (Button) findViewById(R.id.btn_savepicture);

		btn_preview.setOnClickListener(this);
		btn_takepicture.setOnClickListener(this);
		btn_savepicture.setOnClickListener(this);
		// ��holder��ӻص�
		surfaceHolder.addCallback(new Callback() {

			@Override
			/**
			 * surfaceView����ʱִ�д˷���
			 */
			public void surfaceCreated(SurfaceHolder holder) {
				if (mCamera == null) {
					// ��������ͷ
					mCamera = openCamera();
					isPreviewing = true;
				}
			}

			@Override
			/**
			 * surfaceView���ݸı�ʱִ�д˷���
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
			 * surfaceView����ʱִ�д˷���
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
            // �����Ҫ�������ҵ�ǰδ������muteMode�����ÿ��Է���Preference�У�
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
	}

	/**
	 * ��������ɶʱ��ص��� ���ڶ�����дΪnull
	 */
	private ShutterCallback shutterCallback = new ShutterCallback() {

		@Override
		public void onShutter() {

		}
	};
	/**
	 * ��������ɶʱ��ص��� ���ڶ�����дΪnull
	 */
	private PictureCallback rawPictureCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

		}
	};
	/**
	 * ����ʱ��ص����˷���
	 */
	private PictureCallback jpegPictureCallback = new PictureCallback() {

		@Override
		/**
		 * data����ͼƬ���ֽ�����
		 */
		public void onPictureTaken(byte[] data, Camera camera) {
			 // ��������
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, volumn,
                    AudioManager.FLAG_ALLOW_RINGER_MODES);
			// ����֮��ͼƬ����ʾ�����ģ�������ʱ��û�б���
			// ������պ� ��ͼƬ���bitmap �����ʱ����
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			// ����ʱ��ͼƬָ����·�� ��ʵ����·�����ļ���
			imagePath = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DCIM).toString()
					+ File.separator
					+ "PicTest_"
					+ System.currentTimeMillis()
					+ ".jpg";
		}
	};

	/**
	 * ���� ������ͷ ����ϵͳ���
	 * 
	 * @return
	 */
	private Camera openCamera() {
		// �ֻ��ϵ�����ͷ�ĸ���
		int cameraCount = 0;
		Camera camera = null;
		// ��������ͷ��Ϣ���� �������Ŀǰ��û���ݵ� ������������Camera.getCameraInfo(cameraIndex,
		// cameraInfo);
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		// ��ȡ�ܹ��ж��ٸ�����ͷ
		cameraCount = Camera.getNumberOfCameras();
		for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
			// ��ȡ����ͷ����Ϣ C����ģʽ
			Camera.getCameraInfo(cameraIndex, cameraInfo);
			// ����Ǻ�����ͷ�������ͷ
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				// ��ָ��������ͷ
				camera = Camera.open(cameraIndex);
				// Ϊ����ͷ���ò��� �����������Ժ�ǵ�set��ȥ ��Ȼ��Ч
				Parameters params = camera.getParameters();
				// �����Զ��Խ� Ҫ���ж����Ƿ�֧���Զ��Խ�
				if (params.getSupportedFocusModes().contains(
						Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
					params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				}
				camera.cancelAutoFocus(); // ��������� ��Ȼ��Ч
				// params.setPreviewFormat(ImageFormat.JPEG);
				params.setPictureFormat(PixelFormat.JPEG);
				// ����������Զ� Ҫ��Ȩ��  android:name="android.permission.FLASHLIGHT"
				params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				// ���������ֻ��ֱ��� ����������
				// �ֱ���Ҫ���� �����õĻ��ĵ���Ƭ��ģ��
//				params.setPictureSize(1280, 720);
				params.setPictureSize(4208, 2368);
				// ������Ļ��ת
				params.set("rotation", 90);
				
				camera.setParameters(params);
				// ��������ͷ��ʾ�ķ��� �������Ϊ�� ��������� ��֪��Ϊ�� ������������Ǹ�params.set("rotation",
				// 90);Ҫһ������
				// ����������������õĻ��������ͼƬ�Ǻ���ģ����������ú󣬱���Ķ��������
				camera.setDisplayOrientation(90);
				// mCurrentCameraIndex = cameraIndex;
			}
		}

		return camera;
	}

	/**
	 * ���ݺ������Զ�����preview���� �ⷽ�� ��ʱû����
	 * 
	 * @param activity
	 * @param cameraId
	 * @param camera
	 */
	// private void setCameraDisplayOrientation(Activity activity, int cameraId,
	// Camera camera) {
	// // C����ģʽ��ȡcamerainfo
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
	 * �ñ����ͼƬ����������ҵ�
	 * 
	 * @param path
	 */
	private void scanFileToPhotoAlbum(String path) {
		// ý��ɨ�����
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
			// ������Ԥ����ʱ����������Ԥ������
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
				// ���� �������겢û�б��� �����ʹ��ǰ�����������Ļ������Զ�дnull Ŀǰ�ǻ�ص��������������ķ�����
				// ��һ��������Ϊnull ��������
				mCamera.takePicture(null, rawPictureCallback,
						jpegPictureCallback);
			}
			break;
		case R.id.btn_savepicture:
			if (bitmap == null) {
				MyToast.showToast(act, "���ȵ������");
				return;
			}
			// �ļ����� �������bitmap��ͼƬ�ļ�
			File imageFile = new File(imagePath);
			if (!imageFile.getParentFile().exists()) {
				imageFile.getParentFile().mkdirs();
			}
			try {
				BufferedOutputStream bos = new BufferedOutputStream(
						new FileOutputStream(imagePath));
				// bos.write(data);
				// ����Ϊjpeg��ʽ�����Ҳ�ѹ��
				bitmap.compress(CompressFormat.JPEG, 100, bos);
				bos.flush();
				bos.close();
				// ���������ͼ�������ǲ���ʾ��Ƭ�ģ�����֮��ͼ�������ʾ
				scanFileToPhotoAlbum(imagePath);
				MyToast.showToast(MainActivity.this, "ͼƬ����ɹ�" + imagePath);
				bitmap = null;
				try {
					// ����ɹ��󣬼�������ƬԤ�������Ե������
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
	 * ���� ��Ļ���ֻ���״̬ Ҫ��Ȩ�� android.permission.WAKE_LOCK ����ͷ �洢����ҪȨ��
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
	 * ���� ȡ����Ļ����״̬
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
		// ��������ʱȡ����Ļ����
		releaseWakeLock();
	}
}
