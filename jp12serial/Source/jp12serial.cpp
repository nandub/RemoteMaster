/* 
   
   This file is part of the JP1.x serial (RS-232) library used by various
   applications in the JP1 remote control user community to communicate with
   JP1.x (Flash) remote controls.  These applications include the 'IR.exe'
   utility, the 'Remote Master' IR utility (aka 'RMIR'), and several
   command-line utilities.

   Authors/Copyright/License:
 
      The JP1.x serial library was written by Greg Bush ('gfb107') and
      modified by several others, including 'binky123', Chris Nappi
      ('xnappo'), Bengt Martensson ('Barf'), 'osolfan', Bill Marr
      ('WagonMaster'), Kevin Timmerman and Graham Dixon ('mathdon').
      Mac OS X support by Michael Dreher ('MikeT').
 
      Copyright (C) 2006-2016 Greg Bush (et al)
      
      The JP1.x serial library is free software: you can redistribute it
      and/or modify it under the terms of the GNU General Public License as
      published by the Free Software Foundation, either version 3 of the
      License, or (at your option) any later version.
 
      The JP1.x serial library is distributed in the hope that it will be
      useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
      General Public License for more details.
 
      You should have received a copy of the GNU General Public License along
      with the JP1.x serial library.  If not, see
      <http://www.gnu.org/licenses/>.

   Files:

      jp12serial.cpp -- main library source code

      jp12serial.hpp -- main library header file

      jp12serial.dll -- Dynamic Link Library (DLL) for use by Windows 
                        applications

      jp12serial.so  -- shared object (library) file for use by 
                        Unix/Linux/Solaris/etc applications

   Additional files used only by Windows:

      ftdi.cpp       -- source code of FTDI interface functions

	  ftdi.h         -- header file of FTDI interface functions

	  ftd2xx.h       -- FTDI-provided file for interfacing to ftd2xx.dll

	  StdAfx.h       -- minimal header file of common settings

	  The files ftdi.cpp and ftdi.h are also used by IRScope.

   Building Instructions:

      This source code is intended to be compiled into both a Dynamic Link
      Library ('.dll') file for Windows users and a shared object library file
      ('.so') for Unix/Linux/Solaris/etc users.  

      Compile command:

         Windows + MinGW:  g++ -I "c:\Program Files\Java\jdk1.6.0_12\include" 
		     -I "c:\Program Files\Java\jdk1.6.0_12\include\win32" -shared
			 -o jp12serial.dll jp12serial.cpp ftdi.cpp -Wl,--kill-at

			 (amend the JDK Include parameters as appropriate)

         Linux + GCC:  gcc -shared -o libjp12serial.so jp12serial.cpp
      
   Miscellaneous Information:

      To download the latest version of this library and for other
      JP1{.x}-related information and files, please visit the forums at Rob
      Crowe's excellent JP1 site:
      
         http://www.hifi-remote.com/forums/

   Version History / Change Log:

      Note: Prior to version 0.14, no change log was kept.

	  Version 0.22 (13 October 2016)
		This version adds support for JP3.1.  This is a 32-bit version of JP1.4N but with an
		important timing difference.  There is a delay of nearly 5 seconds between sending a
		GetInfo request and the arrival of the response.  To allow for this, ReadSerial() has
		been modified to accept an additional optional parameter, a maximum wait duration in
		seconds as a double.  A safety feature has also been added to prevent other interface
		types being tried if the remote passes initial tests as being JP1.4/JP2 but the call
		to jp2_14GetInfoAndSig() fails.  It is possible that testing for other types may
		corrupt a JP1.4/JP2 remote.

		As a result of the changes in v0.21 this version also supports JP2N, which is the
		modification of JP2 corresponding to the change from JP1.4 to JP1.4N. 

	  Version 0.21 (28 April 2016)
		This version adds support for JP1.4N, a modified JP1.4 format that has a different
		structure for the signature block.  This new format also uses a new checksum algorithm,
		a 16-bit XOR rather than the 8-bit checksums of all earlier UEI interfaces.  The only
		impact on the code here is that it is no longer possible to identify a checksum by
		its two hex bytes adding to 0xFF.
	  
	  Version 0.20 (26 Jun 2014)
	     This release version is almost identical to the Beta version. The intervening time 
		 has revealed only one minor issue, one too many pages being erased in write operations
		 in remotes with a page size > 1.  Also removed TODOs from the code; disabled tracing 
		 by default.
	  
	  Version 0.20 Beta (21 Sep 2012)
	     Support for remotes with address range > 64KB (JP3?) added by Michael Dreher ('MikeT').
         Using FlushInput() to clear any data which is still in the receive buffer. Don't use
         the read() function because it takes much longer.
         Added tracing of serial communication which can be activated by setting the
         #define TRACE_SERIAL.
		 Detection of FTDI chips modified by Graham Dixon (mathdon) to recognise the new FT230X
		 chip used in Tommy Tyler's new interface leads and widgets.

	  Version 0.19 (released 21 Aug 2012)
	     Support for JP1.4 and JP2 remotes added by Dave Reed (3FG).  Includes a minor change
		 from the beta version issued 14 Dec 2011 as 0.18f_JP2 to handle an issue for these
		 remotes when using transistor-based interfaces.
 
      Version 0.18 (released 30 Oct 2010):
		 Fixed initialization of serial port, replaced "options.c_lflag |= IEXTEN"
         by "options.c_lflag &= ~IEXTEN;".
         Added #ifdefs for serial flags which are not available on OS X. Cleared flags
		 ONLCR and OXTABS. Courtesy of Michael Dreher ('MikeT')

         Cleaned up error handling for hSerial: always use INVALID_HANDLE_VALUE instead of NULL,
         courtesy of Michael Dreher ('MikeT')
 
		 Dynamically searches for plugged-in cables because there are no predefined or builtin
         serial / parallel devices and the name of the USB devices contain the serial number of
         the FTDI chip, courtesy of Michael Dreher ('MikeT').
 
	  Version 0.17 (released 3 Jul 2010):
         Amended getSignature() to return both JP1 and JP1.x styles of
		 signature.  This function is used by RMIR but not IR.exe, and
		 without this change the Kevin Timmerman JP1 EEPROM Programming
		 adapter (see note on Version 0.15 below) would not work with
		 RMIR.

      Version 0.16 (released 11 Feb 2010):
         Changes made by Kevin Timmerman and Graham Dixon to speed up
		 finding the remote.  On initial access during a session, priority
		 in the search order of COM ports is given to ports with FTDI chips
		 (excluding IR Widgets). The port located on initial start-up is
		 remembered and used in subsequent accesses.

      Version 0.15 (released 26 Jan 2010):
	     Changes made by Kevin Timmerman to allow use of the JP1 EEPROM Programming
		   adapter.  http://www.compendiumarcana.com/jp1epa

		 - Global variable "blockSize" size added. This is used when writing
		   data to the remote with the 'S' command (writeBlock() function).
		   It is set to 128 for flash remotes, or 32 for EEPROM remotes. The
		   type of remote is determined by the first char of the response to
		   the 'I' command (getIdentity() function). The EEPROM programming
		   adapter will respond with "eeprom", so 'e' will set the block
		   size to 32. 'M' or 'S' will set the block size to 128. The smaller
		   block size for EE is required due to the speed of EE and the limited
		   buffer in the EE adapter.

		 - The erasePages() function will not be called by writeRemote() if
		   the pageSize is 1 or less. EEPROM do not need page erasure and have
		   one byte write granularity. 

	     - Cleaned up formatting and structure in some places.

      Version 0.14 (released 13 Sep 2009):
  
         Except where noted otherwise, these changes were done by Bill Marr
         ('WagonMaster').
      
         - Fixed a bug whereby the routine used to delay ('sleep') in a
           non-Windows (Unix, Linux, Solaris, et al) environment was off by a
           factor of 1000, erroneously sleeping for the specified number of
           MICROseconds instead of the specified number of MILLIseconds.
           
         - Fixed a bug whereby the code intended to set and hold a UART
           'break' condition in a non-Windows (Unix, Linux, Solaris, et al)
           environment was erroneously using code to set the 'break'
           condition, sleep for a while (250 milliseconds in many
           implementations), then release the 'break' condition.
           
           This problem was substantially masked by the 'sleep' bug mentioned
           above, which was causing the process of putting a remote control
           into 'serial communication' mode to succeed where it would have
           otherwise failed with this bug in place.
           
           A corresponding fix was also needed to release the UART 'break'
           condition when commanded.
           
         - Properly preserve and restore the port settings of any opened
           RS-232 serial port.  Previously, the port's settings were
           overwritten, corrupting the settings for all ports (and therefore
           potentially all devices connected to them, like a dial-up modem)
           which may have been opened in the search for a valid JP1.x (Flash)
           remote control.
           
         - Fix a problem whereby error messages in a non-Windows (Unix, Linux,
           Solaris, et al) environment were erroneously showing up split
           across lines and out of sequence.
           
           The fix involves replacing 'perror()' with 'strerror()'.
           
         - Enhanced error reporting for serial port initialization.
           
         - Added support for Solaris environments, courtesy of Bengt
           Martensson ('Barf') and 'osolfan'.
           
         - Added a new routine to determine if the specified string parameter
           looks like a valid serial port name, for any of the supported
           platforms, courtesy of Bengt Martensson ('Barf').
           
           (At the moment, no current utilities use this routine, but future
           improvements to some of the command-line utilities will use it.)
           
         - In the routine to read a block of memory from the remote control,
           explicitly initialize that block to all zeroes, courtesy of Bengt
           Martensson ('Barf').
           
         - Remove code to define and initialize a couple of variables
           ('readAddress' and 'addressLimit') that were not used anywhere,
           courtesy of Bengt Martensson ('Barf')
           
         - Explicit variable casting was added in several spots to eliminate
           compile-time warnings, courtesy of Bengt Martensson ('Barf').
           
         - Explicitly specify 'void' parameter lists in routines.
           
         - Use '#ifndef WIN32' in lieu of '#ifdef __linux' so that building on
           Unix platforms other than Linux works as expected, courtesy of
           Bengt Martensson ('Barf').

         - Provide for conditional compilation in environments that don't have
           the Java SDK/JDK (and therefore the header files) installed.  (The
           default is to expect the Java headers to be present and therefore
           to build the Java routines.)
           
         - Move some definitions from the C source file to the header file,
           for better separation and in anticipation of future improvements.
           
         - Various cosmetic cleanups, including comment changes, additions,
           etc.

         - Allow use of Microsoft Visual C++ as well as MinGW when compiling,
           courtesy of Graham Dixon ('mathdon').

         - Replace uses of non-standard "or" operator (which doesn't compile
           under Microsoft Visual C++) with conventional "||" operator,
           courtesy of Graham Dixon ('mathdon').

*/

