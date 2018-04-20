package pl.patrykheciak.tapetkagl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import model.scene.Scene;
import model.scene.SceneParser;

public abstract class OpenGLES2WallpaperService extends GLWallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new OpenGLES2Engine();
    }

    class OpenGLES2Engine extends GLWallpaperService.GLEngine {

        private GLSurfaceView.Renderer renderer;
        private int width;
        private int height;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            setTouchEventsEnabled(true);

            // Check if the system supports OpenGL ES 2.0.
            final ActivityManager activityManager =
                    (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final ConfigurationInfo configurationInfo =
                    activityManager.getDeviceConfigurationInfo();
            final boolean supportsEs2 =
                    configurationInfo.reqGlEsVersion >= 0x20000;

            if (supportsEs2) {
                // Request an OpenGL ES 2.0 compatible context.
                setEGLContextClientVersion(2);

                // On Honeycomb+ devices, this improves the performance when
                // leaving and resuming the live wallpaper.
                setPreserveEGLContextOnPause(true);

                // Set the renderer to our user-defined renderer.
                renderer = getNewRenderer();
                setRenderer(renderer);


                if (renderer instanceof MyRenderer) {


                    MyRenderer lor = (MyRenderer) renderer;


                    ArrayList<String> lines = new ArrayList<>();

                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(
                                new InputStreamReader(getAssets().open("scena.brp"), "UTF-8"));

                        // do reading, usually loop until end of file reading
                        String mLine;
                        while ((mLine = reader.readLine()) != null) {
                            //process line
                            lines.add(mLine);
                        }
                    } catch (IOException e) {
                        //log the exception
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                //log the exception
                            }
                        }
                    }

                    Scene scene = SceneParser.Companion.parse(lines);
                    lor.setScene(scene);
                }
            } else {
                // This is where you could create an OpenGL ES 1.x compatible
                // renderer if you wanted to support both ES 1 and ES 2.
                return;
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
            if (renderer instanceof MyRenderer) {
                MyRenderer lor = (MyRenderer) renderer;
                lor.setXOffset(xOffset);
            }
        }


        @Override
        public void onTouchEvent(MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                float y = event.getY();
                int width = Resources.getSystem().getDisplayMetrics().widthPixels;
                int height = Resources.getSystem().getDisplayMetrics().heightPixels;

                float radious = Math.min(width, height) / 4;

                if (x * x + (y  - height/2) * (y  - height/2) < radious * radious) {
                    launchWebpage("http://www.goo.gl");
                    Log.d("Wallpaper", "lewy");
                }

                if ((x - width) * (x - width) + (y  - height/2) * (y  - height/2) < radious * radious) {
                    launchWebpage("http://www.abc.xyz");
                    Log.d("Wallpaper", "prawy");
                }
            }

            super.onTouchEvent(event);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.d("TAG", "visibility" + visible);
        }

        private void launchWebpage(String url) {

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
    }

    abstract GLSurfaceView.Renderer getNewRenderer();
}
