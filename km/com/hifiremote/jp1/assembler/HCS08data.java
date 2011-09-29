package com.hifiremote.jp1.assembler;

public class HCS08data
{
  public static final String[][] AddressModes = {
    { "Nil", "", "" },                          { "Dir", "B1Z1", "$%02X" }, 
    { "Dir2", "B2R2Z1", "$%02X, $%04X" },       { "Rel", "B1R1", "$%04X" },
    { "Ix1", "B1Z1", "$%02X, X" },              { "Ix2", "B2A1", "$%02X%02X, X" }, 
    { "Ix", "", ",X" },                         { "Ixp", "B1R1", "X+, $%04X" }, 
    { "Ix1p", "B2R2Z1", "$%02X, X+, $%04X" },   { "Imd", "B2Z2", "#$%02X, $%02X" },
    { "Imm", "B1", "#$%02X" },                  { "Imm2", "B2A1", "#$%02X%02X" }, 
    { "DD", "B2Z3", "$%02X, $%02X" },           { "Ext", "B2A1", "$%02X%02X" }, 
    { "DIxp", "B1Z1", "$%02X, X+" },            { "IxpD", "B1Z1", "X+, $%02X" },
    { "SP1", "B1Z1", "$%02X, SP" },             { "SP1r", "B2R2Z1", "$%02X, SP, $%04X" }, 
    { "SP2", "B2A1", "$%02X%02X, SP" },         { "EQU4", "", "$%04X" },
    { "EQU2", "", "$%02X" }
  };


