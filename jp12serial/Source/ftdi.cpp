// ftdi.cpp : implementation file
//

/*
	Copyright (C) 2012 Graham Dixon

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

#include "stdafx.h"
#include "ftd2xx.h"
#include "ftdi.h"

typedef FTD2XX_API FT_STATUS (WINAPI *typeFT_Open)(
	int deviceNumber,
	FT_HANDLE *pHandle
	);

typedef FTD2XX_API FT_STATUS (WINAPI *typeFT_OpenEx)(
    PVOID pArg1,
    DWORD Flags,
    FT_HANDLE *pHandle
    );

typedef FTD2XX_API FT_STATUS (WINAPI *typeFT_Close)(
    FT_HANDLE ftHandle
    );

typedef FTD2XX_API FT_STATUS (WINAPI *typeFT_CreateDeviceInfoList)(
	LPDWORD lpdwNumDevs
	);

typedef FTD2XX_API FT_STATUS (WINAPI *typeFT_GetDeviceInfoList)(
	FT_DEVICE_LIST_INFO_NODE *pDest,
	LPDWORD lpdwNumDevs
	);

typedef FTD2XX_API FT_STATUS (WINAPI *typeFT_EE_Read)(
    FT_HANDLE ftHandle,
	PFT_PROGRAM_DATA pData
	);

typedef FTD2XX_API FT_STATUS (WINAPI *typeFT_EEPROM_Read)(
	FT_HANDLE ftHandle,
	void *eepromData,
	DWORD eepromDataSize,
	char *Manufacturer,
	char *ManufacturerId,
	char *Description,
	char *SerialNumber
	);

typedef FTD2XX_API FT_STATUS (WINAPI *typeFT_GetComPortNumber)(
    FT_HANDLE ftHandle,
	LPLONG	lpdwComPortNumber
	);

typeFT_Open m_FT_Open;
typeFT_OpenEx m_FT_OpenEx;
typeFT_Close m_FT_Close;
typeFT_EE_Read m_FT_EE_Read;
typeFT_EEPROM_Read m_FT_EEPROM_Read;
typeFT_GetComPortNumber m_FT_GetComPortNumber;
typeFT_CreateDeviceInfoList m_FT_CreateDeviceInfoList;
typeFT_GetDeviceInfoList m_FT_GetDeviceInfoList;

FT_DEVICE_LIST_INFO_NODE *devInfo;

BOOL SetFunctions(HINSTANCE hFTDILib)
{
	int missing;
#ifndef FTDI_NOERROR
	CString errStr;
#endif
	m_FT_OpenEx=reinterpret_cast<typeFT_OpenEx>(GetProcAddress(hFTDILib,"FT_OpenEx"));
	m_FT_Close=reinterpret_cast<typeFT_Close>(GetProcAddress(hFTDILib,"FT_Close"));
	m_FT_EE_Read=reinterpret_cast<typeFT_EE_Read>(GetProcAddress(hFTDILib,"FT_EE_Read"));
	m_FT_EEPROM_Read=reinterpret_cast<typeFT_EEPROM_Read>(GetProcAddress(hFTDILib,"FT_EEPROM_Read"));
	m_FT_GetComPortNumber=reinterpret_cast<typeFT_GetComPortNumber>(GetProcAddress(hFTDILib,"FT_GetComPortNumber"));
	m_FT_CreateDeviceInfoList=reinterpret_cast<typeFT_CreateDeviceInfoList>(GetProcAddress(hFTDILib,"FT_CreateDeviceInfoList"));
	m_FT_GetDeviceInfoList=reinterpret_cast<typeFT_GetDeviceInfoList>(GetProcAddress(hFTDILib,"FT_GetDeviceInfoList"));
	missing = (		!m_FT_OpenEx
				|	!m_FT_Close<<1
				|	!m_FT_EE_Read<<2
				|	!m_FT_GetComPortNumber<<3
				|	!m_FT_CreateDeviceInfoList<<4
				|	!m_FT_GetDeviceInfoList<<5 );
#ifndef FTDI_NOERROR
	errStr.Format("Missing DLL functions.  Error %i.\n\n", missing);
	errStr += "You need version 2.04.06 of the FTDI driver (issued March 13, 2008)\n";
	errStr += "or later for Autolocate Widget to work with an FT232R-based widget\n";
	errStr += "but 2.08.24 (issued April 25, 2012) or later for newer FT230X-based\nwidgets.";
	if (missing) AfxMessageBox(errStr);
#endif

	return !missing;
}

int GetNumFTDIDevices()
{ 
	DWORD numDevs;
	int result = -1;
	int i = 0;
	BOOL found;
	
	// This loop tries reading the device info up to 5 times, as Tommy was experiencing sporadic cases of
	// it failing with FT230X chip
	for (; i < 5 && result == -1; i++) {
		devInfo = NULL;
		found = TRUE;
		if (m_FT_CreateDeviceInfoList(&numDevs) == FT_OK) {
			result = (int)numDevs;
			if (numDevs > 0) { 
				// allocate storage for list based on numDevs				
				devInfo = (FT_DEVICE_LIST_INFO_NODE*)malloc(sizeof(FT_DEVICE_LIST_INFO_NODE)*numDevs);  
				if (m_FT_GetDeviceInfoList(devInfo,&numDevs) == FT_OK) {
					for (int j = 0; j < numDevs; j++) {
						if (devInfo[j].Type == FT_DEVICE_UNKNOWN) {
							found = FALSE;
							if (devInfo[j].Flags & FT_FLAGS_OPENED)
							{
								m_FT_Close(devInfo[j].ftHandle);
							}
						}
					}
				}
				else if (i < 4) {
					// set up for repeat as get device info list failed
					result = -1;
				}
				else // i==4 (last repeat) && m_FT_GetDeviceInfoList failed
				{
					devInfo = NULL;
				}

				if (!found && i < 4) {
					// set up for repeat as unknown device type found
					free(devInfo);
					result = -1;
				}
			}
		}
		else {
			// create device info list failed; if i < 4 set up for repeat, if already last repeat (i == 4) we need to return -1
			result = -1;
		}
	}

#ifndef FTDI_NOERROR
	if (m_ftdiTest) {
		CString nstr;
		nstr.Format("Number of FTDI chips found: %i", result);
		AfxMessageBox(result == -1 ? "Search for FTDI chips failed" :
			result == 0 ? "No FTDI chips found" : nstr);
		if (result > 0 && devInfo == NULL) {
			AfxMessageBox("FTDI device information could not be retrieved");
		}
		else if (result > 0 && found && i > 1) {
			nstr.Format("Needed %i iterations to identify all chip types", i);
			AfxMessageBox(nstr);
		}
	}
#endif

	return result;
}

int TestFTDIDevice(int ndx, LONG* pComPort)
{
	if (devInfo==NULL) {
		return -1;
	}
	int widgetTest = -1;
	FT_HANDLE ftHandle;
	char ManufacturerBuf[64]; 
	char ManufacturerIdBuf[64];

	DWORD Type = devInfo[ndx].Type;
	char *DescriptionBuf = devInfo[ndx].Description; 
	char *SerialNumberBuf = devInfo[ndx].SerialNumber;

#ifndef FTDI_NOERROR
	CString teststr;
#endif

	if ( Type != FT_DEVICE_232R && Type != FT_DEVICE_X_SERIES ){

#ifndef FTDI_NOERROR
		CString errstr = "You have a device with an FTDI chip of unknown type which\n";
			   errstr += "may be a Widget.  For Autolocate to work with such widgets, you\n";
			   errstr += "need version 2.08.24 of the FTDI driver (issued April 25, 2012) or\n";
			   errstr += "later.";
		AfxMessageBox(errstr);
#endif

		return -1;
	}

	if (m_FT_OpenEx(SerialNumberBuf, FT_OPEN_BY_SERIAL_NUMBER, &ftHandle) != FT_OK) {

#ifndef FTDI_NOERROR
		if (m_ftdiTest) {
			teststr.Format("Chip %i could not be opened", ndx+1);
			AfxMessageBox(teststr);
		}
#endif

		return -1;
	}

	if (Type == FT_DEVICE_232R){
		FT_PROGRAM_DATA ftData;
 		ftData.Signature1 = 0x00000000; 
		ftData.Signature2 = 0xffffffff; 
		ftData.Version = 0x00000002; // EEPROM structure with FT232R extensions
		ftData.Manufacturer = ManufacturerBuf; 
		ftData.ManufacturerId = ManufacturerIdBuf; 
		ftData.Description = DescriptionBuf; 
		ftData.SerialNumber = SerialNumberBuf; 
 
		if (m_FT_EE_Read(ftHandle, &ftData) == FT_OK) {
			widgetTest = (ftData.Cbus3 == FT_232R_CBUS_CLK6 );

#ifndef FTDI_NOERROR
			if (m_ftdiTest) {
				if (!widgetTest) {
					teststr.Format("Chip %i has C3 parameter %i (FT232R chip, not a Widget)", ndx+1, ftData.Cbus3);
				}
				else {
					teststr.Format("Success!  Chip %i is an FT232R-based Widget!", ndx+1);
				}
				AfxMessageBox(teststr);
			}
		}
		else if (m_ftdiTest) {
			teststr.Format("EEPROM of FT232R chip %i could not be read", ndx+1);
			AfxMessageBox(teststr);		
#endif

		}
	}
	else {	// Type==FT_DEVICE_X_SERIES

#ifndef FTDI_NOERROR
		if (!m_FT_EEPROM_Read)
		{
			CString errstr = "You have a device with an FTDI X-Series chip which may be\n";
			       errstr += "a Widget.  For Autolocate to work with such widgets, you\n";
				   errstr += "need version 2.08.24 of the FTDI driver (issued April 25, 2012)\n";
				   errstr += "or later.";
			AfxMessageBox(errstr);
			return -1;
		}
#endif

		FT_EEPROM_HEADER ft_eeprom_header;
		ft_eeprom_header.deviceType = FT_DEVICE_X_SERIES;
		FT_EEPROM_X_SERIES ft_eeprom_x_series;
		ft_eeprom_x_series.common = ft_eeprom_header;
		ft_eeprom_x_series.common.deviceType = FT_DEVICE_X_SERIES;
		if (m_FT_EEPROM_Read(ftHandle,&ft_eeprom_x_series, sizeof(ft_eeprom_x_series),
				ManufacturerBuf, ManufacturerIdBuf, DescriptionBuf, SerialNumberBuf) == FT_OK) {
			widgetTest = (ft_eeprom_x_series.Cbus3 == FT_X_SERIES_CBUS_CLK6 );

#ifndef FTDI_NOERROR
			if (m_ftdiTest) {
				if (!widgetTest) {
					teststr.Format("Chip %i has C3 parameter %i (X-Series chip, not a Widget)", ndx+1, ft_eeprom_x_series.Cbus3);
				}
				else {
					teststr.Format("Success!  Chip %i is an FT230X-based Widget!", ndx+1);
				}
				AfxMessageBox(teststr);
			}
		}
		else if (m_ftdiTest) {
				teststr.Format("EEPROM of X-Series chip %i could not be read", ndx+1);
				AfxMessageBox(teststr);
#endif

		}		
	}

#ifndef FTDI_NOERROR
	if (widgetTest >= 0 && m_FT_GetComPortNumber(ftHandle,pComPort) != FT_OK) {
		teststr = "COM port of Widget could not be determined.\n\n";
		teststr += "You need version 2.04.06 of the FTDI driver \n(issued March 13, 2008) ";
		teststr += "or later for \nAutolocate Widget to work.";
		AfxMessageBox(teststr);
	}
	else if (m_ftdiTest && widgetTest >= 0) {
		teststr.Format("COM port of Chip %i is COM%i", ndx+1, *pComPort);
		AfxMessageBox(teststr);
	}
#else
	if (m_FT_GetComPortNumber(ftHandle,pComPort) != FT_OK) widgetTest = -1;
#endif

	m_FT_Close(ftHandle);
	return widgetTest; 
}

