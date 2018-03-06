package com.hifiremote.jp1.assembler;

public class MAXQ610data
{  
  public static final String[][] zeroLabels = {
    { "PF0", "94", "PF", "10" },
    { "PD00", "30", "PD", "40", "2" },
    { "TXD0", "10", "TXD", "10" },
    { "TXB0", "20", "TXB", "10" },
    { "Tmp0", "BE", "Tmp", "0A" }, // probably BE-CF all available as temp store
    { "TogType", "89" },
    { "TogMask", "8A" },
    { "TXBcount", "B6", "TXBcount is total number of bits to send (but apparently not used)" },
    { "TXDcount", "B7", "TXDcount is number of TXD bytes to send" },
    { "MinRpts", "B8", "At least MinRpts frames sent after the first frame, more only if key held" },
    { "StatFlags", "BA", "Status flags" },
    { "CtrlFlags", "BB", "Control flags" },
    { "ToggleCtr", "E0", "ToggleCtr is incremented on each keypress" },
    { "MinKeyHeld", "E3", "Key is treated as held until MinKeyHeld frames have been sent" }
  };
  
  public static final String[][] AddressModes = {
    { "Nil", "B1", "" },
    { "Imm", "N3B1M2", "#%02Xh" },
    { "Rel", "N3B1M2R1", "%02Xh" },
    { "Src", "N3B1M3", "%s" },
    { "Dst", "N12M4", "%s" },
    { "Accb", "N1B1", "Acc.%d" },
    { "CAccb", "N1B1", "C, Acc.%d" },
    { "AccbC", "N1B1", "Acc.%d, C" },
    { "Cond", "N4C1", "%s" },
    { "CondRel", "N7C4M2R1", "%3$s, %1$02Xh" },
    { "CondSrc", "N7C4M3", "%3$s, %1$s" },
    { "CImmb", "N7M2", "C, #%02Xh.%3$d" },
    { "CSrcb", "N7M3", "C, %s.%3$d" },
    { "DstbImm0", "N13M5", "%2$s.%1$d, #0" },
    { "DstbImm1", "N13M5", "%2$s.%1$d, #1" },
    { "DstImm", "N15M6", "%3$s, #%1$02Xh" },
    { "DstImmZ", "N15M6Z1", "%3$s, #%1$02Xh" },
    { "DstSrc", "N15M7", "%3$s, %1$s" }
  };
  
  public static final String[] aluOps = {
    "*", "AND", "OR", "XOR", "ADD", "SUB", "ADDC", "SUBB"
  };
  
  public static final String[] bitOps = {
    "*", "AND", "OR", "XOR", "*", "*", "MOVE", "MOVE"
  };
  
  public static final String[] bitModes = {
    "Nil", "Accb", "Accb", "Accb", "Nil","Nil", "CAccb", "AccbC"
  };
  
  public static final String[] accOps = {
    "*", "CPL", "SLA", "SLA2", "RL", "RLC", "SLA4", "XCHN", 
    "XCH", "NEG", "SR", "SRA4", "RR", "RRC", "SRA2", "SRA"
  };
  
  public static final String[] miscOpsC = {
    "MOVE", "MOVE", "CPL", "NOP"
  };
  
  public static final String[][] miscModesC = {
    { "", "B1", "C, #0" }, { "", "B1", "C, #1" }, { "", "B1", "C" }, { "", "B1", "" }
  };
  
  public static final String[][] miscModesP = {
    { "", "N3B1M3", "%s" }, {}, {}, { "", "N3B1M3", "%s" }, { "", "N3B1M3", "LC[0], %s" }, { "", "N3B1M3", "LC[1], %s" }
  };
  
  public static final String[][] miscModesPImm = {
    { "", "N3B1M2", "#%02Xh" }, {}, {}, { "", "N3B1M2R1", "%02Xh" }, { "", "N3B1M2R1", "LC[0], %02Xh" }, { "", "N3B1M2R1", "LC[1], %02Xh" }
  };

  public static final String[] miscOpsP = {
    "PUSH", "*", "*", "CALL", "DJNZ", "DJNZ"
  };

  public static final String[] conditionCodes = {
    "", "Z", "C", "E", "S", "NZ", "NC", "NE"
  };
  
  public static final String[][] sources = {
    { "AP", "APC", "*", "*", "PSF", "IC", "*", "*",
      "SC", "IPR0", "*", "*", "*", "*", "CKCN", "WDCN" },
    { "A[0]", "A[1]", "A[2]", "A[3]", "A[4]", "A[5]", "A[6]", "A[7]",
      "A[8]", "A[9]", "A[10]", "A[11]", "A[12]", "A[13]", "A[14]", "A[15]" },
    { "Acc", "A[AP]" },
    { },
    { "IP" },
    { "@SP--", "SP", "IV", "*", "*", "*", "LC[0]", "LC[1]",
      "@SPI--" },
    { "@BP[OFFS]", "@BP[OFFS++]", "@BP[OFFS--]", "OFFS", "DPC", "GR", "GRL", "BP",
      "GRS","GRH", "GRXL", "FP" },
    { "@DP[0]", "@DP[0]++", "@DP[0]--", "DP[0]", "@DP[1]", "@DP[1]++", "@DP[1]--", "DP[1]",
      "@CP", "@CP++", "@CP--", "CP" }
  };
  
  public static final String[][] dests = {
    { "AP", "APC", "*", "*", "PSF", "IC", "*", "*", 
      "SC", "IPR0", "*", "*", "*", "*", "CKCN", "WDCN" },
    { "A[0]", "A[1]", "A[2]", "A[3]", "A[4]", "A[5]", "A[6]", "A[7]",
      "A[8]", "A[9]", "A[10]", "A[11]", "A[12]", "A[13]", "A[14]", "A[15]" },
    { "Acc" },
    { "PFX[0]", "PFX[1]", "PFX[2]", "PFX[3]", "PFX[4]", "PFX[5]", "PFX[6]", "PFX[7]" },
    { },
    { "@++SP", "SP", "IV", "*", "*", "*", "LC[0]", "LC[1]" },
    { "@BP[OFFS]", "@BP[++OFFS]", "@BP[--OFFS]", "OFFS", "DPC", "GR", "GRL", "BP",
      "*", "GRH" },
    { "@DP[0]", "@++DP[0]", "@--DP[0]", "DP[0]", "@DP[1]", "@++DP[1]", "@--DP[1]", "DP[1]",
        "*", "*", "*", "CP" }
  };
  
  public static final int[] oscData = { 6000000, 2, 1 };
 
}
