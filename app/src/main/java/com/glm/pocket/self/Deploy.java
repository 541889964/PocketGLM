package com.glm.pocket.self;

import com.glm.pocket.agent.tools.TermuxEnv;
import com.glm.pocket.net.GitHub;

/** 推送: 生成部署脚本→Termux执行(git+自动建仓) */
public class Deploy{
 public static String run(){
  String u=GitHub.user(),t=GitHub.token();
  if(u==null||t==null)
   return "✗ 未配置GitHub — github_set {\"user\":\"..\",\"token\":\"ghp_..\"} 或MT2两文件";
  String s="U='"+u+"'\nT='"+t+"'\n"
   +"cd $HOME/pocketglm || exit 9\n"
   +"command -v git >/dev/null 2>&1 || pkg install -y git >/dev/null 2>&1\n"
   +"command -v git >/dev/null 2>&1 || { echo NO_GIT; exit 8; }\n"
   +"command -v curl >/dev/null 2>&1 || pkg install -y curl >/dev/null 2>&1\n"
   +"curl -s -X POST -H \"Authorization: Bearer $T\" -H 'Accept: application/vnd.github+json' https://api.github.com/user/repos -d '{\"name\":\"PocketGLM\",\"private\":false}' >/dev/null 2>&1\n"
   +"git init >/dev/null 2>&1\n"
   +"git config user.email \"$U@users.noreply.github.com\"\n"
   +"git config user.name \"$U\"\n"
   +"git add -A\n"
   +"git commit -m \"deploy\" >/dev/null 2>&1 || true\n"
   +"git branch -M main 2>/dev/null\n"
   +"git remote remove origin 2>/dev/null\n"
   +"git remote add origin https://$U:$T@github.com/$U/PocketGLM.git\n"
   +"if git push -u origin main 2>/dev/null; then echo PUSH_OK\n"
   +"else git pull --rebase origin main >/dev/null 2>&1; git push -f -u origin main 2>/dev/null && echo PUSH_OK_FORCE || echo PUSH_FAIL\nfi\n";
  String out=TermuxEnv.runScript(s,180);
  if(out.contains("PUSH_OK"))return "✓ PUSH_OK 已推送到 "+u+"/PocketGLM (Actions自动编译)";
  if(out.contains("PUSH_OK_FORCE"))return "✓ PUSH_OK_FORCE 强推完成";
  if(out.contains("NO_GIT"))return "✗ Termux装不上git — 手动: pkg install git";
  if(out.contains("无输出"))return out+" (需先装Termux或授权root)";
  return "✗ 推送失败: "+out;
 }
}
