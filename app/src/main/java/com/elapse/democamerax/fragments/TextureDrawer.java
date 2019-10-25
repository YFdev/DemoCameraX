package com.elapse.democamerax.fragments;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * author : Kevin.ning
 * e-mail :
 * date   : 2019/10/25 11:05
 * desc   :
 * version: 1.0
 */
public class TextureDrawer {
    private FloatBuffer buffer;
    private int mOESTextureId = -1;
    private int vertexShader = -1;
    private int fragmentShader = -1;
    int shaderProgram = -1;

    private int aPositionLocation = -1;
    private int aTextureCoordLocation = -1;
    private int uTextureMatrixLocation = -1;
    private int uTextureSamplerLocation = -1;

    TextureDrawer(int OESTextureId) {
        init(OESTextureId);
    }

    FloatBuffer getBuffer() {
        return buffer;
    }

    int getShaderProgram() {
        return shaderProgram;
    }

    private void init(int OESTextureId) {
        mOESTextureId = OESTextureId;
        buffer = createBuffer(vertexData);
        String VERTEX_SHADER = "attribute vec4 aPosition;\n" +
                "uniform mat4 uTextureMatrix;\n" +
                "attribute vec4 aTextureCoordinate;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main()\n" +
                "{\n" +
                "  vTextureCoord = (uTextureMatrix * aTextureCoordinate).xy;\n" +
                "  gl_Position = aPosition;\n" +
                "}";
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES uTextureSampler;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main()\n" +
                "{\n" +
                "  vec4 vCameraColor = texture2D(uTextureSampler, vTextureCoord);\n" +
                "  float fGrayColor = (0.3*vCameraColor.r + 0.59*vCameraColor.g + 0.11*vCameraColor.b);\n" +
                "  gl_FragColor = vec4(fGrayColor, fGrayColor, fGrayColor, 1.0);\n" +
                "}\n";
        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        shaderProgram = linkProgram(vertexShader, fragmentShader);
    }

    private FloatBuffer createBuffer(float[] vertexData) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        buffer.position(0);
        return buffer;
    }

    private int loadShader(int type, String shaderSource) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Create Shader Failed!" + GLES20.glGetError());
        }
        GLES20.glShaderSource(shader, shaderSource);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int linkProgram(int verShader, int fragShader) {
        int program = GLES20.glCreateProgram();
        if (program == 0) {
            throw new RuntimeException("Create Program Failed!" + GLES20.glGetError());
        }
        GLES20.glAttachShader(program, verShader);
        GLES20.glAttachShader(program, fragShader);
        GLES20.glLinkProgram(program);

        GLES20.glUseProgram(program);
        return program;
    }

    void drawTexture(float[] transformMatrix) {
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, TextureDrawer.POSITION_ATTRIBUTE);
        aTextureCoordLocation = GLES20.glGetAttribLocation(shaderProgram, TextureDrawer.TEXTURE_COORD_ATTRIBUTE);
        uTextureMatrixLocation = GLES20.glGetUniformLocation(shaderProgram, TextureDrawer.TEXTURE_MATRIX_UNIFORM);
        uTextureSamplerLocation = GLES20.glGetUniformLocation(shaderProgram, TextureDrawer.TEXTURE_SAMPLER_UNIFORM);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTextureId);
        GLES20.glUniform1i(uTextureSamplerLocation, 0);
        GLES20.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, transformMatrix, 0);

        if (buffer != null) {
            buffer.position(0);
            GLES20.glEnableVertexAttribArray(aPositionLocation);
            GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 16, buffer);

            buffer.position(2);
            GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
            GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 16, buffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }
    }

    private float[] vertexData = new float[]{
            1f, 1f, 1f, 1f,
            -1f, 1f, 0f, 1f,
            -1f, -1f, 0f, 0f,
            1f, 1f, 1f, 1f,
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f};

    private static String POSITION_ATTRIBUTE = "aPosition";
    private static String TEXTURE_COORD_ATTRIBUTE = "aTextureCoordinate";
    private static String TEXTURE_MATRIX_UNIFORM = "uTextureMatrix";
    private static String TEXTURE_SAMPLER_UNIFORM = "uTextureSampler";


    public static int createOESTextureObject() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return tex[0];
    }

}
