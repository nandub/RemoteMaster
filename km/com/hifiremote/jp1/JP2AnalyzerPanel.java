package com.hifiremote.jp1;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.hifiremote.jp1.JP2Analyzer;

public class JP2AnalyzerPanel extends JPanel
{
  public JP2AnalyzerPanel()
  {
    BoxLayout bl = new BoxLayout( this, BoxLayout.Y_AXIS );
    setLayout( bl );
    setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    
    analysisText = new JTextArea( 10, 40 );
    analysisText.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    analysisText.setEditable( false );
    analysisText.setLineWrap( true );
    analysisText.setWrapStyleWord( true );
    analysisScroll = new JScrollPane( analysisText );
    add( analysisScroll );
  }
  
  public void set( Processor proc, Hex hex )
  {
    JP2Analyzer jp2 = new JP2Analyzer();
    analysisText.setText( jp2.analyze( proc, hex ) );
    javax.swing.SwingUtilities.invokeLater( new Runnable()
    {
      public void run()
      {
        analysisScroll.getVerticalScrollBar().setValue( 0 );
      }
    } );
  }
  
  public void setDescription()
  {
    analysisText.setText( pfDescription );
    javax.swing.SwingUtilities.invokeLater( new Runnable()
    {
      public void run()
      {
        analysisScroll.getVerticalScrollBar().setValue( 0 );
      }
    } );
  }
  
  private JTextArea analysisText = new JTextArea();
  private JScrollPane analysisScroll = null;
  private String pfDescription = 
      "Interpretation of PF bytes for JP2 protocols:\n" +
          "\n" +
          "PF0  Bit 7   1 if code block is present, 0 if absent \n" +
          "     Bit 6   1 if uses lead-out, 0 if no lead-out\n" +
          "     Bit 5   1 if lead-out is total time, 0 if it is gap time \n" +
          "     Bits 0-3  number of format bytes after PF0\n" +
          "\n" +
          "\n" +
          "PF1  Bit 7   1 = use normal lead-in on first frame, lead-in sent on following frames provided bit 6 set, but it is normal lead-out with OFF time halved and no data is sent); 0=bit 6 policy applies to all frames, with same lead-in timing on all frames\n" +
          "     Bit 6   1 = use lead-in (normal/alternate according to bit 0), 0=omit lead-in\n" +
          "     Bit 5,4   behaviour on outer repeat (ie after PF3:0-5 repeats ended, from held keypress or outer repeat counter); determines response from function 55 after PF3 repeats and when function 54 returns true:\n" +
          "               0 = no repeat (PF3 repeat count is exact number)\n" +
          "               1 = repeat on all buttons\n" +
          "               2 = repeat if active button is in repeating group (Vol+/-, Ch+/-, FF, Rew)\n" +
          "               3 = send One-ON in place of first repeat, nothing on later repeats\n" +
          "     Bit 3   follow data with an end-frame burst (see PF3.6)? 1=yes, 0=no\n" +
          "     Bit 2   One-ON precedes lead-out?  1=yes, 0=no\n" +
          "     Bit 1   1=alternate lead-out, 0=normal lead-out\n" +
          "     Bit 0   1=alternate leadin, 0=normal leadin\n" +
          "\n" +
          "PF2  Bits 0-4 = Number of TX bytes in block (header byte of TX data)\n" +
          "     Bits 5-7   Determines behaviour after function 55 returns false:\n" +
          "                0: terminate\n" +
          "                1: execute next signal block \n" +
          "                2: if function 54 returns true then execute next signal block \n" +
          "                   else terminate \n" +
          "                3: if function 54 returns true then re-execute protocol block (which will pick the 1st signal block)\n" +
          "                   else terminate \n" +
          "                4: if function 54 returns true then re-execute protocol block\n" +
          "                   else execute next signal block \n" +
          "                5: re-execute protocol block (which will pick the 1st signal block) \n" +
          "                6: re-execute current signal block \n" +
          "                7: same as 0\n" +
          "function 55 tests inner repeat required (PF3:0-5 times, + function 54 in accordance with PF1:4-5)\n" +
          "function 54 tests outer repeat required (keypress held, real or simulated, and PF0:6 = 1)\n" +
          "\n" +
          "PF3  Bits 0-5 Repeat count (absent=0, i.e. just one frame, no repeats)\n" +
          "     Bit 6   1=Use alternate lead-in as end-frame burst, 0=Use normal lead-in as end-frame burst\n" +
          "     Bit 7   1=disable IR when repeats from held keypress end\n" +
          "\n" +
          "PF4  Bit 7   1 = send mid-frame burst after N data bits\n" +
          "     Bits 0-6  the number N of data bits\n" +
          "\n" +
          "PF5  Bits 4-7 select either the current or next signal block according to the button pressed, the current one when condition below is true, next otherwise; there may be a sequence of signal blocks with these bits not all zero, terminating in one with all zero.\n" +
          "     Bit 7 = 1 and button is in repeating group (Vol+/-, Ch+/-, FF/Rew, also SkipFwd/Back on 6440)\n" +
          "     Bit 6 = 1 and button is in volume group (Vol+/-, Mute but just Vol+/- on 6440)\n" +
          "     Bit 5 = 1 and button is Master Power (or just Power?)\n" +
          "     Bit 4 = 1 and button is Record\n" +
          "\n" +
          "PF6  Bit 7   when set, process code block of signal block before, rather than after, applying signal spec to fixed and variable bytes\n";
}
