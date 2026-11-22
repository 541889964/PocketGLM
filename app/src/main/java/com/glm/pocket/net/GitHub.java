package com.glm.pocket.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** GitHub客户端 — 凭据优先级: App内置配置(github_set写入) > MT2两文件 */
public class GitHub{
 static final String CFG="/storage/emulated/0/Download/pocketglm/github.txt";
 static final String MT2U="/storage/emulated/0/MT2/用户名.txt";
 static final String MT2T="/storage/emulated/0/MT2/Token.txt";
 static String u,t;

 static String rd(String f,int n){
  try{
   BufferedReader br=new BufferedReader(new FileReader(f));
   String l=null;for(int i=0;i<n;i++)l=br.readLine();
   br.close();return l==null?null:l.trim();
  }catch(Exception e){return null;}
 }
 public static String user(){
  if(u!=null)return u;
  if(new File(CFG).exists())u=rd(CFG,1);
  if(u==null)u=rd(MT2U,1);
  return u;
 }
 public static String token(){
  if(t!=null)return t;
  if(new File(CFG).exists())t=rd(CFG,2);
  if(t==null)t=rd(MT2T,1);
  return t;
 }
 /** App内配凭据: 写CFG并即刻生效(优先于MT2) */
 public static String set(String user,String token){
  try{
   if(user==null||user.isEmpty()||token==null||token.isEmpty())
    return "✗ user和token都不能为空";
   Writer w=new FileWriter(CFG,false);
   w.write(user+"\n"+token+"\n");w.close();
   u=null;t=null;
   return "✓ 已保存(优先于MT2) "+status();
  }catch(Exception e){return "✗ 写入失败: "+e;}
 }
 public static String status(){
  try{
   org.json.JSONObject j=new org.json.JSONObject();
   j.put("source",new File(CFG).exists()?"app内置配置":"MT2文件");
   String lg=who();
   if(lg==null)j.put("pat_ok",false).put("hint",
    "PAT无效或未配置 — github.com→Settings→Developer settings→Tokens(classic)勾repo, 或github_set");
   else{
    j.put("pat_ok",true).put("login",lg);
    Resp r=api("GET","/repos/"+lg+"/PocketGLM",null,true);
    j.put("repo",r.code==200?"已存在":"待首推自动建");
   }
   return j.toString();
  }catch(Exception e){return "{\"err\":\""+e+"\"}";}
 }
 public static String who(){
  try{
   Resp r=api("GET","/user",null,true);
   if(r.code!=200)return null;
   Matcher m=Pattern.compile("\"login\":\"([^\"]+)\"").matcher(r.body);
   return m.find()?m.group(1):null;
  }catch(Exception e){return null;}
 }
 public static class Resp{public int code;public String body;
  public Resp(int c,String b){code=c;body=b;}}
 public static Resp api(String method,String path,String json,boolean auth){
  try{
   HttpURLConnection c=(HttpURLConnection)new URL("https://api.github.com"+path).openConnection();
   c.setRequestMethod(method);
   c.setRequestProperty("Accept","application/vnd.github+json");
   c.setRequestProperty("User-Agent","PocketGLM");
   if(auth){String tk=token();if(tk==null)return new Resp(-1,"无GitHub凭据");
    c.setRequestProperty("Authorization","Bearer "+tk);}
   if(json!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
    OutputStream os=c.getOutputStream();os.write(json.getBytes(StandardCharsets.UTF_8));os.close();}
   int code=c.getResponseCode();
   InputStream is=code<400?c.getInputStream():c.getErrorStream();
   BufferedReader br=new BufferedReader(new InputStreamReader(is));
   StringBuilder sb=new StringBuilder();String l;
   while((l=br.readLine())!=null)sb.append(l);
   br.close();
   return new Resp(code,sb.toString());
  }catch(Exception e){return new Resp(-1,e+"");}
 }
}
