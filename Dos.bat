@echo off
@title Server Console
set CLASSPATH=.;dist\*
java -server -Dnet.sf.odinms.wzpath=wz\ tools.Dos
pause