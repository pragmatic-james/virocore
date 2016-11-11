package com.viro.renderer;

import android.graphics.SurfaceTexture;
import android.view.Surface;

public class VideoSink implements SurfaceTexture.OnFrameAvailableListener, FrameListener {

    /**
     * The underlying Android Graphics surface.
     */
    private Surface mSurface;

    /**
     * The underlying GL surface texture.
     */
    private SurfaceTexture mSurfaceTexture;

    /**
     * True if the surface's data has been updated.
     */
    private boolean mSurfacedUpdated;

    /**
     * Create a new VideoSink wrapping the given native OpenGL
     * texture ID.
     *
     * @param textureId The native texture ID.
     */
    public VideoSink(int textureId) {
        mSurfaceTexture = new SurfaceTexture(textureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mSurface = new Surface(mSurfaceTexture);

        synchronized (this) {
            mSurfacedUpdated = false;
        }
    }

    @Override
    public void onDrawFrame() {
        synchronized (this) {
            if (mSurfacedUpdated) {
                mSurfaceTexture.updateTexImage();
                mSurfacedUpdated = false;
            }
        }
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        /*
         * SurfaceTexture calls here when it has new
         * data available. Call may come in from any thread,
         * so synchronized. No OpenGL calls can be done here.
         */
        mSurfacedUpdated = true;
    }

    Surface getSurface() {
        return mSurface;
    }

    void releaseSurface() {
        mSurface.release();
    }
}
