package com.glm.pocket.agent.tools;
import android.os.*;
import android.webkit.*;
import android.content.*;
import org.json.*;
import java.io.*;
import com.glm.pocket.agent.tools.*;

import com.glm.pocket.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.net.*;
import com.glm.pocket.self.*;
import java.util.*;
import java.text.*;
import android.app.*;
import android.database.*;
import android.net.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import android.accessibilityservice.*;

import android.content.Intent;
import android.os.Environment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/** 已验证的Termux通道: RUN_COMMAND → out文件轮询 */
public class TermuxPython{
 public static String run(JSONObject a){
  try{
   String code=a.optString("code","");
   File dir=new File(Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOWNLOADS),"pocketglm/py");
   dir.mkdirs();
   long ts=System.currentTimeMillis();
   File in=new File(dir,"in_"+ts+".py"),out=new File(dir,"out_"+ts+".txt");
   Writer w=new OutputStreamWriter(new java.io.FileOutputStream(in),"UTF-8");
   w.write(code);w.close();
   Intent i=new Intent("com.termux.RUN_COMMAND")
    .setClassName("com.termux","com.termux.app.RunCommandService");
   i.putExtra("com.termux.RUN_COMMAND_PATH","/data/data/com.termux/files/usr/bin/sh");
   i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",new String[]{"-c",
    "python3 '"+in.getAbsolutePath()+"' > '"+out.getAbsolutePath()+"' 2>&1"});
   com.glm.pocket.MainActivity.CTX.startService(i);
   long last=-1;
   for(int s=0;s<25;s++){
    Thread.sleep(1000);
    if(out.exists()&&out.length()>0&&out.length()==last)break;
    last=out.exists()?out.length():-1;
   }
   if(!out.exists()||out.length()==0)
    return "(无输出 — 需装Termux且设 allow-external-apps=true)";
   BufferedReader br=new BufferedReader(new FileReader(out));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l).append('\n');
   br.close();
   String o=sb.toString().trim();
   return o.length()>1500?o.substring(0,1500):(o.isEmpty()?"(无输出)":o);
  }
  catch(Exception e){return "✗ "+e;}
 }
}
