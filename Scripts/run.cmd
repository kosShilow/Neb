@echo off
set home=C:/Users/Public/Neb_new/Scripts
set python=C:/Python27/python.exe
set java="C:/Program Files/Java/jre1.8.0_141/bin/java.exe" 

echo %python% %home%/filter_nodes.py
%python% %home%/filter_nodes.py

echo %python% %home%/image_nodes.py
%python% %home%/image_nodes.py

echo %java% -jar %home%/GraphLayout.jar 
%java% -jar %home%/GraphLayout.jar 

