package com.glm.pocket.agent;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Codex式approval三档: off(拒绝)/ask(询问)/auto(放行) — 危险操作过闸 */
public class ApprovalGate{
 public static CountDownLatch latch;
 public static volatile boolean verdict;
 public static String mode(Context c){
  return c.getSharedPreferences("pg",0).getString("approval","ask");
 }
 /** @return true=放行 */
 public static boolean ask(Context c,String desc){
  String m=mode(c);
  if("auto".equals(m))return true;
  if("off".equals(m))return false;
  try{
   verdict=false;latch=new CountDownLatch(1);
   com.glm.pocket.MainActivity.self().post(()->com.glm.pocket.MainActivity.self()
    .evaluateJavascript("window.__approve('"+desc.replace("'","\\'")+"')",null));
   latch.await(90,TimeUnit.SECONDS);
   return verdict;
  }catch(Exception e){return false;}
 }
 public static void resolve(boolean ok){
  verdict=ok;
  if(latch!=null)latch.countDown();
 }
}
