package com.glm.pocket;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.glm.pocket.agent.AgentKernel;
import com.glm.pocket.agent.ApprovalGate;
import com.glm.pocket.agent.loader.PluginLoader;
import com.glm.pocket.net.GlmClient;
import com.glm.pocket.self.SelfModifier;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity{
 public static Context CTX;
 static WebView wv;
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  CTX=this;
  if(Build.VERSION.SDK_INT>=30&&!Environment.isExternalStorageManager()){
   try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
    Uri.parse("package:"+getPackageName())));}
   catch(Exception e){startActivity(new Intent(
    android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}
  }
  SelfModifier.ensureUi();          // L2: 出厂UI→可变副本
  PluginLoader.scan();              // 热插件扫描
  wv=new WebView(this);
  setContentView(wv);
  WebSettings s=wv.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  wv.setWebChromeClient(new android.webkit.WebChromeClient(){
   @Override public boolean onJsConfirm(android.webkit.WebView v,String u,String m,
    android.webkit.JsResult r){r.confirm();return true;}
  });
  wv.addJavascriptInterface(new Bridge(),"Android");
  wv.loadUrl("file://"+SelfModifier.uiDir().getAbsolutePath()+"/index.html");
 }
 public static WebView self(){return wv;}
 public static void reloadUi(){
  wv.post(()->wv.loadUrl("file://"+SelfModifier.uiDir().getAbsolutePath()+"/index.html"));
 }
 public static void openA11ySettings(){
  CTX.startActivity(new Intent(android.provider.Settings
   .ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 }
 class Bridge{
  void push(final String type,final String data){
   try{
    final String js="window.__push("+new JSONObject()
     .put("type",type).put("data",data==null?"":data).toString()+")";
    wv.post(()->wv.evaluateJavascript(js,null));
   }catch(Exception ignore){}
  }
  @JavascriptInterface public void chat(String json){
   new Thread(()->{
    try{
     JSONObject c=new JSONObject(json);
     String text=c.optString("text",""),conv=c.optString("conv","");
     boolean net=c.optBoolean("net",false);
     String mode=c.optString("mode","chat");
     if("chat".equals(mode)){
      String sys=com.glm.pocket.agent.SelfModel.build("对话",net);
      JSONObject pl=GlmClient.payload(sys+"\n\n# 用户\n"+text,conv,net,"deep_thinking","glm-5.3");
      GlmClient.Result r=GlmClient.stream(pl,(t,x)->push("chunk",
       new JSONObject().put("t",t).put("x",x).toString()));
      push("meta",new JSONObject().put("model",r.model).put("cid",r.cid)
       .put("think",r.think).put("err",r.err==null?"":r.err).toString());
     }else{
      AgentKernel.run(text,conv,net,"full".equals(mode),(type,data)->push(type,data));
     }
    }catch(Exception e){push("err",String.valueOf(e));}
   }).start();
  }
  @JavascriptInterface public void history(){
   new Thread(()->push("convs",GlmClient.history().toString())).start();
  }
  @JavascriptInterface public void del(String conv){
   new Thread(()->push("deleted",GlmClient.delete(conv))).start();
  }
  @JavascriptInterface public void plugins(){
   new Thread(()->push("plugins",PluginLoader.installedList())).start();
  }
  @JavascriptInterface public void reload(){reloadUi();}
  @JavascriptInterface public void a11y(){openA11ySettings();}
  @JavascriptInterface public void approveResult(String ok){
   ApprovalGate.resolve("true".equals(ok));
  }
 }
}
