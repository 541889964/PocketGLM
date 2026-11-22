package com.glm.pocket.agent;

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
