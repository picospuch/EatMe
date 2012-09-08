package cn.puch;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * 这个Service目前有两个功能:
 * 1. 向系统说明这个程序应当以一个Service对待
 * 2. 后台播放音乐
 * @author puch
 *
 */
public class EatMeService extends Service implements EatMeConstants
{
	private final int SHOW_MAIN_ACTIVITY=32;
	private NotificationManager mNM;
	private String text="EatMe is still running!";
	private String title="EatMe";
	private MediaPlayer mplayer=null;
	public class LocalBinder extends Binder {
		public EatMeService getService() {
			return EatMeService.this;
		}
	}
	
	@Override
	public void onCreate() {
		Log.i("p7", "onCreate()");
		mNM=(NotificationManager) this.getSystemService(this.NOTIFICATION_SERVICE);
		showNotification();
	}
	private void showNotification() {
		Notification notification=new Notification(R.drawable.icon,"EatMeService", System.currentTimeMillis());
		PendingIntent pendingIntent=PendingIntent.getService(this, 0, new Intent(this,EatMeService.class).putExtra("MSG", SHOW_MAIN_ACTIVITY), 0);
		notification.setLatestEventInfo(this, title, text, pendingIntent);
		mNM.notify(0, notification);
	}
	
	private boolean isPaused=false;
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("p7", "Recievied startId:" + startId +":" + intent);
		switch (intent.getIntExtra("MSG", SHOW_MAIN_ACTIVITY)) {
		case SHOW_MAIN_ACTIVITY:
			break;
		case PLAY_MSG:
			mplayer=MediaPlayer.create(this, R.raw.poolmooncolor);
			mplayer.setLooping(true);
			mplayer.start();
			break;
		case STOP_MSG:
			mplayer.stop();
			mplayer.release();
			mplayer=null;
			break;
		case PAUSE_MSG:
			if (isPaused) {
				mplayer.start();
				isPaused=false;
			} else {
				mplayer.pause();
				isPaused=true;
			}
			break;
		}
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i("p7", "onDestroy");
		mNM.cancel(0);
		Toast.makeText(this, "Bye!", Toast.LENGTH_SHORT).show();
		if (mplayer!=null) {
			mplayer.release();
			mplayer=null;
		}
	}
	
	private final IBinder mBinder=new LocalBinder();
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
