package cn.puch;

import cn.puch.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

/**
 * 负责android应用框架
 * @author puch
 *
 */
public class EatMe extends Activity implements OnClickListener, Handler.Callback, EatMeConstants
{
	// 应用程序全局资源引用
	private Resources osr;
	private CRun aCRun;
	private MLogic aMLogic;
    private VView aVView;
    private View mCurtain;
    private ViewSwitcher mViewSwitcher;
    private TextView mState;
    private EatMeService aService;
    private boolean mIsBound=false;
	private static String ICICLE_KEY="eat-me";
	
	
	
//========================Activity生命周期=============================
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.osr=this.getResources();
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        
        // 连接MVC
        aVView=(VView) findViewById(R.id.aVView);
        aCRun=new CRun(aVView, this);
        aVView.setController(aCRun);
        aMLogic=new MLogic(aCRun);
        aCRun.setLogic(aMLogic);
        
        mCurtain=this.findViewById(R.id.curtain);
        mState=(TextView) this.findViewById(R.id.state);
        
        // activity生命周期处理初始化
        if (savedInstanceState==null) {
        	//刚刚启动程序
    		Log.i("p5", aVView.isViewReady()+":"+aCRun.isControllerReady()+":"+aMLogic.isModelReady());
        	if (aVView.isViewReady()
           			&& aMLogic.isModelReady()
        			&& aCRun.isControllerReady())
        	{
        		mCurtain.setOnClickListener(new OnClickListener() {
        			@Override
        			public void onClick(View v) {
//        				if (v==curtain) {
        					aCRun.setStart();
        					// start play bg music
        					Intent intent=new Intent(EatMe.this,EatMeService.class);
        					intent.putExtra("MSG", PLAY_MSG);
        					EatMe.this.startService(intent);
        					showNext();
        					LinearLayout l=(LinearLayout) findViewById(R.id.toolbar);
        					l.startLayoutAnimation();
        					mCurtain.setOnClickListener(EatMe.this);
//        				}
        			}
                });
        	}
        } else {
            Bundle map = savedInstanceState.getBundle(ICICLE_KEY);
            if (map != null) {
                aMLogic.setScore(map.getInt(ICICLE_KEY));
            } else {
                //Pause?
            }
        }
        
        // 建立工具条
        makeToolbar();
        //绑定Service
		Log.i("p6", "onClickMusicButton");
		//Toast.makeText(this, "it works", Toast.LENGTH_SHORT).show();
		doBindService();
    }

	@Override
	protected void onStart() {
		// activity comes to fg;
		super.onStart();
	
	}
	
    @Override
    protected void onPause() {
    	// activity comes to bg;
    	super.onPause();
        if (aCRun.isStopped()) {
        	// 如果已经停止, 结束算了
        	finish();
        } else {
        	// Pause the game along with the activity
            aCRun.setSuspend();
        	setPauseScreen();
    		showPrevious();
        }
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	this.stopService(new Intent(this,EatMeService.class));
    	doUnbindService();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Store the game state
    	//现只保存分数
        outState.putInt(ICICLE_KEY, aMLogic.getScore());
    }
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
    
	public void makeToolbar() {
		//GridView toolbar=(GridView) findViewById(R.id.toolbar);
	}
//=========================各种回调=====================================
	
	// 显示主游戏VView
	public void showNext() {
		// 由于VView不响应ViewSwitcher的动画(Bug of ViewSwitcher?);
		// toobar相应, 这会造成不一致, 所以先将toobar隐藏, 让其也不相应动画(这种方法不奏效)
		// 最终选择放弃使用ViewSwitcher
		LinearLayout toobar=(LinearLayout) findViewById(R.id.toolbar);
		//toobar.setVisibility(View.GONE);
		//mViewSwitcher.showNext();
		//toobar.setVisibility(View.VISIBLE);
		mCurtain.startAnimation(AnimationUtils.loadAnimation(this, R.anim.curtain));
		mCurtain.setVisibility(View.INVISIBLE);
		toobar.startLayoutAnimation();
	}
	// 显示Curtain
	public void showPrevious() {
//		mViewSwitcher.showPrevious();
		mCurtain.setVisibility(View.VISIBLE);
	}
	// 主界面暂停
	public void setPauseScreen() {
		mCurtain.setBackgroundColor(Color.parseColor("#ff000000"));
		TextView mState=(TextView) this.findViewById(R.id.state);
		mState.setText("Score:"+aMLogic.getScore());
	}
    @Override
    public void onClick(View v) {
    	if (v==mCurtain) {
    		Log.i("p6", "RAA::onClick()");
			if (aCRun.isSuspended()) {
				aCRun.setResume();
				showNext();
			} else {
				aCRun.setSuspend();
				mState.setText("Score:"+aMLogic.getScore());
				showPrevious();
			}
    	}
    }

	public void onClickPauseButton(View v) {
    	//Log.d()不能正常工作, 在真机下
    	Log.i("p", "testIt");
		aCRun.setSuspend();
		setPauseScreen();
		showPrevious();
    }

	public void onClickAboutButton(View v) {
		Intent intent=new Intent();
		intent.setClass(this,AboutActivity.class);
		startActivity(intent);
	}
	
	private boolean isOn=true;
	public void onClickMusicButton(View v) {
			if (isOn) {
				((ImageButton) v).setImageResource(R.drawable.music_off_button);
				isOn=false;
			} else {
				((ImageButton) v).setImageResource(R.drawable.music_on_button);
				isOn=true;
			}
			Intent intent=new Intent(this,EatMeService.class);
			intent.putExtra("MSG", this.PAUSE_MSG);
			this.startService(intent);
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		// what==0意味着从CRun传来game over了消息了
		if (msg.what==0) {
			//if (mCurtain.getVisibility()==View.GONE) {
				showPrevious();
				aCRun.setStop();
				// TODO 应该通过CRun通知MLogic更新分数
				mState.setText("Score:"+aMLogic.getScore());
				// set Curtain to blood color;
				mCurtain.setBackgroundColor(Color.parseColor("#7fff0000"));
//				mCurtain.setOnClickListener(new OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						finish();
//					}
//				});
				mCurtain.setOnClickListener(null);
				final ImageButton ib=(ImageButton) findViewById(R.id.retry);
				ib.setVisibility(View.VISIBLE);
				ib.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						aMLogic.setScore(0);
						aCRun.setStart();
						//准备重新开始
						mCurtain.setOnClickListener(EatMe.this);
						showNext();
						ib.setVisibility(View.GONE);
					}
				});
			//}
		}
		return false;
	}
	
//============================Service相关=======================================
    private ServiceConnection aConnection=new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			aService=((EatMeService.LocalBinder) service).getService();
			//Toast.makeText(EatMe.this, "onServiceConnected", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// 不会发生的, 因为Client和Service在同一个进程中
			aService=null;
			Toast.makeText(EatMe.this, "onServiceDisonnected", Toast.LENGTH_SHORT).show();
		}
    };
    
    private void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(EatMe.this, 
                EatMeService.class), aConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(aConnection);
            mIsBound = false;
        }
    }
    
}
