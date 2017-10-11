package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

// TODO: Auto-generated Javadoc
/**
 * The Class RawDataPanel.
 */
public class RawDataPanel extends RMPanel
{

  /**
   * Instantiates a new raw data panel.
   */
  public RawDataPanel()
  {
    model = new RawDataTableModel();
    JP1Table table = new JP1Table( model )
    {
      @Override
      public String getToolTipText( MouseEvent e ) 
      {
        String tip = null;
        java.awt.Point p = e.getPoint();
        int row = rowAtPoint( p );
        int col = columnAtPoint( p );
        int offset = 16 * row + col - 1;
        boolean showTip = false;
        if ( col != 0 && settingAddresses.containsKey( offset ) ) 
        { 
          tip = "Highlighted bits: ";
          int end = highlight.length - 1;
          for ( int i = 0; i < 8; i++ )
          {
            if ( !highlight[ end - 8 * settingAddresses.get( offset ) - i ].equals( Color.WHITE ) )
            {
              tip += i;
              showTip = true;
            }
          }
        } 
        return showTip ? tip : null;
      }
    };
    table.initColumns( model );
    table.setGridColor( Color.lightGray );
    table.getTableHeader().setResizingAllowed( false );
    table.setDefaultRenderer( UnsignedByte.class, byteRenderer );
    JScrollPane scrollPane = new JScrollPane( table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
    Dimension d = table.getPreferredScrollableViewportSize();
    d.width = table.getPreferredSize().width;
    table.setPreferredScrollableViewportSize( d );
    add( scrollPane, BorderLayout.WEST );
    
    JPanel panel = new JPanel( new BorderLayout() );
    panel.setBorder( BorderFactory.createEmptyBorder( 20, 10, 5, 5 ) );
    infoBox = Box.createVerticalBox();
    infoBox.setAlignmentX( LEFT_ALIGNMENT );
    infoBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 15, 5 ) );
    panel.add( infoBox, BorderLayout.PAGE_START );
    add( panel, BorderLayout.CENTER );
    JPanel p = new JPanel( new BorderLayout() );
    panel.add( p, BorderLayout.LINE_START );

    infoBox.add( signatureLabel );
    infoBox.add( strut1 );
    infoBox.add( regionLabel );
    infoBox.add( strut2 );
    infoBox.add( languageLabel );
    infoBox.add( Box.createVerticalStrut( 5 ) );
    infoBox.add( processorLabel );
    infoBox.add( Box.createVerticalStrut( 5 ) );
    infoBox.add( interfaceLabel );
    infoBox.add( Box.createVerticalStrut( 5 ) );
    infoBox.add( versionLabel1 );
    infoBox.add( Box.createVerticalStrut( 5 ) );
    infoBox.add( versionLabel2 );

    Font boldFont = ( new JLabel() ).getFont().deriveFont( Font.BOLD );
    infoLabel1.setFont( boldFont );
    infoLabel2.setFont( boldFont );
    infoLabel2.setForeground( Color.BLUE );
    
    ActionListener al = new ActionListener()
    {
      @Override
      public void actionPerformed( ActionEvent e )
      {
        set( remoteConfig );
      }
    };
    
    normalBtn = new JRadioButton( "Normal data" );
    normalBtn.setSelected( true );
    normalBtn.addActionListener( al );
    baseBtn = new JRadioButton( "Baseline data" );
    baseBtn.setSelected( false );
    baseBtn.addActionListener( al );
    ButtonGroup bg = new ButtonGroup();
    bg.add( normalBtn );
    bg.add( baseBtn );
    
