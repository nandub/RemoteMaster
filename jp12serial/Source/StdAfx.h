// stdafx.h : include file for standard system include files,
//  or project specific include files that are used frequently, but
//      are changed infrequently
//

#define WINVER 0x0500
#define VC_EXTRALEAN		// Exclude rarely-used stuff from Windows headers
#define FTDI_NOERROR		// Prevent error messages from FTDI support

#include <windows.h>
//#include <time.h>


// the following defines come from the Windows DDK; to avoid the dependency to the DDK
// they have been copied from http://source.winehq.org/source/include/ddk/ntddser.h
#include <WinIoCtl.h>
//#include <ddk/ntddser.h>
#define IOCTL_SERIAL_PURGE CTL_CODE (FILE_DEVICE_SERIAL_PORT, 19, METHOD_BUFFERED, FILE_ANY_ACCESS)
/* IOCTL_SERIAL_PURGE constants */
#define SERIAL_PURGE_TXABORT              0x00000001
#define SERIAL_PURGE_RXABORT              0x00000002
#define SERIAL_PURGE_TXCLEAR              0x00000004
#define SERIAL_PURGE_RXCLEAR              0x00000008
