package com.tencent.liteav.demo.trtc.customCapture;

/*****************************************************************
 *
 *                 测试自定义采集功能 TestSendCustomVideoData
 *
 *  该示例代码通过从手机中的一个视频文件里读取视频画面，并通过 TRTCCloud 的 sendCustomVideoData 接口，
 *  将这些视频画面送给 SDK 进行编码和发送。
 *
 *  本示例代码中采用了 texture，也就是 openGL 纹理的方案，这是 android 系统下性能最好的一种视频处理方案。
 *
 *  1. start()：传入一个视频文件的文件路径，并启动一个 GLThread 线程，该线程启动后会有两种回调通知出来：
 *  2. onSurfaceTextureAvailable(): 承载视频画面的“画板（SurfaceTexture）”已经准备好了。
 *                                  由于从视频文件中读取出的纹理为 OES 外部纹理，不能直接传给 TRTC SDK，
 *                                  因此我们委托 GLThread 线程创建了一个 SurfaceTexture。
 *
 *  3. onTextureProcess(): 当 GLThread 线程关联的“画板”内容发生变更时，也就是有新的一帧视频渲染上来时，
 *                         GLThread 就会触发该回调。此时，我们就可以向 TRTC SDK 中 sendCustomVideoData() 了。
 *
 ******************************************************************/

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLContext;
import android.util.Log;

import com.faceunity.nama.FURenderer;
import com.tencent.liteav.demo.trtc.customCapture.openGLBaseModule.GLThread;
import com.tencent.liteav.demo.trtc.utils.CameraUtils;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;


public class TestSendCustomCameraData implements GLThread.IGLSurfaceTextureListener {
    private static String TAG = "TestSendCustomCameraData";

    private Context mContext;
    private TRTCCloud mTRTCCloud;
    private boolean mIsSending;

    private GLThread mGLThread;
    private SurfaceTexture mSurfaceTexture;

    private FURenderer mFURenderer;

    public TestSendCustomCameraData(Context context) {
        mContext = context;
        mTRTCCloud = TRTCCloud.sharedInstance(mContext);
        mIsSending = false;

        mFURenderer = new FURenderer.Builder(context)
                .setInputTextureType(FURenderer.INPUT_2D_TEXTURE)
                .build();
    }

    public FURenderer getFURenderer() {
        return mFURenderer;
    }

    public synchronized void start() {
        if (mIsSending) {
            return;
        }

        //启动一个 OpenGL 线程，该线程用于定时 sendCustomVideoData()
        mGLThread = new GLThread();
        mGLThread.setListener(this);
        mGLThread.start();

        mIsSending = true;
    }

    private final Object mCameraLock = new Object();
    private Camera mCamera;
    private int mCameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int mCameraWidth = 1280;
    private int mCameraHeight = 720;
    private int mCameraOrientation;

    @SuppressWarnings("deprecation")
    public void openCamera(final int cameraType) {
        try {
            synchronized (mCameraLock) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                int cameraId = 0;
                int numCameras = Camera.getNumberOfCameras();
                for (int i = 0; i < numCameras; i++) {
                    Camera.getCameraInfo(i, info);
                    Camera.getCameraInfo(i, info);
                    if (info.facing == cameraType) {
                        cameraId = i;
                        mCamera = Camera.open(i);
                        mCameraType = cameraType;
                        break;
                    }
                }
                if (mCamera == null) {
                    cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                    Camera.getCameraInfo(cameraId, info);
                    mCamera = Camera.open(cameraId);
                    mCameraType = cameraId;
                }
                if (mCamera == null) {
                    throw new RuntimeException("No cameras");
                }

                mCameraOrientation = info.orientation;
                CameraUtils.setCameraDisplayOrientation((Activity) mContext, cameraId, mCamera);

                Camera.Parameters parameters = mCamera.getParameters();

                int[] size = CameraUtils.choosePreviewSize(parameters, mCameraWidth, mCameraHeight);
                mCameraWidth = size[0];
                mCameraHeight = size[1];
                mCamera.setParameters(parameters);
                Log.d(TAG, "openCamera: camWidth:" + mCameraWidth + ", camHeight:" + mCameraHeight
                        + ", camOrientation:" + mCameraOrientation + ", camFacing:" + mCameraType);
            }
            cameraStartPreview();
        } catch (Exception e) {
            Log.e(TAG, "openCamera: ", e);
            releaseCamera();
        }
    }

    private void cameraStartPreview() {
        try {
            synchronized (mCameraLock) {
                if (mCamera == null) {
                    return;
                }
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.startPreview();
            }
        } catch (Exception e) {
            Log.e(TAG, "cameraStartPreview: ", e);
        }
    }

    public void releaseCamera() {
        try {
            synchronized (mCameraLock) {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "releaseCamera: ", e);
        }
    }

    public void changeCamera() {
        mIsSending = false;
        releaseCamera();
        openCamera(mCameraType == Camera.CameraInfo.CAMERA_FACING_FRONT ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
        mIsSending = true;
        if (mFURenderer != null) {
            mFURenderer.onCameraChange(mCameraType, mCameraOrientation);
        }
    }

    public synchronized void stop() {
        if (!mIsSending) {
            return;
        }
        mIsSending = false;

        releaseCamera();

        if (mGLThread != null) {
            mGLThread.stop();
        }

    }

    // 为了减少对象的创建，复用视频帧数据对象
    private TRTCCloudDef.TRTCVideoFrame videoFrame = new TRTCCloudDef.TRTCVideoFrame();
    private TRTCCloudDef.TRTCTexture tRTCTexture = new TRTCCloudDef.TRTCTexture();

    /**
     * 承载视频画面的“画板（SurfaceTexture）”已经准备好了，需要我们创建一个 MovieVideoFrameReader，并与之关联起来。
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureAvailable: " + Thread.currentThread().getName());
        mSurfaceTexture = surfaceTexture;
        openCamera(mCameraType);
        mGLThread.setInputSize(mCameraWidth, mCameraHeight);
        mFURenderer.onSurfaceCreated();

        videoFrame.texture = tRTCTexture;
        videoFrame.width = mCameraHeight;
        videoFrame.height = mCameraWidth;
        videoFrame.timestamp = 0;
        videoFrame.pixelFormat = TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D;
        videoFrame.bufferType = TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE;
    }

    /**
     * 当 GLThread 线程关联的“画板”内容发生变更时，也就是有新的一帧视频渲染上来时，
     * GLThread 就会触发该回调。此时，我们就可以向 TRTC SDK 中 sendCustomVideoData()了。
     */
    @Override
    public int onTextureProcess(int textureId, EGLContext eglContext) {
        if (!mIsSending) {
            return textureId;
        }

        // 通过 FU 纹理输入接口，进行美颜滤镜处理，然后将视频帧通过纹理方式塞给SDK
        videoFrame.texture.textureId = mFURenderer.onDrawFrameSingleInput(textureId, mCameraHeight, mCameraWidth);
        videoFrame.texture.eglContext14 = eglContext;
        mTRTCCloud.sendCustomVideoData(videoFrame);
        return textureId;
    }

    @Override
    public void onSurfaceTextureDestroy(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureDestroy: " + Thread.currentThread().getName());
        mFURenderer.onSurfaceDestroyed();
    }

}
