package com.elapse.democamerax.fragments;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.TextureView;
import androidx.annotation.NonNull;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;


/**
 * author : Kevin.ning
 * e-mail :
 * date   : 2019/10/25 11:04
 * desc   :
 * version: 1.0
 */
public class Renderer implements SurfaceTexture.OnFrameAvailableListener {
    private static final int MSG_INIT = 1;
    private static final int MSG_RENDER = 2;
    private static final int MSG_DEINIT = 3;
    private static final int MSG_INIT_SURFACE = 4;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private TextureView mTextureView;
    private int mOESTextureId = 0;
    private TextureDrawer mFilterEngine;
    private FloatBuffer mDataBuffer;
    private int mShaderProgram = -1;
    private float[] transformMatrix = new float[16];
    private EGL10 mEgl;
    private EGLDisplay mEGLDisplay = EGL10.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL10.EGL_NO_CONTEXT;
    private EGLConfig[] mEGLConfig = new EGLConfig[1];
    private EGLSurface mEglSurface;
    private SurfaceTexture mOESSurfaceTexture;

    public Renderer() {

    }

    public void init(TextureView textureView, int OESTextureId) {
        mTextureView = textureView;
        mOESTextureId = OESTextureId;
        mHandlerThread = new HandlerThread("Renderer Thread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_INIT:
                        initEGL();
                        break;
                    case MSG_RENDER:
                        drawFrame();
                        break;
                    case MSG_INIT_SURFACE:
                        initSurfaceTexture();
                        break;
                    case MSG_DEINIT:
                        deInitSurfaceTexture();
                        break;
                    default:
                        break;
                }
            }
        };
        mHandler.sendEmptyMessage(MSG_INIT);
    }

    private void deInitSurfaceTexture() {
        if (mOESSurfaceTexture != null) {
            mOESSurfaceTexture.detachFromGLContext();
        }
    }

    private void initSurfaceTexture() {
        if (mOESSurfaceTexture != null) {
            mOESSurfaceTexture.attachToGLContext(mOESTextureId);
            mOESSurfaceTexture.setOnFrameAvailableListener(this);
        }
    }

    private void initEGL() {
        mEgl = (EGL10) EGLContext.getEGL();
        if (mEgl != null) {
            //获取显示设备
            mEGLDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (mEGLDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed! " + mEgl.eglGetError());
            }
        }

        //version中存放EGL版本号
        int[] version = new int[2];
        //初始化EGL
        if (mEgl != null) {
            if (!mEgl.eglInitialize(mEGLDisplay, version)) {
                throw new RuntimeException("eglInitialize failed! " + mEgl.eglGetError());
            }

            //构造需要的配置列表
            int[] attributes = new int[]{EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8, EGL10.EGL_BUFFER_SIZE, 32, EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT, EGL10.EGL_NONE};
            int[] configsNum = new int[1];

            //EGL选择配置
            if (!mEgl.eglChooseConfig(mEGLDisplay, attributes, mEGLConfig, 1, configsNum)) {
                throw new RuntimeException("eglChooseConfig failed! " + mEgl.eglGetError());
            }
            SurfaceTexture surfaceTexture = null;
            if (mTextureView != null) {
                surfaceTexture = mTextureView.getSurfaceTexture();
                if (surfaceTexture == null) {
                    return;
                }
            }

            //创建EGL显示窗口
            mEglSurface = mEgl.eglCreateWindowSurface(mEGLDisplay, mEGLConfig[0], surfaceTexture, null);
            //创建上下文
            int[] contextAttribs = new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
            mEGLContext = mEgl.eglCreateContext(mEGLDisplay, mEGLConfig[0], EGL10.EGL_NO_CONTEXT, contextAttribs);

            if (mEGLDisplay == EGL10.EGL_NO_DISPLAY || mEGLContext == EGL10.EGL_NO_CONTEXT) {
                throw new RuntimeException("eglCreateContext fail failed! " + mEgl.eglGetError());
            }

            if (!mEgl.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed! " + mEgl.eglGetError());
            }

            mFilterEngine = new TextureDrawer(mOESTextureId);

            mDataBuffer = mFilterEngine.getBuffer();
            mShaderProgram = mFilterEngine.getShaderProgram();
        }

    }

    private void drawFrame() {
        if (mOESSurfaceTexture != null) {
            mOESSurfaceTexture.updateTexImage();
            mOESSurfaceTexture.getTransformMatrix(transformMatrix);
        }
        if (mEgl != null){
            mEgl.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext);
            GLES20.glViewport(0, 0, mTextureView.getWidth(), mTextureView.getHeight());
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(1f, 1f, 0f, 0f);
            mFilterEngine.drawTexture(transformMatrix);
            mEgl.eglSwapBuffers(mEGLDisplay, mEglSurface);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_RENDER);
        }
    }

     public void initOESTexture(SurfaceTexture st) {
        mOESSurfaceTexture = st;
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_INIT_SURFACE);
        }
    }
}
