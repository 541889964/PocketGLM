package com.glm.pocket.self;
import android.os.*;
import android.webkit.*;
import org.json.*;
import java.util.*;
import java.io.*;
import com.glm.pocket.self.*;
import com.glm.pocket.net.*;
import com.glm.pocket.*;

import com.glm.pocket.agent.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.agent.tools.*;
import java.text.*;
import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import android.accessibilityservice.*;

import android.os.Environment;

import com.glm.pocket.MainActivity;
import com.glm.pocket.net.GitHub;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 云编译闭环: Deploy推送 → 按commit sha盯Actions(8s轮询) → artifact自动解包 → root可直装 */
public class CloudBuilder{
 public static String build(){
  String push=Deploy.run();
  MainActivity.selfPush("build",push);
  if(!push.contains("✓"))return push;
  try{
   String user=GitHub.user();
   GitHub.Resp c=GitHub.api("GET","/repos/"+user+"/PocketGLM/commits/main",null,true);
   if(c.code!=200)return "✗ 取commit失败 HTTP"+c.code;
   String sha=new JSONObject(c.body).getString("sha");
   MainActivity.selfPush("build","👁 监控 Actions (commit "+sha.substring(0,7)+")");
   long t0=System.currentTimeMillis();
   while(System.currentTimeMillis()-t0<20*60*1000L){
    Thread.sleep(8000);
    GitHub.Resp r=GitHub.api("GET","/repos/"+user+"/PocketGLM/actions/runs?per_page=5",null,true);
    if(r.code!=200)continue;
    JSONArray rs=new JSONObject(r.body).optJSONArray("workflow_runs");
    if(rs==null)continue;
    for(int i=0;i<rs.length();i++){
     JSONObject run=rs.optJSONObject(i);
     if(run==null||!sha.equals(run.optString("head_sha")))continue;
     String st=run.optString("status"),con=run.optString("conclusion");
     String url=run.optString("html_url");
     MainActivity.selfPush("build","⚙ "+st+(con.isEmpty()?"":" → "+con)
      +" · 第"+(System.currentTimeMillis()-t0)/60000+"分钟");
     if("completed".equals(st)){
      if("success".equals(con)){
       String apk=downloadArtifact(user);
       return "✓ 云编译成功!\nAPK已自动下载回手机: "+apk
        +"\n安装: install_apk {\"path\":\""+apk+"\"}\n日志: "+url;
      }
      return "✗ 云编译失败 — 看日志: "+url;
     }
     break;
    }
   }
   return "⚠ 20分钟未完成 — 手动看: https://github.com/"+user+"/PocketGLM/actions";
  }catch(Exception e){return "✗ "+e;}
 }
 static String downloadArtifact(String user){
  try{
   GitHub.Resp a=GitHub.api("GET","/repos/"+user+"/PocketGLM/actions/artifacts?per_page=10",null,true);
   JSONArray arts=a.code==200?new JSONObject(a.body).optJSONArray("artifacts"):null;
   if(arts==null||arts.length()==0)return "(未找到artifact)";
   JSONObject best=null;
   for(int i=0;i<arts.length();i++){
    JSONObject o=arts.optJSONObject(i);
    if(o!=null&&!o.optBoolean("expired")){best=o;break;}
   }
   if(best==null)return "(artifact全部过期)";
   HttpURLConnection c=(HttpURLConnection)new URL(
    "https://api.github.com/repos/"+user+"/PocketGLM/actions/artifacts/"
     +best.getLong("id")+"/zip").openConnection();
   c.setRequestProperty("Authorization","Bearer "+GitHub.token());
   c.setRequestProperty("Accept","application/vnd.github+json");
   c.setInstanceFollowRedirects(true);
   if(c.getResponseCode()!=200)return "(下载HTTP"+c.getResponseCode()+")";
   File base=new File(Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOWNLOADS),"pocketglm/apk");
   base.mkdirs();
   File zip=new File(base,"artifact.zip");
   OutputStream os=new FileOutputStream(zip);
   byte[]b=new byte[8192];int n;
   InputStream is=c.getInputStream();
   while((n=is.read(b))>0)os.write(b,0,n);
   os.close();is.close();
   ZipInputStream zs=new ZipInputStream(new FileInputStream(zip));
   ZipEntry e;
   while((e=zs.getNextEntry())!=null){
    if(e.getName().endsWith(".apk")){
     File apk=new File(base,"PocketGLM-"+System.currentTimeMillis()+".apk");
     FileOutputStream fo=new FileOutputStream(apk);
     while((n=zs.read(b))>0)fo.write(b,0,n);
     fo.close();zs.close();
     return apk.getAbsolutePath()+" ("+(apk.length()/1048576)+"MB)";
    }
   }
   zs.close();
   return "(zip内无APK?)";
  }catch(Exception e){return "✗ 下载: "+e;}
 }
 public static String installApk(String path){
  if(com.glm.pocket.agent.tools.RootShell.available()){
   String r=com.glm.pocket.agent.tools.RootShell.su("pm install -r '"+path+"'",60);
   return r.contains("Success")?"✓ 已安装: "+path:"✗ pm install: "+r;
  }
  return "⚠ 无root — APK在: "+path+" (文件管理器点击安装)";
 }
}
