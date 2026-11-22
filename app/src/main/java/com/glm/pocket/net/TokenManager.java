package com.glm.pocket.net;

import com.glm.pocket.agent.loader.*;
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

import com.glm.pocket.agent.tools.RootShell;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 凭据自愈v2 — 实测: ~179天长效token=refresh类(本接口401不认),
 *  只有<24h短效token可用 → 逐个HTTP200真验后择优。
 *  全失效时拉起官方App(com.zhipuai.qingyan)刷新token再抓。 */
public class TokenManager{
 static final String TOKEN_FILE="/storage/emulated/0/Download/zhipu_tokens.txt";
 static final String RX="eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}";
 static final String OFFICIAL_PKG="com.zhipuai.qingyan";

 /** 指定token真验一次(全套签名头打history接口) */
 static int verifyToken(String tok,String dev){
  try{
   String ts=String.valueOf(System.currentTimeMillis()),nc=GlmClient.hex(16);
   HttpURLConnection c=(HttpURLConnection)
    new URL(GlmClient.HIST+"?assistant_id="+GlmClient.MAIN).openConnection();
   c.setConnectTimeout(15000);c.setReadTimeout(20000);
   c.setRequestProperty("Authorization","Bearer "+tok);
   c.setRequestProperty("User-Agent","okhttp/4.9.3");
   c.setRequestProperty("X-Device-Id",dev);
   c.setRequestProperty("App-Name","chatglm");
   c.setRequestProperty("X-App-Platform","android");
   c.setRequestProperty("X-App-Version","0.0.1");
   c.setRequestProperty("X-App-fr","default");
   c.setRequestProperty("X-Lang","zh");
   c.setRequestProperty("X-Sign",GlmClient.sign(ts,nc));
   c.setRequestProperty("X-Nonce",nc);
   c.setRequestProperty("X-Timestamp",ts);
   c.setRequestProperty("X-Request-Id",GlmClient.hex(16));
   return c.getResponseCode();
  }catch(Exception e){return -1;}
 }

 public static String grab(){
  String r=scanOnce();
  if(r.contains("★"))return r;
  try{ // 全失效→拉起官方App让它刷新token→回桌面→再抓
   android.content.Intent i=com.glm.pocket.MainActivity.CTX.getPackageManager()
    .getLaunchIntentForPackage(OFFICIAL_PKG);
   if(i!=null){
    i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    com.glm.pocket.MainActivity.CTX.startActivity(i);
    Thread.sleep(9000);
    try{com.glm.pocket.agent.tools.A11yService.home();}catch(Exception ignore){}
    return scanOnce();
   }
  }catch(Exception ignore){}
  return r;
 }

 static String scanOnce(){
  StringBuilder rep=new StringBuilder();
  String out=RootShell.su("for d in "+OFFICIAL_PKG+"; do grep -rhoaE '"+RX+"' "
   +"/data/data/$d/shared_prefs /data/data/$d/files /data/data/$d/databases "
   +"/data/data/$d/app_webview /data/data/$d/cache 2>/dev/null; done "
   +"| sort -u | head -20",25);
  List<String> cands=new ArrayList<>();
  Matcher m=Pattern.compile(RX).matcher(out);
  while(m.find())if(!cands.contains(m.group()))cands.add(m.group());
  String old=fileToken();
  if(old!=null&&!cands.contains(old))cands.add(old);
  if(cands.isEmpty())return "✗ 0候选 — 需root(Magisk授权)+官方App登录态";
  rep.append("候选").append(cands.size()).append(": ");
  String best=null;long bestExp=0;
  for(String t:cands){
   JSONObject p=payload(t);
   if(p==null||!p.has("device_id"))continue;
   long exp=p.optLong("exp",0);
   if(exp>0&&exp*1000<System.currentTimeMillis()){rep.append("·过期 ");continue;}
   int code=verifyToken(t,p.optString("device_id"));
   long h=Math.max(0,(exp*1000-System.currentTimeMillis())/3600000L);
   if(code==200){
    rep.append("✓").append(h).append("h ");
    if(exp>=bestExp){bestExp=exp;best=t;}
   }else rep.append("✗").append(code).append(" ");
  }
  if(best==null)
   return rep+"\n✗ 无200可用token(长效的是refresh类, 接口不认)";
  try{
   Writer w=new FileWriter(TOKEN_FILE,false);
   w.write(best+"\n");w.close();
  }catch(Exception e){return rep+"\n✗ 写文件失败: "+e;}
  GlmClient.reset();
  return rep+"\n★ 已保存可用token(剩"
   +Math.max(0,(bestExp*1000-System.currentTimeMillis())/3600000L)
   +"小时) — 已即刻生效";
 }

 static String fileToken(){
  try{
   BufferedReader br=new BufferedReader(new FileReader(TOKEN_FILE));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l);
   br.close();
   String raw=sb.toString();
   Matcher m=Pattern.compile(RX).matcher(raw);
   if(m.find())return m.group();
   Matcher m2=Pattern.compile("name=\"atoken\">([^<]+)<").matcher(raw);
   return m2.find()?m2.group(1).trim():null;
  }catch(Exception e){return null;}
 }

 static JSONObject payload(String jwt){
  try{
   String[]p=jwt.split("\\.");
   if(p.length<2)return null;
   String b=p[1];while(b.length()%4!=0)b+="=";
   return new JSONObject(new String(Base64.getUrlDecoder().decode(b),
    java.nio.charset.StandardCharsets.UTF_8));
  }catch(Exception e){return null;}
 }

 public static int healthCode(){
  try{
   HttpURLConnection c=(HttpURLConnection)
    new URL(GlmClient.HIST+"?assistant_id="+GlmClient.MAIN).openConnection();
   for(java.util.Map.Entry<String,String> e:GlmClient.headers(false).entrySet())
    c.setRequestProperty(e.getKey(),e.getValue());
   return c.getResponseCode();
  }catch(Exception e){return -1;}
 }

 public static String status(){
  try{
   String tok=fileToken();
   JSONObject j=new JSONObject();
   if(tok==null)j.put("present",false)
    .put("hint","无token — token_grab(root自动抓)或手动放文件");
   else{
    JSONObject p=payload(tok);
    j.put("present",true)
     .put("device_id",p==null?"?":p.optString("device_id","?"))
     .put("hours_left",p!=null&&p.has("exp")
      ?Math.max(0,(p.optLong("exp")*1000-System.currentTimeMillis())/3600000L):-1)
     .put("root",RootShell.available());
   }
   j.put("health_http",healthCode());
   return j.toString();
  }catch(Exception e){return "{\"err\":\""+e+"\"}";}
 }
}
