package com.glm.pocket.agent.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class RootShell{
 static Integer ok=null;
 public static boolean available(){
  if(ok!=null&&ok==1)return true;
  String r=su("id",8);
  ok=r.contains("uid=0")?1:0;
  return ok==1;
 }
 public static String su(String cmd,int timeoutSec){
  try{
   Process p=Runtime.getRuntime().exec(new String[]{"su","-c",cmd});
   final StringBuilder sb=new StringBuilder();
   Thread rd=new Thread(){public void run(){
   try{
    BufferedReader o=new BufferedReader(new InputStreamReader(p.getInputStream()));
    BufferedReader e=new BufferedReader(new InputStreamReader(p.getErrorStream()));
    String l;
    while((l=o.readLine())!=null)sb.append(l).append('\n');
    while((l=e.readLine())!=null)sb.append(l).append('\n');
   }catch(Exception ignore){}}};
   rd.start();
   if(!p.waitFor(timeoutSec,TimeUnit.SECONDS))p.destroyForcibly();
   rd.join(1500);
   return sb.toString();
  }catch(Exception e){return "";}
 }
}
