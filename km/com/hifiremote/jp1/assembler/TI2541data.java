package com.hifiremote.jp1.assembler;

public class TI2541data
{
  public static final String[][] Instructions = {
    { "MOV", "Dir2" },           { "LSL", "Dir3" },
    { "LSR", "Dir3" },           { "ADD", "Dir3" },
    { "SUB", "Dir3" },           { "OR", "Dir3" },
    { "AND", "Dir3" },           { "XOR", "Dir3" },
    { "MULT", "Dir3" },          { "DIV", "Dir3" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    
    { "MOV", "Imm2" },           { "LSL", "Imm3" },
    { "LSR", "Imm3" },           { "ADD", "Imm3" },
    { "SUB", "Imm3" },           { "OR", "Imm3" },
    { "AND", "Imm3" },           { "XOR", "Imm3" },
    { "MULT", "Imm3" },          { "DIV", "Imm3" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    
    { "MOVW", "Dir2" },          { "LSLW", "Dir3" },
    { "LSRW", "Dir3" },          { "ADDW", "Dir2" },
    { "SUBW", "Dir2" },          { "ORW", "Dir2" },
    { "ANDW", "Dir2" },          { "XORW", "Dir2" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },

    { "MOVW", "Immw" },          { "LSLW", "Imm3" },
    { "LSRW", "Imm3" },          { "ADDW", "Immw" },
    { "SUBW", "Immw" },          { "ORW", "Immw" },
    { "ANDW", "Immw" },          { "XORW", "Immw" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    
    { "MOV", "Ind2" },           { "MOVI", "Ind2" },
    { "MOVD", "Ind2" },          { "IMOV", "Ind2" },
    { "DMOV", "Ind2" },          { "LSL", "Ind2" },
    { "LSR", "Ind2" },           { "ADD", "Ind2" },
    { "SUB", "Ind2" },           { "OR", "Ind2" },
    { "AND", "Ind2" },           { "XOR", "Ind2" },
    { "MOV", "Ind1" },           { "???", "Nil" },
    { "???", "Nil" },            { "NOP", "Nil" },
    
    { "DBBC", "Dir3" },          { "SWAP", "Dir2" },
    { "MOVN", "Imm3" },          { "MOV", "Indx" },
    { "CARRIER", "Immc" },       { "BRA", "BrNZ" },
    { "BRA", "BrZ" },            { "BRA", "Rel1" },
    { "DBNZ", "Rel2" },          { "BSR", "Rel1" },
    { "CALL", "Fun1W" },         { "BRA", "BrT" },
    { "BRA", "BrF" },            { "RTS", "Nil" },
    { "TIMING", "Immc" },        { "CARRIER", "Imm3" },
    
    { "UMOVN", "Imm3" },         { "UTIMN", "Imm11" },
    { "UMOV", "Imm2" },          { "SIGPTR", "Imm0w" },
    { "UMOVN", "Ind1n" },        { "SEND", "Imm23" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "???", "Nil" },
    { "???", "Nil" },            { "END", "Nil" }    
  };
    
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
    { "BPcount", "38", "BPcount is the number of burst pairs to send" },
    { "MARKdata", "3C", "Two bytes giving ON and TOTAL carrier times for signal MARK" },
    { "SPACEdata", "3E", "Two bytes giving ON and TOTAL carrier times for signal SPACE" },
    { "MARK0data", "40", "Two bytes giving ON and TOTAL carrier times for MARK in 0-burst" },
    { "StatFlags", "42", "Status flags" },
    { "CtrlFlags", "43", "Control flags" },
    { "ToggleCtr", "12", "ToggleCtr is incremented on each keypress" },
    { "MinKeyHeld", "15", "Key is treated as held until MinKeyHeld frames have been sent" }
  };
  
  public static final int[] oscData = { 10000000, 0, 0 };
}
