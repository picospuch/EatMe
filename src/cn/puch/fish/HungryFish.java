package cn.puch.fish;

import java.util.Random;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import cn.puch.R;

/**
 * 饿鱼
 * @author puch
 *
 */
public class HungryFish implements IFish
{
	// 把一条鱼看成正方形之后的边长
	private int mySize=50;
	// 把一条鱼看成圆之后的半径
	// 每条鱼的坐标原点在中心
	private int myRadius=mySize/2;
	// 标准移动速度
	private int step=3;
	// 池塘大小poolSize目前和gamePad一样
	private int poolSize=0;
	private Random rnd=new Random();
	private final int maxFishNum=20;
	// 需要两个, 一个为正向, 一个为水平翻转
	private AnimationDrawable ad[];
	// 播放动画的游标
	private int cursor[]=new int[maxFishNum];
	private int maxFrameNums=0;
	// 当前个体数量
	private int curFishNum=0;
	// 个体位置
	private int px[]=new int[maxFishNum];
	private int py[]=new int[maxFishNum];
	// 个体方向加速度
	private int ax[]=new int[maxFishNum];
	private int ay[]=new int[maxFishNum];
	private byte flip[]=new byte[maxFishNum];
	private int moveCnt[]=new int[maxFishNum];
	private Rect myBounds;
	private Drawable aDrawable;
	// me相关信息
	private Point mePos;
	private int meSize;
	private VectorFish2Me vf2m;
	/**
	 * constructor
	 * @param r 应用资源对象
	 */
	public HungryFish(Resources r) {
		ad=new AnimationDrawable[]{(AnimationDrawable) r.getDrawable(R.anim.hungryfish),
			(AnimationDrawable) r.getDrawable(R.anim.hungryfish_r)};
		// 假定hungryfish和hungryfish_r具有相同的帧数
		maxFrameNums=ad[0].getNumberOfFrames()-1;
		myBounds=new Rect();
		vf2m=new VectorFish2Me();
	}

	@Override
	public int getFishSize() {
		return mySize;
	}

	@Override
	public void nextFrame(Bitmap b) {
		Canvas c=new Canvas(b);
		Log.w("p", "HungryFish::nextFrame");
		for (int i=0;i<curFishNum;++i) {
			// 如果现在输出的是最后一帧, 就准备输出第一帧
			// 效率? cursor=(cursor+1)%(maxframeNums+1)
			int tmp=cursor[i];
			cursor[i]=(cursor[i] == (maxFrameNums)) ? 0 : (cursor[i]+1);
			myBounds.set(px[i]-myRadius, py[i]-myRadius, px[i]+myRadius, py[i]+myRadius);
			aDrawable=ad[flip[i]].getFrame(tmp);
			aDrawable.setBounds(myBounds);
			aDrawable.draw(c);
		}
	}
	
	@Override
	public void addOne() {
		if (curFishNum>maxFishNum-1) return;
		if (rnd.nextBoolean()) {
			px[curFishNum]=-myRadius;
//			py[curFishNum]=myRadius+rnd.nextInt(poolSize-mySize);
			py[curFishNum]=mePos.y;
			ax[curFishNum]=step;
			ay[curFishNum]=0;
			flip[curFishNum]=0;
		} else {
			px[curFishNum]=poolSize-1+myRadius;
//			py[curFishNum]=myRadius+rnd.nextInt(poolSize-mySize);
			py[curFishNum]=mePos.y;
			ax[curFishNum]=-step;
			ay[curFishNum]=0;
			flip[curFishNum]=1;
		}
		++curFishNum;
	}
	
	@Override
	public int getMaxFishNum() {
		return maxFishNum;
	}

	@Override
	public boolean isReady() {
		if (ad==null) return false;
		if (poolSize==0) return false;
		return true;
	}

	// 和MLogic::moveMe一样, 基本单位是1px
	@Override
	public void move() {
		for (int i=0;i<curFishNum;++i) {
			px[i]+=ax[i];
			py[i]+=ay[i];
			if (px[i] < myRadius) {
				px[i]=myRadius;
				ax[i]=-ax[i];
				flip[i]=0;
			} else if (px[i] > poolSize-myRadius) {
				px[i]=poolSize-myRadius;
				ax[i]=-ax[i];
				flip[i]=1;
			}
			if (py[i] <myRadius) {
				py[i]=myRadius;
				ay[i]=-ay[i];
			} else if (py[i] >poolSize-myRadius) {
				py[i]=poolSize-myRadius;
				ay[i]=-ay[i];
			}
			// Burst Motion!!!
			if ((moveCnt[i]++)%250==150) {
				// 每400cycle burst!!!下
				vf2m.refreshLength();
				ax[i]=(int) vf2m.setPos(px[i], py[i]).getAX();
				ay[i]=(int) vf2m.setPos(px[i], py[i]).getAY();
				flip[i]=(byte) (ax[i] > 0 ? 0 : 1);
			} else if (ax[i]>step || ax[i]<-step) {
				// cool down!!! to step
				ax[i]+=ax[i]>0 ? (-1) : 1;
			} else if (ay[i]!=0) {
				// cool down!!!
				ay[i]+=ay[i]>0 ? (-1) : 1;
			}
		}
	}

	@Override
	public void setPoolSize(int poolSize) {
		this.poolSize=poolSize;
	}

	@Override
	// 是否已经把me吃掉了
	public boolean isEatMe(Point mePos, int meSize) {
		// 在这个测试中为burst!!!更新mePos和meSize
		this.mePos=mePos;
		this.meSize=meSize;
		for (int i=0;i<curFishNum; ++i) {
			//Log.i("p6",vf2m.setPos(px[i], py[i]).getDistance()+":"+Math.abs(meSize+mySize)/5);
			if (vf2m.setPos(px[i], py[i]).getDistance() < Math.abs(meSize+mySize)/3)
				return true;
		}
		return false;
	}
	
	/**
	 * 从一条鱼到Me的向量
	 * @author puch
	 *
	 */
	class VectorFish2Me
	{
		// 以pos为原点,y轴向下为正向, mePos在其中的坐标
		private float x=0,y=0;
		public VectorFish2Me() {
		}
		public VectorFish2Me setPos(float x, float y) {
			this.x=mePos.x-x;
			this.y=mePos.y-y;
			return this;
		}
		// 以pos为原点, 返回mePos在极坐标中的半径
		public float getDistance() {
			return (float) Math.sqrt(Math.pow(x,2)+
					Math.pow(y, 2));
		}
		// 给burst的建议加速度
		int length=10;
		// 刷新burst力度
		public void refreshLength() {
			length=rnd.nextInt(10)+10;
		}
		public float getAX() {
			return (this.x/getDistance())*length;
		}
		public float getAY() {
			return (this.y/getDistance())*length;
		}
	}
}
