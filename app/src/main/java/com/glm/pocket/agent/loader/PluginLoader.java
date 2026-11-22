package com.glm.pocket.agent.loader;
import android.webkit.*;
import org.json.*;
import java.util.*;
import java.io.*;
import com.glm.pocket.agent.loader.*;

import com.glm.pocket.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.agent.tools.*;
import com.glm.pocket.net.*;
import com.glm.pocket.self.*;
import java.text.*;
import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import android.accessibilityservice.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/** DSH式热插件层: Download/pocketglm/plugins/<name>/{manifest.json,entry}
 *  python插件=Termux执行 stdin契约 / js·ui插件=WebView注入 */
public class PluginLoader{
 public static final Map<String,PluginDef> loaded=new LinkedHashMap<>();
 public static File root(){
  return new File(android.os.Environment.getExternalStoragePublicDirectory(
   android.os.Environment.DIRECTORY_DOWNLOADS),"pocketglm/plugins");
 }
 /** 启动/安装后扫描, 免重启热注册 */
 public static int scan(){
  loaded.clear();
  File[] ds=root().listFiles();
  int n=0;
  if(ds!=null)for(File d:ds){
   if(!d.isDirectory())continue;
   File mf=new File(d,"manifest.json");
   if(!mf.exists())continue;
   try{
    BufferedReader br=new BufferedReader(new FileReader(mf));
    StringBuilder sb=new StringBuilder();String l;
    while((l=br.readLine())!=null)sb.append(l);
    br.close();
    JSONObject j=new JSONObject(sb.toString());
    PluginDef p=new PluginDef();
    p.name=j.getString("name");p.type=j.getString("type");
    p.entry=j.optString("entry","");p.dir=d.getAbsolutePath();
    p.tools=j.optJSONArray("tools");
    loaded.put(p.name,p);n++;
   }catch(Exception ignore){}
  }
  return n;
 }
 /** AI自我认知: 已装插件清单(名字/功能/args), 注入系统提示词 */
 public static String installedList(){
  if(loaded.isEmpty())return "(暂无插件 — 你可用 install_plugin 自己写并安装, 规范见PLUGIN_SPEC)";
  StringBuilder b=new StringBuilder();
  for(PluginDef p:loaded.values()){
   b.append("- 插件 ").append(p.name).append(" [").append(p.type).append("] dir=").append(p.dir).append('\n');
   if(p.tools!=null)for(int i=0;i<p.tools.length();i++){
    JSONObject t=p.tools.optJSONObject(i);
    if(t!=null)b.append("  · 工具 ").append(t.optString("name")).append(": ")
     .append(t.optString("desc")).append(" args=").append(t.optJSONObject("args")).append('\n');
   }
  }
  return b.toString();
 }
 /** 调用已注册插件工具; 返回给模型的结果文本 */
 public static String invoke(String tool,JSONObject args){
  for(PluginDef p:loaded.values()){
   if(p.tools!=null)for(int i=0;i<p.tools.length();i++){
    JSONObject t=p.tools.optJSONObject(i);
    if(t!=null&&tool.equals(t.optString("name")))
     return "python".equals(p.type)?runPython(p,args):"(js/ui插件由前端执行, 此处仅python)";
   }
  }
  return "✗ 未注册的工具: "+tool;
 }
 /** python插件契约: echo {tool,args} | python3 entry → {"ok":..,"result":..} */
 static String runPython(PluginDef p,JSONObject args){
  try{
   File in=new File(new File(p.dir),"__call.json");
   OutputStreamWriter w=new OutputStreamWriter(new java.io.FileOutputStream(in),"UTF-8");
   w.write(new JSONObject().put("tool",args.optString("__tool","")).put("args",args).toString());
   w.close();
   File out=new File(new File(p.dir),"__out.txt");
   out.delete();
   android.content.Intent i=new android.content.Intent("com.termux.RUN_COMMAND")
    .setClassName("com.termux","com.termux.app.RunCommandService");
   i.putExtra("com.termux.RUN_COMMAND_PATH","/data/data/com.termux/files/usr/bin/sh");
   i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",new String[]{"-c",
    "cat '"+in.getAbsolutePath()+"' | python3 '"+new File(p.dir,p.entry).getAbsolutePath()
    +"' > '"+out.getAbsolutePath()+"' 2>&1"});
   com.glm.pocket.MainActivity.CTX.startService(i);
   long last=-1;
   for(int s=0;s<30;s++){
    Thread.sleep(1000);
    if(out.exists()&&out.length()>0&&out.length()==last)break;
    last=out.exists()?out.length():-1;
   }
   if(!out.exists()||out.length()==0)return "(插件无输出/超时30s)";
   BufferedReader br=new BufferedReader(new FileReader(out));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l);
   br.close();
   String o=sb.toString().trim();
   return o.length()>1500?o.substring(0,1500):o;
  }catch(Exception e){return "✗ "+e;}
 }
}
