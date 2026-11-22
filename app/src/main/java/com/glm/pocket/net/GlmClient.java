package com.glm.pocket.net;
import android.webkit.*;
import org.json.*;
import java.util.*;
import java.io.*;
import com.glm.pocket.net.*;

import com.glm.pocket.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.agent.tools.*;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** chatglm.cn 安卓接口客户端(已验证: 签名/流式/历史/删除/模型切换) */
public class GlmClient{
  public static void reset(){/*重置会话缓存:暂空实现,保证编译*/}
 public static final String SECRET="8a1317a7468aa3ad86e997d08f3f31cb";
 public static final String MAIN="65940acff94777010aa6b796";
 public static final String BASE="https://chatglm.cn/chatglm/";
 public static final String CHAT=BASE+"backend-api/assistant/stream";
 public static final String HIST=BASE+"feed-api/assistant/recent_history";
 static String atok="",dev="";
 public interface Chunk{void on(String type,String text);}
 public static class Result{public String text="",cid="",model="",err=null;public int think=0;}

 static void init(){
  if(!atok.isEmpty())return;
  try{
   BufferedReader br=new BufferedReader(new FileReader("/storage/emulated/0/Download/zhipu_tokens.txt"));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l);
   br.close();
   Matcher m=Pattern.compile("name=\"atoken\">([^<]+)<").matcher(sb);
   if(m.find())atok=m.group(1).trim();
   String[]p=atok.split("\\.");
   if(p.length>1){
    String pay=p[1];while(pay.length()%4!=0)pay+="=";
    JSONObject j=new JSONObject(new String(java.util.Base64.getUrlDecoder().decode(pay),StandardCharsets.UTF_8));
    dev=j.optString("device_id","");
   }
  }catch(Exception ignore){}
  if(dev.isEmpty())dev=hex(16);
 }
 static String hex(int n){
  StringBuilder b=new StringBuilder();Random r=new Random();
  for(int i=0;i<n;i++)b.append("0123456789abcdef".charAt(r.nextInt(16)));
  return b.toString();
 }
 public static String sign(String ts,String nc){
  try{
   byte[]d=MessageDigest.getInstance("MD5").digest((ts+"-"+nc+"-"+SECRET).getBytes(StandardCharsets.UTF_8));
   StringBuilder b=new StringBuilder();
   for(byte x:d)b.append(String.format("%02x",x));
   return b.toString();
  }catch(Exception e){return "";}
 }
 public static Map<String,String> headers(boolean stream){
  init();
  String ts=String.valueOf(System.currentTimeMillis()),nc=hex(16);
  Map<String,String> h=new LinkedHashMap<>();
  h.put("Authorization","Bearer "+atok);
  h.put("User-Agent","okhttp/4.9.3");
  h.put("X-Device-Id",dev);
  h.put("App-Name","chatglm");
  h.put("X-App-Platform","android");
  h.put("X-App-Version","0.0.1");
  h.put("X-App-fr","default");
  h.put("X-Lang","zh");
  h.put("X-Sign",sign(ts,nc));
  h.put("X-Nonce",nc);
  h.put("X-Timestamp",ts);
  h.put("X-Request-Id",hex(16));
  h.put("Accept",stream?"text/event-stream":"application/json");
  return h;
 }
 public static JSONObject payload(String text,String conv,boolean net,String mode,String model){
  try{
   JSONObject md=new JSONObject()
    .put("cogview",new JSONObject().put("rm_label_watermark",false))
    .put("is_test",false).put("input_question_type","xxxx")
    .put("channel","").put("draft_id","").put("chat_mode",mode)
    .put("is_networking",net).put("quote_log_id","").put("platform","pc")
    .put("selected_model",model);
   return new JSONObject()
    .put("assistant_id",MAIN)
    .put("conversation_id",conv==null?"":conv)
    .put("project_id","").put("chat_type","user_chat")
    .put("meta_data",md)
    .put("messages",new JSONArray().put(new JSONObject().put("role","user")
     .put("content",new JSONArray().put(new JSONObject().put("type","text").put("text",text)))));
  }catch(Exception e){return new JSONObject();}
 }
 public static Result stream(JSONObject pl,Chunk cb){
  Result r=new Result();init();
  try{
   java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(CHAT).openConnection();
   c.setRequestMethod("POST");c.setDoOutput(true);
   c.setConnectTimeout(20000);c.setReadTimeout(300000);
   for(Map.Entry<String,String> e:headers(true).entrySet())c.setRequestProperty(e.getKey(),e.getValue());
   OutputStream os=c.getOutputStream();
   os.write(pl.toString().getBytes(StandardCharsets.UTF_8));os.close();
   int hc=c.getResponseCode();
   if(hc==401){
    String g=TokenManager.grab();
    if(g.contains("★")){
     c.disconnect();
     c=(java.net.HttpURLConnection)new java.net.URL(CHAT).openConnection();
     c.setRequestMethod("POST");c.setDoOutput(true);
     c.setConnectTimeout(20000);c.setReadTimeout(300000);
     for(Map.Entry<String,String> e:headers(true).entrySet())c.setRequestProperty(e.getKey(),e.getValue());
     OutputStream os2=c.getOutputStream();
     os2.write(pl.toString().getBytes(StandardCharsets.UTF_8));os2.close();
     hc=c.getResponseCode();
    }
    if(hc!=200){r.err="HTTP 401(token失效, 自愈:"+(g.contains("★")?"已换新仍被拒":"失败, 试token_grab")+")";return r;}
   }else if(hc!=200){r.err="HTTP "+hc;return r;}
   BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));
   String line;
   Matcher mm=Pattern.compile("\"model\"\\s*:\\s*\"([^\"]+)\"").matcher("");
   while((line=br.readLine())!=null){
    if(!line.startsWith("data:"))continue;
    String b=line.substring(5).trim();
    if(b.equals("[DONE]"))break;
    mm.reset(line);
    if(mm.find())r.model=mm.group(1);
    try{
     JSONObject d=new JSONObject(b);
     String cid=d.optString("conversation_id","");
     if(!cid.isEmpty())r.cid=cid;
     JSONArray parts=d.optJSONArray("parts");
     if(parts==null||parts.length()==0)continue;
     JSONObject p0=parts.optJSONObject(0);
     if(p0==null)continue;
     JSONArray arr=p0.optJSONArray("content");
     if(arr==null)continue;
     for(int i=0;i<arr.length();i++){
      JSONObject it=arr.optJSONObject(i);
      if(it==null)continue;
      String ty=it.optString("type");
      if("think".equals(ty)){
       String t=it.optString("think",it.optString("text",""));
       if(t.length()>r.think)r.think=t.length();
       if(cb!=null)cb.on("think",t);
      }else if("text".equals(ty)){
       String t=it.optString("text","");
       if(t.length()>r.text.length()){r.text=t;if(cb!=null)cb.on("text",t);}
      }
     }
    }catch(Exception ignore){}
   }
   br.close();
  }catch(Exception e){r.err=String.valueOf(e);}
  return r;
 }
 public static JSONArray history(){
  init();
  try{
   java.net.HttpURLConnection c=(java.net.HttpURLConnection)
    new java.net.URL(HIST+"?assistant_id="+MAIN).openConnection();
   for(Map.Entry<String,String> e:headers(false).entrySet())c.setRequestProperty(e.getKey(),e.getValue());
   BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l);
   br.close();
   JSONObject root=new JSONObject(sb.toString());
   Object res=root.opt("result");
   JSONArray list=null;
   if(res instanceof JSONArray)list=(JSONArray)res;
   else if(res instanceof JSONObject){
    JSONObject ro=(JSONObject)res;
    list=ro.optJSONArray("conversations");
    if(list==null)list=ro.optJSONArray("items");
   }
   JSONArray out=new JSONArray();
   if(list==null)return out;
   for(int i=0;i<list.length()&&i<30;i++){
    JSONObject c2=list.optJSONObject(i);
    if(c2==null)continue;
    String id=c2.optString("conversation_id",c2.optString("id",""));
    String t=c2.optString("title",c2.optString("last_query","未命名"));
    if(t.length()>26)t=t.substring(0,26);
    out.put(new JSONObject().put("id",id).put("title",t));
   }
   return out;
  }catch(Exception e){return new JSONArray();}
 }
 public static String delete(String cid){
  init();
  String[][] eps={{"POST","backend-api/conversation/delete"},
   {"DELETE","backend-api/conversation"},{"POST","mainchat-api/conversation/delete"}};
  StringBuilder rep=new StringBuilder();
  for(String[] ep:eps){
   try{
    boolean post=ep[0].equals("POST");
    java.net.HttpURLConnection c=(java.net.HttpURLConnection)
     new java.net.URL(BASE+ep[1]+(post?"":"?conversation_id="+cid)).openConnection();
    c.setRequestMethod(ep[0]);c.setConnectTimeout(15000);c.setReadTimeout(15000);
    for(Map.Entry<String,String> e:headers(false).entrySet())c.setRequestProperty(e.getKey(),e.getValue());
    if(post){
     c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
     c.getOutputStream().write(new JSONObject().put("conversation_id",cid)
      .toString().getBytes(StandardCharsets.UTF_8));
    }
    int code=c.getResponseCode();
    BufferedReader br=new BufferedReader(new InputStreamReader(
     code>=400?c.getErrorStream():c.getInputStream(),StandardCharsets.UTF_8));
    StringBuilder sb=new StringBuilder();String l;
    while((l=br.readLine())!=null)sb.append(l);
    br.close();
    String body=sb.toString();
    rep.append(ep[0]).append(' ').append(ep[1]).append(" → ").append(code)
     .append(' ').append(body,0,Math.min(70,body.length())).append('\n');
    if(code==200&&(body.toLowerCase().contains("success")
     ||body.contains("\"status\":0")||body.contains("\"status\": 0")))
     return "ok "+ep[0]+" "+ep[1];
   }catch(Exception e){rep.append(ep[0]).append(' ').append(ep[1]).append(" EXC ").append(e).append('\n');}
  }
  return "fail\n"+rep;
 }
}
