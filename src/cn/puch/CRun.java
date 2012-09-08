package cn.puch;

import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnKeyListener;

/**
 * 负责游戏整体协调控制
 * @author puch
 *
 */
public class CRun implements Runnable, OnKeyListener
{
	// 控制线程是否继续运行
	private volatile boolean rFlag=false;
	private volatile boolean threadSuspended=false;
	private VView v = null;
	private MLogic m = null;
	// 系统上下文
	public Context osc = null;
	public Resources osr = null;
	private SurfaceHolder aSurfaceHolder;
	// 主内存画布, 所有要绘制的内容都应先绘制在这个画布上面;
	private Bitmap aBitmap;
	// 游戏逻辑可以访问的绘图区域
	private Bitmap gamePad;
	private Bitmap bt;
	private Random rnd=new Random();
	// 整个背景画布的大小
	private int bgSize = 480;
	// 设备屏幕的大小
	private int SCREEN_WIDTH=320;
	private int SCREEN_HEIGHT=480;
	private Rect physicalDisplay;
	private Rect validArea;
	// gamePad的大小
	private int gamePadSize=320;
	private Point p0=new Point(0,0);
	private SensorManager aSensorManager;
//	private OSListener aOSListener;
	private Thread aMThread;
	private Thread aCThread;
	Handler aHandler;
	private OSListener aOSListener;
	
	public boolean isControllerReady() {
		if (v.requestFocus()==false) return false; 
		return true;
	}
	
	public CRun(VView v, EatMe main) {
		this.v=v;
		this.osc=v.getContext();
		this.osr=v.getResources();
		this.aHandler=new Handler(main);
		
		//get displya metric
		DisplayMetrics metrics = new DisplayMetrics();
		main.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		physicalDisplay=new Rect(0, 0 ,metrics.widthPixels, metrics.heightPixels);
		if (bgSize<SCREEN_WIDTH ||bgSize<SCREEN_HEIGHT) Log.e("p", "bgSize is too small");
		validArea=new Rect((bgSize-SCREEN_WIDTH)/2,0,(bgSize-SCREEN_WIDTH)/2+SCREEN_WIDTH, SCREEN_HEIGHT);
		// assert bgSize is big enough
		bt=BitmapFactory.decodeResource(osc.getResources(), R.drawable.sa);
		// aBitmap设置为bgSize见方
		aBitmap=Bitmap.createBitmap(bgSize, bgSize, Config.ARGB_8888);
		aBitmap.setDensity(160);
		// gamePad for MLogic
		gamePad=Bitmap.createBitmap(gamePadSize, gamePadSize, Config.ARGB_8888);
		gamePad.setDensity(160);
//		aCanvas=new Canvas(aBitmap);
//		aCanvas.translate((bgSize-SCREEN_WIDTH)/2, 0);
		// 获得传感器
		aSensorManager= (SensorManager) osc.getSystemService(osc.SENSOR_SERVICE);
		aOSListener=new OSListener();
	}
	
