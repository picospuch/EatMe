package cn.puch.fish;

import android.graphics.Bitmap;
import android.graphics.Point;

/*
 * 表示一种鱼, 一种鱼是一个鱼的集合, 他们的逻辑是一样的, 放到一个实现类里面即可, 如HungryFish;
 */
public interface IFish
{
	// 获得一种鱼的个体限量
	int getMaxFishNum();
	// 获得鱼的大小
	int getFishSize();
	// 获得鱼的动画下帧
	public void nextFrame(Bitmap b);
	// 增加这种鱼的数目
	public void move();
	// 设定池塘的大小
	public void setPoolSize(int poolSize);
	public void addOne();
	// 是否准备好, 该加载的资源加载好了, 该配置的东西配置好了
	boolean isReady();
	public boolean isEatMe(Point mePos, int meSize);
}
