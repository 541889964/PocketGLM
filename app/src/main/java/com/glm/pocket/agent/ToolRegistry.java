package com.glm.pocket.agent;

import com.glm.pocket.MainActivity;
import com.glm.pocket.agent.loader.PluginLoader;
import com.glm.pocket.agent.tools.A11yService;
import com.glm.pocket.agent.tools.A11yTools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cordis式内核: 一切皆插件 — 内置7件+自进化10件, 插件动态并入 */
public class ToolRegistry{
 public static final Map<String,Tool> reg=new LinkedHashMap<>();
 static{
  put(tool("web_search","联网搜索(时效性问题必先用)","{\"query\":\"关键词\"}",false,a->WebSearch.run(a)));
  put(tool("open_url","抓取网页正文","{\"url\":\"https://..\"}",false,a->WebSearch.fetchUrl(a)));
  put(tool("python","在Termux执行Python3(计算/文件/自动化)","{\"code\":\"print(1+1)\"}",true,a->TermuxPython.run(a)));
  put(tool("list_apps","列出手机上的第三方应用","{}",false,a->A11yTools.listApps()));
  put(tool("open_app","启动某个应用","{\"pkg\":\"com.xx\"}",true,a->A11yTools.openApp(a.optString("pkg"))));
  put(tool("screen_read","读当前屏幕全部文本(UI理解)","{}",false,a->A11yService.screenText()));
  put(tool("input_text","向当前焦点输入框输入文字","{\"text\":\"你好\"}",true,a->
   A11yService.inputText(a.optString("text"))?"✓ 已输入":"✗ 失败(无焦点/无障碍未开)"));
  put(tool("screen_tap","点击屏幕坐标","{\"x\":540,\"y\":1200}",true,a->
   A11yService.tap(a.optInt("x"),a.optInt("y"))?"✓ 已点击":"✗ 失败"));
  put(tool("screen_swipe","滑动屏幕","{\"x1\":540,\"y1\":1500,\"x2\":540,\"y2\":600}",true,a->
   A11yService.swipe(a.optInt("x1"),a.optInt("y1"),a.optInt("x2"),a.optInt("y2"))?"✓ 已滑动":"✗ 失败"));
  put(tool("press_key","返回/主页/多任务","{\"key\":\"back|home|recents\"}",true,a->{
   String k=a.optString("key","back");
   boolean r="home".equals(k)?A11yService.home():"recents".equals(k)?A11yService.recents():A11yService.back();
   return r?"✓ "+k:"✗ 失败";}));
  put(tool("read_file","读自进化范围内文件","{\"path\":\"/storage/emulated/0/Download/pocketglm/plugins/x/main.py\"}",false,a->{
   try{return com.glm.pocket.self.SelfModifier.read(a.optString("path"));}
   catch(Exception e){return "✗ "+e.getMessage();}}));
  put(tool("write_file","写文件(自动备份,限范围)","{\"path\":\"..\",\"content\":\"..\"}",true,a->{
   try{return com.glm.pocket.self.SelfModifier.write(a.optString("path"),a.optString("content"));}
   catch(Exception e){return "✗ "+e.getMessage();}}));
  put(tool("edit_file","精确替换文件片段","{\"path\":\"..\",\"find\":\"..\",\"replace\":\"..\"}",true,a->{
   try{
    String p=a.optString("path"),s=com.glm.pocket.self.SelfModifier.read(p);
    String f=a.optString("find"),r=a.optString("replace");
    if(!s.contains(f))return "✗ find片段未找到";
    return com.glm.pocket.self.SelfModifier.write(p,s.replace(f,r));
   }catch(Exception e){return "✗ "+e.getMessage();}}));
  put(tool("syntax_check","装前质检(py_compile/node --check)","{\"path\":\"..\"}",false,a->
   SyntaxCheck.run(a.optString("path"))));
  put(tool("install_plugin","安装插件: 质检→热注册→立即可用","{\"dir\":\"<plugins下目录名>\"}",true,a->
   PluginInstall.run(a.optString("dir"))));
  put(tool("list_plugins","列出已装插件及功能","{}",false,a->PluginLoader.installedList()));
  put(tool("remove_plugin","卸载插件(删目录)","{\"name\":\"..\"}",true,a->{
   File d=new File(PluginLoader.root(),a.optString("name"));
   if(!d.exists())return "✗ 不存在";
   return rm(d)?"✓ 已卸载 "+a.optString("name"):"✗ 删除失败";}));
  put(tool("reload_ui","热重载界面(改UI后立即生效)","{}",true,a->{
   MainActivity.reloadUi();return "✓ UI已重载";}));
  put(tool("rollback","回滚文件到备份","{\"backup\":\"<备份路径>\",\"dest\":\"<目标>\"}",true,a->
   com.glm.pocket.self.SelfModifier.rollback(a.optString("backup"),a.optString("dest"))));
  put(tool("export_patch","导出Java补丁包zip→Download(给云编译)","{}",false,a->PatchExport.run()));
  put(tool("token_status","查看智谱凭据状态(有效期/健康/root)","{}",false,a->com.glm.pocket.net.TokenManager.status()));
  put(tool("token_grab","root从官方App(com.zhipuai.qingyan)重抓atoken,逐个真验后生效","{}",true,a->com.glm.pocket.net.TokenManager.grab()));
  put(tool("env_check","检查Termux本地环境(python/pip/node/git)","{}",false,a->com.glm.pocket.agent.tools.TermuxEnv.check()));
  put(tool("env_install","AI自动补环境:清华源+批量安装,速度优先","{\"what\":\"python|node|git|all\"}",true,a->com.glm.pocket.agent.tools.EnvDoctor.install(a.optString("what","all"))));
  put(tool("install_apk","安装APK(root pm install)","{\"path\":\"/storage/emulated/0/Download/pocketglm/apk/x.apk\"}",true,a->com.glm.pocket.self.CloudBuilder.installApk(a.optString("path"))));
  put(tool("github_set","App内配置GitHub凭据(用户名+PAT,优先于MT2)","{\"user\":\"xxx\",\"token\":\"ghp_xxx\"}",true,a->com.glm.pocket.net.GitHub.set(a.optString("user"),a.optString("token"))));
  put(tool("github_status","查看GitHub凭据与仓库状态","{}",false,a->com.glm.pocket.net.GitHub.status()));
  put(tool("cloud_build","推送→监控Actions→APK自动下载回手机","{}",true,a->com.glm.pocket.self.CloudBuilder.build()));
 }
 interface Fn{String go(org.json.JSONObject a) throws Exception;}
 static void put(Tool t){reg.put(t.name(),t);}
 static Tool tool(String n,String d,String s,boolean dg,Fn f){
  return new Tool(){
   public String name(){return n;}public String desc(){return d;}
   public String schema(){return s;}public boolean dangerous(){return dg;}
   public String run(org.json.JSONObject a) throws Exception{return f.go(a);}
  };
 }
 static boolean rm(File f){
  if(f.isDirectory()){File[]c=f.listFiles();if(c!=null)for(File x:c)rm(x);}
  return f.delete();
 }
 /** 内置工具清单(自我认知用) */
 public static String builtinList(){
  StringBuilder b=new StringBuilder();
  for(Tool t:reg.values())
   b.append("- ").append(t.name()).append(": ").append(t.desc())
    .append(" args=").append(t.schema()).append(t.dangerous()?" [需批准]":"").append('\n');
  return b.toString();
 }
 /** 插件工具名并入查找 */
 public static boolean has(String n){return reg.containsKey(n)||PluginLoader.loaded.size()>0&&isPluginTool(n);}
 static boolean isPluginTool(String n){
  for(com.glm.pocket.agent.loader.PluginDef p:PluginLoader.loaded.values())
   if(p.tools!=null)for(int i=0;i<p.tools.length();i++){
    JSONObject t=p.tools.optJSONObject(i);
    if(t!=null&&n.equals(t.optString("name")))return true;
   }
  return false;
 }
 /** 统一执行口: 危险操作过approval闸门, 插件工具分流 */
 public static String run(JSONObject call){
  String n=call.optString("tool","");
  Tool t=reg.get(n);
  try{
   if(t!=null){
    if(t.dangerous()&&!ApprovalGate.ask(MainActivity.CTX,
     "Agent请求执行: "+n+" "+call.optJSONObject("args")))return "✗ 用户拒绝";
    return t.run(call.optJSONObject("args")==null?new JSONObject():call.getJSONObject("args"));
   }
   if(isPluginTool(n))
    return PluginLoader.invoke(n,call.optJSONObject("args")==null?new JSONObject():call.getJSONObject("args"));
   return "✗ 未知工具: "+n;
  }catch(Exception e){return "✗ "+e;}
 }
 /** 平衡花括号解析模型输出的工具JSON */
 public static JSONObject extract(String text){
  int i=text.lastIndexOf("\"tool\"");
  if(i<0)return null;
  int s=text.lastIndexOf('{',i);
  if(s<0)return null;
  int depth=0;boolean instr=false,esc=false;
  for(int j=s;j<Math.min(s+4000,text.length());j++){
   char c=text.charAt(j);
   if(instr){
    if(esc)esc=false;
    else if(c=='\\')esc=true;
    else if(c=='"')instr=false;
   }else{
    if(c=='"')instr=true;
    else if(c=='{')depth++;
    else if(c=='}'){
     depth--;
     if(depth==0){
      try{
       JSONObject d=new JSONObject(text.substring(s,j+1));
       if(has(d.optString("tool","")))return d;
      }catch(Exception ignore){}
      return null;
     }
    }
   }
  }
  return null;
 }
}
