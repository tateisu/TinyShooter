package jp.juggler.TinyShooter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.opengl.GLUtils;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import jp.juggler.util.LogCategory;

public class SpriteDrawer {
	static final LogCategory log= new LogCategory("SpriteDrawer");

    /**
     * Find the smallest power of two >= the input value.
     * (Doesn't work for negative numbers.)
     */
    static int roundUpPower2(int x) {
    	if( x <= 0 ) return 1;
    	if( x > 0x40000000 ) return 0x40000000;
    	int n =1;
    	while( n < x ) n<<=1;
    	return n;
    }
	
    Bitmap.Config texture_config;
    Bitmap mBitmap;
    Canvas mCanvas;
    Paint mPaint;
    int mTextureID;

    ArrayList<Sprite> item_map = new ArrayList<Sprite>();
    

    int mState;
    static final int STATE_NEW = 0;
    static final int STATE_INITIALIZED = 1;
    static final int STATE_ADDING = 2;
    static final int STATE_DRAWING = 3;

    static class Sprite {
        int[] crop;
    	int show_w;
    	int show_h;
        Sprite(int x,int y,int w,int h,int show_w,int show_h){
        	crop = new int[]{ x,y,w,h};
        	this.show_w =show_w;
        	this.show_h = show_h;
        }
    }

