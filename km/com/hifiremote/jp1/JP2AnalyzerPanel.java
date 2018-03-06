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
    JP2Analyzer maxq = new JP2Analyzer();
    analysisText.setText( maxq.analyze( proc, hex ) );
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
}