// 
// Define this whenever the Java SDK/JDK is available.  By undefining it (or 
// simply commenting it out), it is possible to build this library without the 
// Java header files, but if you do so it will NOT work with RMIR and the 
// compiled binary SHOULD NOT BE ISSUED FOR GENERAL USE. 
// 
#define JAVA_SDK_AVAILABLE

#ifndef WIN32
// you might enable command tracing here, but currently it only works on Linux and MacOSX
//#define TRACE_SERIAL
#endif

#ifdef WIN32
#define BUILD_DLL
#endif

#define SIGLEN (26)
#define JP2INFOCOUNT (9)
#define ADRLEN32 (4)
#define ADRLEN16 (2)
#define GET_BE_ADRLEN(adrLen, ptr) ((adrLen == ADRLEN32) ? GET_BE32(ptr) : GET_BE16(ptr))
#define PUT_BE_ADRLEN(adrLen, ptr, value) ((adrLen == ADRLEN32) ? PUT_BE32(ptr, value) : PUT_BE16(ptr, value))

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <ctime>

#ifdef WIN32
#include "stdafx.h"
// Uncomment next line if using MessageBox to debug
// #pragma comment(lib,"user32.lib");
#include "ftdi.h"
#else
#include <dirent.h>
#endif

#include "jp12serial.hpp"

#ifdef JAVA_SDK_AVAILABLE
#include "com_hifiremote_jp1_io_JP12Serial.h"
#endif

#ifdef _MSC_VER
#pragma warning(disable:4996)	// Supress security warnings for unsafe string copy
#endif

#ifndef WIN32
#include <unistd.h>                   /* UNIX standard function definitions */
#include <fcntl.h>                    /* File control definitions           */
#include <errno.h>                    /* Error number definitions           */
#include <termios.h>                  /* POSIX terminal control definitions */
//#include <termio.h>                   /* POSIX terminal control definitions */
#include <sys/ioctl.h>
#include <sys/time.h>
#include <time.h>
#endif


bool jp2_14GetInfoAndSig();
void jp12PrintLastError( void );

#define READBLOCKSIZE (0x80)
EXPORT HANDLE hSerial = INVALID_HANDLE_VALUE;
static char activePort[MAX_DEV_FNAME_LEN] = "";
static unsigned char cmdBuff[ 0x20 ];
static unsigned char jp2info[ ADRLEN32 * JP2INFOCOUNT ]; // 9 values, either 16 or 32 bits each
static char signature[ SIGLEN + 1 ];
static JP12ADDR eepromAddress = 0;
static JP12OFFS eepromSize = 0;
static int pageSize = 0;
static int blockSize = 0;
static unsigned int adrLen = ADRLEN16; // 16-bit RCs
static bool flagAbort = false;  // a safety measure, set true when no further interface types should be tested

#ifdef WIN32
HINSTANCE m_hFTDILib = 0;

// 
// This variable is a Device Control Block (DCB) which holds the
// communications port state when it is first opened.  It is used to restore
// the state of any comm port which has been opened and had its settings
// altered.
// 
DCB saved_comm_state_dcb;

#else

// 
// This variable is a terminal I/O structure which holds the communications
// port state when it is first opened.  It is used to restore the state of any
// comm port which has been opened and had its settings altered.
// 
struct termios termios_saved;

#endif

#ifdef __APPLE__ //AKA Mac OS X
#define FTDI_SERIAL_DEVNAME "cu.usbserial"
#else
const char *portNames[] =
{
#ifdef WIN32
  "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
  "COM10", "COM11", "COM12", "COM13", "COM14", "COM15", "COM16", "COM17", "COM18", "COM19",
  "COM20", "COM21", "COM22", "COM23", "COM24", "COM25", "COM26", "COM27", "COM28", "COM29",
#endif
#ifdef __linux
  "/dev/ttyS0", "/dev/ttyS1", "/dev/ttyS2", "/dev/ttyS3", "/dev/ttyS4",
  "/dev/ttyS5", "/dev/ttyS6", "/dev/ttyS7", "/dev/ttyS8", "/dev/ttyS9",
  "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3",
  "/dev/ttyUSB4", "/dev/ttyUSB5", "/dev/ttyUSB6", "/dev/ttyUSB7",
  "/dev/ttyUSB8", "/dev/ttyUSB9",
#endif
#ifdef __solaris
  "/dev/ttya", "/dev/ttyb", "/dev/cua/a", "/dev/cua/b",
#endif
  NULL
};

const unsigned NUMPORTS = sizeof(portNames) / sizeof(char *);

const char *orderedPortNames[NUMPORTS];
#endif

// Do not change the order of the Connection types as this enum value is exported
// in versions later than 0.18_JP2.  Add new values at the end.  The upper byte 
// (bits 8..15) is used as flags.  In adding new values, note that the following 
// values are defined elsewhere by other interfaces:
// 0x106, 0x201 in CommHID; 0x301 in JPS. 
enum ConnectType {
	JP2_32BitAddr = 0x100, JP2_Simpleset = 0x300, JP2_NewFormat = 0x400,  // flags which are combined with existing interfaces
	NONE = 0, JP2_14 = 1, JP13 = 2, JP12 = 3, JP11 = 4, JP12Orig = 5, // add new types in this line
	JP2_14X = (JP2_14 | JP2_32BitAddr), JP2_14S = JP2_Simpleset, JP2_14N = (JP2_14 | JP2_NewFormat ), JP2_14XN = (JP2_14X | JP2_NewFormat ) // combined types
} Connection;

bool ConnIsJP2(void)
{
	switch(Connection)
	{
		case JP2_14:
		case JP2_14N:
		case JP2_14S:
		case JP2_14X:
		case JP2_14XN:
			return true;
			break;

		default:
			return false;
			break;
	}
}

// Functions to access big endian (BE) memory buffers by passing a character pointer
inline unsigned int GET_BE16(unsigned char* bptr)
{
  return bptr[0] << 8 | bptr[1];
}

inline unsigned long GET_BE32(unsigned char* bptr)
{
  return bptr[0] << 24 | bptr[1] << 16 | bptr[2] << 8 | bptr[3];
}

inline void PUT_BE16(unsigned char* bptr, unsigned int value)
{
  bptr[0] = (value>>8) & 0xff;
  bptr[1] =  value     & 0xff;
}

inline void PUT_BE32(unsigned char* bptr, unsigned long value)
{
  bptr[0] = (value>>24) & 0xff;
  bptr[1] = (value>>16) & 0xff;
  bptr[2] = (value>>8)  & 0xff;
  bptr[3] =  value      & 0xff;
}

int jp12EscapeComm( int func )
{
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;

#ifdef WIN32
	int rc = EscapeCommFunction( hSerial, func );
#else
	int status;
	int rc = !ioctl( hSerial, TIOCMGET, &status );
	if(rc)
	{
		switch ( func )
		{
			case CLRDTR:
				status &= ~TIOCM_DTR;
				rc = !ioctl( hSerial, TIOCMSET, &status );
				break;
			case SETDTR:
				status |= TIOCM_DTR;
				rc = !ioctl( hSerial, TIOCMSET, &status );
				break;
			case CLRRTS:
				status &= ~TIOCM_RTS;
				rc = !ioctl( hSerial, TIOCMSET, &status );
				break;
			case SETRTS:
				status |= TIOCM_RTS;
				rc = !ioctl( hSerial, TIOCMSET, &status );
				break;
			case CLRBREAK: 
				rc = !ioctl(hSerial, TIOCCBRK, 0);
				break;
			case SETBREAK: 
				rc = !ioctl(hSerial, TIOCSBRK, 0);
				break;
			default:
				printf( "Unknown function %i\n", func );
				rc = 0;
				break;
		}
	}

#endif
	if ( !rc )
	{
		printf( "EscapeCommFunction( %i ) failed: ", func );
		jp12PrintLastError();
	}

	return rc;
}


#ifdef TRACE_SERIAL
FILE* fh_trace = NULL;
double traceStartTime = 0.0;
void CloseTrace()
{
	if(fh_trace)
		fclose(fh_trace);
	fh_trace = NULL;
}

void OpenTrace()
{
	CloseTrace();
	fh_trace = fopen("ser_trace.txt", "a");
	struct timeval tv;
	if(gettimeofday(&tv, NULL) == 0)
	{
		traceStartTime = (double)tv.tv_sec + ((double)tv.tv_usec / 1.0E6L);
		struct tm ltm;
		localtime_r(&tv.tv_sec, &ltm);
		if(fh_trace)
		{
			fprintf(fh_trace, "\n\n================================================================================\n");
			fprintf(fh_trace, "Trace start time: %04d-%02d-%02dT%02d:%02d:%02d.%03d \n",
				ltm.tm_year+1900, ltm.tm_mon+1, ltm.tm_mday, ltm.tm_hour, ltm.tm_min, ltm.tm_sec, tv.tv_usec / 1000);
		}
	}
}

void TraceTOD()
{
	struct timeval tv;
	if(fh_trace && (gettimeofday(&tv, NULL) == 0))
	{
		double traceTime = ((double)tv.tv_sec + ((double)tv.tv_usec / 1.0E6L)) - traceStartTime;
		fprintf(fh_trace, "%06ldms ", (long)(traceTime * 1000.0L));
	}
}
#else
void OpenTrace() { }
void CloseTrace() { }
void TraceTOD() { }
#endif

