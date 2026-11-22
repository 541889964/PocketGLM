package com.glm.pocket;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class MainActivity extends Activity{

 public static Context CTX;
 public static MainActivity self;

 /* ═══ 适配区:CI若报签名错误,只改这里4行 ═══ */
 static final String MODEL="glm-5.3";
 static final String NET="";
 static final String[] THINK_L={"普通","深度","极致"};
 static final String[] THINK_V={"normal","deep_thinking","extreme"}; //仅deep_thinking已确认

 String conv="",thinkMode="deep_thinking";
 int thinkIdx=1,bgIdx=0;
 volatile boolean stop=false;
 TextView live=null,title,meta,trace,plugins;
 LinearLayout chat,side,pills,root;
 ScrollView chatScroll;
 EditText input;

 static final int[][] BGP={{0xFF0E1117,0xFF1A1F2B},{0xFF141428,0xFF2A1F3D},{0xFF0B1614,0xFF12241F}};
 public static final List<View> UI_PLUGINS=new ArrayList<>();

 public static void addPluginView(View v){UI_PLUGINS.add(v);if(self!=null)self.refreshPlugins();}
 public static void push(String type,String json){
  if(self==null)return;
  self.runOnUiThread(()->self.route(type,json));
 }

 protected void onCreate(Bundle b){
  super.onCreate(b);
  self=this;CTX=this;
  final boolean pad=getResources().getConfiguration().smallestScreenWidthDp>=600;
  try{bgIdx=Integer.parseInt(getSharedPreferences("ui",0).getString(pad?"bg_pad":"bg_phone","0"));bgIdx=bgIdx<0||bgIdx>=BGP.length?0:bgIdx;}catch(Exception e){bgIdx=0;}

  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);

  LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);
  head.setPadding(dp(16),dp(10),dp(16),dp(8));
  title=new TextView(this);title.setText("PocketGLM");
  title.setTextSize(18);title.setTypeface(Typeface.DEFAULT_BOLD);title.setTextColor(Color.WHITE);
  meta=new TextView(this);meta.setText(pad?"平板模式 · 双栏":"手机模式 · 单栏 · 长按标题换壁纸");
  meta.setTextSize(12);meta.setTextColor(0xFF9AA3B2);
  head.addView(title);head.addView(meta);
  title.setOnLongClickListener(v->{bgIdx=(bgIdx+1)%BGP.length;applyBg(pad);return true;});
  root.addView(head);

  HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);
  pills=new LinearLayout(this);pills.setPadding(dp(12),dp(4),dp(12),dp(4));
  for(int i=0;i<THINK_L.length;i++){final int idx=i;
   pills.addView(pill(THINK_L[i],()->{thinkIdx=idx;thinkMode=THINK_V[idx];paintPills();push("trace",jjson("模式","思考="+THINK_L[idx]));}));}
  pills.addView(pill("停止",()->{stop=true;sealBubble();push("trace",jjson("控制","已请求停止");}));
  pills.addView(pill("轨迹",()->{side.setVisibility(side.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);}));
  paintPills();
  hs.addView(pills);root.addView(hs);

  LinearLayout body=new LinearLayout(this);
  body.setOrientation(pad?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);

  chat=new LinearLayout(this);chat.setOrientation(LinearLayout.VERTICAL);
  chat.setPadding(dp(12),dp(8),dp(12),dp(8));
  chatScroll=new ScrollView(this);chatScroll.addView(chat);
  LinearLayout.LayoutParams clp=pad
   ?new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1)
   :new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1);
  body.addView(chatScroll,clp);

  side=new LinearLayout(this);side.setOrientation(LinearLayout.VERTICAL);
  side.setPadding(dp(10),dp(8),dp(10),dp(8));
  trace=new TextView(this);trace.setText("[轨迹]\n");trace.setTextSize(12);trace.setTextColor(0xFF9AA3B2);
  trace.setMovementMethod(new ScrollingMovementMethod());
  plugins=new TextView(this);plugins.setTextSize(12);plugins.setTextColor(0xFF9AA3B2);plugins.setPadding(0,dp(10),0,0);
  LinearLayout sideCol=new LinearLayout(this);sideCol.setOrientation(LinearLayout.VERTICAL);
  sideCol.addView(trace);sideCol.addView(plugins);
  ScrollView sv2=new ScrollView(this);sv2.addView(sideCol);
  side.addView(sv2,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));
  body.addView(side,pad
   ?new LinearLayout.LayoutParams(dp(280),LinearLayout.LayoutParams.MATCH_PARENT)
   :new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(140)));
  side.setVisibility(pad?View.VISIBLE:View.GONE);
  root.addView(body,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));

  LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(10),dp(6),dp(10),dp(10));bar.setGravity(Gravity.CENTER_VERTICAL);
  input=new EditText(this);input.setHint("问点什么…");input.setTextSize(14);
  input.setTextColor(Color.WHITE);input.setHintTextColor(0xFF6B7280);input.setMaxLines(4);
  GradientDrawable ig=new GradientDrawable();ig.setCornerRadius(dp(10));ig.setColor(0xFF1B2028);
  input.setBackground(ig);input.setPadding(dp(12),dp(10),dp(12),dp(10));
  bar.addView(input,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
  SendBtn send=new SendBtn(this);
  send.setOnClickListener(v->onSend(input.getText().toString()));
  bar.addView(send,new LinearLayout.LayoutParams(dp(44),dp(44)));
  root.addView(bar);

  applyBg(pad);setContentView(root);
  bubble("已就绪 · 胶囊切换思考档 · 长按标题换壁纸 · 长按气泡复制 · /帮助看命令",false);
  try{com.glm.pocket.net.PluginLoader.scan();}catch(Throwable t){}
  refreshPlugins();
  push("trace",jjson("就绪","界面=原生 · 形态="+(pad?"平板":"手机")));
 }

 TextView pill(String label,Runnable r){
  TextView p=new TextView(this);p.setText(label);p.setTextSize(13);p.setTextColor(Color.WHITE);
  p.setPadding(dp(14),dp(6),dp(14),dp(6));
  GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));g.setColor(0xFF2A2F3A);
  p.setBackground(g);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
  lp.rightMargin=dp(8);p.setLayoutParams(lp);
  p.setOnClickListener(v->r.run());
  return p;
 }
 void paintPills(){
  for(int i=0;i<pills.getChildCount();i++){
   GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));
   g.setColor(i==thinkIdx?0xFF2B5CAD:0xFF2A2F3A);
   pills.getChildAt(i).setBackground(g);
  }
 }
 void applyBg(boolean pad){
  root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,BGP[bgIdx]));
  getSharedPreferences("ui",0).edit().putString(pad?"bg_pad":"bg_phone",String.valueOf(bgIdx)).apply();
 }

 TextView bubble(String text,boolean user){
  TextView t=new TextView(this);
  t.setText(text);t.setTextIsSelectable(true);t.setTextSize(15);
  t.setTextColor(user?Color.WHITE:0xFFE8EAED);
  t.setPadding(dp(12),dp(8),dp(12),dp(8));
  GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(12));
  g.setColor(user?0xFF2B5CAD:0xFF232833);
  t.setBackground(g);
  t.setOnLongClickListener(v->{copy(t.getText().toString());return true;});
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(300),LinearLayout.LayoutParams.WRAP_CONTENT);
  lp.gravity=user?Gravity.RIGHT:Gravity.LEFT;
  chat.addView(t,lp);
  chat.addView(new TextView(this),new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(6)));
  chatScroll.post(()->chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
  return t;
 }

 void route(String type,String json){
  try{
   JSONObject o=new JSONObject(json);
   if("chunk".equals(type)){
    String d=o.has("t")?o.optString("t",""):o.optString("x","");
    if(!d.isEmpty())appendChunk(d);
   }else if("meta".equals(type)){
    String m=o.optString("model",""),e=o.optString("err",""),c=o.optString("cid","");
    if(!c.isEmpty())conv=c;
    meta.setText((m.isEmpty()?"":"模型 "+m)+(e.isEmpty()?"":" · "+e));
    sealBubble();
   }else{
    trace.setText("["+type+"] "+o.toString()+"\n"+trace.getText().toString());
   }
  }catch(Exception ex){
   trace.setText("["+type+"] "+json+"\n"+trace.getText().toString());
  }
 }
 void appendChunk(String d){
  if(live==null)live=bubble("",false);
  live.setText(live.getText().toString()+d);
  chatScroll.post(()->chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
 }
 void sealBubble(){live=null;}

 void onSend(String raw){
  String text=raw.trim();if(text.isEmpty())return;
  if(text.equals("/清空")){chat.removeAllViews();live=null;return;}
  if(text.equals("/帮助")){bubble("命令:/清空 /帮助 · 胶囊切思考档 · 长按标题换壁纸 · 轨迹面板看工具调用",false);return;}
  input.setText("");bubble(text,true);live=null;stop=false;paintPills();
  final String f=text;
  new Thread(()->{
   try{
    push("trace",jjson("状态","思考="+THINK_L[thinkIdx]+" · 模型="+MODEL));
    String sys=com.glm.pocket.agent.SelfModel.build("对话",NET);
    String mode=THINK_V[thinkIdx];
    JSONObject pl=GlmClient.payload(sys+"\n\n# 用户\n"+f,conv,NET,mode,MODEL);
    GlmClient.Result r=GlmClient.stream(pl,(t,x)->{if(!stop)push("chunk",jjson(t,x));});
    push("meta",new JSONObject().put("model",r.model).put("cid",r.cid).put("think",r.think).put("err",r.err==null?"":r.err).toString());
    if(r.err!=null&&!r.err.isEmpty()&&!mode.equals("deep_thinking")){
     push("trace",jjson("降级","当前档位后端不识别,自动重试深度模式"));
     pl=GlmClient.payload(sys+"\n\n# 用户\n"+f,conv,NET,"deep_thinking",MODEL);
     GlmClient.Result r2=GlmClient.stream(pl,(t,x)->{if(!stop)push("chunk",jjson(t,x));});
     push("meta",new JSONObject().put("model",r2.model).put("cid",r2.cid).put("think",r2.think).put("err",r2.err==null?"":r2.err).toString());
    }
   }catch(Exception e){
    push("meta","{\"model\":\"\",\"cid\":\"\",\"think\":0,\"err\":\""+String.valueOf(e).replace("\"","'")+"\"}");
   }
  }).start();
 }

 void refreshPlugins(){
  if(plugins==null)return;
  plugins.setText(UI_PLUGINS.isEmpty()
   ?"插件挂载点:调用 MainActivity.addPluginView(View) 可把控件挂进侧栏"
   :"已挂载插件控件 "+UI_PLUGINS.size()+" 个");
 }
 void copy(String s){
  ClipboardManager c=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
  c.setPrimaryClip(ClipData.newPlainText("pocketglm",s));
  Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();
 }
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 static String jjson(Object t,Object x){try{return new JSONObject().put("t",String.valueOf(t)).put("x",String.valueOf(x)).toString();}catch(Exception e){return "{}";}}

 class SendBtn extends View{
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  SendBtn(Context c){super(c);}
  protected void onDraw(Canvas cv){
   p.setColor(0xFF2B5CAD);
   Path path=new Path();
   path.moveTo(getWidth()*0.30f,getHeight()*0.24f);
   path.lineTo(getWidth()*0.76f,getHeight()*0.50f);
   path.lineTo(getWidth()*0.30f,getHeight()*0.76f);
   path.close();
   cv.drawPath(path,p);
  }
 }
}
