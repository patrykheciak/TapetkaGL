package pl.patrykheciak.tapetkagl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import model.scene.Camera;
import model.scene.Part;
import model.scene.Scene;
import model.scene.Triangle;
import pl.patrykheciak.tapetkagl.scene.Vec3d;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class MyRenderer implements GLSurfaceView.Renderer {
    private final long time;
    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private float[] mViewMatrix = new float[16];

    /**
     * Store the projection matrix. This is used to project the scene onto a 2D viewport.
     */
    private float[] mProjectionMatrix = new float[16];

    /**
     * Allocate storage for the final combined matrix. This will be passed into the shader program.
     */
    private float[] mMVPMatrix = new float[16];

    /**
     * Store our model data in a float buffer.
     */
    private final FloatBuffer mTriangle1Vertices;
    private final FloatBuffer mTriangle2Vertices;
    private final FloatBuffer mTriangle3Vertices;
    private FloatBuffer mTriangle4Vertices;

    /**
     * This will be used to pass in the transformation matrix.
     */
    private int mMVPMatrixHandle;

    /**
     * This will be used to pass in model position information.
     */
    private int mPositionHandle;

    /**
     * This will be used to pass in model color information.
     */
    private int mColorHandle;

    private int mNormalHandle;

    /**
     * How many bytes per float.
     */
    private final int mBytesPerFloat = 4;

    /**
     * How many elements per vertex.
     */
    private final int mStrideBytes = 10 * mBytesPerFloat; // bylo 7

    /**
     * Offset of the position data.
     */
    private final int mPositionOffset = 0;

    /**
     * Size of the position data in elements.
     */
    private final int mPositionDataSize = 3;

    /**
     * Offset of the color data.
     */
    private final int mColorOffset = 3;

    private final int mNormalOffset = mColorOffset + 4;

    /**
     * Size of the color data in elements.
     */
    private final int mColorDataSize = 4;

    private final int mNormalDataSize = 3;

    private float XOffset = 0f;
    private Scene scene;

    private Vec3d norm;
    private Vec3d vLook;
    private double vLookLength;

    /**
     * Initialize the model data.
     */
    public MyRenderer() {
        // Define points for equilateral triangles.


        time = System.nanoTime();
        final float[] triangle1VerticesData = {
                // X, Y, Z,
                // R, GG, B, A
                -0.5f, 0f, 0.0f,
                1.0f, 0.0f, 0.0f, 1.0f,

                0.5f, 0f, 0.0f,
                0.0f, 0.0f, 1.0f, 1.0f,

                0.0f, 1f, 0.0f,
                0.0f, 1.0f, 0.0f, 1.0f};

        // This triangle is yellow, cyan, and magenta.
        final float[] triangle2VerticesData = {
                // X, Y, Z,
                // R, GG, B, A
                -0.25f, -0.25f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f,

                -0.25f, 0.25f, 0.0f,
                0.0f, 1.0f, 1.0f, 1.0f,

                0.0f, 0.25f, 0.0f,
                1.0f, 0.0f, 1.0f, 1.0f};

        // This triangle is white, gray, and black.
        final float[] triangle3VerticesData = {
                // X, Y, Z,
                // R, GG, B, A
                -0.1f, -0.1f, 0.0f,
                1.0f, 1.0f, 1.0f, 1.0f,

                0.1f, -0.1f, 0.0f,
                0.5f, 0.5f, 0.5f, 1.0f,

                0.1f, 0.1f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f};

        // Initialize the buffers.
        mTriangle1Vertices = ByteBuffer.allocateDirect(triangle1VerticesData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangle2Vertices = ByteBuffer.allocateDirect(triangle2VerticesData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangle3Vertices = ByteBuffer.allocateDirect(triangle3VerticesData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mTriangle1Vertices.put(triangle1VerticesData).position(0);
        mTriangle2Vertices.put(triangle2VerticesData).position(0);
        mTriangle3Vertices.put(triangle3VerticesData).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Set the background clear color to gray.
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);

        // enable face culling feature
        GLES20.glEnable(GL10.GL_CULL_FACE);
// specify which faces to not draw
        GLES20.glCullFace(GL10.GL_BACK);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = ""
                + "uniform mat4 u_MVPMatrix;      \n"        // A constant representing the combined model/view/projection matrix.
                + "attribute vec4 a_Position;     \n"        // Per-vertex position information we will pass in.
                + "attribute vec4 a_Color;        \n"        // Per-vertex color information we will pass in.
                + "attribute vec4 a_Normal;"
                + "varying vec4 v_Color;          \n"        // This will be passed into the fragment shader.
                + "varying float lightIntensity;"
                + "void main()                    \n"        // The entry point for our vertex shader.
                + "{                              \n"
                + "   vec3 lightDirection = vec3(0.0, -1.0, 0.0);"
                + "   lightIntensity = max(0.0, min(1.0, dot(a_Normal.xyz, lightDirection)));"
//                + "   v_Color = vec4(lightIntensity,lightIntensity,lightIntensity,1.0);          \n"        // Pass the color through to the fragment shader.
//                // It will be interpolated across the triangle.
                + "   v_Color = a_Color;          \n"        // Pass the color through to the fragment shader.
                // It will be interpolated across the triangle.
                + "   gl_Position = u_MVPMatrix   \n"    // gl_Position is a special variable used to store the final position.
                + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                + "}                              \n";    // normalized screen coordinates.

        final String fragmentShader = ""
                + "precision highp float;       \n"        // Set the default precision to medium. We don't need as high of a
                // precision in the fragment shader.
                + "varying vec4 v_Color;          \n"        // This is the color from the vertex shader interpolated across the
                // triangle per fragment.
                + "varying float lightIntensity;"
                + "void main()                    \n"        // The entry point for our fragment shader.
                + "{                              \n"
                + "   vec3 white = vec3(1.0, 1.0, 1.0);"
                + "   gl_FragColor = vec4(0.7 * v_Color.rgb + 0.3 * white * lightIntensity, 1.0);"
                + "}                              \n";

        // Load in the vertex shader.
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (vertexShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }

        if (vertexShaderHandle == 0) {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the fragment shader shader.
        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (fragmentShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }

        if (fragmentShaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
            GLES20.glBindAttribLocation(programHandle, 2, "a_Normal");

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(programHandle, "a_Normal");

        GLES20.glUseProgram(programHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // Position the eye behind the origin.
        float radious = 5f;
        float eyeX = 0.0f;
        float eyeY = 0.0f;
        float eyeZ = 0.0f;

        // We are looking toward the distance
        float lookX = 0.0f;
        float lookY = 0.0f;
        float lookZ = 0.0f;

        if (scene != null) {
            Camera cam = scene.getCamera();

            if (norm == null) {
                Vec3d v0 = new Vec3d(cam.getV0().getX(), cam.getV0().getY(), cam.getV0().getZ());
                Vec3d vc = new Vec3d(cam.getVc().getX(), cam.getVc().getY(), cam.getVc().getZ());
                vLook = new Vec3d();
                vLook.sub(v0, vc);
                vLookLength = vLook.length();
                vLook.normalize();

                norm = new Vec3d();
                Vec3d vLookSlightlyUp = new Vec3d(vLook);
                vLookSlightlyUp.add(new Vec3d(0.0, 1.0, 0.0));
                norm.cross(vLook, vLookSlightlyUp);
                norm.normalize();
            }

            long dt = System.nanoTime() - time;

            float dtSeconds = (float) (dt / 4_000_000_000.0) + 2 * XOffset;

            Vec3d forward = new Vec3d(vLook);
            forward.mul(0.66 * vLookLength * (Math.sin(dtSeconds + 1.5 * Math.PI) / 2.0 + 0.5));

            Vec3d sides = new Vec3d(norm);
            sides.mul(Math.sin(dtSeconds));

            Vec3d sum = new Vec3d();
            sum.add(forward);
            sum.add(sides);

            eyeX = (float) (cam.getV0().getX() + sum.x);
            eyeY = (float) (cam.getV0().getY() + sum.y);
            eyeZ = (float) (cam.getV0().getZ() + sum.z);

            lookX = (float) cam.getVc().getX();
            lookY = (float) cam.getVc().getY();
            lookZ = (float) cam.getVc().getZ();
        }

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        Matrix.setIdentityM(mModelMatrix, 0);
        drawAllTriangles(mTriangle4Vertices);
    }

    private void drawAllTriangles(final FloatBuffer aTriangleBuffer) {
        // Pass in the position information
        int triangles = scene.getTrngs().length;
        int i = 0;

        aTriangleBuffer.position(30 * i);  // position offset
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        aTriangleBuffer.position(30 * i + mColorOffset);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Pass in the normal information
        aTriangleBuffer.position(30 * i + mNormalOffset);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mNormalHandle);

        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3 * scene.getTrngs().length);
    }

    private void drawTriangle(final FloatBuffer aTriangleBuffer) {
        // Pass in the position information
        aTriangleBuffer.position(mPositionOffset);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        aTriangleBuffer.position(mColorOffset);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                mStrideBytes, aTriangleBuffer);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }

    public void setXOffset(float XOffset) {
        this.XOffset = XOffset;
    }

    public void setScene(Scene scene) {
        this.scene = scene;

        // TODO having the scene loaded render it :c

        int vertexBytes = 3;
        int colorsBytes = 4;
        int normalBytes = 3;
        mTriangle4Vertices = ByteBuffer.allocateDirect(3 * (colorsBytes + vertexBytes + normalBytes) * scene.getTrngs().length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        for (int iTri = 0; iTri < scene.getTrngs().length; iTri++) {
            int[] tri = scene.getTrngs()[iTri]; // indeksy wierzcholkow trojkata
            double[] doublev0 = scene.getVtxs()[tri[0]]; // wierzcholek 0
            double[] doublev1 = scene.getVtxs()[tri[1]]; // wierzcholek 1
            double[] doublev2 = scene.getVtxs()[tri[2]]; // wierzcholek 2

            float[] v0 = new float[vertexBytes + colorsBytes + normalBytes];
            float[] v1 = new float[vertexBytes + colorsBytes + normalBytes];
            float[] v2 = new float[vertexBytes + colorsBytes + normalBytes];

            for (int i = 0; i < doublev0.length; i++) {
                v0[i] = (float) doublev0[i];
                v1[i] = (float) doublev1[i];
                v2[i] = (float) doublev2[i];
            }


            for (int partI = 0; partI < scene.getParts().size(); partI++) {
                Part p = scene.getParts().get(partI);
                if (p.getTrinagleIds().contains(iTri)) {


                    float red = (float) (((double) p.getR()) / 255);
                    float green = (float) (((double) p.getGG()) / 255);
                    float blue = (float) (((double) p.getB()) / 255);
                    v0[3] = red;
                    v1[3] = red;
                    v2[3] = red;

                    v0[4] = green;
                    v1[4] = green;
                    v2[4] = green;

                    v0[5] = blue;
                    v1[5] = blue;
                    v2[5] = blue;
                }
            }

            v0[6] = 0.05f;
            v1[6] = 0.05f;
            v2[6] = 0.05f;

            // normale
            double[] normale0v = scene.getNormals()[tri[0]]; //normal zerowego wierzcholka trojkata
            double[] normale1v = scene.getNormals()[tri[1]]; //normal zerowego wierzcholka trojkata
            double[] normale2v = scene.getNormals()[tri[2]]; //normal zerowego wierzcholka trojkata
            for (int i = 7; i < 10; i++) {
                v0[i] = (float) normale0v[i - 7];
                v1[i] = (float) normale1v[i - 7];
                v2[i] = (float) normale2v[i - 7];
            }

            mTriangle4Vertices.put(v0);
            mTriangle4Vertices.put(v1);
            mTriangle4Vertices.put(v2);
        }
    }
}