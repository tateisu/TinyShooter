package jp.juggler.TinyShooter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Random;

// import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import jp.juggler.TinyShooter.R;
import jp.juggler.TinyShooter.ListWithPool.ItemFactory;
import jp.juggler.TinyShooter.SpriteDrawer.Sprite;
import jp.juggler.util.LogCategory;
import jp.juggler.util_gl.*;


public class MyRenderer implements GLSurfaceView.Renderer{
    static final LogCategory log = new LogCategory("Renderer");
    static final int fixed_one = 0x10000;
    static final boolean debug = false;
    DisplayMetrics metrics;
    Context context;
  
    Projector mProjector = new Projector();

    Triangle mTriangle = new Triangle();
    int tri_texture_id;
    
    
    float[] mScratch = new float[8];

    LabelMaker mLabels;
    Paint mLabelPaint = new Paint();
    int label_id_a;
    int label_id_b;
    int label_id_c;
    
    
    SpriteDrawer mSprites;
    int sprite_id_box1;
    int sprite_id_box1_fill;
    int sprite_id_circle1;
    int sprite_id_circle1_fill;
    int sprite_meca1;
    int sprite_meca2;
    int sprite_tama;
    int sprite_tama2;
    int sprite_bg;
    AsciiDrawer ascii_drawer;

    static class InputRect{
    	int sprite_off;
    	int sprite_on;
    	boolean bPressed;
    	boolean bLastPress;
    	boolean bEnabled;
    	int left;
    	int top;
    	int right;
    	int bottom;
    }
    int[] arrow_sprite_id = new int[9];
    int arrow_direction = -1; // -1 = off, 0が上、時計回りに進んで7が左上 

    ArrayList<InputRect> input_list = new ArrayList<InputRect>();
    void add_input(int x,int y,int w,int h,int off,int on){
    	InputRect rect = new InputRect();
    	// FIXME: レイアウト変更に対応させないといけない
    	if( x>=0){
    		rect.left = x;
    		rect.right = rect.left + w;
    	}else{
    		rect.right = mWidth +x +1;
    		rect.left = rect.right -w;
    	}
    	if(y>=0){
        	rect.top  = y;
        	rect.bottom = rect.top + h;
    	}else{
    		log.d("height=%d,y=%d,h=%d",mHeight,y,h);
        	rect.bottom  = mHeight + y +1;
        	rect.top = rect.bottom -h;
    	}
    	log.d("input_rect %d,%d,%d,%d",rect.left,rect.top,rect.right,rect.bottom);
    	rect.sprite_off = off;
    	rect.sprite_on = on;
    	rect.bEnabled = true;
		input_list.add(rect);
    }

    public int dp2px(float dp){
    	return (int)(0.5f + metrics.density * dp );
    }
    
    // ctor
    public MyRenderer(Context context,DisplayMetrics metrics) {
        this.context = context;
        this.metrics = metrics;
        
        log.d("Build.BOARD=%s",Build.BOARD);
        log.d("Build.BRAND=%s",Build.BRAND);
        log.d("Build.DEVICE=%s",Build.DEVICE);
        log.d("Build.DISPLAY=%s",Build.DISPLAY);
        log.d("Build.FINGERPRINT=%s",Build.FINGERPRINT);
        log.d("Build.HOST=%s",Build.HOST);
        log.d("Build.ID=%s",Build.ID);
        log.d("Build.MODEL=%s",Build.MODEL);
        log.d("Build.PRODUCT=%s",Build.PRODUCT);
        log.d("Build.TAGS=%s",Build.TAGS);
        log.d("Build.TIME=%s",Build.TIME);
        log.d("Build.TYPE=%s",Build.TYPE);
        log.d("Build.USER=%s",Build.USER);
        
        // ラベル生成用のペイント
        mLabelPaint.setARGB(0xff, 0xff, 0xff, 0xff);
        mLabelPaint.setAntiAlias(true);
        mLabelPaint.setTextSize(dp2px(20));
        mLabelPaint.setShadowLayer(1,0,0,0xff000000);
        
        meca1.x =0;
        meca1.z =(int)(field_back*0.8f);

        
        Meca2.list.clear();
        Tama1.list.clear();
        Tama2.list.clear();
        Expl.list.clear();
    }

