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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.hifiremote.jp1.ProtocolDataPanel.Mode;

public class ManualEditorPanel extends JPanel implements ActionListener, ChangeListener, DocumentListener
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
    outerPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, devicePanel, scrollPane );
    outerPane.setResizeWeight( 0.5 );
    tabbedPane.addTab( "Device Data", outerPane );
    
    procBox = manualSettingsPanel.getProcBox();
    procBox.addActionListener( this );
    
    TableLayout tl = new TableLayout( size );
    JPanel identPanel = new JPanel( tl );
    identPanel.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
    manualSettingsPanel.getProtocolName().setColumns( 50 );
    manualSettingsPanel.getProtocolName().getDocument().addDocumentListener( this );
    manualSettingsPanel.getVariantName().getDocument().addDocumentListener( this );
    manualSettingsPanel.getPid().getDocument().addDocumentListener( this );
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
    devicePanel.add( buttonPanel, "1, 11, 3, 11" );
  }
  
  public void setRemote( Remote remote )
  {
    manualSettingsPanel.getAssemblerModel().setRemote( remote );
  }
  
  public JButton getTranslationButton()
  {
    return translationButton;
  }

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    JTextArea deviceText = manualSettingsPanel.getDeviceText();
    if ( source == translationButton )
    {
      devicePanel.stopEditing();
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
    else if ( source == procBox )
    {
      resetTabbedPanes();
    }
    
  }
  
  @Override
  public void stateChanged( ChangeEvent e )
  {
    int indexAsm = tabbedPane.indexOfTab( "Assembler" );
    int indexDisasm = tabbedPane.indexOfTab( "Disassembler" );
    int indexOutput = tabbedPane.indexOfTab( "Output Data" );
    int indexAnalyzer = tabbedPane.indexOfTab( "Analyzer" );
    int index = tabbedPane.getSelectedIndex();
    if ( index < 0 )
    {
      return;
    }
    if ( index == indexAsm && indexDisasm >= 0 )
    {
      tabbedPane.setComponentAt( indexDisasm, null );
      tabbedPane.setComponentAt( indexAsm, manualSettingsPanel );
      manualSettingsPanel.setMode( Mode.ASM );
      manualSettingsPanel.getAssemblerPanel().optionsPanel.setVisible( false );
      manualSettingsPanel.setVisibility();
    }
    else if ( index == indexDisasm && indexAsm >= 0 )
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
    else if ( index == indexAnalyzer )
    {
      Processor proc = ( Processor )procBox.getSelectedItem();
      Protocol prot = manualSettingsPanel.getProtocol();
      if ( proc != null && prot != null )
      {
        manualSettingsPanel.getAnalyzerPanel().set( proc, prot.getCode( proc ) );
      }
    }
  }
  
  private int getTabIndex( Component c )
  {
    for ( int i = 0; i < tabbedPane.getTabCount(); i++ )
    {
      if ( tabbedPane.getComponentAt( i ).equals( c ) )
      {
        return i;
      }
    }
    return -1;
  }
  
  private int checkTabbedPane( String name, Component c, boolean test, int index )
  {
    return checkTabbedPane( name, c, test, index, null, true );
  }
  
  private int checkTabbedPane( String name, Component c, boolean test, int index, String tooltip, boolean enabled )
  {
    int tabIndex = tabbedPane.indexOfTab( name );
    if ( test )
    {
      if ( tabIndex < 0 )
      {
        tabbedPane.insertTab( name, null, c, tooltip, index );
        tabbedPane.setEnabledAt( index, enabled );
      }
      index++;
    }
    else if ( tabIndex >= 0 )
    {
      tabbedPane.remove( index );
    }
    return index;
  }
  
  private void resetTabbedPanes()
  {
    Processor p = ( Processor )procBox.getSelectedItem();
    boolean jp2 = p.getDataStyle() < 0;
    if ( inJP2mode == jp2 )
    {
      return;
    }
    inJP2mode = jp2;
    int index = checkTabbedPane( "Assembler", manualSettingsPanel, !jp2, 0 );
    index = checkTabbedPane( "Hex Code", manualSettingsPanel.getTablePanel(), true, index );
    index = checkTabbedPane( "Disassembler", null, !jp2, index );
    index = checkTabbedPane( "Analyzer", manualSettingsPanel.getAnalyzerPanel(), jp2, index );
    index = checkTabbedPane( "PF Description", manualSettingsPanel.getPfDescriptionPanel(), jp2, index );
    index = checkTabbedPane( "Device Data", outerPane, true, index );
    tabbedPane.setSelectedIndex( 0 );
  }
  
  public void documentChanged( DocumentEvent e )
  {
    int indexOutput = tabbedPane.indexOfTab( "Output Data" );
    int index = tabbedPane.getSelectedIndex();
    if ( index < 0 )
    {
      return;
    }
    if ( index == indexOutput )
    {
      manualSettingsPanel.getOutputPanel().updatePBOutput();
    }
  }
  
  public void changedUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  public void insertUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  public void removeUpdate( DocumentEvent e )
  {
    documentChanged( e );
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
  private JSplitPane outerPane = null;
  private JTabbedPane tabbedPane = null;
  private ManualDevicePanel devicePanel = null;
  private JButton translationButton = null;
  private JComboBox< Processor > procBox = new JComboBox< Processor >();
  private Component owner = null;
  private boolean inJP2mode = false;

}
