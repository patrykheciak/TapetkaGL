package pl.patrykheciak.tapetkagl;

import android.opengl.GLSurfaceView;

public class MyWallpaperService extends OpenGLES2WallpaperService {
    @Override
    GLSurfaceView.Renderer getNewRenderer() {
        return new MyRenderer();
    }
}