    public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
    	log.d("onSurfaceCreated config=%s",config.getClass().getName());
    	
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);

        /*
         * Some one-time OpenGL initialization can be made here
         * probably based on features of this particular context
         */
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,GL10.GL_FASTEST);

        gl.glClearColor(.5f, .5f, .5f, 1);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        /*
         * Create our texture. This has to be done each time the
         * surface is created.
         */

        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);
        tri_texture_id = textures[0];
        gl.glBindTexture(GL10.GL_TEXTURE_2D, tri_texture_id);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_MAG_FILTER,GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,GL10.GL_CLAMP_TO_EDGE);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,GL10.GL_REPLACE);

        InputStream is = context.getResources().openRawResource(R.raw.robot);
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if(bitmap!=null){
    	        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
    	        bitmap.recycle();
            }
        } finally {
            try { is.close(); } catch(IOException e) {}
        }

        if (mLabels != null) {
            mLabels.shutdown(gl);
        } else {
            mLabels = new LabelMaker(true, 256, 64);
        }
        mLabels.initialize(gl);
        mLabels.beginAdding(gl);
        label_id_a = mLabels.add(gl, "A", mLabelPaint,1);
        label_id_b = mLabels.add(gl, "B", mLabelPaint,1);
        label_id_c = mLabels.add(gl, "C", mLabelPaint,1);
        mLabels.endAdding(gl);

        if (ascii_drawer != null) {
            ascii_drawer.shutdown(gl);
        } else {
            ascii_drawer = new AsciiDrawer();
        }
        ascii_drawer.initialize(gl, mLabelPaint,true);
        
        if (mSprites != null) {
        	mSprites.shutdown(gl);
        } else {
        	mSprites = new SpriteDrawer(256,256,16,16,Bitmap.Config.ARGB_4444);
        }
        mSprites.initialize(gl);
        mSprites.beginAdding(gl);
        sprite_id_box1 = mSprites.add(gl,context,R.raw.box1);
        sprite_id_box1_fill = mSprites.add(gl,context,R.raw.box1_fill);
        sprite_id_circle1 = mSprites.add(gl,context,R.raw.circle1);
        sprite_id_circle1_fill = mSprites.add(gl,context,R.raw.circle1_fill);
        sprite_meca1 = mSprites.add(gl,context,R.raw.meca1);
        sprite_meca2 = mSprites.add(gl,context,R.raw.meca2);
        sprite_tama = mSprites.add(gl,context,R.raw.tama);
        sprite_tama2 = mSprites.add(gl,context,R.raw.tama2);
        sprite_bg = mSprites.add(gl,context,R.raw.bg);
        int sprite_expl = mSprites.add(gl,context,R.raw.expl1);
        
        Tama1.sq = new int[4];
        Tama1.sq[3]=mSprites.add(gl,context,R.raw.sq0);
        Tama1.sq[2]=mSprites.add(gl,context,R.raw.sq1);
        Tama1.sq[1]=mSprites.add(gl,context,R.raw.sq2);
        Tama1.sq[0]=mSprites.add(gl,context,R.raw.sq3);

        
        arrow_sprite_id[0] = mSprites.add(gl,context,R.raw.arrowkey_off);
        for(int dir=0;dir<=7;++dir){
        	arrow_sprite_id[dir+1] = mSprites.addRot(gl,context,R.raw.arrowkey_up,dir*45);
        }
        mSprites.endAdding(gl);
        
        Meca1.sprite = new ScaledSprite(mSprites, sprite_meca1);
        Meca2.sprite = new ScaledSprite(mSprites, sprite_meca2);
        Tama1.sprite = new ScaledSprite(mSprites, sprite_tama );
        Tama2.sprite = new ScaledSprite(mSprites, sprite_tama2);
        Expl.sprite  = new ScaledSprite(mSprites, sprite_expl );
    }
    
    int mWidth;
    int mHeight;
    float field_density;
    
    public synchronized void onSurfaceChanged(GL10 gl, int w, int h) {
        log.d("onSurfaceChanged size=%dx%d,density=%f",w,h,metrics.density);
        mWidth  = w;
        mHeight = h;

        // update input rectangle
        input_list.clear();
        add_input(0,0,dp2px(128),dp2px(128),sprite_id_box1,sprite_id_box1_fill);
        add_input(-1,-1,dp2px(32),dp2px(64),sprite_id_box1,sprite_id_box1_fill);
        add_input(-1,-1-dp2px(64+16),dp2px(32),dp2px(64),sprite_id_circle1,sprite_id_circle1_fill);
        for(InputRect rect: input_list){
        	rect.bPressed = rect.bLastPress = false;
        }
        arrow_direction = 0;
        
        // ゲーム表示用のビューポートのサイズを計算する
        {
        	int arrow_height = dp2px(128);
			w = mWidth;
			h = mHeight-arrow_height;
			
			// 480x800のスクリーンの場合、方向キーを除くと横320dp,405.33dp の描画範囲が得られる。
			// キリのいいところで 320x400dp くらいが表示されるようにしたい…
			final int x_ratio = (w *0x10000/ expect_w);
			final int y_ratio = (h *0x10000/ expect_h);
			// 過剰率が少ない方にあわせて拡大
			if( x_ratio <= y_ratio ){
				field_density = w/(float)expect_w;
				gameview_w = w;
				gameview_h = (w * expect_h * 0x1000 / expect_w + 0x800)>>12; 
			}else{
				field_density = h/(float)expect_h;
				gameview_h = h;
				gameview_w = (h * expect_w * 0x1000 / expect_h + 0x800)>>12; 
			}
			gameview_x = (mWidth - gameview_w)/2;
			gameview_y = arrow_height + (mHeight-arrow_height - gameview_h)/2;
			log.d("gameview %d,%d-%d,%d density=%f",gameview_x,gameview_y,gameview_w,gameview_h,field_density);
        }
        // ゲーム表示用のプロジェクションのパラメータを計算する
        {
            // プロジェクション行列を設定する
            int ratio = (int)(0x10000* gameview_w / gameview_h );
            gameproj_left   = -ratio;
            gameproj_right  =  ratio;
            gameproj_bottom =  0x10000;
            gameproj_top    = -0x10000;
            gameproj_near   = (int)(0x10000* 0.5f);
            gameproj_far    = (int)(0x10000* 10000f);
        }
        
        Meca1.sprite.updateScale( field_density, 2.0f);
        Meca2.sprite.updateScale( field_density, 2.0f);
        Tama1.sprite.updateScale( field_density, 0.7f);
        Tama2.sprite.updateScale( field_density, 2.0f);
        Expl .sprite.updateScale( field_density, 3.0f);

    }
    // 期待する画面サイズというか縦横比率.いちおうdp単位
    final int expect_w = 320;
	final int expect_h = 400;
	
	// ビューポート範囲
    int gameview_x;
    int gameview_y;
    int gameview_w;
    int gameview_h;
    
    // プロジェクション
    int gameproj_left;
    int gameproj_right;
    int gameproj_bottom;
    int gameproj_top;
    int gameproj_near;
    int gameproj_far;
    
    float camera_y;
    
    long t_prev=0;

    // int input_height = 16*12;

    final int field_left  = -(0x10000 * expect_w /2);
    final int field_right =  (0x10000 * expect_w /2);
    final int field_front =  (0x10000 * expect_h /2);
    final int field_back  = -(0x10000 * expect_h /2);
    final int field_y = 0;
    
    Meca1 meca1 = new Meca1();
    

    Random random = new Random();

    long meca2_spawn_timer = 0;
    long meca2_spawn_limit = 1200;
    
    long tama_spawn_timer = 0;
    long tama_spawn_limit = 800;
    long tama_spawn_speed = 0x10000; 

    long tama2_spawn_last = 0;

    boolean bGameStart = false;
    boolean bGameOver = false;
    String over_telop = null;
    long nLiveTime = 0;

   
    public synchronized void onDrawFrame(GL10 gl) {
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);
        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,GL10.GL_MODULATE);

        // バッファのクリア
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        // ゲーム空間用のビューポート
        gl.glViewport(gameview_x,gameview_y,gameview_w,gameview_h);
        mProjector.setCurrentView(gameview_x, gameview_y, gameview_w, gameview_h);