    choicePanel = new JPanel( new GridLayout( 2, 1, 0, 0 ) );
    choicePanel.setBorder( BorderFactory.createTitledBorder( " Display " ) );
    choicePanel.add( normalBtn );
    choicePanel.add( baseBtn );
    choicePanel.setVisible( false );
    p.add(  choicePanel, BorderLayout.PAGE_START );
  }

  /**
   * Sets the.
   * 
   * @param remoteConfig
   *          the remote config
   */
  @Override
  public void set( RemoteConfiguration remoteConfig )
  {
    this.remoteConfig = remoteConfig;
    if ( remoteConfig != null )
    {
      Remote remote = remoteConfig.getRemote();
      RemoteMaster rm = ( RemoteMaster )SwingUtilities.getAncestorOfClass( RemoteMaster.class, this );
      short[] dataToShow = rm.useSavedData() ? remoteConfig.getSavedData() : 
        baseBtn.isSelected() ? remoteConfig.getBaselineData() : remoteConfig.getData();
      int dataEnd = remoteConfig.getDataEnd( dataToShow );
      if ( dataEnd != dataToShow.length )
      {
        dataToShow = Arrays.copyOf( dataToShow, remoteConfig.getDataEnd( dataToShow ) );
      }
      model.set( dataToShow, remote.getBaseAddress() );
      byteRenderer.setRemoteConfig( remoteConfig );
      highlight = remoteConfig.getHighlight();
      settingAddresses = remoteConfig.hasSegments() ? remoteConfig.getSettingMap() : remote.getSettingAddresses();
      String sig = remoteConfig.getSigString();
      if ( sig == null )
      {
        sig = remote.getSignature();
      }
      signatureLabel.setText( "Signature:  " + sig );
      String region = remoteConfig.getRegionDisplayName();
      if ( region != null )
      {
        strut1.setVisible( true );
        regionLabel.setVisible( true );
        regionLabel.setText( "Region:  " + region );
      }
      else
      {
        strut1.setVisible( false );
        regionLabel.setVisible( false );
      }
      String language = remoteConfig.getLanguageDisplayName();
      if ( language != null )
      {
        strut2.setVisible( true );
        languageLabel.setVisible( true );
        languageLabel.setText( "Language:  " + language );
      }
      else
      {
        strut2.setVisible( false );
        languageLabel.setVisible( false );
      }
      processorLabel.setText( "Processor:  " + remote.getProcessorDescription() );
      interfaceLabel.setText( "Interface:  " + remote.getInterfaceType() );
      versionLabel1.setText( "" );
      versionLabel2.setText( "" );
      int n = 1;
      if ( remote.getExtenderVersionParm() != null )
      {
        versionLabel1.setText( "Extender version:  " + 
            remote.getExtenderVersionParm().getExtenderVersion( remoteConfig ) );
        n++;
      }
      String text = remoteConfig.getEepromFormatVersion();
      if ( text != null && !text.equals( sig ) )
      {
        JLabel lbl = ( n == 1 ) ? versionLabel1 : versionLabel2;
        lbl.setText( "E2 format version: " + text );
      }
      if ( remoteConfig.getOwner().useSavedData() )
      {
        if ( !Arrays.asList( infoBox.getComponents() ).contains( infoLabel1 ) )
        {
          infoBox.add( Box.createVerticalStrut( 5 ), 0 );
          infoBox.add( infoLabel2, 0 );
          infoBox.add( infoLabel1, 0 );
          validate();
        }
      }
      else
      {
        if ( Arrays.asList( infoBox.getComponents() ).contains( infoLabel1 ) )
        {
          infoBox.remove( 0 );
          infoBox.remove( 0 );
          infoBox.remove( 0 );
          validate();
        }
      }
      choicePanel.setVisible( !rm.useSavedData() && remoteConfig.getBaselineData() != null );
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.RMPanel#addRMPropertyChangeListener(java.beans.PropertyChangeListener)
   */
  @Override
  public void addRMPropertyChangeListener( PropertyChangeListener l )
  {
    if ( model != null && l != null )
    {
      model.addPropertyChangeListener( l );
    }
  }

  RemoteConfiguration remoteConfig = null;
  RawDataTableModel model = null;
  UnsignedByteRenderer byteRenderer = new UnsignedByteRenderer();
  
  JLabel signatureLabel = new JLabel();
  JLabel regionLabel = new JLabel();
  JLabel languageLabel = new JLabel();
  JLabel processorLabel = new JLabel();  
  JLabel interfaceLabel = new JLabel();  
  JLabel versionLabel1 = new JLabel( "" );
  JLabel versionLabel2 = new JLabel( "" );
  JLabel infoLabel1 = new JLabel( "Values in black: RMIR data displayed" );
  JLabel infoLabel2 = new JLabel( "Values in blue: Original data displayed" );
  Component strut1 = Box.createVerticalStrut( 5 );
  Component strut2 = Box.createVerticalStrut( 5 );
  
  Box infoBox = null;
  JPanel choicePanel = null;
  JRadioButton normalBtn = null;
  JRadioButton baseBtn = null;
  
  private Color[] highlight = null;
  private HashMap< Integer, Integer >settingAddresses = null;
  
}