  public static final String[][] Instructions = {
    { "BRSET0", "Dir2" },     { "BRCLR0", "Dir2" },
    { "BRSET1", "Dir2" },     { "BRCLR1", "Dir2" },
    { "BRSET2", "Dir2" },     { "BRCLR2", "Dir2" },
    { "BRSET3", "Dir2" },     { "BRCLR3", "Dir2" },
    { "BRSET4", "Dir2" },     { "BRCLR4", "Dir2" },
    { "BRSET5", "Dir2" },     { "BRCLR5", "Dir2" },
    { "BRSET6", "Dir2" },     { "BRCLR6", "Dir2" },
    { "BRSET7", "Dir2" },     { "BRCLR7", "Dir2" },

    { "BSET0", "Dir" },       { "BCLR0", "Dir" },
    { "BSET1", "Dir" },       { "BCLR1", "Dir" },
    { "BSET2", "Dir" },       { "BCLR2", "Dir" },
    { "BSET3", "Dir" },       { "BCLR3", "Dir" },
    { "BSET4", "Dir" },       { "BCLR4", "Dir" },
    { "BSET5", "Dir" },       { "BCLR5", "Dir" },
    { "BSET6", "Dir" },       { "BCLR6", "Dir" },
    { "BSET7", "Dir" },       { "BCLR7", "Dir" },

    { "BRA", "Rel" },         { "BRN", "Rel" },
    { "BHI", "Rel" },         { "BLS", "Rel" },
    { "BCC", "Rel" },         { "BCS", "Rel" },
    { "BNE", "Rel" },         { "BEQ", "Rel" },
    { "BHCC", "Rel" },        { "BHCS", "Rel" },
    { "BPL", "Rel" },         { "BMI", "Rel" },
    { "BMC", "Rel" },         { "BMS", "Rel" },
    { "BIL", "Rel" },         { "BIH", "Rel" },

    { "NEG", "Dir" },         { "CBEQ", "Dir2" },
    { "LDHX", "Ext" },        { "COM", "Dir" },
    { "LSR", "Dir" },         { "STHX", "Dir" },
    { "ROR", "Dir" },         { "ASR", "Dir" },
    { "ASL", "Dir" },         { "ROL", "Dir" },
    { "DEC", "Dir" },         { "DBNZ", "Dir2" },
    { "INC", "Dir" },         { "TST", "Dir" },
    { "CPHX", "Ext" },        { "CLR", "Dir" },

    { "NEGA", "Nil" },        { "CBEQA", "Imm2" },
    { "MUL", "Nil" },         { "COMA", "Nil" },
    { "LSRA", "Nil" },        { "LDHX", "Imm2" },
    { "RORA", "Nil" },        { "ASRA", "Nil" },
    { "ASLA", "Nil" },        { "ROLA", "Nil" },
    { "DECA", "Nil" },        { "DBNZA", "Rel" },
    { "INCA", "Nil" },        { "TSTA", "Nil" },
    { "MOV", "DD" },          { "CLRA", "Nil" },

    { "NEGX", "Nil" },        { "CBEQX", "Imm2" },
    { "DIV", "Nil" },         { "COMX", "Nil" },
    { "LSRX", "Nil" },        { "LDHX", "Dir" },
    { "RORX", "Nil" },        { "ASRX", "Nil" },
    { "ASLX", "Nil" },        { "ROLX", "Nil" },
    { "DECX", "Nil" },        { "DBNZX", "Rel" },
    { "INCX", "Nil" },        { "TSTX", "Nil" },
    { "MOV", "DIxp" },        { "CLRX", "Nil" },

    { "NEG", "Ix1" },         { "CBEQ", "Ix1p" },
    { "NSA", "Nil" },         { "COM", "Ix1" },
    { "LSR", "Ix1" },         { "CPHX", "Imm2" },
    { "ROR", "Ix1" },         { "ASR", "Ix1" },
    { "ASL", "Ix1" },         { "ROL", "Ix1" },
    { "DEC", "Ix1" },         { "DBNZ", "Ix1" },
    { "INC", "Ix1" },         { "TST", "Ix1" },
    { "MOV", "Imd" },         { "CLR", "Ix1" },

    { "NEG", "Ix" },          { "CBEQ", "Ixp" },
    { "DAA", "Nil" },         { "COM", "Ix" },
    { "LSR", "Ix" },          { "CPHX", "Dir" },
    { "ROR", "Ix" },          { "ASR", "Ix" },
    { "ASL", "Ix" },          { "ROL", "Ix" },
    { "DEC", "Ix" },          { "DBNZ", "Ix" },
    { "INC", "Ix" },          { "TST", "Ix" },
    { "MOV", "IxpD" },        { "CLR", "Ix" },

    { "RTI", "Nil" },         { "RTS", "Nil" },
    { "BGND", "Nil" },        { "SWI", "Nil" },
    { "TAP", "Nil" },         { "TPA", "Nil" },
    { "PULA", "Nil" },        { "PSHA", "Nil" },
    { "PULX", "Nil" },        { "PSHX", "Nil" },
    { "PULH", "Nil" },        { "PSHH", "Nil" },
    { "CLRH", "Nil" },        { "*", "Nil" },
    { "STOP", "Nil" },        { "WAIT", "Nil" },

    { "BGE", "Rel" },         { "BLT", "Rel" },
    { "BGT", "Rel" },         { "BLE", "Rel" },
    { "TXS", "Nil" },         { "TSX", "Nil" },
    { "STHX", "Ext" },        { "TAX", "Nil" },
    { "CLC", "Nil" },         { "SEC", "Nil" },
    { "CLI", "Nil" },         { "SEI", "Nil" },
    { "RSP", "Nil" },         { "NOP", "Nil" },
    { "", "Nil", "1" },       { "TXA", "Nil" },

    { "SUB", "Imm" },         { "CMP", "Imm" },
    { "SBC", "Imm" },         { "CPX", "Imm" },
    { "AND", "Imm" },         { "BIT", "Imm" },
    { "LDA", "Imm" },         { "AIS", "Imm" },
    { "EOR", "Imm" },         { "ADC", "Imm" },
    { "ORA", "Imm" },         { "ADD", "Imm" },
    { "*", "Nil" },           { "BSR", "Rel" },
    { "LDX", "Imm" },         { "AIX", "Imm" },

    { "SUB", "Dir" },         { "CMP", "Dir" },
    { "SBC", "Dir" },         { "CPX", "Dir" },
    { "AND", "Dir" },         { "BIT", "Dir" },
    { "LDA", "Dir" },         { "STA", "Dir" },
    { "EOR", "Dir" },         { "ADC", "Dir" },
    { "ORA", "Dir" },         { "ADD", "Dir" },
    { "JMP", "Dir" },         { "JSR", "Dir" },
    { "LDX", "Dir" },         { "STX", "Dir" },

    { "SUB", "Ext" },         { "CMP", "Ext" },
    { "SBC", "Ext" },         { "CPX", "Ext" },
    { "AND", "Ext" },         { "BIT", "Ext" },
    { "LDA", "Ext" },         { "STA", "Ext" },
    { "EOR", "Ext" },         { "ADC", "Ext" },
    { "ORA", "Ext" },         { "ADD", "Ext" },
    { "JMP", "Ext" },         { "JSR", "Ext" },
    { "LDX", "Ext" },         { "STX", "Ext" },

    { "SUB", "Ix2" },         { "CMP", "Ix2" },
    { "SBC", "Ix2" },         { "CPX", "Ix2" },
    { "AND", "Ix2" },         { "BIT", "Ix2" },
    { "LDA", "Ix2" },         { "STA", "Ix2" },
    { "EOR", "Ix2" },         { "ADC", "Ix2" },
    { "ORA", "Ix2" },         { "ADD", "Ix2" },
    { "JMP", "Ix2" },         { "JSR", "Ix2" },
    { "LDX", "Ix2" },         { "STX", "Ix2" },

    { "SUB", "Ix1" },         { "CMP", "Ix1" },
    { "SBC", "Ix1" },         { "CPX", "Ix1" },
    { "AND", "Ix1" },         { "BIT", "Ix1" },
    { "LDA", "Ix1" },         { "STA", "Ix1" },
    { "EOR", "Ix1" },         { "ADC", "Ix1" },
    { "ORA", "Ix1" },         { "ADD", "Ix1" },
    { "JMP", "Ix1" },         { "JSR", "Ix1" },
    { "LDX", "Ix1" },         { "STX", "Ix1" },

    { "SUB", "Ix" },          { "CMP", "Ix" },
    { "SBC", "Ix" },          { "CPX", "Ix" },
    { "AND", "Ix" },          { "BIT", "Ix" },
    { "LDA", "Ix" },          { "STA", "Ix" },
    { "EOR", "Ix" },          { "ADC", "Ix" },
    { "ORA", "Ix" },          { "ADD", "Ix" },
    { "JMP", "Ix" },          { "JSR", "Ix" },
    { "LDX", "Ix" },          { "STX", "Ix" } };
  
