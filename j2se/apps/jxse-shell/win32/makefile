all: jxta.exe

jxta.exe: jxta.cpp makefile
	gcc -mno-cygwin -O -o jxta.exe jxta.cpp
	strip jxta.exe

clean:
	rm -f jxta.exe