/*
        // プロジェクション
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumx(
        	 gameproj_left   - (gameproj_right*meca1_x/field_right) // left
    		,gameproj_right  + (gameproj_right*meca1_x/field_right) // right
    		,gameproj_bottom - (gamepro/:*meca1_z/field_front) // bottom
    		,gameproj_top    + (gameproj_top*meca1_z/field_front) // top
    		,gameproj_near // znear
    		,gameproj_far  // zfar
        );
        mProjector.getCurrentProjection(gl);

        // カメラ位置を決定
        float cy = (field_front * gameproj_top / gameproj_near );
        float cx = meca1_x;
        float cz = meca1_z;
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl
        		, cx, -cy   , cz
        		, cx, 0.0f  , cz
        		, 0.0f, 0.0f,1.0f
        );
*/        
        // プロジェクション
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumx(
        	 gameproj_left   // left
    		,gameproj_right   // right
    		,-gameproj_bottom  // bottom openglのスクリーン座標はy+が上なので、ここで反転させる
    		,-gameproj_top     // top  openglのスクリーン座標はy+が上なので、ここで反転させる
    		,gameproj_near // znear
    		,gameproj_far  // zfar
        );
        mProjector.getCurrentProjection(gl);

        // カメラ位置を決定
        camera_y = (field_front * (gameproj_near>>12) / (gameproj_top>>12) )>>16;
     //   float cx = meca1.x/(float)0x10000;
     //   float cz = meca1.z/(float)0x10000;
     //   log.d("cy=%f,",cy);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl
        		, 0, camera_y   , 0
        		, 0, 0.0f  , 0
        		, 0.0f, 0.0f,1.0f
        );
        
        long t_now = SystemClock.elapsedRealtime();
        if(t_prev!=0){
            int  t_delta = (int)(t_now - t_prev);
            if(t_delta>1000) t_delta = 1000;
            if(t_delta<0) t_delta = 0;
            
            move(t_now,t_delta);
        }
        t_prev = t_now;
        
        if(debug){
	        gl.glPushMatrix();
	        {
		        int scale = 0x10000;
		        if( input_list.get(1).bPressed) scale = 0x40000;
		        if( input_list.get(2).bPressed) scale = 0x80000;
	
	            long time = SystemClock.uptimeMillis() % 4000L;
	            float angle = 0.090f * ((int) time);
		      //  gl.glTranslatex(tri_pos_x,tri_pos_y,0);
		        gl.glRotatef(angle, 0, 0, 1.0f);
		        gl.glScalex(scale, scale, scale);
		
		        gl.glColor4x(fixed_one,fixed_one, fixed_one, fixed_one);
		        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		        gl.glActiveTexture(GL10.GL_TEXTURE0);
		        gl.glBindTexture(GL10.GL_TEXTURE_2D, tri_texture_id);
		        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,GL10.GL_REPEAT);
		        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,GL10.GL_REPEAT);
		        mTriangle.draw(gl);
		
		        // モデルのローカル座標からスクリーン座標への変換ができるようにする
		        mProjector.getCurrentModelView(gl);
		
		        // ラベルの描画
		        mLabels.beginDrawing(gl);
		        	// 三角形の頂点にラベルを描画
			        drawTriangleLabel(gl, 0, label_id_a);
		        	drawTriangleLabel(gl, 1, label_id_b);
			        drawTriangleLabel(gl, 2, label_id_c);
		        mLabels.endDrawing(gl);
	
	        }
	        gl.glPopMatrix();
        }
        
        mSprites.beginDrawing(gl);
        {
	        Tama1.drawAll(this,gl);
        	if(bGameStart && !bGameOver) drawSpriteIn3D(gl,Meca1.sprite,meca1.x,field_y,meca1.z);
	        Meca2.drawAll(this,gl);
	        Tama2.drawAll(this,gl);
	        Expl.drawAll(this,gl);
	        
	        final int scrool_interval = 30*1000;
	        for(int i=0;i<3;++i){
		        float x = 0;
		        float y = -gameview_h * (t_now%scrool_interval)/(float)scrool_interval;
		        y += i*(gameview_h-1);
		        
		        float sx = x -gameview_x;
		        float sy = y -gameview_y;
		        float sz = 1.0f;;
		        mSprites.drawf(gl,sprite_bg,sx,sy,sz,gameview_w,gameview_h);
	        }

        }
        mSprites.endDrawing(gl);
        
        // 画面全体をビューポートに設定しなおす
        gl.glViewport(0, 0, mWidth, mHeight);
        mProjector.setCurrentView(0, 0, mWidth, mHeight);

        
        ascii_drawer.beginDrawing(gl);
	        // fpsの描画
	        draw_fps(gl);
	        // タッチ部分にテキストを描画
	      //  for(TouchInfo info : touch_list){
	      //  	int width = (int)(0.5+ascii_drawer.width(info.text));
	      //  	int y = mHeight - info.y;
	      //  	ascii_drawer.draw(gl, info.x - (width>>1), y , info.text ,0,fixed_one,0,fixed_one );
	      //  }
	        if(!bGameStart){
	        	String text = "tap to start";
	        	float width = ascii_drawer.width(text);
	        	float x = (mWidth - width)*0.5f;
	        	float y = mHeight * 0.5f;
	        	ascii_drawer.draw(gl, x, y, text,fixed_one/2,fixed_one,0x10,fixed_one);
	        }
	        if(bGameOver){
	        	String text;
	        	text = "game over";
	        	float y = mHeight * 0.5f;
	        	ascii_drawer.draw(gl
	        		,(mWidth - ascii_drawer.width(text))*0.5f
	        		,y+metrics.density*32
	        		,text
	        		,fixed_one/2,fixed_one,0,fixed_one
	        	);
	        	text = over_telop;
	        	ascii_drawer.draw(gl
	        		,(mWidth - ascii_drawer.width(text))*0.5f
	        		,y-metrics.density*32
	        		,text
	        		,fixed_one/2,fixed_one,0,fixed_one
	        	);
	        }
        ascii_drawer.endDrawing(gl);

        mSprites.beginDrawing(gl);
        int n=0;
        for(InputRect rect: input_list){
        	
        	if(rect.bEnabled){
        		int sprite_id;
        		if(n==0){
        			sprite_id = arrow_sprite_id[arrow_direction];
        		}else{
        			sprite_id = (rect.bPressed ? rect.sprite_on : rect.sprite_off);
        		}
	            mSprites.drawi(gl
            		,sprite_id
            		,rect.left
            		,rect.top
            		,rect.right -rect.left
            		,rect.bottom-rect.top
	            );
        	}
        	++n;
        }
        mSprites.endDrawing(gl);
    }


    // FPSの表示
    long pre_time;
    int fps_count;
    int fps_show;
    private void draw_fps(GL10 gl){
/*
    	++fps_count;
        long now = SystemClock.elapsedRealtime();
        if( now - pre_time >= 1000){
        	pre_time = now;
        	fps_show = fps_count;
        	fps_count=0;
        }
        if(fps_show > 0){
        	String text = String.format("dir=%d,fps=%d",arrow_direction,fps_show);
        	float numWidth = ascii_drawer.width(text);
        	float x = mWidth - numWidth;
        	ascii_drawer.draw(gl, x, 0, text,fixed_one,fixed_one/2,0,fixed_one);
        }
*/
    }

    // 三角形の頂点にラベルを表示
    void drawTriangleLabel(GL10 gl, int triangleVertex, int labelId) {
        // 三角家の頂点の座標 
        mScratch[0] = mTriangle.getX(triangleVertex);
        mScratch[1] = mTriangle.getY(triangleVertex);
        mScratch[2] = 0.0f;
        mScratch[3] = 1.0f;
        // スクリーン座標に変換, 出力は scratch[4..7]
        mProjector.project(mScratch, 0, mScratch, 4);
        // 座標からラベルの幅と高さの半分を引いた位置
        float x = mScratch[4] - mLabels.getWidth(labelId)* 0.5f -gameview_x;
        float y = mScratch[5] - mLabels.getHeight(labelId)* 0.5f -gameview_y ;
        // ラベルを描画
        mLabels.draw(gl, x, y, labelId);
    }

    // 3D空間での座標をスクリーン座標に変換して、そこにスプライトを表示する
    float[] drawSpriteIn3D_scratch = new float[]{ 0.0f,0.0f,0.0f,1.0f,0.0f,0.0f,0.0f,0.0f,};
    void drawSpriteIn3D(GL10 gl,ScaledSprite sprite,int x,int y,int z){
        gl.glPushMatrix();
        
        gl.glTranslatex(x,y,z);
        // モデルのローカル座標からスクリーン座標への変換ができるようにする
        mProjector.getCurrentModelView(gl);
        // スクリーン座標に変換, 出力は scratch[4..7]
        mProjector.project(drawSpriteIn3D_scratch, 0, drawSpriteIn3D_scratch, 4);
        
        // 座標からラベルの幅と高さの半分を引いた位置
        float sx = drawSpriteIn3D_scratch[4] - sprite.scaled_offset_x -gameview_x;
        float sy = drawSpriteIn3D_scratch[5] - sprite.scaled_offset_y -gameview_y;
        float sz = drawSpriteIn3D_scratch[6];
        sprite.drawer.drawf(gl,sprite.sprite_id,sx,sy,sz,sprite.scaled_w,sprite.scaled_h);

        gl.glPopMatrix();
    }
    static class TouchInfo extends ListWithPool.Item{
    	static ListWithPool<TouchInfo> list = new ListWithPool<TouchInfo>(3,new ItemFactory<TouchInfo>() {
			public TouchInfo create() { return new TouchInfo(); }
		});
    	int x;
    	int y;
    	// String text;
    }
    
    static String getTouchActionString(int action){
		switch(action){
		default : return "unknown action"; 
		case MotionEvent.ACTION_DOWN: return "DOWN";
		case MotionEvent.ACTION_UP: return "UP"; 
		case MotionEvent.ACTION_POINTER_DOWN: return "A non-primary pointer has gone down";
		case MotionEvent.ACTION_POINTER_UP: return "A non-primary pointer has gone up"; 
		case MotionEvent.ACTION_MOVE: return "MOVE"; 
		case MotionEvent.ACTION_CANCEL: return "CANCEL"; 
		case MotionEvent.ACTION_OUTSIDE: return "OUTSIDE"; 
		}
    	
    }
    
	// タッチイベント
	public synchronized void onTouch(MotionEvent ev){
		int action = ev.getAction() & MotionEvent.ACTION_MASK;
		int pointer_index = (ev.getAction() & MotionEvent.ACTION_POINTER_ID_MASK )>>  MotionEvent.ACTION_POINTER_ID_SHIFT;
		
		TouchInfo.list.clear();

		if(action != MotionEvent.ACTION_UP){
			bGameStart = true;
			// String action_str = getTouchActionString(action);
			//action_str = String.format(" action=(%d)%s",action,action_str);
			
			int count = ev.getPointerCount();
			for(int i=0;i<count;++i){
				if( i==pointer_index && action == MotionEvent.ACTION_POINTER_UP ) continue;
				TouchInfo info = TouchInfo.list.obtain();
				info.x = (int)(0.5+ev.getX(i));
				info.y = (int)(0.5+ev.getY(i));
/*
				info.text = String.format("t%d size=%.2f,press=%.2f%s"
						,ev.getPointerId(i)
						,ev.getSize(i)
						,ev.getPressure(i)
						,(i==pointer_index?action_str:"")
				);
*/
			}
		}
		
		int n=0;
		for(InputRect rect : input_list){
			boolean bPressed_prev =   rect.bPressed;
			rect.bPressed = false;
			int x=0,y=0;
			if(rect.bEnabled){
				for(int i=TouchInfo.list.size()-1;i>=0;--i){
					TouchInfo info = TouchInfo.list.get(i);
					if(info==null) continue;

					y = mHeight - info.y;
		        	if( y >= rect.top && y < rect.bottom ){
						for(int j=TouchInfo.list.size()-1;j>=0;--j){
							TouchInfo info2 = TouchInfo.list.get(j);
							if(info2==null) continue;

		    	        	if( info2.x >= rect.left && info2.x < rect.right ){
		    	        		rect.bPressed = true;
		    	        		x = info2.x;
		    	        		break;
		    	        	}
		        		}
		        	}
		        	if(rect.bPressed) break;
		        }
			}
			if( rect.bPressed && ! bPressed_prev ){
				rect.bLastPress = true;
			}
			if(n==0){
				if(!rect.bPressed){
					arrow_direction = 0;
				}else{
					x = x - ((rect.left+rect.right)>>1);
					y = y - ((rect.top+rect.bottom)>>1);
					if( x*x+y*y < 16*16 ){
						arrow_direction = 0;
					}else{
						int dir = (int)(0.5+ ((float)Math.atan2(x,y)) *  ( 4096/(Math.PI*2)));
						dir += (4096/8/2);
						if(dir<0) dir += 4096;
						arrow_direction = 1+ dir/(4096/8);
					}
				}
			}
			++n;
		}
		return;

	}

   
    void move(long t_now,int t_delta){
	    Expl.moveAll(this,t_delta);

	    // プレイヤー機の移動
	    if( bGameStart && !bGameOver){
	    	meca1.move(this,t_delta);
	    	nLiveTime += t_delta;
	    }
	    if( bGameOver){
	    	int speed = random.nextInt(0x100 * 4000);
	    	int delay =0;
    		Expl.create(this
    			,meca1.x    	 + (random.nextInt(0x10000)-0x8000)*32
    			,meca1.z    	 + (random.nextInt(0x10000)-0x8000)*32
    			,delay
    			,speed
    			,0
    		);
    		speed += random.nextInt(3000*0x100);
	    }
	    
	    // meca2の移動
	    Meca2.moveAll(this,t_delta);

	    // tama2の移動
	    Tama2.moveAll(this,t_delta);

	    // tama2の生成
	    if( bGameStart && !bGameOver){
		    if( t_now >= tama2_spawn_last + 30 && Tama2.list.active_count <4){
		    	tama2_spawn_last = t_now;
		    	Tama2.create(this);
		    }
	    }

	    // meca2の生成
	    if(bGameStart){
		    meca2_spawn_timer += t_delta;
		    while( meca2_spawn_timer >= meca2_spawn_limit ){
		    	meca2_spawn_timer -= (meca2_spawn_limit*(random.nextInt(0x10000)+0x8000))>>16;
		    	Meca2.create(this);
		    }
	    }
	    
	    // ヒットチェック
	    {
	    	final int x_width = (Tama2.sprite.field_w + Meca2.sprite.field_w )>>1;
	    	final int z_width = (Tama2.sprite.field_h + Meca2.sprite.field_h )>>1;

    		for(int j=Meca2.list.size()-1;j>=0;--j){
    			Meca2 target = Meca2.list.get(j);
	    		if(target==null) continue;

	    		for(int i=Tama2.list.size()-1;i>=0;--i){
		    		Tama2 item = Tama2.list.get(i);
		    		if(item==null) continue;
		    		// 命中判定
		    		if( item.x - target.x >  x_width
		        	||  item.x - target.x < -x_width
		        	||  item.z - target.z >  z_width
			        ||  item.z - target.z < -z_width
			        ) continue;

		    		// ショットを消す
		        	Tama2.list.unmark(item);
		        	Expl.create(this
		        		,(item.x+target.x)/2 +16*(random.nextInt(0x10000)-0x8000)
		        		,(item.z+target.z)/2 +16*(random.nextInt(0x10000)-0x8000)
		        		,0
		        		,0
		        		,1
		        	);
		        	// ダメージと破壊
		        	target.hp -= Tama2.pow;
		        	if( target.hp <= 0){
			        	Meca2.list.unmark(target);
		        		int speed = random.nextInt(0x100 * 40000);
			        	for(int delay=0;delay<=2000; delay=1+delay*2,speed /= 2){
			        		Expl.create(this
			        			,target.x    	 + (random.nextInt(0x10000)-0x8000)*32
			        			,target.z    	 + (random.nextInt(0x10000)-0x8000)*32
			        			,delay
			        			,speed
			        			,0
			        		);
			        		speed += random.nextInt(3000*0x100);
			        	}
			        	break;
		        	}
		        }
		    }
	    }
	    Tama2.list.sweep();
	    Meca2.list.sweep();

	    // tamaの移動
	    Tama1.moveAll(this,t_delta);
	    // tamaの生成
	    if(bGameStart){
		    tama_spawn_timer += (t_delta * tama_spawn_speed)>>16;
		    tama_spawn_speed += t_delta;
		    final int speed = (Meca1.sprite.field_w );
		    while(tama_spawn_timer >= tama_spawn_limit && Meca2.list.size() > 0){
		    	tama_spawn_timer -= (tama_spawn_limit*(random.nextInt(0x100)+0x80))>>8;
		    	
		    	int idx = random.nextInt(Meca2.list.size());
		    	Meca2 parent = Meca2.list.get(idx);
		    	Tama1 tama = Tama1.create(this);
		    	tama.x = parent.x;
		    	tama.z = parent.z;
		    	double d = Math.atan2( meca1.z - tama.z,meca1.x - tama.x  );
		    	d+= (random.nextDouble()-0.5) * Math.PI/4;
		    	tama.dx = (int)(Math.cos(d)* speed);
		    	tama.dz = (int)(Math.sin(d)* speed);
		    	
		    }
	    }
	    Tama1.list.sweep();
	    
	    // ヒットチェック
	    if( bGameStart && !bGameOver){
	    	final int x_width = (Meca1.sprite.field_w )>>2;
	    	final int z_width = (Meca1.sprite.field_h )>>2;
	    	
	    	{
    			Meca1 target = meca1;

	    		for(int i=Tama1.list.size()-1;i>=0;--i){
		    		Tama1 item = Tama1.list.get(i);
		    		if(item==null) continue;
		    		// 命中判定
		    		if( item.x - target.x >  x_width
		        	||  item.x - target.x < -x_width
		        	||  item.z - target.z >  z_width
			        ||  item.z - target.z < -z_width
			        ) continue;

		    		// ショットを消す
		        	Tama1.list.unmark(item);
		        	Expl.create(this
		        		,(item.x+target.x)/2
		        		,(item.z+target.z)/2
		        		,0
		        		,0
		        		,1
		        	);
		        	// ダメージと破壊
		        	bGameOver = true;
		        	over_telop = String.format("score: %dmin %dsec"
		        			,nLiveTime/(60*1000)
		        			,(nLiveTime/1000)%60
		        	);
		        	break;
		        }
		    }
	    }
	    
	    
    	// エフェクトの消去
	    Expl.list.sweep();
    }
    
    synchronized void onResume(){
    	
    }
    synchronized void onPause(){
    	
    }
}

