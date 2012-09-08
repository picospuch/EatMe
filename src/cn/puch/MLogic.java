package cn.puch;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import cn.puch.fish.HungryFish;
import cn.puch.fish.IFish;

/**
 * 负责游戏逻辑
 * @author puch
 *
 */
public class MLogic implements Runnable
{
	private volatile boolean rFlag=false;
	private volatile boolean threadSuspended=false;
	private int score=0;
	// 如果me是个圆, 这个值就是它的半径
	private int meSize=20;
	private int amigoSize;
	private Point mePos=new Point();
	private Random rnd=new Random();
	private Bitmap gamepad_bg;
	private AnimationDrawable me;
	// 各种鱼的集合
	private ArrayList<IFish> amigo=new ArrayList<IFish>();
	
	private int gamePadSize = 320;
	private CRun c;
	private Context osc;
	private Resources osr;
	private Bitmap gamePad;
	
	private AnimationDrawable ad;
	
	public boolean isModelReady() {
		if (gamePad==null) return false;
		if (this.c==null) return false;
		return true;
	}
	public MLogic(CRun c) {
		this.c=c;
		this.gamePad=c.getGamePad();
		this.osc=c.osc;
		this.osr=this.osc.getResources();
		gamepad_bg=BitmapFactory.decodeResource(osr, R.drawable.gamepad_bg);
//		me=BitmapFactory.decodeResource(osr, R.drawable.me);
		me=(AnimationDrawable) osr.getDrawable(R.anim.me);
		
		mePos.set((gamePadSize-meSize)/2, (gamePadSize-meSize)/2);
//		this.ad=(AnimationDrawable) this.osc.getResources().getDrawable(R.anim.hungry_fish);
		
		// 加入一种鱼, 一种鱼可以在屏幕上显示几条
		IFish aHungryFish=new HungryFish(osr);
		aHungryFish.setPoolSize(this.gamePadSize);
		if (!aHungryFish.isReady()) Log.e("p", "HungryFish is not Ready!");
		amigo.add(aHungryFish);
		amigoSize=amigo.get(0).getFishSize();
	}
	
	
	public void setSuspend() {
		threadSuspended=true;
	}
	public synchronized void setResume() {
		threadSuspended=false;
		notifyAll();
	}
	/**
	 * 保持游戏逻辑运行
	 */
	@Override
	public void run() {
		Log.w("p", "MLogic::run()");
		rFlag=true;
		int cnt=0;
		
		mainLoop:
		while (rFlag) {
			try {
				if (threadSuspended) {
					synchronized(this) {
						while (threadSuspended) wait();
					}
	            }
				// 先判断再移动!改变顺序可能影响鱼的逻辑
				for (IFish aFish:amigo) {
					// 是否吃掉me了
					if (aFish.isEatMe(mePos, meSize)) {
						c.setMessage(0);
						break mainLoop;
					}
				}
				// 增加一条鱼的间隔为每200cycle
				// TODO 加鱼的时候应该检测游戏总容量和每种鱼的个体限量
				if (cnt++%200==0) amigo.get(0).addOne();
				for (IFish aFish:amigo) {
					aFish.move();
				}
				//一般只在这里调用update()以免造成刷新混乱
				update();
				++score;
				Thread.sleep(32);
			} catch (InterruptedException e) {
				;
			}
		}
	}
	
	/*
	 * 清空状态, 以备再次运行; 设置停止标志
	 */
	public void setStop() {
		// 清空MLogic状态
		mePos.set((gamePadSize-meSize)/2, (gamePadSize-meSize)/2);
		amigo.clear();
		IFish aHungryFish=new HungryFish(osr);
		aHungryFish.setPoolSize(this.gamePadSize);
		if (!aHungryFish.isReady()) Log.e("p", "HungryFish is not Ready!");
		amigo.add(aHungryFish);
		amigoSize=amigo.get(0).getFishSize();
		
		rFlag=false;
	}
	
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score=score;
	}
	// 总是定义此函数一次移动一个单位(1px), 其他事务交由其他函数完成
	public synchronized void moveMe(int dir) {
		switch (dir) {
		case 0:
			--mePos.y;
			break;
		case 1:
			++mePos.y;
			break;
		case 2:
			--mePos.x;
			break;
		case 3:
			++mePos.x;
			break;
		}
		if (mePos.y<meSize/2) {
			mePos.y=meSize/2;
		} else if (mePos.y>gamePadSize-meSize/2) {
			mePos.y=gamePadSize-meSize/2;
		}
		if (mePos.x<meSize/2) {
			mePos.x=meSize/2;
		} else if (mePos.x>gamePadSize-meSize/2) {
			mePos.x=gamePadSize-meSize/2;
		}
		//本应在update中更新me位置和状态, 如此me的状态更新将受限于update的更新频率所以在此更新;
		//me的状态刷新频率应高于Fish, 这样用户就不会感觉到力不从心;
		//nextFrameOfMe();
	}

	//TODO 这个函数需要高效的实现
	private Drawable lastFrameOfMe;
	private int meRefreshCnt=0;
	private int meFrameCnt=0;
	private void nextFrameOfMe() {
		Paint p=new Paint();
		Canvas c=new Canvas(gamePad);
		if (meRefreshCnt%10==0) {
			lastFrameOfMe=me.getFrame(meFrameCnt%me.getNumberOfFrames());
			++meFrameCnt;
		}
		lastFrameOfMe.setBounds(new Rect(mePos.x-meSize, mePos.y-meSize, mePos.x+meSize, mePos.y+meSize));
		lastFrameOfMe.draw(c);
		++meRefreshCnt;
	}
	/**
	 * 刷新gamePad
	 */
	private void update() {
		synchronized (gamePad) {
			Log.w("p", "MLogic::update()");
			Paint p=new Paint();
			Canvas c=new Canvas(gamePad);
//			c.drawColor(Color.CYAN);
			c.drawBitmap(gamepad_bg, 0, 0, null);
			// draw me
//			p.setStyle(Style.FILL);
//			p.setColor(Color.BLUE);
//			p.setMaskFilter(new BlurMaskFilter(20, Blur.INNER));
			// me图片大小为40px, 取中间的mxSize20px为核心, 但绘制时中心坐标应偏移20px
			//c.drawCircle(mePos.x, mePos.y, meSize/2, p);
			nextFrameOfMe();
			for (IFish aFish:amigo) {
				aFish.nextFrame(gamePad);
			}
		}
	}
}
