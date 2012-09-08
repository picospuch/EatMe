package cn.puch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		WebView aWebView=(WebView) findViewById(R.id.aWebView);
		aWebView.getSettings().setJavaScriptEnabled(true);
		
		InputStream in=this.getResources().openRawResource(R.raw.about);
	    StringBuffer aboutData = new StringBuffer(1000);
	    InputStreamReader reader = new InputStreamReader(in);
	    char[] buf = new char[1024];
	    int numRead=0;
	    try {
	    	while((numRead=reader.read(buf)) != -1){
	    		aboutData.append(buf, 0, numRead);
	    	}
	    	reader.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    // TODO 为什么loadData()出问题, 它们之间有什么区别?
	    aWebView.loadDataWithBaseURL("about:blank", aboutData.toString(), "text/html", "utf-8", null);
	    }
}