class Triangle {
    private FloatBuffer mFVertexBuffer;
    private FloatBuffer mTexBuffer;
    private ShortBuffer mIndexBuffer;

    // 0,0を中心とした三角形
    private final static int VERTS = 3;
    private final static float[] sCoords = {
            // X, Y, Z
            -0.5f, -0.25f, 0,
             0.5f, -0.25f, 0,
             0.0f,  0.559016994f, 0
    };
    
    // 指定した頂点の座標を返す
    public float getX(int vertex) {
        return sCoords[3*vertex];
    }

    // 指定した頂点の座標を返す
    public float getY(int vertex) {
        return sCoords[3*vertex+1];
    }

	public Triangle() {

        // Buffers to be passed to gl*Pointer() functions
        // must be direct, i.e., they must be placed on the
        // native heap where the garbage collector cannot
        // move them.
        //
        // Buffers with multi-byte datatypes (e.g., short, int, float)
        // must have their byte order set to native order

        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
        vbb.order(ByteOrder.nativeOrder());
        mFVertexBuffer = vbb.asFloatBuffer();

        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexBuffer = tbb.asFloatBuffer();

        ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();

        for (int i = 0; i < VERTS; i++) {
            for(int j = 0; j < 3; j++) {
                mFVertexBuffer.put(sCoords[i*3+j]);
            }
        }

        for (int i = 0; i < VERTS; i++) {
            for(int j = 0; j < 2; j++) {
                mTexBuffer.put(sCoords[i*3+j] * 2.0f + 0.5f);
            }
        }

        for(int i = 0; i < VERTS; i++) {
            mIndexBuffer.put((short) i);
        }

        mFVertexBuffer.position(0);
        mTexBuffer.position(0);
        mIndexBuffer.position(0);
    }

