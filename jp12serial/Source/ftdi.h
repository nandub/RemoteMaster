// ftdi.h : header file
//

/////////////////////////////////////////////////////////////////////////////

BOOL SetFunctions(HINSTANCE hFTDILib);
int GetNumFTDIDevices();
int TestFTDIDevice(int ndx, LONG* pComPort);

// Open IRScope with command "IRScope -test" to invoke display of test messages
extern BOOL m_ftdiTest;
