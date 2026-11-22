package com.glm.pocket.self;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;

/** L2可变层: 出厂assets→files/ui副本; 写前备份; 回滚; 范围锁 */
public class SelfModifier{
 public static File uiDir(){
  File f=new File(com.glm.pocket.MainActivity.CTX.getFilesDir(),"ui");
  f.mkdirs();return f;
 }
 public static File pluginDir(){return com.glm.pocket.agent.loader.PluginLoader.root();}
 /** 范围锁: 只许写 pocketglm/** 与 files/ui/** */
 public static boolean inScope(String path){
  try{
   String p=new File(path).getCanonicalPath();
   return p.startsWith(pluginDir().getCanonicalPath())
    ||p.startsWith(uiDir().getCanonicalPath())
    ||p.startsWith(new File(pluginDir().getParentFile(),"trace").getCanonicalPath());
  }catch(Exception e){return false;}
 }
 public static String backup(String path){
  try{
   File src=new File(path);
   if(!src.exists())return "";
   File bdir=new File(pluginDir().getParentFile(),"backups");
   bdir.mkdirs();
   File b=new File(bdir,src.getName()+"_"+System.currentTimeMillis()+".bak");
   copy(src,b);
   return b.getAbsolutePath();
  }catch(Exception e){return "";}
 }
 public static void copy(File src,File dst) throws Exception{
  InputStream is=new FileInputStream(src);
  FileOutputStream os=new FileOutputStream(dst);
  byte[]buf=new byte[8192];int n;
  while((n=is.read(buf))>0)os.write(buf,0,n);
  os.close();is.close();
 }
 public static String read(String path) throws Exception{
  if(!inScope(path))throw new Exception("越界: 只许读写 "+pluginDir()+" 与 ui目录");
  BufferedReader br=new BufferedReader(new FileReader(path));
  StringBuilder sb=new StringBuilder();String l;
  while((l=br.readLine())!=null)sb.append(l).append('\n');
  br.close();
  return sb.toString();
 }
 public static String write(String path,String content) throws Exception{
  if(!inScope(path))throw new Exception("越界: 只许写 "+pluginDir()+" 与 ui目录");
  String b=backup(path);
  File f=new File(path);
  f.getParentFile().mkdirs();
  FileWriter w=new FileWriter(f);
  w.write(content);w.close();
  return b.isEmpty()?"(新建,无备份)":"已备份→"+b;
 }
 public static String rollback(String backupPath,String dest){
  try{
   if(!inScope(dest))return "✗ 越界";
   copy(new File(backupPath),new File(dest));
   return "✓ 已回滚 "+dest;
  }catch(Exception e){return "✗ "+e;}
 }
 /** L2: 确保可变UI副本存在(首启从assets复制); 删副本=恢复出厂 */
 public static void ensureUi(){
  File idx=new File(uiDir(),"index.html");
  if(idx.exists())return;
  try{
   InputStream is=com.glm.pocket.MainActivity.CTX.getAssets().open("index.html");
   FileOutputStream os=new FileOutputStream(idx);
   byte[]buf=new byte[8192];int n;
   while((n=is.read(buf))>0)os.write(buf,0,n);
   os.close();is.close();
  }catch(Exception ignore){}
 }
}
