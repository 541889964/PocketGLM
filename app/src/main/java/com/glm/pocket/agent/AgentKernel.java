package com.glm.pocket.agent;
import android.webkit.*;
import org.json.*;
import com.glm.pocket.net.*;
import com.glm.pocket.agent.*;
import com.glm.pocket.*;

import com.glm.pocket.agent.loader.*;
import com.glm.pocket.agent.tools.*;
import com.glm.pocket.self.*;
import java.io.*;
import java.util.*;
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

import com.glm.pocket.MainActivity;
import com.glm.pocket.net.GlmClient;

import org.json.JSONObject;

/** DSH式: Harness拥有工具循环 · Watchdog · trace · 动态自我提示词 */
public class AgentKernel{
 public static final int MAX_STEPS=6;
 public interface Event{void ev(String type,String data);}
 public static void run(String task,String conv,boolean net,boolean full,Event cb){
  TraceLogger tl=new TraceLogger();
  tl.log("task",task);
  String mode=full?"完全":"Agent";
  StringBuilder msgs=new StringBuilder(SelfModel.build(mode,net)+"\n\n# 任务\n"+task);
  int steps=0,think=0;String model="";
  try{
   for(int k=1;k<=MAX_STEPS;k++){
    steps=k;cb.ev("step",String.valueOf(k));tl.log("step",""+k);
    GlmClient.Result r=GlmClient.stream(
     GlmClient.payload(msgs.toString(),conv,net,"deep_thinking","glm-5.3"),null);
    if(r.err!=null){cb.ev("err",r.err);tl.log("err",r.err);return;}
    if(!r.cid.isEmpty())conv=r.cid;
    think+=r.think;model=r.model;tl.log("model",r.text);
    JSONObject call=ToolRegistry.extract(r.text);
    if(call==null){
     cb.ev("final",r.text);
     cb.ev("done",steps+"步·思考"+think+"字·模型"+model);
     return;
    }
    cb.ev("tool",call.toString());tl.log("tool_call",call.toString());
    if(k==MAX_STEPS){
     cb.ev("obs","⚠ Watchdog: 达到最大步数, 强制收尾");
     tl.log("watchdog","");
     msgs.append("\n\n# 你的上一轮输出\n").append(r.text)
      .append("\n\n# OBSERVATION\n⚠ 步数已达上限, 基于已有信息直接给最终答案, 不要再调用工具");
     GlmClient.Result r2=GlmClient.stream(
      GlmClient.payload(msgs.toString(),conv,net,"deep_thinking","glm-5.3"),null);
     cb.ev("final",r2.err!=null?"(收尾失败)"+r2.err:r2.text);
     cb.ev("done",steps+"步(强制收尾)·思考"+think+"字·模型"+model);
     return;
    }
    String obs=ToolRegistry.run(call);
    if(obs.length()>1500)obs=obs.substring(0,1500);
    cb.ev("obs",obs.length()>300?obs.substring(0,300)+"…":obs);
    tl.log("obs",obs);
    String tail=r.text.length()>1200?r.text.substring(r.text.length()-1200):r.text;
    msgs.append("\n\n# 你的上一轮输出\n").append(tail)
     .append("\n\n# OBSERVATION\n").append(obs);
   }
  }finally{tl.close();}
 }
}
