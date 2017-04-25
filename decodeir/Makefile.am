bin_PROGRAMS = decodeir
decodeir_SOURCES = main.cpp
decodeir_LDADD = libDecodeIR.la

ACLOCAL_AMFLAGS           = -I m4

EXTRA_DIST                = compile DecodeIR.html INSTALL pom.xml README
EXTRA_DIST                += Makefile.Linux Makefile.MacOSX StdAfx.h

include_HEADERS           = DecodeIRCaller.h DecodeIR.h

lib_LTLIBRARIES           = libDecodeIR.la
libDecodeIR_la_SOURCES    = DecodeIR.cpp
                          # Use -avoid-version to disable symlinks and SONAME.
libDecodeIR_la_LDFLAGS    = -version-info 0:0:0

javadir                   = src/main/java/com/hifiremote/decodeir

test: decodeir
	./decodeir 0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C

dist-hook:
	mkdir -p $(distdir)/$(javadir)
	install -t $(distdir)/$(javadir) $(srcdir)/$(javadir)/*.java
