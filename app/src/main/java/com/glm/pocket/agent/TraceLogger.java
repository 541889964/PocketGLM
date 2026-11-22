package com.glm.pocket.agent;

import com.glm.pocket.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.agent.tools.*;
import com.glm.pocket.net.*;
import com.glm.pocket.self.*;
import java.util.*;
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

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/** DeepSeek-Harness: every run is traceable — append-only JSONL */
public class TraceLogger{
 PrintWriter pw;
 public TraceLogger(){
  try{
   File d=new File(android.os.Environment.getExternalStoragePublicDirectory(
    android.os.Environment.DIRECTORY_DOWNLOADS),"pocketglm/trace");
   d.mkdirs();
   File f=new File(d,"run_"+new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date())+".jsonl");
   pw=new PrintWriter(new FileWriter(f,true));
  }catch(Exception e){}
 }
 public synchronized void log(String ev,String detail){
  if(pw==null)return;
  try{
   pw.println(new JSONObject()
    .put("t",new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()))
    .put("ev",ev).put("text",detail==null?"":detail));
   pw.flush();
  }catch(Exception ignore){}
 }
 public void close(){if(pw!=null)pw.close();}
}