// The URC7980 has a delay of about 5 seconds before a GetInfo call returns data.  To handle this, an
// optional parameter "duration" has been added to ReadSerial, a time value in seconds as a double. 
// The serial port is read repeatedly until either the requested number of bytes have been read or the
// elapsed time exceeds the specified duration with no bytes read on the last attempt.  So if data is
// being read when elapsed time is reached, reads will continue until the requested number of bytes is
// read or no more bytes remain.  The default duration is 0, giving exactly one read attempt.  Both 
// Windows and non-Windows OS's are handled.
int ReadSerial( HANDLE handle, unsigned char *buff, int len, DWORD* bytesRead, double duration = 0.0 )
{
  int rc = 0;
  *bytesRead = 0;
  DWORD bytes = 0;
  int n = 0;
  clock_t start = clock();
  while (true)
  {
#ifdef WIN32
	rc = ReadFile( handle, buff + *bytesRead, len - *bytesRead, &bytes, NULL);
#else
	bytes = read( handle, buff + *bytesRead, len - *bytesRead );
	rc = bytes < 0 ? 0 : 1;
#endif
	if (rc == 0)
      break;
    *bytesRead += bytes;
	double elapsed = ( clock() - start ) / (double) CLOCKS_PER_SEC;
	if (*bytesRead >= (unsigned int)len || elapsed > duration && bytes == 0)
	  break;
  }
  if(*bytesRead != len)
    rc = 0;

  return rc;
}

#ifdef WIN32
// Wrapper for the Windows WriteFile because it returns ok, even if less than
// the requested number of bytes has been written
int WriteSerial( HANDLE handle, const unsigned char *buff, int len, DWORD* bytesWritten)
{
  *bytesWritten = 0;
  int rc = WriteFile(handle, buff, len, bytesWritten, NULL);
  if(*bytesWritten != len)
    rc = 0;

  return rc;
}

#else // #ifdef WIN32

// 
// Mimic versions of the Windows API routines for non-Windows platforms
// (e.g. Linux)...
// Note that ReadSerial is now handled separately.
//

/*
int ReadSerial( HANDLE handle, unsigned char *buff, int len, DWORD* bytesRead)
{
  int rc = 0;
  *bytesRead = 0;
  int bytes = -1;
  while (*bytesRead < len)
  {
    bytes = read( handle, buff + *bytesRead, len - *bytesRead );
    if(bytes > 0)
    {
      *bytesRead += bytes;
    }
    else
    {
      break;
    }
  }

  if(*bytesRead == len)
    rc = 1;

  return rc;
}
*/

int WriteSerial( HANDLE handle, const unsigned char *buff, int len, DWORD* bytesWritten)
{
  int rc = 0;
  *bytesWritten = write( handle, buff, len );

  if ( *bytesWritten == len )
    rc = 1;

  return rc;
}


int CloseHandle( HANDLE handle )
{
  if ( close( handle ) < 0 )
    return 0;
  return 1;
}

void Sleep( int millisec )
{
  usleep(1000 * millisec);
  return;
}

#endif // #ifdef WIN32


#ifdef TRACE_SERIAL
int ReadSerialTrace(const char* callerFct, long callerLine, HANDLE handle, unsigned char *buff, int len, DWORD* bytesRead)
{
	int rc = ReadSerial(handle, buff, len, bytesRead);

	if(fh_trace)
	{
		TraceTOD();
		fprintf(fh_trace, "Read    [%s:%ld] rc=%d bytesRead=%d len=%d data=", callerFct, callerLine, rc, *bytesRead, len);
		for(int i=0; i < *bytesRead; i++)
			fprintf(fh_trace, "%02x ", buff[i]);
		fprintf(fh_trace, "\n");
	}

	return rc;
}

int WriteSerialTrace(const char* callerFct, long callerLine,  HANDLE handle, const unsigned char *buff, int len, DWORD* bytesWritten)
{
	int rc = WriteSerial(handle, buff, len, bytesWritten);

	if(fh_trace)
	{
		TraceTOD();
		fprintf(fh_trace, "Write   [%s:%ld] rc=%d bytesWritten=%d len=%d data=", callerFct, callerLine, rc, *bytesWritten, len);
		for(int i=0; i < *bytesWritten; i++)
			fprintf(fh_trace, "%02x ", buff[i]);
		fprintf(fh_trace, "\n");
	}
	
	return rc;
}

int jp12EscapeCommTrace(const char* callerFct, long callerLine, int func)
{
	int rc = jp12EscapeComm(func);

	if(fh_trace)
	{
		TraceTOD();
		fprintf(fh_trace, "EscComm [%s:%ld] rc=%d fct=", callerFct, callerLine, rc);
		switch ( func )
		{
			case CLRDTR:
				fprintf(fh_trace, "CLRDTR\n");
				break;
			case SETDTR:
				fprintf(fh_trace, "SETDTR\n");
				break;
			case CLRRTS:
				fprintf(fh_trace, "CLRRTS\n");
				break;
			case SETRTS:
				fprintf(fh_trace, "SETRTS\n");
				break;
			case CLRBREAK: 
				fprintf(fh_trace, "CLRBREAK\n");
				break;
			case SETBREAK: 
				fprintf(fh_trace, "SETBREAK\n");
				break;
			default:
				fprintf(fh_trace, "unknown (%d)\n", func);
				break;
		}
	}

	return rc;
}

#define ReadSerial(handle, buff, len, bytesRead) ReadSerialTrace(__FUNCTION__, __LINE__, handle, buff, len, bytesRead)
#define WriteSerial(handle, buff, len, bytesWritten) WriteSerialTrace(__FUNCTION__, __LINE__, handle, buff, len, bytesWritten)
#define jp12EscapeComm(func) jp12EscapeCommTrace(__FUNCTION__, __LINE__, func)
#endif // #ifdef TRACE_SERIAL

void jp12PrintLastError( void )
{
#ifdef WIN32
  LPVOID lpMsgBuf;
  DWORD dw = GetLastError();

  FormatMessage( FORMAT_MESSAGE_ALLOCATE_BUFFER |
                 FORMAT_MESSAGE_FROM_SYSTEM,
                 NULL,
                 dw,
                 MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
                 ( LPTSTR )&lpMsgBuf,
                 0, NULL );

  printf( "Error %d: %s\n", (int)dw, (char*)lpMsgBuf );

  LocalFree(lpMsgBuf);
#else
  printf("%s\n", strerror(errno));
#endif
}

int FlushInput(HANDLE fh)
{
	int rc = 1;
#ifndef WIN32
#ifdef TCIFLUSH
	rc = tcflush(fh, TCIFLUSH) == 0; // POSIX, e.g. Max OSX
#else
#ifdef  TIOCFLUSH
	int flushcode = FREAD; // FREAD=1
    rc = ioctl(fh, TIOCFLUSH, (char *) &flushcode) == 0; // BSD e.g. Mac OSX
#else
#ifdef TCFLSH
    rc = ioctl(fh, TCFLSH, TCIFLUSH) == 0; // SysIII, e.g. Linux
#else
#error Missing ioctrl to flush input buffer
#endif
#endif
#endif

#else // Win32

	// http://msdn.microsoft.com/en-us/library/windows/desktop/aa364439%28v=vs.85%29.aspx
	// http://msdn.microsoft.com/en-us/library/windows/hardware/ff546655%28v=vs.85%29.aspx
	// http://msdn.microsoft.com/en-us/magazine/cc163415.aspx 
	DWORD purgeMask = SERIAL_PURGE_RXCLEAR;
	DWORD purgeRet = 0;
	DWORD bytesOut = 0;
	rc = DeviceIoControl(
		fh, //_In_ HANDLE hDevice,
		IOCTL_SERIAL_PURGE, //_In_ DWORD dwIoControlCode
		&purgeMask, //_In_opt_ LPVOID lpInBuffer
		sizeof(purgeMask), //_In_ DWORD nInBufferSize
		&purgeRet, //_Out_opt_ LPVOID lpOutBuffer
		sizeof(purgeRet), //_In_ DWORD nOutBufferSize
		&bytesOut, //_Out_opt_ LPDWORD lpBytesReturned
		NULL //_Inout_opt_ LPOVERLAPPED lpOverlapped
	);
#endif

	return rc;
}

