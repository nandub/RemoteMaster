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
      ('WagonMaster'), and Graham Dixon ('mathdon').
      Mac OS X support by Michael Dreher ('MikeT').
 
      Copyright (C) 2006-2009 Greg Bush (et al)
      
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

   Version History / Change Log:

      Note: Prior to version 0.14, no change log was kept.
      
      Version 0.18 (released 30 Oct 2010):
         - Added Mac OS X support for FTDI based cable, courtesy of Michael Dreher ('MikeT')

      Version 0.14 (released 13 Sep 2009):
  
         Except where noted otherwise, these changes were done by Bill Marr
         ('WagonMaster').
      
         - Move some definitions from the C source file to the header file,
           for better separation and in anticipation of future improvements.
           
         - Added a new routine to determine if the specified string parameter
           looks like a valid serial port name, for any of the supported
           platforms, courtesy of Bengt Martensson ('Barf').
           
           (At the moment, no current utilities use this routine, but future
           improvements to some of the command-line utilities will use it.)
           
         - Provide external routine references for the non-Windows routines.

         - Explicitly specify 'void' parameter lists in routines.
           
         - Use '#ifndef WIN32' in lieu of '#ifdef __linux' so that building on
           Unix platforms other than Linux works as expected, courtesy of
           Bengt Martensson ('Barf').

         - Use preprocessor directives to allow use of Microsoft Visual C++ as
           well as MinGW when compiling, courtesy of Graham Dixon ('mathdon').

*/

// the maximum length of a device path name
#define MAX_DEV_FNAME_LEN (127+1)

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#ifdef WIN32
#include <windows.h>

// 
// Allow compilation for Windows platforms using Microsoft Visual C++ (which
// doesn't provide 'strncasecmp()') as well as MinGW.
// 
#ifdef _MSC_VER
#define strncasecmp _strnicmp
#endif

#ifdef BUILD_DLL
#define EXPORT __declspec(dllexport)
#else
#define EXPORT __declspec(dllimport)
#endif

#else  // !WIN32

#define INVALID_HANDLE_VALUE (-1)
#define EXPORT

#define HANDLE int
#define DWORD int

#define SETRTS   0
#define CLRRTS   1
#define SETDTR   2
#define CLRDTR   3
#define SETBREAK 4
#define CLRBREAK 5

#endif // !WIN32


#ifndef WIN32
  extern int ReadFile( int handle, unsigned char *buff, int len, int *bytesRead, int *overlapped );
  extern int WriteFile( int handle, const unsigned char *buff, int len, int *bytesWritten, int *overlapped );
  extern int CloseHandle( int handle );
  extern void Sleep( int millisec );
#endif

#ifdef __cplusplus
extern "C"
{
#endif

#define JP12ADDR int
#define JP12OFFS int

	//EXPORT         int jp12Init( char* portName );
	//EXPORT         int jp12Connect( void );
	//EXPORT         int jp12GetIdentity( unsigned char *buffer );

	EXPORT        bool serialPortLike(const char *s);
	EXPORT const char *getInterfaceName( void );
	EXPORT const char *getInterfaceVersion( void );
	EXPORT const char *openRemote( const char *portName );
	EXPORT         int closeRemote( void );
	EXPORT         int resetRemote( void );
	EXPORT const char *getRemoteSignature( void );
	EXPORT         JP12ADDR getRemoteEepromAddress( void );
	EXPORT         JP12OFFS getRemoteEepromSize( void );
	EXPORT         int readRemote( JP12ADDR address, unsigned char *buffer, int length );
	EXPORT         int writeRemote( JP12ADDR address, const unsigned char *buffer, int length );
	EXPORT         int getJP12InterfaceType( void );
	EXPORT        bool getJP2info( unsigned char *buffer, int length );

#ifdef __cplusplus
}
#endif
