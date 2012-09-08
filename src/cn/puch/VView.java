package cn.puch;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 负责游戏显示
 * @author puch
 *
 */
public class VView extends SurfaceView implements SurfaceHolder.Callback
{
	private CRun c;
	private Thread aCThread;
	private int clipSize;
	private SurfaceHolder aSurfaceHolder=null;
	
	public boolean isViewReady() {
		if (this.c==null) return false;
		return true;
	}
	
	public VView(Context context) {
		super(context);
	}

	public VView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0);
	}
	
	public VView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}
	
	private void init(Context context, AttributeSet attrs, int defStyle) {
		TypedArray a=context.obtainStyledAttributes(attrs, R.styleable.VView);
		clipSize=a.getInt(R.styleable.VView_clipSize, 32);
		a.recycle();
		this.getHolder().addCallback(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public synchronized void surfaceCreated(SurfaceHolder holder) {
		// 这时候才算是这个VView初始化完毕, 可以启动线程,注册侦听器
		Log.i("p", "surfaceCreated");
		this.aSurfaceHolder=holder;
		// Controller中的线程主要用来刷屏, 所以只有在得到surfaceholder之后才能启动线程;
		notify();

	}

	//用到wait()必须synchronized
	public synchronized SurfaceHolder getSurfaceHolder() {
		if (aSurfaceHolder==null) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return aSurfaceHolder;
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	public void setController(CRun c) {
		this.c=c;
		
	}
}
