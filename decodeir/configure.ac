#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.
AC_CONFIG_MACRO_DIR([m4])

AC_PREREQ([2.69])
AC_INIT(DecodeIR, 2.45, [http://www.hifi-remote.com/forums])
AC_CONFIG_SRCDIR([DecodeIR.cpp])
AC_CONFIG_HEADERS([config.h])

AM_INIT_AUTOMAKE([foreign])
LT_INIT([disable-static])

# Checks for programs.
AC_PROG_CXX
AC_PROG_CC
AX_PROG_JAVAH

# Checks for libraries.

# Checks for header files.
AC_CHECK_HEADERS([stdlib.h string.h])

# Checks for typedefs, structures, and compiler characteristics.
AC_CHECK_HEADER_STDBOOL
AC_C_INLINE
AC_TYPE_INT64_T

# Checks for library functions.
AC_FUNC_ERROR_AT_LINE
AC_CHECK_FUNCS([floor memset])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