int jp12Init( char* portName )
{
	if(hSerial != INVALID_HANDLE_VALUE)
        	return 0;

#ifdef WIN32
	hSerial = CreateFile( portName,
		GENERIC_READ | GENERIC_WRITE,
		0,
		0,
		OPEN_EXISTING,
		FILE_ATTRIBUTE_NORMAL,
		0 );
	if ( hSerial == INVALID_HANDLE_VALUE )
	{
		return 0;
	}

	DCB dcbSerialParams = { 0 };
	dcbSerialParams.DCBlength = sizeof( dcbSerialParams );

	if ( !GetCommState(hSerial, &dcbSerialParams))
	{
		CloseHandle( hSerial );
	        hSerial = INVALID_HANDLE_VALUE;
		return 0;
	} else {
		memcpy(&saved_comm_state_dcb, &dcbSerialParams, sizeof dcbSerialParams);
	}

	dcbSerialParams.BaudRate = 38400;
	dcbSerialParams.ByteSize = 8;
	dcbSerialParams.StopBits = TWOSTOPBITS;
	dcbSerialParams.Parity = NOPARITY;
	dcbSerialParams.fDtrControl = DTR_CONTROL_ENABLE;
	dcbSerialParams.fRtsControl = RTS_CONTROL_ENABLE;
	dcbSerialParams.fOutX = FALSE;
	dcbSerialParams.fInX = FALSE;

	if ( !SetCommState( hSerial, &dcbSerialParams ))
	{
		CloseHandle( hSerial );
	        hSerial = INVALID_HANDLE_VALUE;
		return 0;
	}

	COMMTIMEOUTS timeouts={0};
	timeouts.ReadIntervalTimeout = 50;
	timeouts.ReadTotalTimeoutConstant = 50;
	timeouts.ReadTotalTimeoutMultiplier = 10;

	timeouts.WriteTotalTimeoutConstant = 50;
	timeouts.WriteTotalTimeoutMultiplier = 10;
	if ( !SetCommTimeouts( hSerial, &timeouts ))
	{
		//    
		// Restore the state for the comm port we're about to close...
		//    
		if (!SetCommState(hSerial, &saved_comm_state_dcb))
		{
			printf("Error restoring original state for comm port '%s': ", portName);
			jp12PrintLastError();
		}
		CloseHandle( hSerial );
	        hSerial = INVALID_HANDLE_VALUE;
		return 0;
	}

#else

  hSerial = open( portName, O_RDWR | /* O_NOCTTY | */ O_NDELAY );
  if ( hSerial == INVALID_HANDLE_VALUE )
  {
    return 0;
  }

  struct termios options;
  if (tcgetattr( hSerial, &options ) < 0) {   /* Get the current options for the port*/
     printf("Error ('%s') reading comm port '%s' attributes!\n", strerror(errno), portName);
     CloseHandle(hSerial);
     hSerial = INVALID_HANDLE_VALUE;
     return 0;
  }
  else memcpy(&termios_saved, &options, sizeof(options));   // preserve port settings

  options.c_cflag &= ~PARENB;
  options.c_cflag &= ~PARODD;
  options.c_cflag &= ~HUPCL;
  options.c_cflag &= ~CSIZE;
  options.c_cflag &= ~CRTSCTS;
  options.c_cflag |= CREAD;
  options.c_cflag |= CLOCAL;
  options.c_cflag |= CS8;
  options.c_cflag |= CSTOPB;
  
  options.c_iflag &= ~BRKINT;
  options.c_iflag &= ~PARMRK;
  options.c_iflag &= ~INPCK;
  options.c_iflag &= ~ISTRIP;
  options.c_iflag &= ~INLCR;
  options.c_iflag &= ~IGNCR;
  options.c_iflag &= ~ICRNL; 
  options.c_iflag &= ~IXON;
  options.c_iflag &= ~IXOFF;
  options.c_iflag &= ~IXANY;
  options.c_iflag &= ~IMAXBEL;
  options.c_iflag &= ~IGNPAR;
#ifndef __solaris
  options.c_iflag &= ~IUTF8;
#endif
#ifdef IUCLC
  options.c_iflag &= ~IUCLC;
#endif
  options.c_iflag |= IGNBRK;

  options.c_oflag &= ~OPOST;
  options.c_oflag &= ~ONLCR;
#ifdef OXTABS
  options.c_oflag &= ~OXTABS;
#endif
#ifdef OLCUC
  options.c_oflag &= ~OLCUC;
#endif
  options.c_oflag &= ~OCRNL; 
  options.c_oflag &= ~ONOCR;
  options.c_oflag &= ~ONLRET;
  options.c_oflag &= ~OFILL;
  options.c_oflag &= ~OFDEL;
  options.c_oflag |= ONLCR;
  //options.c_oflag |= NL0;
  //options.c_oflag |= CR0;
  //options.c_oflag |= TAB0;
  //options.c_oflag |= BS0;
  //options.c_oflag |= VT0;
  //options.c_oflag |= FF0;
   
  options.c_lflag &= ~ISIG;
  options.c_lflag &= ~ICANON;
  options.c_lflag &= ~ECHO;
  options.c_lflag &= ~ECHONL;
#ifdef XCASE
  options.c_lflag &= ~XCASE;
#endif
  options.c_lflag &= ~TOSTOP;
  options.c_lflag &= ~ECHOPRT;
  options.c_lflag &= ~IEXTEN; // this is needed on a Mac
  options.c_lflag |= ECHOE;
  options.c_lflag |= ECHOK;
  options.c_lflag |= NOFLSH;
  options.c_lflag |= ECHOCTL;
  options.c_lflag |= ECHOKE;
  
	
  options.c_cc[VMIN]  = 0;
  options.c_cc[VTIME] = 1; // tenth of a second
  /*
   * Set the baud rates to 38400...
   */
  cfsetispeed( &options, B38400 );
  cfsetospeed( &options, B38400 );

  /*
   * Set the new options for the port...
   */

  tcsetattr( hSerial, TCSANOW, &options);

  fcntl( hSerial, F_SETFL, 0 );
#endif

  OpenTrace(); // should be done before the first IO call

  // DTR and RTS are already set, when we come here
//  jp12EscapeComm( SETDTR );
//  jp12EscapeComm( SETRTS );

  // calling resetRemote() here causes a quick LED blink
  // and then another reset into bootloader mode and
  // then another reset into remote mode followed by 2 LED blnks.
  // This is because fmIR calls ifStart/ReadOrWrite/ifStop for
  // each upload/download. ifStart calls openRemote() which in turn
  // calls jp12Init(). openRemote() then calls jp12Connect() which
  // does a reset into bootloader mode. ifStop calls closeRemote()
  // which resets remote and remote goes into remote mode followed
  // by 2 LED blinks.
  // resetRemote();                // reset remote and restart as a remote
  return 1;
}


// ******************************** Connection ********************************

// Reset JP1.2/JP1.3 remote and go into serial mode
// Works with JP1.2 design with no IDC-5 connection
// Works with JP1.2 remote and JP1.x design with IDC-5 connection
// Does NOT work with JP1.3 remote and JP1.x design with IDC-5 connection
//   because IDC-5 is High and puts JP1.3 remote into Tool mode
int jp12Connect( void )
{
	if ( !jp12EscapeComm( CLRRTS ))   // OFF/-10V/1 makes IDC-5 High
		return 0;
	if ( !jp12EscapeComm( CLRBREAK )) // OFF/-10V/1 makes IDC-4 High
		return 0;

	Sleep( 100 );
	if ( !jp12EscapeComm( CLRDTR ))   // OFF/-10V/1, discharge capacitor
		return 0;
	Sleep( 10 );                      // allow capacitor to discharge
	if ( !jp12EscapeComm( SETBREAK )) // ON/+10V/0 makes IDC-4 Low
		return 0;                       // JP1.3 remote tests for Low 56 cycles after Power On Reset
										// so need to set break ON early
	if ( !jp12EscapeComm( SETDTR ))   // ON/+10V/0, Reset pulse delay(10-20ms) from capacitor charge up
		return 0;                       // this generates the result pulse width

	Sleep( 100 );  // wait at least 20ms+50ms with IDC-4 Low for JP1.2 to go into BootLoader mode
	if ( !jp12EscapeComm( CLRBREAK )) // OFF/-10V/1 makes IDC-4 High
		return 0;

	Sleep( 10 );
		return 1;
}

int JP2_14StartOrStopComm( bool start )  {
	static unsigned char initComm[4] ={0x00, 0x02, 0x51, 0x53};
	static unsigned char exitComm[4] ={0x00, 0x02, 0x52, 0x50};
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;

	// try to clear out spurious bytes seen with transistor based serial interface 
	FlushInput(hSerial);

	DWORD bytesWritten = 0;
	if ( start ) {
		if ( !WriteSerial( hSerial, initComm, 4, &bytesWritten))
			return 0;
	} else {
		if ( !WriteSerial( hSerial, exitComm, 4, &bytesWritten))
			return 0;
	}
	DWORD bytesRead = 0;
	// do this also on stop, to clear serial receive buffer
	if (!ReadSerial(hSerial, cmdBuff, 4, &bytesRead) || (cmdBuff[2] != 0))
	{
		if (!start)
			return 1;  //Probably it won't respond since we've returned it to remote mode (actually it does)
		else
			return 0;
	}
	return 1;
}

//JP 1.4 and JP2 remotes simply require a 35 mS Low pulse of RTS (reset -- IDC-2)
// to go into host communication mode.  It will return to ordinary remote
// mode when JP2_14StartOrStopComm( false) is executed.
//
// JP3.1 appears to need more than 60ms.  Change 35ms to 80ms to be on safe side.

bool jp2_14Connect( void ) {
	int rc = 1;
	if ( (rc = jp12EscapeComm( SETRTS )))   // ON/+10V/0 makes IDC-2 Low
	{
		Sleep( 80 );
		if ( (rc = jp12EscapeComm( CLRRTS ))) // OFF/-10V/1 makes IDC-2 High
		{
			Sleep( 80 );
			// If jp2_14GetInfoAndSig() succeeds then set abort flag as false.
			// If it fails then leave this flag unchanged, it may be true or false.
			if ( (rc = jp2_14GetInfoAndSig()))
				flagAbort = false;
		}
	}

	if(!rc)
	{
		// restore initial state
		jp12EscapeComm( SETRTS );   // ON/+10V/0 makes IDC-2 Low
	}

	return (rc != 0); // to avoid warning C4800 from MS Visual Studio 2010
}

// Reset JP1.3 remote and go into serial mode
// Works with JP1.x design with IDC-5 connection
// Works with JP1.2 design with no IDC-5 connection
// Does NOT work for JP1.2 remote with JP1.x design with IDC-5
//   connection because IDC-5 is Low and puts JP1.2 remote into BDM mode
int jp13Connect( void )
{
	if ( !jp12EscapeComm( SETRTS ))   // ON/+10V/0 makes IDC-5 Low
		return 0;
	if ( !jp12EscapeComm( CLRBREAK )) // OFF/-10V/1 makes IDC-4 High
		return 0;

	Sleep( 100 );
	if ( !jp12EscapeComm( CLRDTR ))   // OFF/-10V/1, discharge capacitor
		return 0;
	Sleep( 10 );                      // allow capacitor to discharge
	if ( !jp12EscapeComm( SETBREAK )) // ON/+10V/0 makes IDC-4 Low
		return 0;                       // JP1.3 remote tests for Low 56 cycles after Power On Reset
										// so need to set break ON early

	if ( !jp12EscapeComm( SETDTR ))   // ON/+10V/0, Reset pulse delay(10-20ms) from capacitor charge up
		return 0;                       // this generates the result pulse width

	Sleep( 100 );  // wait at least 50ms with IDC-4 Low for JP1.3 to go into BootLoader mode
	if ( !jp12EscapeComm( CLRBREAK )) // OFF/-10V/1 makes IDC-4 High
		return 0;

	Sleep( 10 );

	return 1;
}

// Reset JP1.1 remote and go into serial mode
int jp11Connect( void )
{
	if ( !jp12EscapeComm( CLRDTR ))   // OFF/-10V/1, discharge capacitor
		return 0;
	Sleep( 10 );                      // allow capacitor to discharge
	if ( !jp12EscapeComm( SETRTS ))   // ON/+10V/0 makes IDC-5 /START Low
		return 0;
	if ( !jp12EscapeComm( SETDTR ))   // ON/+10V/0, Reset pulse delay(10-20ms) from capacitor charge up
		return 0;                       // this generates the result pulse width
	Sleep( 25 );                      // wait at least 20ms to allow for max reset pulse delay
	if ( !jp12EscapeComm( CLRRTS ))   // OFF/-10V/1 makes IDC-5 /START High
		return 0;
	Sleep( 10 );
	return 1;
}

