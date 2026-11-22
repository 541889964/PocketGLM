package com.glm.pocket.agent.tools;
import android.webkit.*;
import com.glm.pocket.agent.tools.*;

import com.glm.pocket.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.agent.loader.*;
import com.glm.pocket.net.*;
import com.glm.pocket.self.*;
import java.io.*;
import java.util.*;
import java.text.*;
import org.json.*;
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

/** 补环境提速: 清华源 / 跳过已装 / 批量一个事务 / pip国内源 / root装完把属主修回Termux用户 */
public class EnvDoctor{
 public static String install(String what){
  boolean py=what.contains("py")||what.contains("all");
  boolean nd=what.contains("node")||what.contains("all");
  boolean gt=what.contains("git")||what.contains("all");
  StringBuilder s=new StringBuilder();
  s.append("M=https://mirrors.tuna.tsinghua.edu.cn/termux\n");
  s.append("S=$PREFIX/etc/apt/sources.list\n");
  s.append("[ -f \"$S\" ] && sed -i \"s@https://termux.net@$M@g;s@http://termux.net@$M@g\" \"$S\"\n");
  s.append("for f in $PREFIX/etc/apt/sources.list.d/*.list; do [ -f \"$f\" ] && sed -i \"s@https://termux.net@$M@g;s@http://termux.net@$M@g\" \"$f\"; done\n");
  s.append("apt-get update -y -qq\n");
  s.append("P=\"\"\n");
  if(py){s.append("command -v python3 >/dev/null 2>&1 || P=\"$P python\"\n");
         s.append("command -v pip3 >/dev/null 2>&1 || P=\"$P python-pip\"\n");}
  if(nd)s.append("command -v node >/dev/null 2>&1 || P=\"$P nodejs\"\n");
  if(gt)s.append("command -v git >/dev/null 2>&1 || P=\"$P git\"\n");
  s.append("if [ -n \"$P\" ]; then pkg install -y $P; fi\n");
  s.append("command -v pip3 >/dev/null 2>&1 && pip3 config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple >/dev/null 2>&1\n");
  s.append("chown -R $(stat -c '%u:%g' /data/data/com.termux 2>/dev/null) $PREFIX 2>/dev/null || true\n");
  s.append("echo === FINAL ===\n");
  s.append("for t in python3 pip3 node git; do command -v $t >/dev/null 2>&1 && echo \"$t OK\" || echo \"$t MISSING\"; done\n");
  s.append("echo ENV_DONE\n");
  return TermuxEnv.runScript(s.toString(),600);
 }
}