    public void draw(GL10 gl) {
        gl.glFrontFace(GL10.GL_CW );
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
        gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS,GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
    }
}

class ScaledSprite{
	SpriteDrawer drawer;
	int sprite_id;
	float scaled_w;
	float scaled_h;
	float scaled_offset_x;
	float scaled_offset_y;
	int field_w;
	int field_h;
	ScaledSprite(SpriteDrawer drawer,int sprite_id){
		this.drawer = drawer;
		this.sprite_id = sprite_id;
	}
	void updateScale(float density,float scale){
		Sprite item = drawer.getItem(sprite_id);
		scaled_w = item.show_w * density * scale;
		scaled_h = item.show_h * density * scale;
		scaled_offset_x = scaled_w/2;
		scaled_offset_y = scaled_h/2;
		field_w= (int)(item.show_w * scale *0x10000);
		field_h= (int)(item.show_h * scale *0x10000);
	}
}
class Meca1{
	static ScaledSprite sprite;
	int x;
	int z;
	void move(MyRenderer env,int t_delta){
	    // meca1の移動
	    if(env.arrow_direction!=0){
	    	int spd = (0x3000 * (int)t_delta);
	    	int spd7 = (int)(spd*0.7f);
	    	switch(env.arrow_direction){
	    	case 1: z += spd;             break;
	    	case 2: z += spd7; x += spd7; break;
	    	case 3:            x += spd;  break;
	    	case 4: z -= spd7; x += spd7; break;
	    	case 5: z -= spd;             break;
	    	case 6: z -= spd7; x -= spd7; break;
	    	case 7:            x -= spd;  break;
	    	case 8: z += spd7; x -= spd7; break;
	    	}
	    }
	    int size = sprite.field_w >>1;
	    x = x < env.field_left+size ? env.field_left+size : x > env.field_right-size ? env.field_right-size:x;
	    z = z < env.field_back+size ? env.field_back+size : z > env.field_front-size ? env.field_front-size:z;
	}
}