// Reset JP1.2/JP1.3 remote and go into serial mode(Using Original circuit design)
// JP1.3 remote tests for Low 56 cycles after Power On Reset so need to set break ON early
// JP1.2 remote tests for Low 50ms after Power On Reset
int jp12OriginalConnect( void )
{
	if ( !jp12EscapeComm( SETBREAK )) // ON/+10V/0 makes IDC-4 Low
		return 0;
	if ( !jp12EscapeComm( SETRTS ))   // ON/+10V/0 makes IDC-2 /RESET Low
		return 0;
	Sleep( 10 );
	if ( !jp12EscapeComm( CLRRTS ))   // OFF/-10V/1 makes IDC-2 /RESET High
		return 0;
	Sleep( 100 ); // wait at least 50ms with IDC-4 Low for JP1.2 to go into BootLoader mode
	if ( !jp12EscapeComm( CLRBREAK )) // OFF/-10V/1 makes IDC-4 High
		return 0;
	Sleep( 10 );
	return 1;
}


// ************************************ Flash Protocol *********************************

int jp12Test( void )
{
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;

	FlushInput(hSerial);

	cmdBuff[ 0 ] = 'E';
	DWORD bytesWritten = 0;
	if ( !WriteSerial( hSerial, cmdBuff, 1, &bytesWritten))
		return 0;

	DWORD bytesRead = 0;
	if ( !ReadSerial( hSerial, cmdBuff, 1, &bytesRead))
		return 0;

	if ( cmdBuff[ 0 ] != 6 )
		return 0;

	return 1;
}

unsigned char jp12ComputeCheckSum( const unsigned char *data, int length )
{
	unsigned char sum = 0;
	while(length--) sum ^= *data++;
	return sum;
}

int jp12GetIdentity( unsigned char *buffer )
{

	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;

	cmdBuff[ 0 ] = 'I';
	DWORD bytesWritten = 0;
	if ( ! WriteSerial( hSerial, cmdBuff, 1, &bytesWritten))
		return 0;

	DWORD bytesRead = 0;
	// response may be shorter than requested, ignore the error when bytesRead > 0
	if ( !ReadSerial( hSerial, buffer, 20, &bytesRead) && (bytesRead == 0))
		return 0;

	int len = bytesRead - 1;
	if ( buffer[ len ] != jp12ComputeCheckSum( buffer, len ))
		return 0;

	return len;
}

int jp12GetVersion( unsigned char *buffer )
{
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;

	cmdBuff[ 0 ] = 'V';
	DWORD bytesWritten = 0;
	if ( ! WriteSerial( hSerial, cmdBuff, 1, &bytesWritten))
	{
		printf( "Error sending 'V'ersion command: " );
		jp12PrintLastError();
		return 0;
	}

	DWORD bytesRead = 0;
	// response may be shorter than requested, ignore the error when bytesRead > 0
	if ( !ReadSerial( hSerial, buffer, 20, &bytesRead) && (bytesRead == 0))
	{
		printf( "Error reading 'V'ersion results: " );
		jp12PrintLastError();
		return 0;
	}

	int len = bytesRead - 1;
	if ( buffer[ len ] != jp12ComputeCheckSum( buffer, len ))
	{
		printf( "Version checksum mismatch!\n" );
		return 0;
	}
	return len;
}

bool jp2_14GetInfoAndSig()
{
	unsigned char buffer[2 + 1 + 100 + 1]; // 88 bytes are needed for JP3.1; allow 100 maximum
	unsigned char getInfo[4] ={0x00, 0x02, 0x50, 0x52};
	unsigned char getSig[10] ={0x00, 0x06, 0x01, 0x00, 0x00, 0x00, 0x2C, 0x00, 0x2C, 0x00};

	if (!JP2_14StartOrStopComm( true )) 
		return false;
	DWORD bytesWritten = 0;
	if ( ! WriteSerial( hSerial, getInfo, 4, &bytesWritten)) 
		return false;

	// If WriteSerial succeeded then the remote is JP1.4 or later, so if there is a subeequent
	// error then there is a problem.  In that case it may not be safe to try other interface types, 
	// so set the abort flag.  If this routine returns true then the abort flag is cancelled by the
	// calling routing.
    flagAbort = true;

	// The URC-7980 takes nearly 5 seconds before returning the getInfo response so use the
	// optional parameter in ReadSerial to allow for this.
	DWORD bytesRead = 0;
	if ( !ReadSerial( hSerial, buffer, 8, &bytesRead, 5.0) || (buffer[ 2 ] != 0) )
		return false;

	// JP3 and JP3.1 remotes have a longer response
	adrLen = ADRLEN16; // 16-bit RCs
	if(GET_BE16(&buffer[0]) == (2+1+ADRLEN32+1))
	{
		// read the additional bytes
		if ( !ReadSerial( hSerial, &buffer[8], 2, &bytesRead) )
			return false;
		adrLen = ADRLEN32; // 32 bit system
	}

	unsigned long reqLen = 4 + adrLen;
	unsigned int readLen = 6;  // initially read only first 6 bytes of sig block
	unsigned long sigAddr = GET_BE_ADRLEN(adrLen, &buffer[5]);
	if (sigAddr > 0x100000)  // No flash sizes are likely to exceed 0x100000 so something is badly wrong
		return false;
	PUT_BE16(&getSig[0], reqLen);
	PUT_BE_ADRLEN(adrLen, &getSig[3], sigAddr); // copy address of signature / jp12info-table
	PUT_BE16(&getSig[reqLen - 1], readLen ); // read-len
	getSig[reqLen + 1] = jp12ComputeCheckSum( getSig, reqLen + 1 );

	if ( ! WriteSerial( hSerial, getSig, reqLen + 2, &bytesWritten)) 
		return false;
	bytesRead = 0;
	if ( !ReadSerial( hSerial, buffer, 2 + 1 + readLen + 1, &bytesRead) || (buffer[ 2 ] != 0) )
		return false;

	// There are two formats for the first 6 bytes.  It is either an ASCII signature or a 2-byte checksum followed by
	// a 32-bit big-endian block length.  Distinguish by testing upper 16 bits of potential block length
	if (GET_BE16(&buffer[2 + 1 + 2]) == 0) {
		// Sig block starts with checksum followed by 32-bit block length.
		// Prepare to read the block, up to maximum of 100 bytes.
		readLen = GET_BE32(&buffer[2 + 1 + 2]);  // Get the block length.
		if (readLen > 96)
			readLen = 96;  // Set to max of 96, i.e. packet size of 2 + 1 + 96 + 1 = 100
		PUT_BE16(&getSig[reqLen - 1], readLen ); // read-len
		getSig[reqLen + 1] = jp12ComputeCheckSum( getSig, reqLen + 1 );

		if ( ! WriteSerial( hSerial, getSig, reqLen + 2, &bytesWritten)) 
			return false;
		bytesRead = 0;
		if ( !ReadSerial( hSerial, buffer, 2 + 1 + readLen + 1, &bytesRead) || (buffer[ 2 ] != 0) )
			return false;

		// Block length is followed by 6-byte ASCII signature.  
		for ( int i = 0; i < 6; i++ ) {
			signature[ i ] = buffer[ 2 + 1 + 6 + i ] & 0x7F;
		}
		signature[ 6 ] = '\0';

		// There are still two formats for the data bytes that follow the signature.  In format A
		// the signature block length is 0x200, but bytes beyond the first 0x40 are all 0xFF.
		// In format B, used in JP1.4N and JP3.1, there are more data bytes but there are no
		// padding 0xFFs.  Both data structures contain the address of the E2 area, but in different
		// locations.  Format B also holds the E2 length, at an offset > 0x40.  Use this to distinguish
		// the two formats.
		eepromSize = GET_BE32(&buffer[2 + 1 + 0x40 + 4 * adrLen]); // location in format B
		if ((eepromSize & 0xFFFF) == 0xFFFF)  // characterizes format A
		{
			eepromAddress = GET_BE32(&buffer[2 + 1 + 0x2B + 2 * adrLen]); // 32-bit E2 address in format A
			// No flash sizes are likely to exceed 0x100000 so check eepromAddress
			if (eepromAddress < 0 || eepromAddress >= 0x100000 )
				return false;
			// E2 length is not held in sig block in these remotes, so read it from E2 itself
			unsigned char getSigLen[10] = { 0x00, 0x08, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x00 };
			PUT_BE32(&getSigLen[3], eepromAddress);
			getSigLen[9] = jp12ComputeCheckSum(getSigLen, 9);
			if ( ! WriteSerial( hSerial, getSigLen, 10, &bytesWritten)) 
				return false;
			bytesRead = 0;
			if ( !ReadSerial( hSerial, buffer, 2 + 1 + 6 + 1, &bytesRead) || (buffer[ 2 ] != 0) )
				return false;
			eepromSize = GET_BE32(&buffer[2 + 1 + 2]);
			Connection = JP2_14S;
		}
		else  // format B
		{
			eepromAddress = GET_BE_ADRLEN(adrLen, &buffer[2 + 1 + 0x2C + 3 * adrLen]);
			// No flash sizes are likely to exceed 0x100000 so check eepromAddress
			if (eepromAddress < 0 || eepromAddress >= 0x100000 )
				return false;
			Connection = adrLen == ADRLEN16  ? JP2_14N : JP2_14XN;
		}
	}
	else  // Sig block starts with ASCII signature but has an extended signature of 26 (SIGLEN) bytes
	{
		// Prepare to read the full sig block data
		readLen = adrLen == ADRLEN16 ? SIGLEN + adrLen * JP2INFOCOUNT : 64;
		PUT_BE16(&getSig[reqLen - 1], readLen ); // read-len
		getSig[reqLen + 1] = jp12ComputeCheckSum( getSig, reqLen + 1 );

		if ( ! WriteSerial( hSerial, getSig, reqLen + 2, &bytesWritten)) 
			return false;
		bytesRead = 0;
		if ( !ReadSerial( hSerial, buffer, 2 + 1 + readLen + 1, &bytesRead) || (buffer[ 2 ] != 0) )
			return false;
		
		for ( int i = 0; i < SIGLEN; i++ ) {
			signature[ i ] = buffer[ 2 + 1 + i ] & 0x7F;  // set extended signature
		}
		signature[ SIGLEN ] = '\0';

		for ( unsigned int i = 0; i < adrLen * JP2INFOCOUNT; i++ ) {
			jp2info[ i ] = buffer[ 2 + 1 + SIGLEN + i ];  // set full set of jp2 info
		}

		eepromAddress = GET_BE_ADRLEN(adrLen, &buffer[2 + 1 + SIGLEN + 4 * adrLen ]);
		// No flash sizes are likely to exceed 0x100000 so check eepromAddress
		if (eepromAddress < 0 || eepromAddress >= 0x100000)
			return false;
		eepromSize    = GET_BE_ADRLEN(adrLen, &buffer[2 + 1 + SIGLEN + 5 * adrLen ]) - eepromAddress + 1;
		// Also check eepromSize
		if (eepromSize < 0 || eepromSize >= 0x100000)
			return false;

		if(adrLen == ADRLEN32)
			Connection = JP2_14X;
		else
			Connection = JP2_14;
	}

	if (signature[ 0 ] == 0x33) {  //the digit "3"
		pageSize  = 0x080;	//S3F80 can erase a sector of 128 bytes	
	} 
	else if (signature[ 0 ] == 0x36) {  //the digit "6"
		pageSize  = 0x0800;	//TI2540 erases a sector of 2K bytes but can write one 32-bit word	
	} 
	else {
		pageSize  = 0x0200; // 512  MAXQ610 erases a minimum of 512 bytes, but can write 1 word
	}
	blockSize = 0x0080; // 128 
	return true;
}

