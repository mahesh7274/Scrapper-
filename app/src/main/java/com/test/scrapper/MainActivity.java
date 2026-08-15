package com.test.scrapper;
import android.app.*;
import android.os.*;
import android.widget.*;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
public class MainActivity extends Activity {
 public void onCreate(Bundle b){
  super.onCreate(b);
  if(!Python.isStarted()) Python.start(new AndroidPlatform(this));
  String r=Python.getInstance().getModule("test_bs4").callAttr("test").toString();
  TextView t=new TextView(this); t.setText("BUILD TEST OK\\n"+r); t.setTextSize(22); setContentView(t);
 }
}
