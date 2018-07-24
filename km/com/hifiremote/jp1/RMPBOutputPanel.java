package com.hifiremote.jp1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class RMPBOutputPanel extends JPanel
{
  public RMPBOutputPanel( ManualSettingsPanel msp )
  {
    this.msp = msp;
    BoxLayout bl = new BoxLayout( this, BoxLayout.Y_AXIS );
    setLayout( bl );
    setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

    Box box = Box.createHorizontalBox();
    box.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ) );
    add( box );

    JLabel rmpbLabel = new JLabel( "Output formatted as protocols.ini entry" );
    rmpbLabel.setAlignmentY( 1f );
    box.add( rmpbLabel );
    
    rmpbText = new JTextArea( 10, 40 );
    rmpbText.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    rmpbText.setEditable( false );
    rmpbText.setLineWrap( true );
    rmpbText.setWrapStyleWord( true );
    rmpbText.setBackground( rmpbLabel.getBackground() );
    JScrollPane scroll = new JScrollPane( rmpbText );
    add( scroll );
    
    add( Box.createVerticalStrut( 20 ) );
    
    box = Box.createHorizontalBox();
    box.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ) );
    add( box );

    JLabel pbLabel = new JLabel( "Output formatted in PB style" );
    pbLabel.setAlignmentY( 1f );
    box.add( pbLabel );
    
    pbText = new JTextArea( 10, 40 );
    pbText.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    pbText.setEditable( false );
    pbText.setLineWrap( true );
    pbText.setWrapStyleWord( true );
    pbText.setBackground( rmpbLabel.getBackground() );
    scroll = new JScrollPane( pbText );
    add( scroll );
  }
  
  public void updatePBOutput()
  {
    int firstRow = -1;
    String text = null;
    StringBuilder sbPB = new StringBuilder();
    StringBuilder sbRMPB = new StringBuilder();
    String ls = System.getProperty( "line.separator" );
    sbRMPB.append( "[" + msp.getProtocolName().getText().trim() + "]" + ls );
    sbRMPB.append( "PID=" + msp.getPid().getText().trim() + ls );
    String variant = msp.getVariantName().getText().trim();
    if ( !variant.equals( "" ) )
    {
      sbRMPB.append( "VariantName=" + variant + ls );
    }
    String intro = msp.getDeviceText().getText().replaceFirst("\\s++$", "").replaceAll( "\\r", "" ).replaceAll( "\\n", ls );
    sbRMPB.append( intro + ls );

    for ( int row = 0; row < msp.getTablePanel().getProcs().length; row++ )
    {
      Processor proc = msp.getTablePanel().getProcs()[ row ];
      if ( ( text = getPBOutput( proc ) ) == null )
      {
        continue;
      }
      if ( firstRow < 0 )
      {
        firstRow = row;
      }
      else
      {
        sbPB.append( ls + ls );
      }
      sbPB.append( text );
      sbRMPB.append( "Code." + proc.getEquivalentName() + "=" );
      sbRMPB.append( msp.getProtocol().getCode( proc ).toRawString() + ls );
    }
    pbText.setText( sbPB.toString() );
    rmpbText.setText( sbRMPB.toString() ); 
  }
  
  public String getPBOutput( Processor proc )
  {
    String procName = proc.getFullName();
    Hex code = msp.getProtocol().getCode( proc );
    if ( code == null || code.length() == 0 )
    {
      return null;
    }
    
    // Use same name-forms as PB
    if ( proc instanceof S3C80Processor )
    {
      boolean isNew = ( ( S3C80Processor )proc ).testCode( code ) == S3C80Processor.CodeType.NEW;
      procName = isNew ? "S3C8+" : "S3C8";
    }
    else if ( procName.equals( "740" ) )
    {
      procName = "P8/740";
    }
    
    String ls = System.getProperty( "line.separator" );
    StringBuilder buff = new StringBuilder( 300 );
    buff.append( "Upgrade protocol 0 = " );
    buff.append( msp.getPid().getText().trim() );
    buff.append( " (" );
    buff.append( procName );
    buff.append( ")" );
    buff.append( ' ' );
    buff.append( msp.getProtocolName().getText().trim() );
    String variant = msp.getVariantName().getText().trim();
    
    if ( !variant.equals( "" ) )
    {
      buff.append( ':' + variant );
    }
    buff.append( " (RMPB " );
    buff.append( RemoteMaster.version + " build " + RemoteMaster.getBuild() );
    buff.append( ')' );

    try
    {
      BufferedReader rdr = new BufferedReader( new StringReader( code.toString( 16 ) ) );
      String line = null;
      while ( ( line = rdr.readLine() ) != null )
      {
        buff.append( ls );
        buff.append( line );
      }
    }
    catch ( IOException ioe )
    {
      ioe.printStackTrace( System.err );
    }
    buff.append( ls + "End" );
    return buff.toString();
  }
  
  public JTextArea getPbText()
  {
    return pbText;
  }

  public JTextArea getRmpbText()
  {
    return rmpbText;
  }
  
  public void reset()
  {
    pbText.setText( "" );
    rmpbText.setText( "" );
  }

  private ManualSettingsPanel msp = null;
  private JTextArea rmpbText = new JTextArea();
  private JTextArea pbText = new JTextArea();
}
