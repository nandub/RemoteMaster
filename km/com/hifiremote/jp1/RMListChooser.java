package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.hifiremote.jp1.io.CommHID;
import com.hifiremote.jp1.io.CommHID.FileData;

public class RMListChooser extends JDialog implements ListSelectionListener, ActionListener
{
  private static RMListChooser dialog = null;
  private static String entry = null;
  private static Remote remote = null;
  
  private JList< String > list = null;
  private DefaultListModel< String > model = new DefaultListModel< String >();
  private JButton okButton = new JButton();
  private JButton cancelButton = new JButton();
  private JPanel opPanel = null;
  private JLabel statusLabel = null;
  private CommHID io = null;
  private JFileChooser fileChooser = null;
  
  private JRadioButton noneButton = null;
  private JRadioButton deleteButton = null;
  private JRadioButton saveButton = null;
  
  private RMListChooser( Component c, CommHID io, boolean write  )
  {
    super( ( JFrame )SwingUtilities.getRoot( c ) );
    setTitle( "File Selector" );
    setModal( true );
    this.io = io;
    setModel( write );
    String fwVersion = io.getXZITEVersion( null );
    String heading = "Remote = " + remote.getName() + ".\nFirmware version = ";
    heading += ( fwVersion != null ? fwVersion : "Unknown" ) + "\n\n";
    heading += write
        ? "Select the firmware file to upload to the remote:"
        : "Files are listed together with their length in bytes.\n"
          + "Select the file to delete from the remote or save to the PC:";
    list = new JList< String >( model );
    int rowHeight = list.getFontMetrics( list.getFont() ).getHeight() + 2;
    list.setVisibleRowCount( 15 );
    list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    list.setPreferredSize( new Dimension( 400, rowHeight * ( model.getSize() + 1 ) ) );
    list.addListSelectionListener( this );
    
    JComponent contentPane = ( JComponent )getContentPane();
    contentPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    
    JPanel selectPanel = new JPanel( new BorderLayout() );
    selectPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    contentPane.add( selectPanel, BorderLayout.CENTER);
    
    JScrollPane view = new JScrollPane( list );
    view.setViewportBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    view.setBackground( Color.WHITE );
    selectPanel.add( view, BorderLayout.CENTER );
    
    JPanel infoPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
//    infoPanel.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );
    contentPane.add( infoPanel, BorderLayout.PAGE_START );
    JLabel label  = new JLabel();
    JTextArea area = new JTextArea( heading );
    area.setBackground( label.getBackground() );
    area.setFont( label.getFont() );
    area.setEditable( false );
    infoPanel.add( area );

    JPanel bottomPanel = new JPanel( new BorderLayout() );
    contentPane.add( bottomPanel, BorderLayout.PAGE_END );
    bottomPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 2, 2 ) );
    
    JPanel statusPanel = new JPanel( new BorderLayout() );
    bottomPanel.add( statusPanel, BorderLayout.PAGE_END );
    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
    statusPanel.add( buttonPanel, BorderLayout.LINE_END );
    
    statusLabel = new JLabel();
    statusLabel.setText( "DELETING..." );
    statusLabel.setForeground( Color.RED );
    statusLabel.setFont( statusLabel.getFont().deriveFont( Font.BOLD ) );
    statusLabel.setVisible( false );
    statusPanel.add( statusLabel, BorderLayout.LINE_START );
    
    if ( !write )
    {
      opPanel = setOpPanel();
      bottomPanel.add( opPanel, BorderLayout.CENTER );
    }
    
    okButton.addActionListener( this );
    okButton.setText( write ? "Upload" : "Execute" );
    okButton.setEnabled( false );
    buttonPanel.add( okButton );
    cancelButton.addActionListener( this );
    cancelButton.setText( "Close" );
    buttonPanel.add( cancelButton );
  }
  
  private class DeleteTask extends SwingWorker< Void, Void >
  {
    @Override
    protected Void doInBackground() throws Exception
    {
      List< String > names = new ArrayList< String >();
      names.add( entry );
      boolean result = io.deleteSystemFiles( names );
      if ( !result )
      {
        String title = "Delete request";
        String message = "Attempt to delete file " + entry + " failed.";
        JOptionPane.showMessageDialog( RMListChooser.this, message, title, JOptionPane.ERROR_MESSAGE );
      }
      setModel( false );
      statusLabel.setVisible( false );
      return null;
    }
  }
  
  private class UploadTask extends SwingWorker< Void, Void >
  {
    @Override
    protected Void doInBackground() throws Exception
    {
      List< String > names = new ArrayList< String >();
      names.add( entry );
      File sysFile = RemoteMaster.getRmirSys();
      boolean result = io.writeSystemFiles( sysFile, names, 0 );
      if ( !result )
      {
        String title = "Upload request";
        String message = "Attempt to upload file " + entry + " failed.";
        JOptionPane.showMessageDialog( RMListChooser.this, message, title, JOptionPane.ERROR_MESSAGE );
      }
      statusLabel.setVisible( false );
      return null;
    }
  }
  
  private class SaveTask extends SwingWorker< Void, Void >
  {
    File file = null;
    
    public SaveTask( File file )
    {
      this.file = file;
    }
    
    @Override
    protected Void doInBackground() throws Exception
    {
      byte[] data = io.readSystemFile( entry );
      try
      {
        FileOutputStream output = new FileOutputStream( file, false );
        output.write( data, 0, data.length );
        output.flush();
        output.close();
      }
      catch ( Exception e )
      {
        String title = "Save request";
        String message = "Attempt to save file " + entry + " failed.";
        JOptionPane.showMessageDialog( RMListChooser.this, message, title, JOptionPane.ERROR_MESSAGE );
      }
      statusLabel.setVisible( false );
      return null;
    }
  }
  
  private void setModel( boolean write )
  {
    model.clear();
    if ( write )
    {
      File sysFile = RemoteMaster.getRmirSys();
      if ( io.setFileData( sysFile ) )
      {
        LinkedHashMap< String, FileData > upgradeData = io.getUpgradeData();
        for ( String name : CommHID.xziteSysNames )
        {
          if ( name.indexOf( '.' ) > 0 && upgradeData.keySet().contains( name.toUpperCase() ) )          
          model.addElement( name );
        }
      }
    }
    else
    {
      LinkedHashMap< String, Integer > map =  io.getXZITEfileList();
      for ( String name : map.keySet() )
      {
        model.addElement( name + "  " + map.get( name ) );
      }
    }
  }
  
  public static void showDialog( RemoteMaster owner, CommHID io, boolean write )
  {
    RemoteConfiguration config = owner.getRemoteConfiguration();
    remote = config != null ? config.getRemote() : null;
    dialog = new RMListChooser( owner, io, write );
    dialog.pack();
    dialog.setLocationRelativeTo( owner );
    owner.setInterfaceState( null );
    dialog.setVisible( true );
    return;
  }
  
  public static void reset()
  {
    if ( dialog != null )
    {
      dialog.dispose();
      dialog = null;
    }
  }
  
  private JPanel setOpPanel()
  {
    noneButton = new JRadioButton( "None" );
    deleteButton = new JRadioButton( "Delete" );
    saveButton = new JRadioButton( "Save" );
    ButtonGroup grp = new ButtonGroup();
    grp.add( noneButton );
    grp.add( deleteButton );
    grp.add( saveButton );
    noneButton.setSelected( true );
    JPanel panel = new JPanel( new FlowLayout( FlowLayout.CENTER ) );
    panel.add( noneButton );
    panel.add( deleteButton );
    panel.add( saveButton );
    panel.setBorder( BorderFactory.createTitledBorder( " Operation to perform: " ) );
    return panel;
  }
  
  @Override
  public void actionPerformed( ActionEvent event )
  {
    Object source = event.getSource();
    
    if ( source == okButton )
    {
      if ( statusLabel.isVisible() )
      {
        // Do nothing if an operation is in progress
        return;
      }
      
      entry = list.getSelectedValue();
      if ( entry != null && entry.indexOf( ' ' ) > 0 )
      {
        entry = entry.substring( 0, entry.indexOf( ' ' ) );
      }
      if ( ( opPanel == null || !noneButton.isSelected() ) && entry == null )
      {
        String title = "File selector";
        String message = "You have not selected a file!";
        JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
        return;
      }
      else if ( opPanel == null )
      {
        statusLabel.setText( "UPLOADING " + entry + "..." );
        statusLabel.setVisible( true );
        ( new UploadTask() ).execute();
        return;
      }
      else if ( opPanel != null && deleteButton.isSelected() )
      {
        statusLabel.setText( "DELETING " + entry + "..." );
        statusLabel.setVisible( true );
        ( new DeleteTask() ).execute();
        return;
      }
      else if ( opPanel != null && saveButton.isSelected() )
      {
        if ( fileChooser == null )
        {
          fileChooser = new JFileChooser( RemoteMaster.getWorkDir() );
        }
        fileChooser.setSelectedFile( new File( entry ) );
        int rval = fileChooser.showSaveDialog( this );
        if ( rval == JFileChooser.APPROVE_OPTION )
        {
          statusLabel.setText( "SAVING " + entry + "..." );
          statusLabel.setVisible( true );
          File file = fileChooser.getSelectedFile();
          ( new SaveTask( file ) ).execute();
        }
        return;
      }
    }
    else if ( source == cancelButton )
    {
      if ( !statusLabel.isVisible() )
      {
        // The user should not press Cancel while an operation is in progress,
        // but don't close remote if this is done, perhaps in an error situation.
        io.closeRemote();
      }
      entry = null;
      setVisible( false );
    } 
  }

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    okButton.setEnabled( list.getSelectedValue() != null );
  }

}
