package jp.juggler.util_gl;

import android.opengl.Matrix;

import javax.microedition.khronos.opengles.GL10;

/**
 * A utility that projects
 *
 */
public class Projector {
	// ビューポート
    int mX;
    int mY;
    int mViewWidth;
    int mViewHeight;

    // モデルビュー行列
    public float[] mModelView = new float[16];
    
    // プロジェクション行列
    public float[] mProjection = new float[16];


    boolean mMVPComputed;
    float[] mMVP = new float[16];
    float[] mV = new float[4];

    // 現在のビューポートを覚えておく
    public void setCurrentView(int x, int y, int width, int height) {
        mX = x;
        mY = y;
        mViewWidth = width;
        mViewHeight = height;
    }

    // 現在のモデルビューを覚えておく
    // 副作用：現在の行列モードを GL_MODELVIEWにしてしまう
    public void getCurrentModelView(GL10 gl) {
    	MatrixTrackingGL.getMatrix(gl, GL10.GL_MODELVIEW, mModelView);
        mMVPComputed = false;
    }

    // 現在のプロジェクション行列を覚えておく
    // 副作用：現在の行列モードを GL_PROJECTIONにしてしまう
    public void getCurrentProjection(GL10 gl) {
    	MatrixTrackingGL.getMatrix(gl, GL10.GL_PROJECTION, mProjection);
        mMVPComputed = false;
    }

    // モデル座標からスクリーン座標への座標玄関を行う
    // objのobjoffsetから4要素が入力のx,y,z,1
    // winのwinOffsetから4要素が出力のx,y,z,?
    public void project(float[] obj, int objOffset, float[] win, int winOffset) {
        if (!mMVPComputed) {
            Matrix.multiplyMM(mMVP, 0, mProjection, 0, mModelView, 0);
            mMVPComputed = true;
        }

        Matrix.multiplyMV(mV, 0, mMVP, 0, obj, objOffset);

        float rw = 1.0f / mV[3];

        win[winOffset    ] = mX + mViewWidth  * (mV[0] * rw + 1.0f) * 0.5f;
        win[winOffset + 1] = mY + mViewHeight * (mV[1] * rw + 1.0f) * 0.5f;
        win[winOffset + 2] = (mV[2] * rw + 1.0f) * 0.5f;
    }
}
