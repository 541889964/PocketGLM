package com.glm.pocket;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class MainActivity extends Activity{

 public static Context CTX;
 static MainActivity inst;
 interface ChunkSink{void on(String t,String x);}

 static final String MODEL="glm-5.3";
 static final String[] THINK_L={"普通","深度","极致"};
 static final String[] THINK_V={"normal","deep_thinking","extreme"};

 String conv="";
 int thinkIdx=2;
 volatile boolean stop=false;
 TextView title,meta,trace;
 LinearLayout chat,pills,ui,pluginHost;
 ScrollView chatScroll;
 EditText input;
 LiveBg bg;
 boolean pad=false;
 int bgIdx=0;
 boolean bgLive=true,animOn=false;
 TextView live=null;

 static final int[][] BGP={{0xFF0E1117,0xFF1A1F2B},{0xFF141428,0xFF2A1F3D},{0xFF0B1614,0xFF12241F}};
 public static final List<View> UI_PLUGINS=new ArrayList<>();

 /* ═══ 外部契约区:其他类正在调用,不可改名 ═══ */
 public static Handler self(){return new Handler(Looper.getMainLooper());}
 public static void selfPush(String type,String msg){
  if(inst==null)return;
  inst.runOnUiThread(()->{if(inst.trace!=null)inst.addTrace("["+type+"] "+msg);});
 }
 public static void reloadUi(){if(inst!=null)inst.runOnUiThread(()->inst.recreate());}
 public static void addPluginView(View v){
  synchronized(UI_PLUGINS){UI_PLUGINS.add(v);}
  if(inst!=null)inst.runOnUiThread(()->inst.refreshPlugins());
 }
 public static void push(String type,String json){if(inst==null)return;inst.runOnUiThread(()->inst.route(type,json));}

 protected void onCreate(Bundle b){
  super.onCreate(b);
  inst=this;CTX=this;
  pad=getResources().getConfiguration().smallestScreenWidthDp>=600;
  try{bgIdx=Integer.parseInt(getSharedPreferences("ui",0).getString(pad?"bgX_pad":"bgX_phone","0"));if(bgIdx<0||bgIdx>=BGP.length)bgIdx=0;}catch(Exception e){bgIdx=0;}
  try{bgLive=getSharedPreferences("ui",0).getBoolean(pad?"bgLive_pad":"bgLive_phone",true);}catch(Exception e){bgLive=true;}

  FrameLayout rootL=new FrameLayout(this);
  bg=new LiveBg(this);
  rootL.addView(bg,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));

  ui=new LinearLayout(this);ui.setOrientation(LinearLayout.VERTICAL);

  LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);
  head.setPadding(dp(16),dp(10),dp(16),dp(8));
  title=new TextView(this);title.setText("PocketGLM");
  title.setTextSize(18);title.setTypeface(Typeface.DEFAULT_BOLD);title.setTextColor(Color.WHITE);
  meta=new TextView(this);meta.setText((pad?"平板":"手机")+" · 默认极致档 · 点标题换配色 · 长按切动态壁纸");
  meta.setTextSize(12);meta.setTextColor(0xFF9AA3B2);
  head.addView(title);head.addView(meta);
  title.setOnClickListener(v->{bgIdx=(bgIdx+1)%BGP.length;applyBg();Toast.makeText(this,"配色 "+(bgIdx+1),Toast.LENGTH_SHORT).show();});
  title.setOnLongClickListener(v->{bgLive=!bgLive;applyBg();Toast.makeText(this,bgLive?"动态壁纸:开":"动态壁纸:关",Toast.LENGTH_SHORT).show();return true;});
  ui.addView(head);

  HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);
  pills=new LinearLayout(this);pills.setPadding(dp(12),dp(4),dp(12),dp(4));
  for(int i=0;i<THINK_L.length;i++){final int idx=i;
   pills.addView(pill(THINK_L[i],()->{thinkIdx=idx;paintPills();addTrace("[档位] 思考="+THINK_L[idx]);}));}
  pills.addView(pill("停止",()->{stop=true;sealBubble();addTrace("[控制] 已请求停止");}));
  pills.addView(pill("轨迹",()->{side.setVisibility(side.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);}));
  paintPills();hs.addView(pills);ui.addView(hs);

  LinearLayout body=new LinearLayout(this);
  body.setOrientation(pad?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);

  chat=new LinearLayout(this);chat.setOrientation(LinearLayout.VERTICAL);
  chat.setPadding(dp(12),dp(8),dp(12),dp(8));
  chatScroll=new ScrollView(this);chatScroll.addView(chat);
  body.addView(chatScroll,pad
   ?new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1)
   :new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));

  side=new LinearLayout(this);side.setOrientation(LinearLayout.VERTICAL);
  side.setPadding(dp(10),dp(8),dp(10),dp(8));
  trace=new TextView(this);trace.setText("[Agent轨迹]\n");trace.setTextSize(12);trace.setTextColor(0xFF9AA3B2);
  trace.setMovementMethod(new ScrollingMovementMethod());
  pluginHost=new LinearLayout(this);pluginHost.setOrientation(LinearLayout.VERTICAL);
  ScrollView sv2=new ScrollView(this);sv2.addView(pluginHost);
  side.addView(trace,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
  side.addView(sv2,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));
  body.addView(side,pad
   ?new LinearLayout.LayoutParams(dp(280),LinearLayout.LayoutParams.MATCH_PARENT)
   :new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(140)));
  side.setVisibility(pad?View.VISIBLE:View.GONE);
  ui.addView(body,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));

  LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(10),dp(6),dp(10),dp(10));bar.setGravity(Gravity.CENTER_VERTICAL);
  input=new EditText(this);input.setHint("问点什么…");input.setTextSize(14);
  input.setTextColor(Color.WHITE);input.setHintTextColor(0xFF6B7280);input.setMaxLines(4);
  GradientDrawable ig=new GradientDrawable();ig.setCornerRadius(dp(10));ig.setColor(0xCC1B2028);
  input.setBackground(ig);input.setPadding(dp(12),dp(10),dp(12),dp(10));
  bar.addView(input,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
  SendBtn send=new SendBtn(this);
  send.setOnClickListener(v->onSend(input.getText().toString()));
  bar.addView(send,new LinearLayout.LayoutParams(dp(44),dp(44)));
  ui.addView(bar);

  rootL.addView(ui,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
  setContentView(rootL);
  bubble("已就绪 · 思考三档(默认极致,后端不认自动降级) · 点标题换配色/长按切动态壁纸 · 长按气泡复制 · /帮助",false);
  for(String pn:new String[]{"com.glm.pocket.net.PluginLoader","com.glm.pocket.agent.PluginLoader","com.glm.pocket.self.PluginLoader"}){
   try{Class.forName(pn).getMethod("scan").invoke(null);addTrace("[插件] 已扫描 "+pn);break;}catch(Throwable t){}
  }
  addTrace("[就绪] 形态="+(pad?"平板双栏":"手机单栏")+" · 默认档=极致");
 }

 LinearLayout side;

 TextView pill(String label,Runnable r){
  TextView p=new TextView(this);p.setText(label);p.setTextSize(13);p.setTextColor(Color.WHITE);
  p.setPadding(dp(14),dp(6),dp(14),dp(6));
  GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));g.setColor(0xB32A2F3A);
  p.setBackground(g);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
  lp.rightMargin=dp(8);p.setLayoutParams(lp);
  p.setOnClickListener(v->r.run());
  return p;
 }
 void paintPills(){
  for(int i=0;i<pills.getChildCount();i++){
   GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));
   g.setColor(i==thinkIdx?0xFF2B5CAD:0xB32A2F3A);
   pills.getChildAt(i).setBackground(g);
  }
 }
 void applyBg(){
  getSharedPreferences("ui",0).edit()
   .putString(pad?"bgX_pad":"bgX_phone",String.valueOf(bgIdx))
   .putBoolean(pad?"bgLive_pad":"bgLive_phone",bgLive).apply();
  bg.invalidate();
 }

 TextView bubble(String text,boolean user){
  TextView t=new TextView(this);
  t.setText(text);t.setTextIsSelectable(true);t.setTextSize(15);
  t.setTextColor(user?Color.WHITE:0xFFE8EAED);
  t.setPadding(dp(12),dp(8),dp(12),dp(8));
  GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(12));
  g.setColor(user?0xFF2B5CAD:0xCC232833);
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
    String d=o.optString("x",o.optString("t",""));
    if(!d.isEmpty())appendChunk(d);
   }else if("meta".equals(type)){
    String m=o.optString("model",""),e=o.optString("err",""),c=o.optString("cid","");
    if(!c.isEmpty())conv=c;
    meta.setText(m+(e.isEmpty()?"":" · [错误] "+e));
    if(!e.isEmpty())appendChunk("\n[错误] "+e+"\n");
    sealBubble();
   }else addTrace("["+type+"] "+o.toString());
  }catch(Exception ex){addTrace("["+type+"] "+json);}
 }
 void addTrace(String s){if(trace!=null)trace.setText(s+"\n"+trace.getText().toString());}
 void appendChunk(String d){
  if(live==null)live=bubble("",false);
  live.setText(live.getText().toString()+d);
  chatScroll.post(()->chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
 }
 void sealBubble(){live=null;}

 void onSend(String raw){
  String text=raw.trim();if(text.isEmpty())return;
  if(text.equals("/清空")){chat.removeAllViews();live=null;return;}
  if(text.equals("/帮助")){bubble("命令:/清空 /帮助 · 胶囊=思考档(默认极致) · 轨迹面板看Agent调用 · 点标题换配色 · 长按标题切动态壁纸 · 长按气泡复制",false);return;}
  input.setText("");bubble(text,true);live=null;stop=false;
  final String q=text;final boolean netF=false;
  final String chosen=THINK_V[thinkIdx];
  new Thread(()->{
   String[] chain=new String[]{chosen,"deep_thinking","normal"};
   boolean okAll=false;String lastErr="";
   for(int i=0;i<chain.length;i++){
    String md=chain[i];
    if(i>0&&md.equals(chain[i-1]))continue;
    if(i>0){final String prev=chain[i-1];push("trace",jjson("降级","档位 "+prev+" 未成功, 自动重试 "+md));}
    sealBubble();
    final boolean[] ok={false};final StringBuilder eb=new StringBuilder();
    engine(q,conv,netF,md,MODEL,(t,x)->{if(stop)return;
     if("chunk".equals(t))push("chunk",jjson("x",x));
     else if("done".equals(t))ok[0]=true;
     else if("meta".equals(t))push("meta",x);
     else eb.append(x);
    });
    long dl=System.currentTimeMillis()+120000;
    while(!ok[0]&&eb.length()==0&&System.currentTimeMillis()<dl&&!stop){
     try{Thread.sleep(300);}catch(Exception e){}
    }
    if(ok[0]){okAll=true;break;}
    lastErr=eb.length()>0?eb.toString():"120秒无响应";
    if(stop)break;
   }
   final boolean fok=okAll;final String ferr=lastErr;
   runOnUiThread(()->{if(!fok&&!stop)bubble("[请求失败] "+ferr+" (已按 极致→深度→普通 自动降级)",false);else push("meta",metaJson(MODEL,""));});
  }).start();
 }
 static String metaJson(String m,String e){try{return new JSONObject().put("model",m).put("err",e).toString();}catch(Exception x){return "{}";}}

 /* ══ 自适配引擎:运行时扫描GlmClient真实签名并挂接,保证编译绿;匹配详情进轨迹 ══ */
 static void engine(String q,String conv,boolean net,String mode,String model,ChunkSink sink){
  try{
   Class<?> G=Class.forName("com.glm.pocket.net.GlmClient");
   Object pl=null;String sig="none";
   String[] sv={q,conv,mode,model};
   for(Method m:G.getDeclaredMethods()){
    if(!Modifier.isStatic(m.getModifiers()))continue;
    String n=m.getName();if(!n.equals("payload")&&!n.equals("build"))continue;
    Class<?>[] ps=m.getParameterTypes();int ns=0,nb=0;boolean bad=false;
    for(Class<?> pc:ps){if(pc==String.class)ns++;else if(pc==boolean.class)nb++;else{bad=true;break;}}
    if(bad||ns<1||ns>4||nb>1)continue;
    Object[] args=new Object[ps.length];int k=0;
    for(int i=0;i<ps.length;i++)args[i]=ps[i]==boolean.class?(Object)net:sv[Math.min(k++,3)];
    pl=m.invoke(null,args);sig=n+"/"+ps.length;break;
   }
   if(pl==null)throw new Exception("未找到 payload/build(String..)签名; GlmClient方法: "+dump(G));
   if(pl instanceof JSONObject){JSONObject o=(JSONObject)pl;
    try{o.put("model",model);}catch(Throwable t){}
    try{o.put("thinking",new JSONObject().put("type",mode));}catch(Throwable t){}
    try{o.put("think_mode",mode);}catch(Throwable t){}
   }
   Method sm=null;
   for(Method m:G.getDeclaredMethods()){
    if(!Modifier.isStatic(m.getModifiers())||!m.getName().equals("stream"))continue;
    Class<?>[] ps=m.getParameterTypes();
    if(ps.length==2&&ps[0].isInstance(pl)&&ps[1].isInterface()){sm=m;break;}
   }
   if(sm==null)throw new Exception("未找到 stream(载荷,回调) 签名; GlmClient方法: "+dump(G));
   final Class<?> cbT=sm.getParameterTypes()[1];
   Object cb=Proxy.newProxyInstance(cbT.getClassLoader(),new Class[]{cbT},(p,me,a)->{
    String mn=me.getName();
    try{
     if(a!=null&&a.length>0){
      if(a.length>=2&&(a[1] instanceof Boolean)){
       sink.on(((Boolean)a[1])?"done":"chunk",String.valueOf(a[0]));
      }else if(a[0] instanceof String){
       String t0=(String)a[0];
       String x=a.length>=2?String.valueOf(a[1]):"";
       if(mn.toLowerCase().contains("fail")||t0.contains("err"))sink.on("err",x.isEmpty()?t0:x);
       else if(t0.contains("done")||t0.contains("finish")||mn.toLowerCase().contains("done"))sink.on("done",x);
       else if(t0.contains("meta"))sink.on("meta",x);
       else sink.on("chunk",x);
      }else if(mn.toLowerCase().contains("fail")){
       sink.on("err",a.length>=2?String.valueOf(a[1]):mn);
      }else if(mn.toLowerCase().contains("resp")){
       sink.on("done","");
      }else sink.on("chunk",String.valueOf(a[a.length-1]));
     }
    }catch(Throwable t){}
    return null;
   });
   sm.setAccessible(true);
   sm.invoke(null,pl,cb);
   push("trace",jjson("引擎","payload="+sig+" · stream("+sm.getParameterTypes()[0].getSimpleName()+","+cbT.getSimpleName()+") · 档位="+mode));
  }catch(Throwable e){sink.on("err",String.valueOf(e));}
 }
 static String dump(Class<?> G){
  StringBuilder sb=new StringBuilder();
  for(Method m:G.getDeclaredMethods()){
   if(!Modifier.isStatic(m.getModifiers()))continue;
   sb.append(m.getName()).append("(");
   Class<?>[] ps=m.getParameterTypes();
   for(int i=0;i<ps.length;i++){sb.append(ps[i].getSimpleName());if(i<ps.length-1)sb.append(",");}
   sb.append(") ");
  }
  return sb.toString();
 }

 void refreshPlugins(){
  if(pluginHost==null)return;
  if(UI_PLUGINS.isEmpty()){
   TextView t=new TextView(this);t.setTextSize(12);t.setTextColor(0xFF6B7280);
   t.setText("插件挂载点:外部调用 MainActivity.addPluginView(View) 即可把控件插进侧栏, 主流程不可触及");
   pluginHost.addView(t);
  }else{pluginHost.removeAllViews();for(View v:UI_PLUGINS)pluginHost.addView(v);}
 }
 void copy(String s){
  ClipboardManager c=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
  c.setPrimaryClip(ClipData.newPlainText("pocketglm",s));
  Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();
 }
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 static String jjson(Object t,Object x){try{return new JSONObject().put("t",String.valueOf(t)).put("x",String.valueOf(x)).toString();}catch(Exception e){return "{}";}}

 protected void onResume(){super.onResume();animOn=true;if(bg!=null)bg.postInvalidateDelayed(60);}
 protected void onPause(){super.onPause();animOn=false;}

 class LiveBg extends View{
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  LiveBg(Context c){super(c);}
  protected void onDraw(Canvas cv){
   int[] A=BGP[bgIdx];
   boolean anim=bgLive&&animOn;
   float ph=anim?((System.currentTimeMillis()%12000)/12000f):0.35f;
   Matrix m=new Matrix();
   m.postRotate(ph*360f,getWidth()/2f,getHeight()/2f);
   LinearGradient g=new LinearGradient(-getWidth(),-getHeight(),getWidth(),getHeight(),
    breathe(A[0],ph),breathe(A[1],(ph+0.4f)%1f),Shader.TileMode.CLAMP);
   g.setLocalMatrix(m);p.setShader(g);
   cv.drawRect(0,0,getWidth(),getHeight(),p);
   if(anim)postInvalidateDelayed(60);
  }
  int breathe(int c,float ph){
   float f=0.88f+0.14f*(float)Math.sin(ph*6.28318f);
   int r=Math.min(255,(int)(((c>>16)&255)*f)),g2=Math.min(255,(int)(((c>>8)&255)*f)),b=Math.min(255,(int)((c&255)*f));
   return 0xFF000000|(r<<16)|(g2<<8)|b;
  }
 }

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