int readJP2_14Block( JP12ADDR address, int blockLength, unsigned char *buffer )
{
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;

	int cmdLen = 2; // 'length' is inserted at the end, so skip it here
	cmdBuff[ cmdLen++ ] = 0x01;  //Read command
	if(adrLen == ADRLEN32)
	{
		cmdBuff[ cmdLen++ ] = (address >> 24) & 0xff;
		cmdBuff[ cmdLen++ ] = (address >> 16) & 0xff;
	}
	cmdBuff[ cmdLen++ ] = (address >>  8) & 0xff;
	cmdBuff[ cmdLen++ ] = address & 0xff;
	cmdBuff[ cmdLen++ ] = (blockLength >>  8) & 0xff;
	cmdBuff[ cmdLen++ ] = blockLength & 0xff;

	// set length and checksum
	cmdLen++; // also count the checksum byte
	cmdBuff[        0 ] = ((cmdLen - 2) >> 8) & 0xff;
	cmdBuff[        1 ] =  (cmdLen - 2)       & 0xff;
	cmdBuff[cmdLen - 1]   = jp12ComputeCheckSum( cmdBuff, cmdLen - 1);
	
	DWORD bytesWritten = 0;  // send command
	if ( !WriteSerial( hSerial, cmdBuff, cmdLen, &bytesWritten))
		return 0;
	
	DWORD bytesRead = 0; // get acknowldegement
	if ( !ReadSerial( hSerial, cmdBuff, 3, &bytesRead) || (cmdBuff[2] != 0)) {
		ReadSerial( hSerial, cmdBuff, 1, &bytesRead); //read out the checksum
		return 0;
	}
	unsigned char ackChkSum = jp12ComputeCheckSum(cmdBuff,3); 
	bytesRead = 0;  //get data
	if ( !ReadSerial( hSerial, buffer, blockLength, &bytesRead))
		return 0;

	ackChkSum ^= jp12ComputeCheckSum( buffer, blockLength);
	bytesRead = 0;	// get checksum
	unsigned char tempByte;
	if ( !ReadSerial( hSerial, &tempByte, 1, &bytesRead) || (tempByte != ackChkSum))
		return 0;

	return bytesRead;
}

int readBlock( int address, int blockLength, unsigned char *buffer )
{
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;

	// fill command
		cmdBuff[ 0 ] = 'R';
		cmdBuff[ 1 ] = address >> 8;
		cmdBuff[ 2 ] = address & 0xFF;
		cmdBuff[ 3 ] = blockLength;
		cmdBuff[ 4 ] = jp12ComputeCheckSum( cmdBuff, 4 );
	for (int i=0; i<blockLength; i++)
		buffer[i] = 0;
	
	// send command
	DWORD bytesWritten = 0;
	if ( !WriteSerial( hSerial, cmdBuff, 5, &bytesWritten))
		return 0;
	
	// get result
	DWORD bytesRead = 0;
	if ( !ReadSerial( hSerial, cmdBuff, 1, &bytesRead))
		return 0;
	if ( cmdBuff[ 0 ] != 'r' )
		return 0;
	bytesRead = 0;
	if ( !ReadSerial( hSerial, buffer, blockLength, &bytesRead))
		return 0;

	DWORD resultLength = 0;
	if ( !ReadSerial( hSerial, cmdBuff, 1, &resultLength))
		return 0;
	if ( cmdBuff[ 0 ] != ( jp12ComputeCheckSum( buffer, blockLength ) ^ 'r' ))
		return 0;
	return bytesRead;
} 
 
int erasePages( JP12ADDR address )
{
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;
	int cmdLen, rdLen;
	if ( ConnIsJP2() ) {
		rdLen = 4;
		cmdLen = 2; // 'length' is inserted at the end, so skip it here
		cmdBuff[ cmdLen++ ] = 3;  // erase command
		if(adrLen == ADRLEN32)
		{
			cmdBuff[ cmdLen++ ] = (address >> 24) & 0xff;
			cmdBuff[ cmdLen++ ] = (address >> 16) & 0xff;
		}
		cmdBuff[ cmdLen++ ] = (address >>  8) & 0xff;
		cmdBuff[ cmdLen++ ] = address & 0xff;
		if(adrLen == ADRLEN32)
		{
			cmdBuff[ cmdLen++ ] = ((address + pageSize - 1) >> 24) & 0xff;
			cmdBuff[ cmdLen++ ] = ((address + pageSize - 1) >> 16) & 0xff;
		}
		cmdBuff[ cmdLen++ ] = ((address + pageSize - 1) >>  8) & 0xff;
		cmdBuff[ cmdLen++ ] = ( address + pageSize - 1)        & 0xff;

		// set length and checksum
		cmdLen++; // also count the checksum byte
		cmdBuff[        0 ] = ((cmdLen - 2) >> 8) & 0xff;
		cmdBuff[        1 ] =  (cmdLen - 2)       & 0xff;
		cmdBuff[cmdLen - 1]   = jp12ComputeCheckSum( cmdBuff, cmdLen - 1);
	} else  {
		int count = 1;
		cmdLen = 6;
		rdLen = 1;
		cmdBuff[ 0 ] = 'C';
		cmdBuff[ 1 ] = address >> 8;
		cmdBuff[ 2 ] = address & 0x00FF;
		cmdBuff[ 3 ] = count >> 8;
		cmdBuff[ 4 ] = count & 0x00FF;
		cmdBuff[ 5 ] = jp12ComputeCheckSum( cmdBuff, 5 );
	}
	DWORD bytesWritten = 0;
	if ( !WriteSerial( hSerial, cmdBuff, cmdLen, &bytesWritten))
		return 0;

	DWORD bytesRead = 0;
	if ( !ReadSerial( hSerial, cmdBuff, rdLen, &bytesRead))
		return 0;
	if ( ConnIsJP2() ) {
		if ( cmdBuff[ 2] != 0 )
		return 0;
	} else {
		if ( cmdBuff[ 0 ] != 6 )
		return 0;
	}
	return 1;
}

int writeBlock( JP12ADDR addr, const unsigned char *data, int length )
{
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;
	int cmdLen, rdLen;
	/// fill in the command header
	if ( ConnIsJP2() ) {
		cmdLen = 2; // 'length' is inserted at the end, so skip it here
		cmdBuff[ cmdLen++ ] = 0x02;  //Write command
		if(adrLen == ADRLEN32)
		{
			cmdBuff[ cmdLen++ ] = (addr >> 24) & 0xff;
			cmdBuff[ cmdLen++ ] = (addr >> 16) & 0xff;
		}
		cmdBuff[ cmdLen++ ] = (addr >>  8) & 0xff;
		cmdBuff[ cmdLen++ ] = addr & 0xff;

		// set length
		cmdLen++; // also count the checksum byte
		cmdBuff[        0 ] = ((cmdLen - 2 + length) >> 8) & 0xff;
		cmdBuff[        1 ] =  (cmdLen - 2 + length)       & 0xff;

		cmdLen--; // the checksum byte is handled in a separate write call
		rdLen = 4;
	}
	else {
		cmdBuff[ 0 ] = 'S';
		cmdBuff[ 1 ] = addr >> 8;
		cmdBuff[ 2 ] = addr & 0x00FF;
		cmdBuff[ 3 ] = length;
		cmdLen = 4;
		rdLen = 1;
	}
								 // compute the checksum
	unsigned char cs = jp12ComputeCheckSum( cmdBuff, cmdLen ) ^ jp12ComputeCheckSum( data, length );
									
	DWORD bytesWritten = 0;        // send the command header
	if ( !WriteSerial( hSerial, cmdBuff, cmdLen, &bytesWritten))
		return 0;
								 
	bytesWritten = 0;				// send the data
	if ( !WriteSerial( hSerial, data, length, &bytesWritten))
		return 0;
								 
	bytesWritten = 0;				// send the checksum
	if ( !WriteSerial( hSerial, &cs, 1, &bytesWritten))
		return 0;
							 
	DWORD bytesRead = 0;			// get the result
	if ( !ReadSerial( hSerial, cmdBuff, rdLen, &bytesRead))
		return 0;
	if ( ConnIsJP2() ) {
		if ( cmdBuff[2] != 0 ) 
			return 0;
	}
	else if ( cmdBuff[ 0 ] != 0x06 )
			return 0;
	return 1;
}

