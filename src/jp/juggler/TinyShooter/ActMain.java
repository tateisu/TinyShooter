package jp.juggler.TinyShooter;

import javax.microedition.khronos.opengles.GL;

import jp.juggler.util.LogCategory;
import jp.juggler.util_gl.MatrixTrackingGL;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class ActMain extends Activity {
	static final LogCategory log = new LogCategory("ActMain");
    GLSurfaceView svGL;
	MyRenderer renderer;
    static final boolean debug = true;

    
    void initUI(){
    	//フルスクリーン表示
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    	// タイトルなし
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
    	// コンテンツビューを設定
		svGL = new MyGLView();
    	setContentView(svGL);
    }
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderer.onResume();
        svGL.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        svGL.onPause();
        renderer.onPause();
        if(isFinishing()){
        	// 終了処理を行う
        }
    }

	class MyGLView extends GLSurfaceView {
		// ctor
		MyGLView(){
			super(ActMain.this);
			// サーフェスがGLオブジェクトを作成する際にラッパーをかぶせる
			setGLWrapper(new GLSurfaceView.GLWrapper() {
				@Override
				public GL wrap(GL gl) {
 	                return new MatrixTrackingGL(gl,log);
 	            }
 	        });

			// メトリクスの取得
	    	DisplayMetrics metrics = new DisplayMetrics();
	    	getWindowManager().getDefaultDisplay().getMetrics(metrics);
	         
     		// 
			renderer = new MyRenderer(ActMain.this,metrics);
     		setRenderer(renderer);
		}
		
		@Override
		public boolean onTouchEvent(final MotionEvent event) {
			queueEvent(new Runnable() {
				@Override
				public void run() {
					renderer.onTouch(event);
				}
			});
			return true;
		}
    }
}