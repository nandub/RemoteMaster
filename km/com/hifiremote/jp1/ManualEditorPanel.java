package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import info.clearthought.layout.TableLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.hifiremote.jp1.ProtocolDataPanel.Mode;

public class ManualEditorPanel extends JPanel implements ActionListener, ChangeListener
{
  public ManualEditorPanel( Component owner )
  {
    double b = 10; // space around border/columns
    double i = 5; // space between rows
    double f = TableLayout.FILL;
    double p = TableLayout.PREFERRED;
    double size[][] =
    {
        {
            b, p, b, f, b, p, b, 100, b
        }, // cols
        {
            b, p, i, p, b
        }
    // rows
    };
    
    this.owner = owner;
    setLayout( new BorderLayout() );
    manualSettingsPanel = new ManualSettingsPanel();
    tabbedPane = new JTabbedPane();
    add( tabbedPane, BorderLayout.CENTER );
    JPanel tablePanel = manualSettingsPanel.getTablePanel();
    tabbedPane.addTab( "Assembler", manualSettingsPanel );
    tabbedPane.addTab( "Hex Code", tablePanel );
    tabbedPane.addTab( "Disassembler", null );
    tabbedPane.addChangeListener( this );
    
    devicePanel = manualSettingsPanel.getDevicePanel();
    devicePanel.setOwner( owner );

    JTextArea deviceText = manualSettingsPanel.getDeviceText();
    ScrollablePanel iniBodyPanel = new ScrollablePanel( new BorderLayout() );
    iniBodyPanel.setScrollableWidth( ScrollablePanel.ScrollableSizeHint.FIT );
    iniBodyPanel.add( deviceText, BorderLayout.CENTER );
    JScrollPane scrollPane = new JScrollPane( iniBodyPanel );
    scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
    scrollPane.setBorder( BorderFactory.createCompoundBorder
        ( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ),
        BorderFactory.createTitledBorder( "Data translators:" ) ) );
    JSplitPane outerPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, devicePanel, scrollPane );
    outerPane.setResizeWeight( 0.5 );
    tabbedPane.addTab( "Device Data", outerPane );
    
    procBox = manualSettingsPanel.getProcBox();
    
    
    TableLayout tl = new TableLayout( size );
    JPanel identPanel = new JPanel( tl );
    identPanel.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
    manualSettingsPanel.getProtocolName().setColumns( 50 );
    JLabel label = new JLabel( "Name:", SwingConstants.RIGHT );
    identPanel.add( label, "1, 1" );
    identPanel.add( manualSettingsPanel.getProtocolName(), "3, 1" );
    label = new JLabel( "VariantName:", SwingConstants.RIGHT );
    identPanel.add( label, "1, 3" );
    identPanel.add( manualSettingsPanel.getVariantName(), "3, 3" );
    label = new JLabel( "Protocol ID:", SwingConstants.RIGHT );
    identPanel.add( label, "5, 1" );
    identPanel.add( manualSettingsPanel.getPid(), "7, 1" );
    label = new JLabel( "Processor:", SwingConstants.RIGHT );
    identPanel.add( label, "5, 3" );
    identPanel.add( procBox, "7, 3" );    
    add( identPanel, BorderLayout.PAGE_START );
    
    translationButton = new JButton( "Create translators" );
    translationButton.setToolTipText( "Set Data translators panel to represent the above parameters" );
    translationButton.addActionListener( this );
    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.CENTER ) );
    buttonPanel.add( translationButton );
    devicePanel.add( buttonPanel, "1, 9, 3, 9" );
  }
  
  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    JTextArea deviceText = manualSettingsPanel.getDeviceText();
    if ( source == translationButton )
    {
      if ( !deviceText.getText().trim().isEmpty() )
      {
        String title = "Data translation";
        String message = "<html>This will overwrite the text that is now in the "
                + "Data translators panel.<br>Do you wish to proceed?</html>";          
        if ( JOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_OPTION, 
            JOptionPane.WARNING_MESSAGE ) != JOptionPane.YES_OPTION )
        {
          return;
        };
      }
      deviceText.setText( manualSettingsPanel.getProtocolText( true, true ) );
    }
    
  }
  
  @Override
  public void stateChanged( ChangeEvent e )
  {
    int indexAsm = tabbedPane.indexOfTab( "Assembler" );
    int indexDisasm = tabbedPane.indexOfTab( "Disassembler" );
    int indexOutput = tabbedPane.indexOfTab( "Output Data" );
    int index = tabbedPane.getSelectedIndex();
    if ( index == indexAsm )
    {
      tabbedPane.setComponentAt( indexDisasm, null );
      tabbedPane.setComponentAt( indexAsm, manualSettingsPanel );
      manualSettingsPanel.setMode( Mode.ASM );
      manualSettingsPanel.getAssemblerPanel().optionsPanel.setVisible( false );
      manualSettingsPanel.setVisibility();
    }
    else if ( index == indexDisasm )
    {
      tabbedPane.setComponentAt( indexAsm, null );
      tabbedPane.setComponentAt( indexDisasm, manualSettingsPanel );
      manualSettingsPanel.setMode( Mode.DISASM );
      manualSettingsPanel.getAssemblerPanel().optionsPanel.setVisible( true );
      manualSettingsPanel.setVisibility();
    }
    else if ( index == indexOutput )
    {
      manualSettingsPanel.getOutputPanel().updatePBOutput();
    }
  }
  
  public JTabbedPane getTabbedPane()
  {
    return tabbedPane;
  }

  public ManualDevicePanel getDevicePanel()
  {
    return devicePanel;
  }

  public ManualSettingsPanel getManualSettingsPanel()
  {
    return manualSettingsPanel;
  }

  private ManualSettingsPanel manualSettingsPanel = null;
  private JTabbedPane tabbedPane = null;
  private ManualDevicePanel devicePanel = null;
  private JButton translationButton = null;
  private JComboBox< Processor > procBox = new JComboBox< Processor >();
  private Component owner = null;
  
}