    /**
     * Create a label maker
     * or maximum compatibility with various OpenGL ES implementations,
     * the strike width and height must be powers of two,
     * We want the strike width to be at least as wide as the widest window.
     *
     * @param fullColor true if we want a full color backing store (4444),
     * otherwise we generate a grey L8 backing store.
     * @param strikeWidth width of strike
     * @param strikeHeight height of strike
     */
    int texture_width;
    int texture_height;
    int sprite_width;
    int sprite_height;
    public SpriteDrawer(
    	 int texture_width
    	,int texture_height
    	,int sprite_width
    	,int sprite_height
 		,Bitmap.Config texture_config
    ){
    	this.texture_width = roundUpPower2(texture_width);
    	this.texture_height = roundUpPower2(texture_height);
    	this.sprite_width = sprite_width;
    	this.sprite_height = sprite_height;
    	this.texture_config = texture_config;
    	
        mPaint = new Paint();
        mPaint.setARGB(0xff, 0xff, 0xff, 0xff);
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);

        
        mState = STATE_NEW;
    }

    /**
     * Call to initialize the class.
     * Call whenever the surface has been created.
     *
     * @param gl
     */
    public void initialize(GL10 gl) {
        mState = STATE_INITIALIZED;
        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);

        // Use Nearest for performance.
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,GL10.GL_CLAMP_TO_EDGE);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,GL10.GL_REPLACE);
    }

    /**
     * Call when the surface has been destroyed
     */
    public void shutdown(GL10 gl) {
        if ( gl != null) {
            if (mState > STATE_NEW) {
                int[] textures = new int[1];
                textures[0] = mTextureID;
                gl.glDeleteTextures(1, textures, 0);
                mState = STATE_NEW;
            }
        }
    }

    void checkState(int oldState, int newState) {
        if (mState != oldState) throw new IllegalArgumentException("Can't call this method now.");
        mState = newState;
    }

    /**
     * Call before adding labels. Clears out any existing labels.
     *
     * @param gl
     */
    public void beginAdding(GL10 gl) {
        checkState(STATE_INITIALIZED, STATE_ADDING);
        mBitmap = Bitmap.createBitmap(texture_width,texture_height, texture_config);
        mBitmap.eraseColor(0);
        mCanvas = new Canvas(mBitmap);
        item_map.clear();
    }

    /**
     * Call to end adding labels. Must be called before drawing starts.
     *
     * @param gl
     */
    public void endAdding(GL10 gl) {
        checkState(STATE_ADDING, STATE_INITIALIZED);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
        // Reclaim storage used by bitmap and canvas.
        mBitmap.recycle();
        mBitmap = null;
        mCanvas = null;
    }

    public int add(GL10 gl, Context context,int resid) {
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inScaled = false;
    	options.inDensity = 0;
    	options.inTargetDensity = 0;
    	
    	Bitmap image = BitmapFactory.decodeResource(context.getResources(),resid,options );
    	int r = add(gl,image,0);
    	image.recycle();
        return r;
    }
    public int addRot(GL10 gl, Context context,int resid,int rotate) {
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inScaled = false;
    	options.inDensity = 0;
    	options.inTargetDensity = 0;
    	Bitmap image = BitmapFactory.decodeResource(context.getResources(),resid,options );
    	int r = add(gl,image,rotate);
    	image.recycle();
        return r;
    }

    public int add( GL10 gl ,Bitmap image ,int rotate){
        checkState(STATE_ADDING, STATE_ADDING);
        int image_w = image.getWidth();
        int image_h = image.getHeight();
        log.d("add image: %d,%d",image_w,image_h);
        
        int n = item_map.size();
        int x_step = this.texture_width / this.sprite_width;
        int x = this.sprite_width * (n % x_step);
        int y = this.sprite_height * (n / x_step);
        if( y >= this.texture_height - this.sprite_height ){
        	 throw new IllegalArgumentException(String.format("Out of texture space. n=%d",n));
        }
        int w = image_w;
        if(w> this.sprite_width ) w = this.sprite_width;
        int h = image_h;
        if(w> this.sprite_height ) w = this.sprite_height;

        RectF src = new RectF(0,0,image_w,image_h);
        RectF dst = new RectF(x,y,x+w,y+h);
    	Matrix m2 = new Matrix();
    	m2.setRectToRect( src,dst, Matrix.ScaleToFit .FILL );
    	m2.preScale(1, -1,image_w/2,image_h/2);
    	m2.preRotate (rotate,image_w/2,image_h/2);
    	mCanvas.drawBitmap(image, m2, mPaint);
        
        item_map.add(new Sprite(x,y,w,h,image.getWidth(),image.getHeight()));
        return n;
    }

    /////////////////////////////////////////////////////////////////////

    public Sprite getItem(int n){
    	return item_map.get(n);
    }

    /**
     * Begin drawing labels. Sets the OpenGL state for rapid drawing.
     *
     * @param gl
     * @param viewWidth
     * @param viewHeight
     */
    public void beginDrawing(GL10 gl) {
        checkState(STATE_INITIALIZED, STATE_DRAWING);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glAlphaFunc(GL10.GL_GREATER,0.1f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_ALPHA_TEST);
        gl.glDepthFunc(GL10.GL_LESS);
        
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
    }
    
    /**
     * Ends the drawing and restores the OpenGL state.
     *
     * @param gl
     */
    public void endDrawing(GL10 gl) {
        checkState(STATE_DRAWING, STATE_INITIALIZED);
        gl.glDisable(GL10.GL_BLEND);
    }
    
    public void draw(GL10 gl, int idx,int x, int y ){
        checkState(STATE_DRAWING, STATE_DRAWING);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        Sprite item = item_map.get(idx);

        // テクスチャのどの部分を使うか指定する
        ((GL11)gl).glTexParameteriv(
        	 GL10.GL_TEXTURE_2D
        	,GL11Ext.GL_TEXTURE_CROP_RECT_OES
        	,item.crop // crop範囲
        	,0
        );
        // テクスチャの内容を画面に描画する
        ((GL11Ext)gl).glDrawTexiOES(
        	 x // x
        	,y // y
        	,0 // z (<= 1.0)
        	,item.show_w  // width
        	,item.show_h  // height
        );
    }
    public void drawi(GL10 gl,int idx, int x, int y ,int w,int h){
        checkState(STATE_DRAWING, STATE_DRAWING);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        Sprite item = item_map.get(idx);

        // テクスチャのどの部分を使うか指定する
        ((GL11)gl).glTexParameteriv(
        	 GL10.GL_TEXTURE_2D
        	,GL11Ext.GL_TEXTURE_CROP_RECT_OES
        	,item.crop // crop範囲
        	,0
        );
        // テクスチャの内容を画面に描画する
        ((GL11Ext)gl).glDrawTexiOES(
        	 x // x
        	,y // y
        	,0 // z (<= 1.0)
        	,w // width
        	,h // height
        );
    }
    public void drawf(GL10 gl,int idx, float x, float y ,float z,float w,float h){
        checkState(STATE_DRAWING, STATE_DRAWING);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        Sprite item = item_map.get(idx);

        // テクスチャのどの部分を使うか指定する
        ((GL11)gl).glTexParameteriv(
        	 GL10.GL_TEXTURE_2D
        	,GL11Ext.GL_TEXTURE_CROP_RECT_OES
        	,item.crop // crop範囲
        	,0
        );
        // テクスチャの内容を画面に描画する
        ((GL11Ext)gl).glDrawTexfOES(
        	 x // x
        	,y // y
        	,z // z
        	,w // width
        	,h // height
        );
    }
}