  public static final String[][] Instructions2 = {
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "NEG", "SP1" },         { "CBEQ", "SP1r" },
    { "*", "Nil" },           { "COM", "SP1" },
    { "LSR", "SP1" },         { "*", "Nil" },
    { "ROR", "SP1" },         { "ASR", "SP1" },
    { "LSL", "SP1" },         { "ROL", "SP1" },
    { "DEC", "SP1" },         { "DBNZ", "SP1r" },
    { "INC", "SP1" },         { "TST", "SP1" },
    { "*", "Nil" },           { "CLR", "SP1" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "LDHX", "Ix" },         { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "LDHX", "Ix2" },        { "*", "Nil" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "LDHX", "Ix1" },        { "*", "Nil" },

    { "SUB", "SP2" },         { "CMP", "SP2" },
    { "SBC", "SP2" },         { "CPX", "SP2" },
    { "AND", "SP2" },         { "BIT", "SP2" },
    { "LDA", "SP2" },         { "STA", "SP2" },
    { "EOR", "SP2" },         { "ADC", "SP2" },
    { "ORA", "SP2" },         { "ADD", "SP2" },
    { "*", "Nil" },           { "*", "Nil" },
    { "LDX", "SP2" },         { "STX", "SP2" },

    { "SUB", "SP1" },         { "CMP", "SP1" },
    { "SBC", "SP1" },         { "CPX", "SP1" },
    { "AND", "SP1" },         { "BIT", "SP1" },
    { "LDA", "SP1" },         { "STA", "SP1" },
    { "EOR", "SP1" },         { "ADC", "SP1" },
    { "ORA", "SP1" },         { "ADD", "SP1" },
    { "*", "Nil" },           { "*", "Nil" },
    { "LDX", "SP1" },         { "STX", "SP1" },

    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "CPHX", "SP1" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "*", "Nil" },           { "*", "Nil" },
    { "LDHX", "SP1" },        { "STHX", "SP1" } };

  public static final String[][] absLabels = {
    { "XmitIR", "FF5F" },
    { "SetupXmitIR", "FF5C" },
    { "XmitIRNoEncode", "FF62" },
    { "IRMarkSpaceByPtr", "FF65" },
    { "IRMarkByPtr", "FF68" },
    { "IRSpaceByReg", "FF74" },
    { "XmitSplitIR", "FF7A" },
    { "EncodeDCBuf", "FF86" },
    { "TestRptReqd", "FF92" },
    { "ChkPowerKey", "FF95" },
    { "ChkRecordKey", "FF98" },
    { "ChkVolKeys", "FF9B" },
    { "ChkVolChFFKeys", "FF8F" },
    { "SetCarrier", "FF6E" },
    { "ChkLowBattery", "FF59" }
  };
  
  public static final String[][] zeroLabels = {
    { "DCBUF", "60", "DCBUF+", "0A" },
    { "PF0", "A2", "PF", "05" },
    { "PD00", "6A", "PD", "16" },
    { "DCNDX", "AC" },
    { "DBYTES", "A9" },
    { "CBYTES", "AA" },
    { "FLAGS", "B3" },
    { "CARRIER", "A7", "CARRIER+", "02" },
    { "RPT", "B2" }
  };

  public static final int[] oscData = { 4000000, 0, 0 };
}

