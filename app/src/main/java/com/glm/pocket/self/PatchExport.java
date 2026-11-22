package com.glm.pocket.self;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PatchExport{
 public static String run(){
  try{
   File src=new File("/data/data/com.termux/files/home/pocketglm/app/src/main/java");
   if(!src.exists())src=new File("/storage/emulated/0/Download/pocketglm/src");
   if(!src.exists())return "找不到源码目录";
   File out=new File("/storage/emulated/0/Download/pocketglm_patch_"+System.currentTimeMillis()+".zip");
   FileOutputStream fo=new FileOutputStream(out);
   ZipOutputStream zos=new ZipOutputStream(fo);
   zipDir(src,src,zos,"");
   zos.close();fo.close();
   return "已打包 "+out.getAbsolutePath()+" (推GitHub→Actions自动出APK)";
  }catch(Exception e){return "E "+e;}
 }
 static void zipDir(File root,File d,ZipOutputStream zos,String prefix) throws Exception{
  File[] ds=d.listFiles();
  if(ds==null)return;
  for(File f:ds){
   if(f.isDirectory()){zipDir(root,f,zos,prefix+f.getName()+"/");continue;}
   zos.putNextEntry(new ZipEntry(prefix+f.getName()));
   FileInputStream fi=new FileInputStream(f);
   byte[] b=new byte[4096];int n;
   while((n=fi.read(b))>0)zos.write(b,0,n);
   fi.close();
   zos.closeEntry();
  }
 }
}
