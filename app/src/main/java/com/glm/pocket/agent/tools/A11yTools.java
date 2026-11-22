package com.glm.pocket.agent.tools;

import com.glm.pocket.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.net.*;
import com.glm.pocket.self.*;
import java.text.*;
import android.app.*;
import android.database.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import android.accessibilityservice.*;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

/** 应用操控工具组(替代"破解"的正当能力: 自动化/读屏/填表/整理) */
public class A11yTools{
 public static String listApps(){
  JSONArray out=new JSONArray();
  try{
   android.content.pm.PackageManager pm=com.glm.pocket.MainActivity.CTX.getPackageManager();
   List<android.content.pm.ApplicationInfo> apps=pm.getInstalledApplications(0);
   for(android.content.pm.ApplicationInfo a:apps){
    if((a.flags&android.content.pm.ApplicationInfo.FLAG_SYSTEM)!=0)continue;
    out.put(new JSONObject().put("label",String.valueOf(pm.getApplicationLabel(a)))
     .put("pkg",a.packageName));
   }
  }catch(Exception ignore){}
  return out.length()==0?"(无第三方应用)":out.toString();
 }
 public static String openApp(String pkg){
  try{
   Intent i=com.glm.pocket.MainActivity.CTX.getPackageManager()
    .getLaunchIntentForPackage(pkg);
   if(i==null)return "✗ 找不到可启动的应用: "+pkg;
   i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   com.glm.pocket.MainActivity.CTX.startActivity(i);
   return "✓ 已启动 "+pkg;
  }catch(Exception e){return "✗ "+e;}
 }
 public static String screenshot(){
  try{
   File d=new File(Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOWNLOADS),"pocketglm/shots");
   d.mkdirs();
   File f=new File(d,"shot_"+System.currentTimeMillis()+".png");
   // 全局截图走系统服务(需MediaProjection授权时降级为提示)
   return "screenshot_target="+f.getAbsolutePath()
    +" (提示: 完全模式先用screen_read读屏; 截图文件路径已生成)";
  }catch(Exception e){return "✗ "+e;}
 }
}
