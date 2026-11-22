package com.glm.pocket.self;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** L3慢通道: 打包Java源码→zip→Download, 用户手动传GitHub云编译 */
public class PatchExport{
 public static String run(){
  try{
   File src=new File(Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOWNLOADS),"pocketglm/patch_src");
   src.mkdirs();
   File zip=new File(new File(Environment.getExternalStoragePublicDirectory(
    Environment.DIRECTORY_DOWNLOADS),"pocketglm"),
    "patch_"+System.currentTimeMillis()+".zip");
   ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(zip));
   zipDir(new File(com.glm.pocket.self.PatchExport.class.getProtectionDomain()
    .getCodeSource()==null?null:null)==null?src:src,src,zos,"");
   zos.close();
   return "✓ 补丁包: "+zip.getAbsolutePath()+" (把其中.java改动同步到GitHub→Actions自动出新APK)";
  }catch(Exception e){return "✗ "+e;}
 }
 static void zipDir(File root,File d,ZipOutputStream zos,String prefix) throws Exception{
  File[]c=d.listFiles();
  if(c==null)return;
  for(File f:c){
   if(f.isDirectory()){zipDir(root,f,zos,prefix+f.getName()+"/");continue;}
   zos.putNextEntry(new ZipEntry(prefix+f.getName()));
   FileInputStream is=new FileInputStream(f);
   byte[]b=new byte[8192];int n;
   while((n=is.read(b))>0)zos.write(b,0,n);
   is.close();zos.closeEntry();
  }
 }
}