#ifdef WIN32
void reorderPorts(int port, bool tail)
{
	// Moves specified port to head of search list if tail == false, to tail if tail == true

	// Pointer to the specifed index in portNames[]
	const char *const p = portNames[port-1];

	// Find the orderedPortNames[] array index of the specified port
	int i;
	for (i=0; i < NUMPORTS && orderedPortNames[i] != p; i++)
	{
	}
	// About if port not found
	if (i == NUMPORTS)
		return;

	OutputDebugString("Moving "); OutputDebugString(p); OutputDebugString(tail ? " to the tail\n" : " to the head\n");

	if (tail)
	{
		// Move down folowing ports, don't touch the NULL at the end of list!
		for (; i < NUMPORTS - 2; ++i)
			orderedPortNames[i] = orderedPortNames[i+1];
		// Put the specified port at the tail of the array
		orderedPortNames[i] = p;
	} else {
		// Move up preceding ports
		for (; i > 0; --i)
			orderedPortNames[i] = orderedPortNames[i-1];
		// Put the specified port at the head of the array
		orderedPortNames[i] = p;
	}

}
#endif

// ****************************** Exported high level functions ********************************

#ifndef __APPLE__ //AKA Mac OS X
static int isPortAvailable(const char *portName )
{
  if(hSerial != INVALID_HANDLE_VALUE)
    return 0;

#ifdef WIN32
  char testName[ 5 + _MAX_PATH + 1 ];
  strcpy( testName , "\\\\.\\" );
  strcat( testName, portName );
  hSerial = CreateFile( testName,
                        GENERIC_READ | GENERIC_WRITE,
                        0,
                        0,
                        OPEN_EXISTING,
                        FILE_ATTRIBUTE_NORMAL,
                        0 );
#else
  hSerial = open( portName, O_RDWR | /* O_NOCTTY | */ O_NDELAY );
#endif

  if ( hSerial == INVALID_HANDLE_VALUE )
  {
    return 0;
  }

  CloseHandle( hSerial );
  hSerial = INVALID_HANDLE_VALUE;
  return 1;
}
#endif // #ifndef __APPLE__ //AKA Mac OS X

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jobjectArray JNICALL Java_com_hifiremote_jp1_io_JP12Serial_getPortNames( JNIEnv *env, jobject jobj )
{
  int count = 0;

#ifdef __APPLE__ //AKA Mac OS X
  // dynamically collect port names like "/dev/cu.usbserial-@6008i8L" "/dev/tty.usbserial-@6008i8L"
  size_t compareLen = strlen(FTDI_SERIAL_DEVNAME);
  DIR* dirp = opendir("/dev");
  dirent* dp;
  while ((dp = readdir(dirp)) != NULL)
    if (!strncmp(dp->d_name, FTDI_SERIAL_DEVNAME, compareLen)) {
      // it is not necessary to check if the port is available because the directory is
      // dynamically created, when the device is available
      ++count;
    }
  (void)closedir(dirp);
  dirp = NULL;
#else
  int bit = 1;
  int portFlags = 0;
  for ( int i = 0; portNames[ i ] != NULL ; ++i )
  {
    if ( isPortAvailable( portNames[ i ]))
    {
      ++count;
      portFlags |= bit;
    }
    bit <<= 1;
  }
#endif

  jobjectArray result;
  jclass strArrCls = env->FindClass( "Ljava/lang/String;" );
  if ( strArrCls == NULL )
    return NULL;                                        /* exception thrown*/

  result = env->NewObjectArray( count, strArrCls, NULL);
  if ( result == NULL )
    return NULL;                              /* out of memory error thrown*/

  int index = 0;

#ifdef __APPLE__ //AKA Mac OS X
  dirp = opendir("/dev");
  while ((index < count) && (dp = readdir(dirp)) != NULL)
    if (!strncmp(dp->d_name, FTDI_SERIAL_DEVNAME, compareLen)) {
      char path[MAX_DEV_FNAME_LEN] = "/dev/";
      strcat(path, dp->d_name);
      jstring name = env->NewStringUTF(path);
      if ( name == NULL )
        return NULL;                          /* out of memory error thrown*/

      env->SetObjectArrayElement( result, index++, name );
    }
  (void)closedir(dirp);
  dirp = NULL;
#else
  bit = 1;
  for ( int i = 0; portNames[ i ] != NULL; i++)
  {
    if (( portFlags & bit ) != 0 )
    {
      jstring name = env->NewStringUTF( portNames[ i ]);
      if ( name == NULL )
        return NULL;                          /* out of memory error thrown*/

      env->SetObjectArrayElement( result, index++, name );
    }
    bit <<= 1;
  }
#endif

  return result;
}
#endif


EXPORT bool serialPortLike(const char *s)
{
#ifdef WIN32
  return strncasecmp(s, "COM", 3) == 0;
#else
  #ifdef __APPLE__ //AKA Mac OS X
    // the port names are like "cu.usbserial-@6008i8L" or "tty.usbserial-@6008i8L"
    return strncmp(s, "/dev/tty", 8) == 0 || strncmp(s, "/dev/cu", 7) == 0;
  #else
    return strncmp(s, "/dev/tty", 8) == 0 || strncmp(s, "/dev/cua", 8) == 0;
  #endif
#endif
}