	public Bitmap getGamePad() {
		return gamePad;
	}
	// 开始游戏
	public synchronized boolean setStart() {
		if (isStopped()) {
		aSensorManager.registerListener(aOSListener, aSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
		v.setOnKeyListener(this);
		// 启动逻辑线程
		aMThread=new Thread(m);
		aMThread.start();
		aCThread=new Thread(this);
		aCThread.start();
		} else {
			return false;
		}
		return true;
	}
	
	public void setSuspend() {
		// 因为目前只为逻辑线程刷屏, 所以直接挂起逻辑线程
		m.setSuspend();
		threadSuspended=true;
		
		v.setOnKeyListener(null);
		aSensorManager.unregisterListener(aOSListener);
	}
	public boolean isSuspended() {
		return threadSuspended;
	}
	public boolean isStopped() {
		// 不在运行状态即停止了
		return !rFlag;
	}
	
	// 注意, 这里的synchronized是必须的, 因为这里用到了notifyAll(), 这是语言需要!
	// ref:http://download.oracle.com/javase/1.4.2/docs/guide/misc/threadPrimitiveDeprecation.html
	public synchronized void setResume() {
		// 继续逻辑线程
		m.setResume();
		threadSuspended=false;
		v.setOnKeyListener(this);
		aSensorManager.registerListener(aOSListener, aSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
		notifyAll();
	}
	/**
	 * 刷屏线程, 准备好IO, 启动逻辑模块, 以一定速率(30frame/900ms)刷屏
	 */
	@Override
	public void run() {
		Log.w("p","CRun::run()");
		rFlag=true;
		decorateGamePad();
		// 必须getSurfaceHolder()成功才能继续执行, 所以这里的getSurfaceHolder()被设计成阻塞的, 直到能够返回holder
		this.aSurfaceHolder=v.getSurfaceHolder();
		synchronized (aSurfaceHolder) {
			while (rFlag) {
				try {
					if (threadSuspended) {
						synchronized(this) {
							while (threadSuspended) wait();
						}
		            }
					update();
					paint();
					Thread.sleep(10);
					//Thread.yield();
				} catch (InterruptedException e) {
					Log.i("p", "CRun is interrupted");
					//e.printStackTrace();
				}
			}
		}
	}
	
	// 与Model通信的另一方法
	public void setMessage(int msgCode) {
		switch (msgCode) {
		case 0:
			aHandler.sendEmptyMessage(0);
			break;
		}
	}
	public void setStop() {
		v.setOnKeyListener(null);
		aSensorManager.unregisterListener(aOSListener);
		m.setStop();
		rFlag=false;
	}
	
	// 更新aBitmap
	private void update() {
		synchronized (aBitmap) {
			Canvas c=new Canvas(aBitmap);
			// 把gamePad绘制到aBitmap上
			synchronized (gamePad) {
				c.drawBitmap(gamePad, (bgSize-gamePadSize)/2, (bgSize-gamePadSize)/2, null);
			}
		}
	}
	
	/**
	 * 把aBitmap的内容输出到设备屏幕上
	 */
	private void paint() {
		//Log.i("p","paint");
		//这里偶尔会获得空指针, 如果获得了, 直接退出执行就好了
		Canvas c = aSurfaceHolder.lockCanvas();
		if (c==null) return;
		//先在aBitmap上绘制, 然后输出到屏幕上
		synchronized (aBitmap) {
			// 目前只输出aBitmap的中间一部分
			c.drawBitmap(aBitmap, validArea, physicalDisplay, null);
			//c.drawBitmap(aBitmap, (SCREEN_WIDTH-bgSize)/2, 0, null);
		}
		aSurfaceHolder.unlockCanvasAndPost(c);
	}

	/**
	 * 对gamePad进行外围装饰
	 */
	private void decorateGamePad() {
		synchronized (aBitmap) {
			Bitmap bg=BitmapFactory.decodeResource(osr, R.drawable.background);
			if (bg==null) Log.e("p", "bg is null");
			Canvas c=new Canvas(aBitmap);
			// 将坐标原点移动到恰当的地方(aBitmap正当中);
			c.translate((bgSize-gamePadSize)/2, 0);
			Paint p=new Paint();
//			p.setStyle(Style.FILL);
//			// 外边框;
//			p.setColor(Color.WHITE);
//			RectF rect=new RectF(-10.0f,-10.0f,gamePadSize+10.0f,gamePadSize+10.0f);
//			c.drawRect(rect, p);
//			// gamePad区域底色
//			p.setColor(Color.CYAN);
//			rect.set(0.0f, 0.0f, gamePadSize, gamePadSize);
//			c.drawRect(rect, p);
			c.drawBitmap(bg, 0, 0, null);
//			aCanvas.drawBitmap(bt, p0.x, p0.y, null);
		}
	}
	
	public void moveMeWrap(final int dir, final int step) {
		for (int i=0;i<step;++i) {
			// 进行step个单位(1px)移动
			//aHandler.post(new Runnable() {
				//@Override
				//public void run() {
					m.moveMe(dir);
				//}
			//});
		}
	}
	/*
	 * 输入控制
	 */
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (event.getAction()==KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
				moveMeWrap(0,3);
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				moveMeWrap(1,3);
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				moveMeWrap(2,3);
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				moveMeWrap(3,3);
				break;
			}
			Log.i("p", "keyCode:"+keyCode);
		}
		return false;
	}
	
	/**
	 * 方向传感器侦听器 Orientation Sensor Listener
	 * @author puch
	 *
	 */
	class OSListener implements SensorEventListener
	{
		private Point p;
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			Log.i("p","onSensorChanged");
			Log.v("p", event.values.toString());
			float pitch=event.values[1];
			float roll=event.values[2];
			int accX=0,accY=0;
			if (-90.0f>pitch||pitch>90.0f) {
				Log.e("p", "bad orientation");
			} else {
				// (int)(expression)后面这个括号必须加上,不然(int)会和一个数结合
				accY=(int) (-pitch/90*30);
				accX=(int) (-roll/90*30);
			}
			if (accY<0) {
				moveMeWrap(0,-accY);
			} else if (accY>0) {
				moveMeWrap(1,accY);
			}
			if (accX<0) {
				moveMeWrap(2,-accX);
			} else if (accX>0) {
				moveMeWrap(3,accX);
			}
			//Log.i("p","pitch&roll:"+pitch+":"+roll);
			//Log.i("p", "acc:"+accY+":"+accX);
			//Log.i("p", ct.getP0().toString());
		}
	}

	public void setLogic(MLogic m) {
		this.m=m;
	}
}