class Meca2 extends ListWithPool.Item{
	static ScaledSprite sprite;
    static ListWithPool<Meca2> list = new ListWithPool<Meca2>(100,new ItemFactory<Meca2>() {
		public Meca2 create() { return new Meca2(); }
	});

    int x;
	int z;
	int dz;
	int target_z;
	int hp = 256;

	static Meca2 create(MyRenderer env){
    	Meca2 item = list.obtain();
    	item.z = env.field_front +sprite.field_h/2 -1;
    	item.x = env.field_left +sprite.field_w +env.random.nextInt(env.field_right-env.field_left-2*sprite.field_w);
    	item.target_z = env.field_back + sprite.field_h + env.random.nextInt(sprite.field_h*3);
    	item.dz = -40000*256;
    	item.hp = 256;
    	return item;
	}	    	

    static void moveAll(MyRenderer env,int t_delta){
    	int zmax = env.field_front + (sprite.field_h>>1);
    	final int dowm_limit = -1024000/2;
    	for(int i=list.size()-1;i>=0;--i){
    		Meca2 item = list.get(i);
    		if(item==null) continue;

    		item.z += (item.dz * t_delta)>>8;
	    	if( item.dz < 0 ){
	    		if(item.z <= item.target_z){
	    			item.dz = 1;
	    		}else if( item.z <= env.meca1.z + sprite.field_h*9 ){
	    			item.dz += t_delta*16240;
	    			if(item.dz>dowm_limit) item.dz=dowm_limit;
	    		}
	    	}else{
	    		item.dz += 10240 * t_delta;
	    		if(item.z >= zmax){
	    			list.unmark(item);
	    		}
	    	}
	    }
    }