EXPORT const char *getInterfaceName( void )
{
  return "JP1.X Serial";
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jstring JNICALL Java_com_hifiremote_jp1_io_JP12Serial_getInterfaceName( JNIEnv *env, jobject jobj )
{
  return env->NewStringUTF( getInterfaceName());
}
#endif


EXPORT const char *getInterfaceVersion( void )
{
  return "0.22";
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jstring JNICALL Java_com_hifiremote_jp1_io_JP12Serial_getInterfaceVersion( JNIEnv *env, jobject jobj )
{
  return env->NewStringUTF( getInterfaceVersion());
}
#endif

bool openPort( const char *portName)
{
	Connection = NONE;
	adrLen = ADRLEN16; // all non JP2_14 RCs use 16-bit addresses
	pageSize = blockSize = eepromSize = eepromAddress = 0;

	if ( flagAbort || portName[0] == 0 ) return false;

	//printf( "Trying port %s\n", portName );

	char new_portName[MAX_DEV_FNAME_LEN] = "";

#ifdef WIN32	
	strcpy(new_portName,"\\\\.\\");
	strncat(&new_portName[0], portName, 5);
#else
	//strcpy(new_portName,portNames[ port ]);
	strcpy(new_portName, portName);
#endif

	// ("Port %s \n",new_portName);
	if ( !jp12Init( new_portName )) {
		return false;
	}
	//try JP2_14 first because it only requires a 35mS low pulse of reset
	//to identify itself
	if ( jp2_14Connect() )
		return true;  	// init test succeeded

	// If jp2_14Connect() fails with flagAbort set then the interface has been
	// identified as JP1.4/JP2.  It is therefore unsafe to try other interface 
	// types.
	if ( flagAbort )
		return false;

	// Try JP1.3 Connect next so IDC-5 is Low
	// IDC-5 High puts JP1.3 Remote in Tool(I2C) mode and
	// subsequent pulses on IDC-4(SCL) and IDC-6(SDA) are
	// mistakenly treated as I2C and can erase flash contents
	// IDC-5 Low and subsequent pulses won't affect JP1.2 Remotes
	if ( !jp13Connect()) {
		closeRemote();
		return false;
	}
	if ( jp12Test()) {
		Connection = JP13;					// 'E' Test passed
	} else {
		if ( !jp12Connect() ) {
			closeRemote();
			return false;
		}
		if ( jp12Test()) {
			Connection = JP12;				// 'E' Test passed
		} else {
			if ( !jp11Connect()) {
				closeRemote();
				return false;
			}
			if ( jp12Test()) {
				Connection = JP11;			// 'E' Test passed
			} else {
				if ( !jp12OriginalConnect()) {
					closeRemote();
					return false;
				}
				if ( jp12Test()) {
					Connection = JP12Orig;	// 'E' Test passed
				} else {
					closeRemote();
					return false;
				}
			}
		}
	}
	int len = jp12GetIdentity( cmdBuff );
	if ( len == 0 ) {
		closeRemote();
		return false;
	}

	if ( cmdBuff[ 0 ] == 'M' ) {			// JP1 Flash Remote
		pageSize  = 0x0200; // 512
		blockSize = 0x0080; // 128
	} else if ( cmdBuff[ 0 ] == 'S' ) {		// JP1 Flash Remote
		pageSize  = 0x0080; // 128
		blockSize = 0x0080; // 128
	} else if ( cmdBuff[ 0 ] == 'e' ) {		// JP1 EEPROM Adapter (new in rev 0.15)
		pageSize  = 0x0001; //   1
		blockSize = 0x0020; //  32
	}

	len = jp12GetVersion( cmdBuff );
	if ( len < 3 )
	{
		closeRemote();
		return false;
	}

	eepromSize = ( cmdBuff[ 0 ] & 0xFF ) << 7;
	eepromAddress = (( cmdBuff[ 1 ] & 0xFF ) << 8 ) | ( cmdBuff[ 2 ] & 0xFF );

	return true;
}

EXPORT const char *openRemote( const char *portName )
{
	flagAbort = false;

	// If a port was specified, use it, don't try others
	if ( portName != NULL )
	{
		strcpy(activePort, portName);
		return openPort( activePort ) ? activePort : NULL;
	}

	// If an active port was found, try it
	if ( activePort[0] )
	{
		//OutputDebugString("Last active port was "); OutputDebugString(activePort); OutputDebugString(", trying it...\n");

		if( openPort( activePort ) )
		{
			return activePort;
		}
	}

#ifndef __APPLE__ //AKA Mac OS X
	// Initialise the port list that will be used as processing order
	int i;
	for (i = 0; i < NUMPORTS; ++i)
		orderedPortNames[i] = portNames[i];
#endif

#ifdef WIN32
	// Identify non-Widget FTDI ports and give them priority

	int num;
	long comPort;

	//	The library ftd2xx.dll is part of the FTDI CDM driver suite for Windows
	//  but will not be present under Linux
	m_hFTDILib = LoadLibrary("ftd2xx.dll");
	if (m_hFTDILib && SetFunctions(m_hFTDILib)
		&& (num = GetNumFTDIDevices()) > 0)
	{
		int iTest;
		for (i = num; i > 0; i--)
		{
			// iTest returns -1 if COM port cannot be determined,
			// otherwise 0 if non-Widget, 1 if Widget
			iTest = TestFTDIDevice(i-1, &comPort); 
			if ((iTest >= 0) && (comPort < NUMPORTS-1)) reorderPorts(comPort, iTest > 0);
		}
	}
#endif


#ifdef __APPLE__ //AKA Mac OS X
	size_t compareLen = strlen(FTDI_SERIAL_DEVNAME);
	DIR* dirp = opendir("/dev");
	dirent* dp;
	while ((dp = readdir(dirp)) != NULL)
		if (!strncmp(dp->d_name, FTDI_SERIAL_DEVNAME, compareLen)) {
		// it is not necessary to check if the port is available because the directory entry is
		// dynamically created, when the device is available
			char path[MAX_DEV_FNAME_LEN] = "/dev/";
			strcat(path, dp->d_name);
			strcpy(activePort, path);
			if( openPort( activePort) )
				break;
		}
	(void)closedir(dirp);
	dirp = NULL;
#else
	// Try all ports in the list
	int port = 0;
	do {
		// Stop at end of port list
		if( !orderedPortNames[ port ] )
		{
			strcpy(activePort, "");
			break;
		}

		strcpy(activePort, orderedPortNames[ port++ ]);

		//OutputDebugString("Trying "); OutputDebugString(activePort); OutputDebugString("\n");

		// Stop when open succeeds
	} while( !openPort( activePort) );
#endif

	return activePort;
}


#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jstring JNICALL Java_com_hifiremote_jp1_io_JP12Serial_openRemote( JNIEnv *env, jobject obj, jstring jPortName )
{
  const char *portName = NULL;
  if ( jPortName != NULL )
  {
    portName = env->GetStringUTFChars( jPortName, NULL );
    if ( portName == NULL )
      return NULL;
  }
  return env->NewStringUTF( openRemote( portName ) );
}
#endif


EXPORT int closeRemote( void )
{
	if(hSerial == INVALID_HANDLE_VALUE)
		return 0;

	resetRemote(); // reset remote and restart as a remote
	CloseTrace();

	//    
	// Restore the state for the comm port we're about to close...
	//    
#ifdef WIN32
	if (m_hFTDILib) {
		FreeLibrary(m_hFTDILib);
	}
	if (!SetCommState(hSerial, &saved_comm_state_dcb)) {
		printf("Error restoring original state for comm port: ");
		jp12PrintLastError();
	}
#else
  if (tcsetattr(hSerial, TCSANOW, &termios_saved) < 0) {
     printf("Error ('%s') restoring serial port settings!\n", strerror(errno));
     return 0;
  }
#endif

	if ( !CloseHandle( hSerial ))
	{
		hSerial = INVALID_HANDLE_VALUE;
		return 0;
	}
	hSerial = INVALID_HANDLE_VALUE;
	return 1;
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT void JNICALL Java_com_hifiremote_jp1_io_JP12Serial_closeRemote( JNIEnv *env, jobject obj )
{
  closeRemote();
}
#endif


// Reset remote and restart as a remote
EXPORT int resetRemote( void )
{
	if (ConnIsJP2()) {
		JP2_14StartOrStopComm( false );
		return 1;
	}
	if (Connection == JP13 || Connection == JP12 || Connection == JP11) {
		// JP1.x /RESET on DTR. Current Design with Resistor and Capacitor on DTR.
		// DTR transition from -10V to +10V causes reset pulse.
		// Need IDC-4 High and IDC-5 High to ensure a Restart on JP1.2 remote with JP1.x design
		// Need IDC-4 High and IDC-5 Low  to ensure a Restart on JP1.3 remote with JP1.x design
		if ( !jp12EscapeComm( CLRBREAK )) // OFF/-10V/1 makes IDC-4 High
			return 0;
		// IDC-5 not set so it should be the same state it was (High or Low) when the
		// remote was opened
		//  if ( !jp12EscapeComm( CLRRTS ))   // OFF/-10V/1 makes IDC-5 High
		//    return 0;

		if ( !jp12EscapeComm( CLRDTR ))   // OFF/-10V/1
			return 0;
		Sleep( 10 );
		if ( !jp12EscapeComm( SETDTR ))   // ON/+10V/0
			return 0;
		Sleep( 100 );  // wait at least 20ms+50ms with IDC-4 High for JP1.2 to go into Restart mode
	}

	if (Connection == JP12Orig) {
		// JP1.2 Original Design with /RESET directly on RTS.
		// RTS transition from 0 to 1 causes reset.
		// Need IDC-4 High to ensure a Restart. IDC-5 is not used.
		if ( !jp12EscapeComm( CLRBREAK )) // OFF/-10V/1 makes IDC-4 High
			return 0;

		if ( !jp12EscapeComm( SETRTS ))   // ON/+10V/0 makes IDC-2 /RESET Low
			return 0;
		Sleep( 10 );
		if ( !jp12EscapeComm( CLRRTS ))   // OFF/-10V/1 makes IDC-2 /RESET High
			return 0;
		Sleep( 100 );  // wait at least 50ms with IDC-4 High for JP1.2 to go into Restart mode
	}

	return 1;
}

EXPORT int getJP12InterfaceType( void )
{
	return Connection;
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jint JNICALL Java_com_hifiremote_jp1_io_JP12Serial_getJP12InterfaceType( JNIEnv *, jobject )
{
  return getJP12InterfaceType();
}
#endif

EXPORT const char *getRemoteSignature( void )
{
	if ( !ConnIsJP2() )  {  //JP2_14 sig is read during GetInfo
		unsigned char firstBytes[10];
		if ( readRemote( eepromAddress, firstBytes, 10 ) != 10 )
			return NULL;
		int sigOffset = ( firstBytes[0] + firstBytes[1] == 0xFF ) ? 2 : 0;
		unsigned char byte;
		int i = 0;
		for ( ; i < 8 && ( byte = firstBytes[ i + sigOffset ], byte >= 0x20 && byte < 0x80 ); i++ ) {
			signature[ i ] = byte;
		}
		for ( ; i < 8; i++ ) {
			signature[ i ] = '_';
		}
		signature[ 8 ] = '\0';
	}
	return signature;
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jstring JNICALL Java_com_hifiremote_jp1_io_JP12Serial_getRemoteSignature( JNIEnv *env, jobject jobj )
{
  return env->NewStringUTF( getRemoteSignature());
}
#endif


EXPORT JP12ADDR getRemoteEepromAddress( void )
{
  return eepromAddress;
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jint JNICALL Java_com_hifiremote_jp1_io_JP12Serial_getRemoteEepromAddress( JNIEnv *, jobject )
{
  return getRemoteEepromAddress();
}
#endif


EXPORT JP12OFFS getRemoteEepromSize( void )
{
  return eepromSize;
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jint JNICALL Java_com_hifiremote_jp1_io_JP12Serial_getRemoteEepromSize( JNIEnv *env, jobject jobj )
{
  return getRemoteEepromSize();
}
#endif


EXPORT int readRemote( JP12ADDR address, unsigned char *buffer, int length )
{
	// only allow reading within the upgrade area
	if ( (address < eepromAddress) ||
		((address + length) > (eepromAddress + eepromSize)))
	{
		return -1;
	}

	int offset = 0;
	int blockLength = READBLOCKSIZE;
	while ( offset < length )
	{
		if (( offset + blockLength ) > length )
			blockLength = length - offset;
		if (ConnIsJP2()) {
			if ( !readJP2_14Block( address + offset, blockLength, buffer + offset ))
				return -1;
		} else {
			if       ( !readBlock( address + offset, blockLength, buffer + offset ))
				return -1;
		}
		offset += blockLength;
	}
	return offset;

}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jint JNICALL Java_com_hifiremote_jp1_io_JP12Serial_readRemote( JNIEnv *env, jobject obj, jint address, jbyteArray jBuffer, jint length )
{
  unsigned char *buffer = ( unsigned char * )env->GetByteArrayElements( jBuffer, 0 );
  int rc = readRemote( address, buffer, length );
  env->ReleaseByteArrayElements( jBuffer, (jbyte *)buffer, 0 );
  return rc;
}
#endif

EXPORT bool getJP2info( unsigned char *buffer, int length )
{
	memset(buffer, 0x00, length);
	memcpy( buffer, jp2info, sizeof( jp2info ) < (size_t)length ? sizeof( jp2info ) : length );
	return true;
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jboolean JNICALL Java_com_hifiremote_jp1_io_JP12Serial_getJP2info( JNIEnv *env, jobject obj, jbyteArray jBuffer, jint length )
{
  unsigned char *buffer = ( unsigned char * )env->GetByteArrayElements( jBuffer, 0 );
  bool b = getJP2info( buffer, length );
  env->ReleaseByteArrayElements( jBuffer, (jbyte *)buffer, 0 );
  return b;
}
#endif

EXPORT int writeRemote( JP12ADDR address, const unsigned char *buffer, int length )
{
	// only allow writing within the upgrade area
	if ( (address < eepromAddress) ||
	   ((address + length) > (eepromAddress + eepromSize)))
	{
		return -1;
	}

	// must have valid page and block sizes
	if( (pageSize < 1) || (blockSize < 1) )
	{
		return -1;
	}

	// Only allow writing on page boundaries, and in pages
	if ((( address % pageSize ) != 0 ) || (( length % pageSize ) != 0 ))
		return -1;

	int offset = 0;
	int blockLength = blockSize;
	while ( offset < length )
	{
		// EEPROM has a page size of 1 and does not need page erasure
		if ((( offset % pageSize ) == 0) && (pageSize > 1))
			erasePages( address + offset );

		if (( offset + blockLength ) > length )
			blockLength = length - offset;

		if ( !writeBlock( address + offset, buffer + offset, blockLength ))
			return -1;

		offset += blockLength;
	}

	return offset;
}

#ifdef JAVA_SDK_AVAILABLE
JNIEXPORT jint JNICALL Java_com_hifiremote_jp1_io_JP12Serial_writeRemote( JNIEnv *env, jobject obj, jint address, jbyteArray jBuffer, jint length )
{
  unsigned char *buffer = ( unsigned char * )env->GetByteArrayElements( jBuffer, 0 );
  int rc = writeRemote( address, buffer, length );
  env->ReleaseByteArrayElements( jBuffer, (jbyte *)buffer, 0 );
  return rc;
}
#endif
