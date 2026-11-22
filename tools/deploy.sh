#!/data/data/com.termux/files/usr/bin/sh
U=$(head -n1 '/storage/emulated/0/MT2/用户名.txt'|tr -d '\r\n ')
T=$(head -n1 '/storage/emulated/0/MT2/Token.txt'|tr -d '\r\n ')
cd $HOME/pocketglm||exit 9
command -v git >/dev/null 2>&1 || pkg install -y git >/dev/null 2>&1
curl -s -X POST -H "Authorization: Bearer $T" -H 'Accept: application/vnd.github+json' \
 https://api.github.com/user/repos -d '{"name":"PocketGLM","private":false}' >/dev/null
git init 2>/dev/null
git config user.email "$U@users.noreply.github.com"
git config user.name "$U"
git add -A
git commit -m "deploy $(date +%m%d-%H%M)" >/dev/null 2>&1
git branch -M main 2>/dev/null
git remote remove origin 2>/dev/null
git remote add origin https://$U:$T@github.com/$U/PocketGLM.git
if git push -u origin main 2>/dev/null; then echo PUSH_OK
else
 git pull --rebase origin main >/dev/null 2>&1
 if git push -f -u origin main 2>/dev/null; then echo PUSH_OK_FORCE
 else echo PUSH_FAIL; fi
fi
