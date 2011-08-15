package com.hifiremote.jp1.assembler;

public class CommonData
{
  public static final Integer[] to15 = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,13, 14, 15 };

  public static final Integer[] to8 = {
    0, 1, 2, 3, 4, 5, 6, 7, 8 };
  
  public static final String[] sigStructs012 = {
    "dev-cmd", "dev-cmd-!dev-!cmd", "dev-!dev-cmd-!cmd", "cmd-dev" };
   
  public static final String[] sigStructs34 = {
    "dev-cmd", "dev-cmd-!dev-!cmd", "dev-!dev-cmd-!cmd", "cmd-!dev", 
    "cmd-dev2", "cmd", "cmd-!cmd", "cmd-!dev-!cmd", "cmd-!dev-dev2", 
    "cmd-!dev-dev2-!cmd", "cmd-dev2-!cmd", "!cmd", "dev", "dev-!cmd", 
    "dev-!dev", "dev-!dev-!cmd", "dev-!dev-cmd", "dev-!dev-dev2", 
    "dev-!dev-dev2-!cmd", "dev-!dev-dev2-cmd", "dev-!dev-dev2-cmd-!cmd", 
    "dev-cmd-!cmd", "dev-cmd-!dev", "dev-cmd-!dev-dev2", 
    "dev-cmd-!dev-dev2-!cmd", "dev-cmd-dev2", "dev-cmd-dev2-!cmd", 
    "dev-dev2", "dev-dev2-!cmd", "dev-dev2-cmd", "dev-dev2-cmd-!cmd", 
    "!dev", "!dev-!cmd", "!dev-cmd", "!dev-cmd-!cmd", "!dev-dev2", 
    "!dev-dev2-!cmd", "!dev-dev2-cmd", "!dev-dev2-cmd-!cmd", "dev2", 
    "dev2-!cmd", "dev2-cmd", "dev2-cmd-!cmd", "devs", "devs-!cmd", 
    "devs-!dev", "devs-!dev-!cmd", "devs-!dev-cmd", "devs-!dev-cmd-!cmd", 
    "devs-!dev-dev2", "devs-!dev-dev2-!cmd", "devs-!dev-dev2-cmd", 
    "devs-!dev-dev2-cmd-!cmd", "devs-cmd", "devs-cmd-!cmd", "devs-cmd-!dev", 
    "devs-cmd-!dev-!cmd", "devs-cmd-!dev-dev2", "devs-cmd-!dev-dev2-!cmd", 
    "devs-cmd-dev2", "devs-cmd-dev2-!cmd", "devs-dev2", "devs-dev2-!cmd", 
    "devs-dev2-cmd", "devs-dev2-cmd-!cmd" }; 


  public static final String[] bitDouble012 = {
    "None", "'0' after every bit", "'1' after every bit", "Double every bit" };

  public static final String[] bitDouble34 = {
    "None", "Bit Double On" };

  public static final String[] repeatType012 = {
    "Minimum", "Forced" };
  
  public static final String[] repeatType34 = {
    "Minimum", "" };
  
  public static final String[] repeatHeld012 = {
    "No", "Yes", "Ch+/-, Vol+/-, FF, Rew", "No data bits in repeat" };
  
  public static final String[] noYes = {
    "No", "Yes" };
  
  public static final String[] leadInStyle = {
    "None", "Same every frame", "1st frame only", "Half-size after 1st" };
  
  public static final String[] leadOutStyle012 = {
    "0 = [-LO]", "1 = [LI], [-LO]", "2 = [OneOn, -LO]", "3 = [LI], [OneOn, -LO]" };

  public static final String[] leadOutStyle34 = {
    "", "[OneOn]", "0 = [-LO]", "2 = [OneOn, -LO]" };
  
  public static final String[] midFrameCode1 = {
    "8D 01 4E", "8D 01 61", "CC FF 7A", "CC 01 C4" };
    
  public static final String[] midFrameCode2 = {
    "F6 01 4E", "F6 01 61", "CD FF 7A", "CD 01 C4" };

  public static final int[] burstOFFoffsets34 = { 52, 57 };
  
  public static final int[] leadinOFFoffsets34 = { 45, 45 };
  
  public static final int[] altLeadoutOffsets34 = { 45, - 40 };
  
  public static String[][][] pfData = {
      { // PF0
        { "2", "0-1",
          "Words to send in device code portion of IR signal\n" + 
          "0 = nothing\n" + 
          "1 = single word of PD00 bits\n" +  
          "2 = word of PD00 bits followed by word of PD10 bits\n" +  
          "3 = all protocol's fixed parameters (PD00 bits each)\n" +
          "First byte is the R01-th (S3C80) / $AC-th (HCS08) in all cases" 
        },
        { "2", "2-3",
          "Words to send in command code portion of IR signal\n" +  
          "0 = nothing\n" +  
          "1 = single word of PD01 bits\n" +  
          "2 = word of PD01 bits followed by word of PD12 bits\n" +  
          "3 = all protocol's variable parameters (PD01 bits each)"
        },
        { "2", "4-5",
          "How to compose the signal (! = complement)\n" +
          "0 = device - command\n" +  
          "1 = device - command - !device - !command\n" +  
          "2 = device - !device - command - !command\n" +  
          "3 = command - device"
        },
        { "1", "6",
          "Is Lead Out gap adjusted for total frame length (Off is Total)\n" +
          "0 = no\n" +
          "1 = yes"
        },
        { "1", "7",
          "Is PF1 present?\n" + 
          "0 = no\n" +
          "1 = yes"
        }
      },
      { // PF1
        { "2", "0-1",
          "Does the signal repeat while button is held down?\n" +  
          "0 = no\n" + 
          "1 = yes\n" + 
          "2 = Ch+/-, Vol+/-, FF, Rew\n" + 
          "3 = no data bits in repeat"
        },
        { "2", "2-3",
          "How to send lead-in burst pair\n" +
          "0 = nothing\n" + 
          "1 = always PD0A/PD0D\n" + 
          "2 = PD0A/PD0D the first time only, nothing afterwards\n" + 
          "3 = PD0A/PD0D the first time, OFF-time halved for repeats and with no data"
        },
        { "1", "4",
          "Is number of repeats taken from PD11?\n" +
          "0 = no\n" +
          "1 = yes" 
        },
        { "2", "5-6",
          "Lead Out On style\n" +
          "0 = [-LO]\n" +
          "1 = [LI], [-LO]\n" +
          "2 = [One On, -LO]\n" +
          "3 = [LI], [One On, -LO]"
        },
        { "1", "7",
          "Is PF2 present?\n" + 
          "0 = no\n" +
          "1 = yes"
        }
      },
      { // PF2
        { "2", "0-1",
          "How to send data for device bytes\n" + 
          "0 = send data as-is\n" + 
          "1 = send 0 after every bit\n" + 
          "2 = send 1 after every bit\n" + 
          "3 = send every bit twice"
        },
        { "2", "2-3",
          "How to send data for command bytes\n" + 
          "0 = send data as-is\n" + 
          "1 = send 0 after every bit\n" + 
          "2 = send 1 after every bit\n" + 
          "3 = send every bit twice"
        },
        { "1", "4",
          "Send zero backwards? (bi-phase)\n" +
          "0 = no\n" +
          "1 = yes"
        },
        { "1", "5",
          "For 0-bit that is in even position (2nd, 4th bits sent etc) " +
          "send burst pair PD06/(PD02+PD04+PD08) instead of PD06/PD08?\n" + 
          "0 = no\n" +
          "1 = yes"
        },
        { "1", "6",
          "Use Extended Lead-Out OFF time, adding 0xFFFF to value in PD0A/PD0B?\n" +
          "0 = no\n" +
          "1 = yes"
        },
        { "1", "7",
          "Is PF3 present?\n" + 
          "0 = no\n" +
          "1 = yes"
        }
      },
      { // PF3
        { "3", "0-2",
          "Determines encoding scheme (translation from data bytes to bit sequence)\n" +
          "Except for following special values, each bit of a data byte is sent as itself, "+
          "starting at most significant bit, unless bit 6 of PF4 is set (see PF4).\n" +
          "1 = as above but value sent is device byte XORed with #$78 (used only for one device byte, " +
          "e.g. in Protocol 002A)\n" +
          "3 = use four bits to send two bits (1000 = 0, 0100 = 1, 0010 = 2, 0001 = 3)\n" +
          "4 + use variable number of bits to send two bits (1 = 0, 10 = 1, 100 = 2, 1000 = 3)\n" +
          "other = normal bitwise encoding, see above\n" +
          "In cases 3 and 4, if an odd number of bits send msb as itself"
        },
        { "1", "3",
          "For first Tx use 2nd set of device and command bytes following first in buffer, " +
          "created by protocol executor (repeat Tx always uses first set)?\n" +
          "0 = no\n" +
          "1 = yes"
        },
        { "1", "4",
          "After all repeats, send one Tx of 2nd set of device and command bytes?\n" +
          "0 = no\n" +
          "1 = yes"
        },
        {
          "1", "5",
          "Use alt leadout from PD13/PD14 instead of PD0A/PD0B?\n" +
          "0 = no\n" +
          "1 = yes"
        },
        {
          "1", "6",
          "Send 0-bursts with alt frequency and duty cycle from PD13/PD14?\n" +
          "0 = no\n" +
          "1 = yes"
        },
        { "1", "7",
          "Is PF4 present?\n" + 
          "0 = no\n" +
          "1 = yes"
        }
      },
      {
        { "1", "0",
          "Immediate repeat for held keypress, skipping minimum hold time?\n" +
          "0 = no\n" +
          "1 = yes"
        },
        { "1", "1",
          "Immediate action on change of keypress, skipping full keypad scan?\n" +
          "0 = no\n" +
          "1 = yes"
        },
        { "2", "2-3",
          "Number of stop bits (0-bits) for asynchronous encoding (see bit 6)\n" +
          "0 = No stop bits\n" +
          "1 = One stop bit\n" +
          "other = Two stop bits"
        },
        { "2", "4-5",
          "Parity bit for asynchronous encoding (see bit 6)\n" +
          "0 = no parity bit\n" +
          "1 = one parity bit, odd parity\n" +
          "other = one parity bit, even parity"
        },
        { "1", "6",
          "When bits 0-2 of PF3 are other than 3 or 4, use asynchronous encoding: " +
          "prepend data bits with one start bit (1-bit), optionally append parity and " +
          "stop bits according to bits 2-5 above?\n" +
          "0 = no\n" +
          "1 = yes"
        },
        { "1", "7",
          "Must be 0\n" +
          "0 = required\n" +
          "1 = error"
        },
      }      
   };
}
