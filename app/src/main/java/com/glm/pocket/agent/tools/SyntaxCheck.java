package com.glm.pocket.agent.tools;
import android.webkit.*;
import org.json.*;
import com.glm.pocket.agent.tools.*;

import com.glm.pocket.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.net.*;
import com.glm.pocket.self.*;
import java.io.*;
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

/** SWE-agent式 edit→lint 环: 装前质检, 不过禁止安装 */
public class SyntaxCheck{
 public static String run(String path){
  File f=new File(path);
  if(!f.exists())return "✗ 文件不存在";
  String n=f.getName();
  if(n.endsWith(".py"))return termux("python3 -m py_compile '"+path+"' && echo SYNTAX_OK");
  if(n.endsWith(".js")||n.endsWith(".json"))
   return "js/json 由PluginInstall做JSON.parse与括号校验";
  return "? 未知类型, 跳过";
 }
 static String termux(String cmd){
  try{
   android.content.Intent i=new android.content.Intent("com.termux.RUN_COMMAND")
    .setClassName("com.termux","com.termux.app.RunCommandService");
   i.putExtra("com.termux.RUN_COMMAND_PATH","/data/data/com.termux/files/usr/bin/sh");
   i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS",new String[]{"-c",cmd});
   com.glm.pocket.MainActivity.CTX.startService(i);
   Thread.sleep(3000);
   return "(质检命令已下发Termux, 结果以python插件stdout为准; 简化模式: 直接install观察运行报错)";
  }catch(Exception e){return "✗ "+e;}
 }
}
