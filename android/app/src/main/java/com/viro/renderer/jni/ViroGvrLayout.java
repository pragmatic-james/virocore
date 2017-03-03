/**
 * Copyright © 2016 Viro Media. All rights reserved.
 */
package com.viro.renderer.jni;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.google.vr.ndk.base.AndroidCompat;
import com.google.vr.ndk.base.GvrApi;
import com.google.vr.ndk.base.GvrLayout;
import com.viro.renderer.FrameListener;
import com.viro.renderer.GLSurfaceViewQueue;

import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Custom Android view encapsulating all GvrLayout
 * setup and initialization for displaying a VR view.
 * Create this view during or post onCreate within
 * the activity lifecycle.
 */
public class ViroGvrLayout extends GvrLayout implements VrView {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("gvr");
        System.loadLibrary("gvr_audio");
        System.loadLibrary("native-lib");
    }
    private RendererJni mNativeRenderer;
    private final RenderContextJni mNativeRenderContext;
    private AssetManager mAssetManager;
    private List<FrameListener> mFrameListeners = new ArrayList();
    private GlListener mGlListener = null;
    private PlatformUtil mPlatformUtil;

    public ViroGvrLayout(Context context, GlListener glListener) {
        super(context);

        final Context activityContext = getContext();

        // Initialize the native renderer.
        final GLSurfaceView glSurfaceView = new GLSurfaceView(activityContext.getApplicationContext());

        mAssetManager = getResources().getAssets();
        mPlatformUtil = new PlatformUtil(
                new GLSurfaceViewQueue(glSurfaceView),
                mFrameListeners,
                activityContext,
                mAssetManager);
        mNativeRenderer = new RendererJni(
                getClass().getClassLoader(),
                activityContext.getApplicationContext(),
                mAssetManager, mPlatformUtil,
                getGvrApi().getNativeGvrContext());
        mNativeRenderContext = new RenderContextJni(mNativeRenderer.mNativeRef);

        mGlListener = glListener;
        // Add the GLSurfaceView to the GvrLayout.
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 0, 0, 0);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setRenderer(new Renderer() {
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                mNativeRenderer.onSurfaceCreated(glSurfaceView.getHolder().getSurface());
                mNativeRenderer.initalizeGl();
                if (mGlListener != null) {
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mGlListener.onGlInitialized();
                        }
                    };
                    new Handler(Looper.getMainLooper()).post(myRunnable);
                }
            }

            public void onSurfaceChanged(GL10 gl, int width, int height) {
                /**
                 * We have to manually notify the GVR surface presenter
                 * if the dimensions of the surface has changed (for example
                 * during a rotation).
                 */
                final GvrApi gvr = getGvrApi();
                if (gvr != null){
                    gvr.refreshDisplayMetrics();
                }

                mNativeRenderer.onSurfaceChanged(glSurfaceView.getHolder().getSurface());
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                for (FrameListener listener : mFrameListeners) {
                    listener.onDrawFrame();
                }
                mNativeRenderer.drawFrame();
            }
        });

        glSurfaceView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                    mNativeRenderer.onTouchEvent(action, event.getX(), event.getY());
                    return true;
                }
                return false;
            }
        });

        setPresentationView(glSurfaceView);

        // According to the GVR documentation, this only sets the activity to "VR mode" and is only
        // supported on Android Nougat and up.
        // We always want VR mode enabled.
        AndroidCompat.setVrModeEnabled((Activity)getContext(), true);
        setStereoModeEnabled(true);

        // Prevent screen from dimming/locking.
        ((Activity)activityContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Prevent screen from switching to portrait
        ((Activity)activityContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Application app = (Application)activityContext.getApplicationContext();
        app.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public RenderContextJni getRenderContextRef(){
        return mNativeRenderContext;
    }

    @Override
    public void setScene(SceneJni scene) {
        mNativeRenderer.setScene(scene.mNativeRef, 1.0f);
    }

    /**
     * This function should only be called once.
     *
     * @param vrModeEnabled - whether or not to use VR or 360 mode.
     */
    @Override
    public void setVrModeEnabled(boolean vrModeEnabled) {

        if (vrModeEnabled) {
            // Enable scan line racing.
            // According to Google, we should only set this to true when we're in stereo mode which is
            // okay to do here because its the default. See https://github.com/googlevr/gvr-android-sdk/issues/316
            if (setAsyncReprojectionEnabled(true)) {
                // Scanline racing decouples the app framerate from the display framerate,
                // allowing immersive interaction even at the throttled clockrates set by
                // sustained performance mode.
                AndroidCompat.setSustainedPerformanceMode((Activity) getContext(), true);
            }
        }

        mNativeRenderer.setVRModeEnabled(vrModeEnabled);
    }

    @Override
    public RendererJni getNativeRenderer() {
        return mNativeRenderer;
    }

    @Override
    public View getContentView() { return this; }

    @Override
    public String getVRPlatform() {
        return "gvr";
    }

    @Override
    public String getHeadset() {
        return mNativeRenderer.getHeadset();
    }

    @Override
    public String getController() {
        return mNativeRenderer.getController();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mNativeRenderer.onPause();
        super.onPause();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mNativeRenderer.onResume();

        // Ensure fullscreen immersion.
        setImmersiveSticky();
        final Context activityContext = getContext();
        ((Activity)activityContext)
                .getWindow()
                .getDecorView()
                .setOnSystemUiVisibilityChangeListener(
                        new OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                    setImmersiveSticky();
                                }
                            }
                        });
        super.onResume();
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        super.shutdown();
        mNativeRenderContext.delete();
        mNativeRenderer.destroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Avoid accidental volume key presses while the phone is in the VR headset.
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public void setImmersiveSticky() {
        ((Activity)getContext())
                .getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        //No-op
    }

    @Override
    public void onActivityStarted(Activity activity) {
        mNativeRenderer.onStart();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mNativeRenderer.onStop();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        //No-op
    }

}
