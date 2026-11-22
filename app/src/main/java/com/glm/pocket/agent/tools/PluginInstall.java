package com.glm.pocket.agent.tools;
import android.webkit.*;
import org.json.*;
import java.io.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.agent.tools.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.*;

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

import com.glm.pocket.agent.loader.PluginLoader;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** 装插件: manifest合法→文件齐全→JSON校验→热注册→自我清单即时更新 */
public class PluginInstall{
 public static String run(String name){
  try{
   File d=new File(PluginLoader.root(),name);
   File mf=new File(d,"manifest.json");
   if(!mf.exists())return "✗ 缺 manifest.json";
   BufferedReader br=new BufferedReader(new FileReader(mf));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l);
   br.close();
   JSONObject j=new JSONObject(sb.toString()); // 非法JSON直接抛
   if(j.optString("name").isEmpty()||j.optString("type").isEmpty())
    return "✗ manifest缺 name/type";
   String ty=j.optString("type");
   if("python".equals(ty)&&!new File(d,j.optString("entry","")).exists())
    return "✗ python插件缺 entry 文件: "+j.optString("entry");
   int n=PluginLoader.scan();
   return "✓ 插件已热安装: "+j.optString("name")+" ["+ty+"] — 立即可用, 已注入我的自我清单"
    +" (当前插件总数:"+n+")";
  }catch(Exception e){return "✗ 安装失败(通常=JSON语法错): "+e;}
 }
}
