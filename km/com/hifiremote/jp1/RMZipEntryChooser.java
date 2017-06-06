package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class RMZipEntryChooser extends JDialog implements ListSelectionListener, ActionListener
{
  private static RMZipEntryChooser dialog = null;
  private static ZipEntry entry = null;
  private static Remote remote = null;
  
  private JList< ZipEntry > zipList = null;
  private DefaultListModel< ZipEntry > zipModel = new DefaultListModel< ZipEntry >();
  private JButton okButton = new JButton( "OK" );
  private JButton cancelButton = new JButton( "Cancel" ); 
  
  private RMZipEntryChooser( Component c, ZipFile zipfile )
  {
    super( ( JFrame )SwingUtilities.getRoot( c ) );
    setTitle( "File Selector" );
    setModal( true );;
    
    Enumeration< ? extends ZipEntry > zipEnum = zipfile.entries();
    while ( zipEnum.hasMoreElements() ) 
    { 
       ZipEntry entry = ( ZipEntry ) zipEnum.nextElement(); 
       zipModel.addElement( entry );
    }

    zipList = new JList< ZipEntry >( zipModel );
    zipList.setVisibleRowCount( 15 );
    zipList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    zipList.setPreferredSize( new Dimension( 250, 400 ) );
    zipList.addListSelectionListener( this );
    
    JComponent contentPane = ( JComponent )getContentPane();
    contentPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    
    JPanel selectPanel = new JPanel( new BorderLayout() );
    selectPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    contentPane.add( selectPanel, BorderLayout.CENTER);
    
    JScrollPane zipView = new JScrollPane( zipList );
    zipView.setViewportBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    zipView.setBackground( Color.WHITE );
    selectPanel.add( zipView, BorderLayout.CENTER );
    
    JPanel infoPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
//    infoPanel.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );
    contentPane.add( infoPanel, BorderLayout.PAGE_START );
    JLabel label  = new JLabel();
    JTextArea area = new JTextArea( "Remote is " + remote.getName() 
        + ".\nSelect the system file to upload to this remote:");
    area.setBackground( label.getBackground() );
    area.setFont( label.getFont() );
    area.setEditable( false );
    infoPanel.add( area );

    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
    contentPane.add( buttonPanel, BorderLayout.PAGE_END );
    
    okButton.addActionListener( this );
    buttonPanel.add( okButton );
    cancelButton.addActionListener( this );
    buttonPanel.add( cancelButton );
  }
  
  public static ZipEntry showDialog( RemoteMaster owner, ZipFile file )
  {
    RemoteConfiguration config = owner.getRemoteConfiguration();
    remote = config != null ? config.getRemote() : null;
    dialog = new RMZipEntryChooser( owner, file );
    dialog.pack();
    dialog.setLocationRelativeTo( owner );
    dialog.setVisible( true );
    return entry;
  }
  
  public static void reset()
  {
    if ( dialog != null )
    {
      dialog.dispose();
      dialog = null;
    }
  }
  
  @Override
  public void actionPerformed( ActionEvent event )
  {
    Object source = event.getSource();
    
    if ( source == okButton )
    {
      entry = zipList.getSelectedValue();
      if ( entry == null )
      {
        String title = "File selector";
        String message = "You have not selected a file to upload!";
        JOptionPane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );
        return;
      }
      setVisible( false );
    }
    else if ( source == cancelButton )
    {
      entry = null;
      setVisible( false );
    } 
  }

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
//    if ( e.getValueIsAdjusting() )
//      return;
//    
//    int selected = zipList.getSelectedIndex();
//    okButton.setEnabled( selected >= 0 );
  }

}
