package com.hifiremote.jp1.assembler;

public class JP2CommonData
{
  public static final String[][] AddressModes = {
    { "Dir3", "B3Z7", "$%02X, $%02X, $%02X" },
    { "Imm3", "B3Z3", "$%02X, $%02X, #$%02X" },
    { "Ind2", "B3Z3", "$%02X, ($%02X)" },
    { "Dir2", "B3Z3", "$%02X, $%02X" },
    { "Imm2", "B3Z1", "$%02X, #$%3$02X" },
    { "Immw", "B3Z1", "$%02X, #$%3$02X%2$02X" },
    { "Ind1", "B3Z3", "($%02X), $%02X" },
    { "Indx",  "B3Z7", "$%02X, $%02X[$%02X]" },
    { "BrNZ", "B3Z2R1", "NZ, $%02X, $%02X, #$%02X" },
    { "BrZ",  "B3Z2R1", "Z, $%02X, $%02X, #$%02X" },
    { "Rel1", "B3R1",   "$%02X" },
    { "Rel2", "B3Z2R1", "$%02X, $%02X" },
    { "Fun1", "B3A1", "$%02X" },
    { "Fun1B", "B3A1", "$%02X, #$%3$02X" },
    { "Fun1Z", "B3A1Z4", "$%02X, #$%3$02X" },
    { "Fun1W", "B3A1M1", "$%02X, #$%02X%02X" },
    { "Immd", "B3", "#$%02X, #$%02X" },
    { "BrT", "B3R1A2", "T, $%02X, $%02X" },
    { "BrF", "B3R1A2", "F, $%02X, $%02X" },
    { "Imm1", "B3", "#$%3$02X" },
    { "Immc", "B3", "#$%2$02X%1$02X, #$%3$02X" },
    { "Imm11", "B3Z1", "$%02X, #$%02X, #$%02X" },
    { "Imm0w", "B3", "#$%3$02X%2$02X" },
    { "Ind1n", "B3Z3", "($%02X), $%02X, #$%02X" },
    { "Imm23", "B3", "#$%2$02X, #$%3$02X" },
    { "Nil", "B3", "" }
    
  };
  
  public static final String[][] absLabels = {
    { "1-burst", "00", "Send Data Signal A" },
    { "0-burst", "01", "Send Data Signal B" },
    { "NormLeadIn", "02", "Send normal lead-in", "3" },
    { "AltLeadIn", "03", "Send alternate lead-in", "5" },
    { "MidFrame", "04", "Send mid-frame burst", "6" },
    { "NormLeadOut", "05", "Send normal lead-out", "2" },
    { "AltLeadOut", "06", "Send alternate lead-out", "4" },
    { "SendMARK", "07" },       // Sends MARK for duration at pointer ($AB:$AA)
    { "SendSPACE", "08" },      // Sends SPACE for duration at pointer ($AB:$AA)
    { "SendBURST", "09" },      // Sends MARK/SPACE burst pair for durations at pointer ($AB:$AA)
    { "WaitTXend", "0A" },      // Waits for current IR transmission to end
    { "GetCarrier10ms", "0B" }, // $A9:$A8 <- number of carrier cycles in 10ms
    { "GetLeadout", "0C" },     // $A7:..:$A4 <- 32-bit lead-out duration with current PF settings
    { "WaitRestartTotLeadOut", "0D", "If lead-out is total-time, wait for timer to stop, then restart it" },
    { "SendFrameStart", "0E", "Send lead-in with current settings" },
    { "SendFrameEnd", "0F", "Send end-frame burst, 1-ON and lead-out with current settings" },
    { "SendFullFrame", "10", "Send one frame from lead-in to lead-out according to settings and data bytes" }, 
    { "SendFrameData", "11", "Send data bytes with current settings, including mid-frame burst if set" }, 
    { "ResetIRControl", "12" }, // Resets IRCN, IRCNB ready for next IR transmission
    { "WaitLeadOutTimer", "13" }, // Wait for total-time lead-out timer to finish
    { "InitTimingItems", "14" },// Initialize timing item sizes to default values
    { "ClearPFregister", "15" },// Clears $94-$A3, PF byte register, to 00's
    { "ProcessExecHdrTimDir", "16" }, // Process header, timing and directive blocks of executor
    { "ProcessExecHdrTim", "17" }, // Process header and timing block of executor
    { "ProcessProtBlk", "18" }, // Process protocol block
    { "ProcessSigBlk", "19" },  // Process signal block
    { "SetRptFromPF3", "1A" },  // $B8, inner repeat counter, gets bits 0-5 of PF3
    { "ProcessSigSpec", "1B" }, // Process signal spec
    { "DoToggle", "1C", "Apply toggle of protocol block" },
    { "WaitTXendDisableIR", "1D", "Wait for IR transmission to end then disable IR module" },
    { "RunNativeCodeA6", "1E" }, // Run native code block with pointer at $A6
    { "RunPseudocodeA6", "1F" }, // Run pseudocode block with pointer at $A6
    { "RunProtPseudocode", "20" }, // Run pseudocode of active protocol block
    { "RunSigPseudocode", "21" },  // Run pseudocode of active signal block
    
    { "TestRecordKey", "50", "Test if Record key" },  // Test if current key is Record
    { "TestPowerKey", "51", "Test if Power key" },   // Test if current key is Power
    { "TestRepeatingKey", "52", "Test if repeating key" },  // Test if current key is a repeating one, Vol+/-, Ch+/-, FF/Rew, SkipFwd/Back
    { "TestVolKey", "53", "Test if volume key" },     // Test if current key is Vol+/-
    { "TestKeyHeld", "54" },    // Test if current key is still held (including simulated holds in macros)
    { "TestRepeatReqd", "55" },  // Test if repeat required according to PF settings
    { "TestLeadOutRunning", "56" },  // Test if total time lead-out timer still running
    { "TestTimeExceeded", "57" },  // Test if max repeat timer (timer 2) expired
    { "TestNextByte", "58" },    // Test next executor byte (True if 1, False if 0)
    { "TestKeyAwaiting", "59" }, // Test if there is a key awaiting processing
   
    { "TimingItem", "70" },     // Sends timing item with index op3
    { "TimingItemAddr", "71" }, // $AB:$AA <- word address of timing item with index op3
    { "Branch", "72" },         // Same as BRA #op3
    { "XMPchecksum", "73", "Set XMP checksum in 4 bytes at specified start" },
    { "SetIRCA", "80" }         // IRCA <- op2:op3
  };

}
