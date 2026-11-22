package com.glm.pocket.agent.tools;

import android.content.Intent;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/** root直读Termux环境(PREFIX/HOME/PATH注入, 免allow-external-apps); 无root退RUN_COMMAND */
public class TermuxEnv{
 public static final String PREFIX="/data/data/com.termux/files/usr";
 public static final String HOME="/data/data/com.termux/files/home";

 public static String runScript(String script,int timeoutSec){
  try{
   File d=new File(Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOWNLOADS),"pocketglm/tmp");
   d.mkdirs();
   long ts=System.currentTimeMillis();
   File sh=new File(d,"s"+ts+".sh"),out=new File(d,"o"+ts+".txt");
   Writer w=new OutputStreamWriter(new FileOutputStream(sh),"UTF-8");
   w.write(script);w.close();
   if(com.glm.pocket.agent.tools.RootShell.available()){
    RootShell.su("PREFIX="+PREFIX+"; HOME="+HOME+"; PATH="+PREFIX+"/bin:/system/bin; "
     +"export PREFIX HOME PATH; sh '"+sh.getAbsolutePath()+"' > '"+out.getAbsolutePath()+"' 2>&1",timeoutSec);
   }else{
    Intent i=new Intent("com.termux.RUN_COMMAND")
     .setClassName("com.termux","com.termux.app.RunCommandService");
    i.putExtra("com.termux.RUN_COMMAND_PATH",PREFIX+"/bin/sh");
    i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",new String[]{"-c",
     "sh '"+sh.getAbsolutePath()+"' > '"+out.getAbsolutePath()+"' 2>&1"});
    com.glm.pocket.MainActivity.CTX.startService(i);
   }
   long last=-1,t0=System.currentTimeMillis();
   while(System.currentTimeMillis()-t0<timeoutSec*1000L){
    Thread.sleep(1500);
    long len=out.exists()?out.length():-1;
    if(len>0&&len==last)break;
    last=len;
   }
   if(!out.exists()||out.length()==0)
    return "✗ 无输出 — Termux未装或root/su不可用 (先env_install或手动装Termux)";
   BufferedReader br=new BufferedReader(new FileReader(out));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l).append('\n');
   br.close();
   String o=sb.toString();
   return o.length()>1500?o.substring(o.length()-1500):o;
  }catch(Exception e){return "✗ "+e;}
 }
 public static String check(){
  return runScript(
   "for t in python3 pip3 node git; do "
   +"command -v $t >/dev/null 2>&1 && echo \"$t: $($t --version 2>&1|head -1)\" "
   +"|| echo \"$t: MISSING\"; done",45);
 }
}
