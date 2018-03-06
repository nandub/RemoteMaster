package com.hifiremote.jp1.assembler;

public class TI2541data
{
  public static final String[][] zeroLabels = {
    { "PF0", "B0", "PF", "10" },
    { "PD00", "C0", "PD", "40", "2" },
    { "TXD0", "6C", "TXD", "10" },
    { "TXB0", "7C", "TXB", "10" },
    { "Tmp0", "2A", "Tmp", "0A" }, // probably BE-CF all available as temp store
    { "TogType", "A5" },
    { "TogMask", "A6" },
    { "TXBcount", "35", "TXBcount is total number of bits to send (but apparently not used)" },
    { "TXDcount", "36", "TXDcount is number of TXD bytes to send" },
    { "MinRpts", "37", "At least MinRpts frames sent after the first frame, more only if key held" },
    { "StatFlags", "42", "Status flags" },
    { "CtrlFlags", "43", "Control flags" },
    { "ToggleCtr", "12", "ToggleCtr is incremented on each keypress" },
    { "MinKeyHeld", "15", "Key is treated as held until MinKeyHeld frames have been sent" }
  };
  
  /* Other known equivalences with MAXQ are, as MAXQ -> TI
   *   84-93 -> A0-AF for protocol block header and options bytes is
   *   consistent with the TogType and TogMask values.
   */
  
  public static final int[] oscData = { 10000000, 0, 0 };
}
