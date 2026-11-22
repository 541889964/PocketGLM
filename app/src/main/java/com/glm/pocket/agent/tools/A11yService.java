package com.glm.pocket.agent.tools;
import android.os.*;
import android.accessibilityservice.*;
import android.view.*;
import android.webkit.*;
import java.util.*;
import com.glm.pocket.agent.tools.*;

import com.glm.pocket.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.net.*;
import com.glm.pocket.self.*;
import java.io.*;
import java.text.*;
import org.json.*;
import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.provider.*;
import android.text.*;
import android.widget.*;
import android.media.*;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/** 无障碍服务: UI-TARS式看屏-操作闭环 的系统通道 */
public class A11yService extends AccessibilityService{
 public static A11yService I;
 @Override protected void onServiceConnected(){I=this;}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){}
 @Override public void onInterrupt(){}
 public static boolean ready(){return I!=null;}
 /** 读屏: 收集当前窗口全部可见文本 */
 public static String screenText(){
  if(I==null)return "✗ 无障碍服务未开启";
  StringBuilder b=new StringBuilder();
  List<AccessibilityNodeInfo> out=new ArrayList<>();
  walk(I.getRootInActiveWindow(),out,0);
  for(AccessibilityNodeInfo n:out){
   CharSequence t=n.getText();
   CharSequence d=n.getContentDescription();
   if(t!=null&&t.length()>0)b.append('[').append(cls(n)).append("] ").append(t).append('\n');
   else if(d!=null&&d.length()>0)b.append('[').append(cls(n)).append("] ").append(d).append('\n');
  }
  return b.length()==0?"(屏幕无文本)":b.substring(0,Math.min(1800,b.length()));
 }
 static String cls(AccessibilityNodeInfo n){
  String c=String.valueOf(n.getClassName());
  int i=c.lastIndexOf('.');
  return i<0?c:c.substring(i+1);
 }
 static void walk(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> out,int depth){
  if(n==null||depth>25||out.size()>120)return;
  out.add(n);
  for(int i=0;i<n.getChildCount();i++)walk(n.getChild(i),out,depth+1);
 }
 /** 点击坐标(API24+手势) */
 public static boolean tap(int x,int y){
  if(I==null)return false;
  Path p=new Path();p.moveTo(x,y);
  GestureDescription.Builder g=new GestureDescription.Builder();
  g.addStroke(new GestureDescription.StrokeDescription(p,0,60));
  return I.dispatchGesture(g.build(),null,null);
 }
 public static boolean swipe(int x1,int y1,int x2,int y2){
  if(I==null)return false;
  Path p=new Path();p.moveTo(x1,y1);p.lineTo(x2,y2);
  GestureDescription.Builder g=new GestureDescription.Builder();
  g.addStroke(new GestureDescription.StrokeDescription(p,0,300));
  return I.dispatchGesture(g.build(),null,null);
 }
 /** 向当前焦点输入框写文本 */
 public static boolean inputText(String t){
  if(I==null)return false;
  AccessibilityNodeInfo n=I.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
  if(n==null)return false;
  Bundle args=new Bundle();
  args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,t);
  return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
 }
 public static boolean back(){if(I==null)return false;
  return I.performGlobalAction(GLOBAL_ACTION_BACK);}
 public static boolean home(){if(I==null)return false;
  return I.performGlobalAction(GLOBAL_ACTION_HOME);}
 public static boolean recents(){if(I==null)return false;
  return I.performGlobalAction(GLOBAL_ACTION_RECENTS);}
}
