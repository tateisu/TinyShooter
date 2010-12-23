package jp.juggler.TinyShooter;

import javax.microedition.khronos.opengles.GL10;

import jp.juggler.util.LogCategory;

import android.graphics.Paint;

public class AsciiDrawer {
	static final LogCategory log=new LogCategory("AsciiDrawer");

    static final int char_start = 32;
    static final int char_end   = 127;
    static final int char_count = (char_end-char_start);
    int[] mWidth = new int[char_count];
    int[] mLabelId = new int[char_count];
	LabelMaker mLabelMaker;

    public AsciiDrawer(){
        mLabelMaker = null;
    }
    public void shutdown(GL10 gl) {
        mLabelMaker.shutdown(gl);
        mLabelMaker = null;
    }
    public void initialize(GL10 gl, Paint paint,boolean fullcolor) {
    	StringBuilder sb = new StringBuilder();
    	for(char c=char_start;c<char_end;++c){
    		sb.append(c);
    	}
    	String test_str = sb.toString();
    	
        mLabelMaker = new LabelMaker(fullcolor
        	, (int)(0.5 + paint.measureText(test_str) ) + char_count*2
        	, (int)(0.5 + paint.getFontSpacing() )
        );
        mLabelMaker.initialize(gl);
        mLabelMaker.beginAdding(gl);
        char[] tmp = new char[1];
        for(char c=char_start;c<char_end;++c){
        	tmp[0]=c;
        	int id = mLabelMaker.add(gl,new String(tmp,0,1), paint,1);
            mLabelId[c-char_start] = id;
            float width = mLabelMaker.getWidth(id);
            mWidth[c-char_start] = (int) Math.ceil(width);
        }
        mLabelMaker.endAdding(gl);
    }

    public void beginDrawing(GL10 gl){
        mLabelMaker.beginDrawing(gl);
    }
    public void endDrawing(GL10 gl){
        mLabelMaker.endDrawing(gl);
    }
    
    // テキストを描画する 
    // 前後に  beginDrawing,endDrawing を呼び出すこと
    // rgba は 固定し小数点で、0x10000 を指定すると 1.0 に相当する  
    public void draw(GL10 gl, float x, float y,String text,int r,int g,int b,int a) {
    	if(text==null) text="null";
    	gl.glColor4x(r, g, b, a);
    	
        for(int i=0,e=text.length();i<e;++i){
            char c = text.charAt(i);
            if( c == '\t'){
            	x += mWidth[char_start*8];
            }else{
                if(c < char_start || c>= char_end) c= '?';
                int idx = c - char_start;
                mLabelMaker.draw(gl, x, y, mLabelId[idx]);
            	x += mWidth[idx]-2;
            }
        }
    }
    
    // テキストの表示幅を調べる
    public float width(String text) {
    	if(text==null) text="null";

    	float width = 0.0f;
        for(int i=0,e=text.length();i<e;++i){
            char c = text.charAt(i);
            if( c== '\t'){
            	width += mWidth[char_start*8];
            }else{
                if(c < char_start || c>= char_end) c= '?';
            	width += mWidth[c -char_start]-2;
            }
        }
        return width;
    }
}
