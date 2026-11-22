package com.glm.pocket.agent.tools;

import com.glm.pocket.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.agent.loader.*;
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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSearch{
 public static String run(JSONObject a){
  String q=a.optString("query","");
  StringBuilder out=new StringBuilder();
  try{out.append(fetch("https://cn.bing.com/search?q="+URLEncoder.encode(q,"UTF-8"),
   "<li class=\"b_algo\".*?<h2><a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a></h2>(.*?)</li>"));}
  catch(Exception ignore){}
  if(out.length()==0)
   try{out.append(fetch("https://html.duckduckgo.com/html/?q="+URLEncoder.encode(q,"UTF-8"),
    "result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>.*?result__snippet[^>]*>(.*?)</a>"));}
   catch(Exception ignore){}
  return out.length()==0?"(搜索无结果)":out.toString();
 }
 public static String fetchUrl(JSONObject a){
  try{
   HttpURLConnection c=(HttpURLConnection)new URL(a.optString("url","")).openConnection();
   c.setRequestProperty("User-Agent","Mozilla/5.0");c.setConnectTimeout(15000);
   BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l);
   br.close();
   String t=sb.toString().replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>"," ")
    .replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim();
   return t.length()>2000?t.substring(0,2000):(t.isEmpty()?"(空页面)":t);
  }catch(Exception e){return "✗ "+e;}
 }
 static String fetch(String url,String rx) throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
  c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13; Mobile) Chrome/120");
  c.setConnectTimeout(12000);
  BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));
  StringBuilder sb=new StringBuilder();String l;
  while((l=br.readLine())!=null)sb.append(l);
  br.close();
  Matcher m=Pattern.compile(rx,Pattern.DOTALL).matcher(sb);
  StringBuilder out=new StringBuilder();
  int n=0;
  while(m.find()&&n<5){
   out.append("- ").append(strip(m.group(2))).append("\n  ").append(m.group(1))
    .append("\n  ").append(strip(m.group(3)),0,Math.min(150,strip(m.group(3)).length())).append('\n');
   n++;
  }
  return out.toString();
 }
 static String strip(String s){return s.replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim();}
}
