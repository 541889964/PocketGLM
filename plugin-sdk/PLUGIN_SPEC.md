# PocketGLM 插件规范 v1 (给AI读的契约)

目录: Download/pocketglm/plugins/<name>/
文件: manifest.json + entry代码

## manifest.json
{"name":"translator","version":"1.0","type":"python",
 "entry":"main.py","desc":"中英互译",
 "tools":[{"name":"translate","desc":"翻译文本","args":{"text":"str","to":"zh|en"}}],
 "perms":["net"]}

## type=python (Termux执行)
stdin 收: {"tool":"<工具名>","args":{...}}
stdout 吐: {"ok":true,"result":"..."} 或 {"ok":false,"error":"..."}
import sys,json
req=json.loads(sys.stdin.read())
a=req["args"]
# ...干活...
print(json.dumps({"ok":True,"result":"译文"},ensure_ascii=False))

## type=js / ui (WebView注入)
注册: PocketGLM.tool("name", fn(args)->string)
特权: PocketGLM.ui.addButton(title,onclick) · PocketGLM.storage.get/set · PocketGLM.llm.chat(text)→Promise

## 质检与安装(硬流程)
1 write_file 写manifest与代码 (限 plugins/ 目录)
2 syntax_check
3 install_plugin → 热注册+自我清单更新
4 真实调用一次新工具验证, 展示真实输出
