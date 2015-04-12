ACLOCAL_AMFLAGS           = -I m4

EXTRA_DIST                = compile DecodeIR.html INSTALL pom.xml README
EXTRA_DIST                += Makefile.Linux Makefile.MacOSX StdAfx.h

include_HEADERS           = DecodeIRCaller.h DecodeIR.h

lib_LTLIBRARIES           = libdecodeir.la
libdecodeir_la_SOURCES    = DecodeIR.cpp
                          # Use -avoid-version to disable symlinks and SONAME.
libdecodeir_la_LDFLAGS    = -version-info 0:0:0

dist-hook:
	mkdir -p $(distdir)/src/main/java/com/hifiremote/decodeir
	cp -a $(top_srcdir)/src/main/java/com/hifiremote/decodeir/*.java  \
	    $(distdir)/src/main/java/com/hifiremote/decodeir