    static void drawAll(MyRenderer env,GL10 gl){
    	for(int i=list.size()-1;i>=0;--i){
    		Meca2 item = list.get(i);
    		if(item==null) continue;
        	env.drawSpriteIn3D(gl,sprite,item.x,env.field_y,item.z);
        }
    }
}

class Tama1 extends ListWithPool.Item{
	static ScaledSprite sprite;
    static ListWithPool<Tama1> list = new ListWithPool<Tama1>(1000,new ItemFactory<Tama1>() {
		public Tama1 create() { return new Tama1(); }
	});

    int x;
	int z;
	int dx;
	int dz;
	static int[] sq;
	int t;

	static Tama1 create(MyRenderer env){
    	Tama1 item = list.obtain();
    	item.t=0;
    	return item;
	}

    static void moveAll(MyRenderer env,int t_delta){
    	int xmin = env.field_left  - (sprite.field_w>>1);
    	int xmax = env.field_right + (sprite.field_w>>1);
    	int zmin = env.field_back  - (sprite.field_h>>1);
    	int zmax = env.field_front + (Meca1.sprite.field_h>>1);
        for(int i=list.size()-1;i>=0;--i){
        	Tama1 item =list.get(i);
        	if(item==null) continue;
    		item.t += t_delta;
        	item.x += (item.dx * t_delta)>>8;
        	item.z += (item.dz * t_delta)>>8;
        	if( item.x <= xmin ||  item.x >= xmax
        	||  item.z <= zmin ||  item.z >= zmax
        	){
        		list.unmark(item);
        		continue;
        	}
        }
    }

    static void drawAll(MyRenderer env,GL10 gl){
    	for(int i=list.size()-1;i>=0;--i){
    		Tama1 item = list.get(i);
    		if(item==null) continue;
    		sprite.sprite_id = sq[ (item.t>>6)&3];
        	env.drawSpriteIn3D(gl,sprite,item.x,env.field_y,item.z);
        }
    }
}
class Tama2 extends ListWithPool.Item {
	static ScaledSprite sprite;
    static ListWithPool<Tama2> list = new ListWithPool<Tama2>(4,new ItemFactory<Tama2>() {
		public Tama2 create() { return new Tama2(); }
	});
	
	static int dz = 0x100 * 80000;
	static int pow = 64;

	int x;
	int z;


