package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.hifiremote.jp1.ProtocolDataPanel.DisplayArea;
import com.hifiremote.jp1.ProtocolDataPanel.Mode;

// TODO: Auto-generated Javadoc
/**
 * The Class ManualSettingsDialog.
 */
public class ManualSettingsDialog extends JDialog implements ActionListener
{
  /**
   * Instantiates a new manual settings dialog.
   * 
   * @param owner
   *          the owner
   * @param protocol
   *          the protocol
   */
  public ManualSettingsDialog( JDialog owner, ManualProtocol protocol )
  {
    super( owner, "Protocol Editor", true );
    createGui( owner, protocol );
  }

  /**
   * Instantiates a new manual settings dialog.
   * 
   * @param owner
   *          the owner
   * @param protocol
   *          the protocol
   */
  public ManualSettingsDialog( JFrame owner, ManualProtocol protocol )
  {
    super( owner, "Protocol Editor", true );
    createGui( owner, protocol );
  }

  /**
   * Creates the gui.
   * 
   * @param owner
   *          the owner
   * @param protocol
   *          the protocol
   */
  private void createGui( Component owner, ManualProtocol protocol )
  {
    addWindowListener( new WindowAdapter()
    {
      @Override
      public void windowClosing( WindowEvent event )
      {
        resetProcBox();
        dispose();
      }
    } );
    setLocationRelativeTo( null );
    // Was relative to owner, but this could cause it to run off top of screen.
    // Relative to null should center it on the screen.
    Container contentPane = getContentPane();
    this.protocol = protocol;
    System.err.println( "protocol=" + protocol );
    
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.LINE_AXIS ) );
    load = new JButton( "Load" );
    load.setToolTipText( "Opens dialog to select a previously saved .rmpb file for loading." );
    load.addActionListener( this );
    buttonPanel.add( load );
    save = new JButton( "Save" );
    save.setToolTipText( "Opens dialog to save assembler listing as a .rmpb file." );
    save.setEnabled( false );
    save.addActionListener( this );
    buttonPanel.add( save );
    buttonPanel.add( Box.createHorizontalGlue() );
    paste = new JButton( "Paste" );
    paste.setToolTipText( "Opens text box for pasting from clipboard a PB/KM/RM-style protocol entry" );
    paste.addActionListener( this );
    buttonPanel.add( paste );
    buttonPanel.add( Box.createHorizontalStrut( 10 ) );
    ok = new JButton( "OK" );
    ok.addActionListener( this );
    ok.setEnabled( false );
    buttonPanel.add( ok );
    cancel = new JButton( "Cancel" );
    cancel.addActionListener( this );
    buttonPanel.add( cancel );
    contentPane.add( buttonPanel, BorderLayout.PAGE_END );
    
    editorPanel = new ManualEditorPanel( this );
    manualSettingsPanel = editorPanel.getManualSettingsPanel(); 
    manualSettingsPanel.reset( false );
    manualSettingsPanel.getDeviceText().setEditable( false );
    manualSettingsPanel.setProtocol( protocol, false );
    manualSettingsPanel.getProcBox().setSelectedIndex( 0 );
    manualSettingsPanel.setMode( Mode.ASM );
    manualSettingsPanel.getAssemblerPanel().setDialogSaveButton( save );
    contentPane.add( editorPanel, BorderLayout.CENTER );
    
    pack();
    Rectangle rect = getBounds();
    int x = rect.x - rect.width / 2;
    int y = rect.y - rect.height / 2;
    setLocation( x, y );
  }

  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == ok )
    {
      userAction = JOptionPane.OK_OPTION;
      manualSettingsPanel.getDevicePanel().updateFixedData();
      protocol = manualSettingsPanel.getProtocol();
      resetProcBox();
      setVisible( false );
      dispose();
    }
    else if ( source == cancel )
    {
      userAction = JOptionPane.CANCEL_OPTION;
      resetProcBox();
      setVisible( false );
      dispose();
    }
    else if ( source == paste)
    {
      String clipboardText = manualSettingsPanel.readClipboard();
      if ( clipboardText == null )
      {
        return;
      }
      ManualCodePanel codePanel = manualSettingsPanel.getTablePanel();
      String[] identStrings = codePanel.importProtocolCode( clipboardText, true );
      boolean validImport = true;
      if ( modeIndex > 0 )
      {
        String pidStr = manualSettingsPanel.getPid().getText();
        if ( pidStr == null ) pidStr = "";
        if ( !pidStr.equals( identStrings[ 2 ] ) )
        {
          validImport = false;
        }
      }
      if ( validImport )
      {
        codePanel.importProtocolCode( clipboardText, false );
        manualSettingsPanel.pid.setValue( new Hex( identStrings[ 2 ] ) );
        Processor p = ProcessorManager.getProcessor( identStrings[ 3 ] );
        manualSettingsPanel.getProcBox().setSelectedItem( p );
        manualSettingsPanel.getDeviceText().setText( manualSettingsPanel.getProtocolText( true, true ) );
      }
      else
      {
        String title = "Invalid import";
        String message = "You can only import code from a protocol with the same\n"
            + "PID as that of the protocol being edited.";
        JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
      }
    }
    else if ( source == load )
    {
      File file = RMProtocolBuilder.getProtocolFile( this, true );
      if ( file == null || !file.exists() )
      {
        return;
      }
      boolean validFile = true;
      String[] fileStrings = manualSettingsPanel.loadRMPB( file, true, modeIndex );

      if ( modeIndex == 0 )
      {
        if ( fileStrings[ 0 ] == null || !fileStrings[ 0 ].startsWith( "Manual Settings" )
            || ( fileStrings[ 1 ] != null && !fileStrings[ 1 ].isEmpty() ) )
        {
          validFile = false;
        }
      }
      else
      {
        String[] idStrings = new String[ 3 ];
        idStrings[ 0 ] = manualSettingsPanel.getProtocolName().getText();
        idStrings[ 1 ] = manualSettingsPanel.getVariantName().getText();
        idStrings[ 2 ] = manualSettingsPanel.getPid().getText();
        for ( int i = 0; i < 3; i++ )
        {
          if ( fileStrings[ i ] == null ) fileStrings[ i ] = "";
          if ( idStrings[ i ] == null ) idStrings[ i ] = "";
          if ( !fileStrings[ i ].equals( idStrings[ i ] ) )
          {
            validFile = false;
            break;
          }
        }
      }
      if ( validFile )
      {
        reset();
        manualSettingsPanel.loadRMPB( file, false, modeIndex );
        save.setEnabled( true );
      }
      else
      {
        String title = "Invalid file";
        String message = modeIndex == 0 ? 
            "You can only load a .rmpb file for a manual protocol." :
              "You can only load a .rmpb file for a protocol whose name, variant name if any,\n"
              + "and PID agree with those of the protocol being edited.";
        JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
      }
    }
    else if ( source == save )
    {
      manualSettingsPanel.saveAs();
    }
  }
  
  /**
   * Message types:
   *   0 = New manual protocol
   *   1 = Edit manual protocol
   *   2 = Edit standard protocol
   * Value is also used as a mode index to determine other behaviour
   */
  public void setMessage( int n )
  {
    modeIndex = n;
    editorPanel.getDevicePanel().setVisible( n != 2 );
    ManualCodePanel tablePanel = manualSettingsPanel.getTablePanel();
    DisplayArea noteArea = tablePanel.getNoteArea();
    editorPanel.getTranslationButton().setEnabled( n < 0 );
//    if ( n == 3 )
//    {
//      String text = "This is a cloned protocol.  The code for all processors may be "
//          + "edited as required.  Use the \"View/Export Ini\" button to view the "
//          + "result in the form of a protocols.ini entry and to save it if desired "
//          + "as a .prot file in the AddOns folder.";
//      noteArea.setText( text );
//      return;
//    }
//    
    String text = "";
    if ( n == 0 || n == 1 ) text += "The processor of the selected remote is ";
    else if ( n == 2 ) text += "A custom protocol is a modified executor for a standard protocol. "
        + "It uses the same translators as that standard protocol and keeps the same name, "
        + "variant name and PID.  Only the executor for the selected remote (";
//    if ( n != 1 )
    {
      JTableX codeTable = tablePanel.getCodeTable();
      if ( codeTable != null && codeTable.getSelectedRow() >= 0 )
      {
        text += ( ( Processor )codeTable.getValueAt( codeTable.getSelectedRow(), 0 ) ).getName();
      }
      else
      {
        text += "??????";
      }
    }
    if ( n == 0 || n == 1 ) text += ".  A Manual Protocol is added to the Protocol Manager and, "
        + "if used in a device upgrade, is stored in a .rmir file of that upgrade. It "
        + "may have code for more than one processor, to allow the upgrade to be used "
        + "with remotes with a range of processors.  It only supports simple translators. "
        + "\n\nUse RMPB to create a .prot file if a custom protocol with complex translators "
        + "is required.";
    else if ( n == 2 ) text += ") is editable.\n\nUse RMPB to create a .prot file if a "
        + "more general customization is required.";
    if ( n >= 1 ) text += "  Code shown in gray is standard code for information only.  "
        + "Code in black may be edited directly by double-cicking.";
    noteArea.setText( text );
  }
  /**
   * Gets the protocol.
   * 
   * @return the protocol
   */
  public ManualProtocol getProtocol()
  {
    if ( userAction != JOptionPane.OK_OPTION )
    {
      return null;
    }
    return protocol;
  }
  
  public ManualSettingsPanel getManualSettingsPanel()
  {
    return manualSettingsPanel;
  }

  public int getModeIndex()
  {
    return modeIndex;
  }
  
  private void reset()
  {
    manualSettingsPanel.reset( false );
    save.setEnabled( false );
  }
  
  private void resetProcBox()
  {
    JComboBox< Processor > procBox = ManualCodePanel.getProcBox();
    ActionListener[] als = procBox.getActionListeners();
    for ( ActionListener al : Arrays.copyOf( als, als.length ) )
    {
      procBox.removeActionListener( al );
    }
  }

  private ManualProtocol protocol = null;
  private int userAction = JOptionPane.CANCEL_OPTION;
  private ManualSettingsPanel manualSettingsPanel = null;
  private ManualEditorPanel editorPanel = null;
  public JButton ok = null;
  public JButton cancel = null;
  private int modeIndex = -1;
  private JButton load = null;
  private JButton save = null;
  private JButton paste = null;
  
}