    static Tama2 create(MyRenderer env){
    	Tama2 item = list.obtain();
    	item.x = env.meca1.x;
    	item.z = env.meca1.z + sprite.field_h - 0x10000 * 8;
    	return item;
	}
	static void moveAll(MyRenderer env,int t_delta){
    	int xmin = env.field_left  - (sprite.field_w>>1);
    	int xmax = env.field_right + (sprite.field_w>>1);
    	int zmin = env.field_back  - (sprite.field_h>>1);
    	int zmax = env.field_front + (sprite.field_h>>1);
    	for(int i=0,e=list.size();i<e;++i){
    		Tama2 item = list.get(i);
    		if(item==null) continue;
	    	item.z += (dz * t_delta)>>8;
	    	if( item.x <= xmin || item.x >= xmax
	    	||  item.z <= zmin || item.z >= zmax
	    	){
	    		list.unmark(item);
	    		continue;
	    	}
    	}
    }
    static void drawAll(MyRenderer env,GL10 gl){
    	for(int i=list.size()-1;i>=0;--i){
    		Tama2 item = list.get(i);
    		if(item==null) continue;
    		
        	env.drawSpriteIn3D(gl,sprite,item.x,env.field_y,item.z);
        }
    }
}
class Expl extends ListWithPool.Item {
	static ScaledSprite sprite;
    static ListWithPool<Expl> list = new ListWithPool<Expl>(128,new ItemFactory<Expl>() {
		public Expl create() { return new Expl(); }
	});
	
	static int speed = 0x100 * 80000;

	int x;
	int z;
	int dx;
	int dz;
	int t;
	int expire;
	float scale_add;
	float scale_delta;

    static Expl create(MyRenderer env,int x,int z,int delay,int speed,int expire){
    	Expl item = list.obtain();
    	item.x = x;
    	item.z = z;
    	double w = env.random.nextDouble()*(Math.PI*2);
    	item.dx = (int)( speed * Math.cos(w) );
    	item.dz = (int)( speed * Math.sin(w) );
    	item.t = -delay;
    	item.expire = expire;
    	item.scale_add = sprite.scaled_w * (env.random.nextFloat()-0.5f);
    	item.scale_delta = item.scale_add; //item.scale_add *  (-8/(float)expl_laptime);
    	return item;
	}
	static void moveAll(MyRenderer env,int t_delta){
    	int xmin = env.field_left  - (sprite.field_w>>1);
    	int xmax = env.field_right + (sprite.field_w>>1);
    	int zmin = env.field_back  - (sprite.field_h>>1);
    	int zmax = env.field_front + (sprite.field_h>>1);
    	for(int i=0,e=list.size();i<e;++i){
    		Expl item = list.get(i);
    		if(item==null) continue;
	    	item.t += t_delta;
	    	if(item.t<0) continue;
	    	if( item.expire >0 && item.t >= item.expire ){
	    		list.unmark(item);
	    		continue;
	    	}
    		item.x += (item.dx*t_delta)>>8;
    		item.z += (item.dz*t_delta)>>8;
	    	if( item.x <= xmin || item.x >= xmax
	    	||  item.z <= zmin || item.z >= zmax
	    	){
	    		list.unmark(item);
	    		continue;
	    	}
    	}
    }
	
	static int[] color_argb = new int[]{
    	0xff,0xff,0xff,0xff,
    	0xff,0xff,0xff,0xa6,
    	0xff,0xff,0xff,0x5b,
    	0xff,0xff,0xff,0x02,
    	0xff,0xff,0xe6,0x02,
    	0xff,0xff,0xd3,0x02,
    	0xff,0xff,0xb3,0x02,
    	0xff,0xff,0x91,0x02,
    	0xff,0xff,0x70,0x00,
    	0xff,0xff,0x52,0x00,
    	0xff,0xff,0x2e,0x00,
    	0xff,0xb5,0x21,0x00,
    	0xff,0x81,0x17,0x00,
    	0xff,0x47,0x0d,0x00,
    	0xff,0x2c,0x2c,0x2c,
    	0xc0,0x2c,0x2c,0x2c,
    	0x80,0x2c,0x2c,0x2c,
    	0x40,0x2c,0x2c,0x2c,
    	0x20,0x2c,0x2c,0x2c,
    	0x00,0x2c,0x2c,0x2c,
    };
	static final int color_count = color_argb.length/4;
	static final int expl_laptime = 2000;
	static{
		for(int i=color_argb.length-1;i>=0;--i){
			color_argb[i] = color_argb[i]*0x10000 /255; 
		}
	}
	
    static void drawAll(MyRenderer env,GL10 gl){
    	for(int i=list.size()-1;i>=0;--i){
    		Expl item = list.get(i);
    		if(item==null) continue;
    		int t = item.t;
    		if(t<0) continue;
    		int n = color_count * t / expl_laptime;
    		if(n>=color_count){
    			list.unmark(item);
    			continue;
    		}
    		n<<=2;
    		gl.glColor4x(color_argb[n+1], color_argb[n+2], color_argb[n+3], color_argb[n+0]);
            gl.glPushMatrix();
            
            gl.glTranslatex(item.x,env.field_y,item.z);
            // モデルのローカル座標からスクリーン座標への変換ができるようにする
            env.mProjector.getCurrentModelView(gl);
            // スクリーン座標に変換, 出力は scratch[4..7]
            env.mProjector.project(env.drawSpriteIn3D_scratch, 0, env.drawSpriteIn3D_scratch, 4);
            
            // 座標からラベルの幅と高さの半分を引いた位置
            float scale = sprite.scaled_w + item.scale_add + item.scale_delta * (item.t /(float)expl_laptime);
            float offset = scale * 0.5f;
            float sx = env.drawSpriteIn3D_scratch[4] - offset -env.gameview_x;
            float sy = env.drawSpriteIn3D_scratch[5] - offset -env.gameview_y;
            float sz = env.drawSpriteIn3D_scratch[6];
            sprite.drawer.drawf(gl,sprite.sprite_id,sx,sy,sz,scale,scale);

            gl.glPopMatrix();
        }
		gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
    }
    
    

}


