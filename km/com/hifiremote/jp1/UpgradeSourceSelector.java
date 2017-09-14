package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import com.hifiremote.jp1.RemoteMaster.LanguageDescriptor;

public class UpgradeSourceSelector extends JDialog implements ActionListener
{
  public UpgradeSourceSelector( JFrame owner, Remote remote, File current, LanguageDescriptor lang )
  {
    super( owner, "Upgrade Source Selector", true );
    this.remote = remote;
    createGui( owner, current, lang );
  }
  
  private void createGui( Component owner, File inFile, LanguageDescriptor lang )
  {
    setLocationRelativeTo( owner );
    this.inFile = inFile;
    this.outFile = inFile;

    (( JPanel )getContentPane()).setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ));

    ButtonGroup group = new ButtonGroup();

    group.add( defaultBtn );
    group.add( otherBtn );
    defaultBtn.addActionListener( this );
    otherBtn.addActionListener( this );
    browseBtn.addActionListener( this );
    okBtn.addActionListener( this );
    cancelBtn.addActionListener( this );
    
    defaultBtn.setSelected( inFile == null );
    otherBtn.setSelected( inFile != null );
    otherBtn.setEnabled( RemoteMaster.admin );
    otherBtn.setToolTipText( "<html>This option is not currently available.  It allows for the<br>"
        + "possibility of modified firmware being created in future.</html>" );
    browseBtn.setEnabled( inFile != null );
    fileField.setText( inFile != null ? inFile.getName() : null );
    
    Box box = Box.createVerticalBox();
    JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
    panel.add( new JLabel( "Select the source for XSight upgrade:" ) );
    box.add( panel );
    panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
    panel.add( defaultBtn );
    box.add( panel );
    panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
    panel.add( otherBtn );
    panel.add( fileField );
    panel.add( browseBtn );
    box.add(  panel );
    if ( !remote.getSignature().equals( "USB0007" ) )
    {
      panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
      panel.add( new JLabel( "Select the additional language support required:" ) );
      box.add(  panel );
      panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
      panel.add( Box.createHorizontalStrut( 20 ) );
      comboModel = new DefaultComboBoxModel< LanguageDescriptor >();
      languageBox = new JComboBox<>( comboModel );
      languageBox.setToolTipText( "<html>The setting \"Current\" keeps, for the upgrade, the same additional<br>"
          + "language (if any) as in the current firmware.  \"None\" removes any<br>"
          + "installed additional language and any other choice installs the named<br>"
          + "language, replacing the current addition if any.  English, Spanish,<br>"
          + "French, German and Italian are permanently installed and not affected<br>"
          + "by this selection" );
      Dimension d = languageBox.getPreferredSize();
      d.width = 100;
      languageBox.setPreferredSize( d );
      panel.add( languageBox );
      box.add(  panel );
      box.add( Box.createVerticalStrut( 5 ) );
      setLanguageBox();
      selectedLanguage = lang != null ? lang : RemoteMaster.defaultLanguage;
      languageBox.setSelectedItem( selectedLanguage );
    }
    panel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
    panel.add( okBtn );
    panel.add( cancelBtn );
    box.add( panel );
    add( box, BorderLayout.PAGE_START );
    pack();
  }
  
  private void setLanguageBox()
  {
    comboModel.removeAllElements();
    comboModel.addElement( RemoteMaster.defaultLanguage );
    if ( defaultBtn.isSelected() )
    {
      comboModel.addElement( RemoteMaster.getLanguage( 0 ) );
      comboModel.addElement( RemoteMaster.getLanguage( 9 ) );
    }
    else
    {
      String name = outFile.getName();
      int bar = name.lastIndexOf( '_' );
      int dot = name.lastIndexOf( '.' );
      String versionStr = name.substring( bar + 1, dot );
      int langNdx = -1;
      if ( versionStr.length() > 7 )
      {
        langNdx = Integer.parseInt( versionStr.substring( 7, 8 ) );
      }
      if ( langNdx >= 0 )
      {
        comboModel.addElement( RemoteMaster.getLanguage( langNdx ) );
      }
      languageBox.setSelectedIndex( 0 );
    }
  }
  
  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == okBtn )
    {
      if ( languageBox != null )
      {
        selectedLanguage = ( LanguageDescriptor )languageBox.getSelectedItem();
      }
      setVisible( false );
    }
    else if ( source == cancelBtn )
    {
      outFile = inFile;
      setVisible( false );
    }
    else if ( source == defaultBtn )
    {
      browseBtn.setEnabled( false );
      setLanguageBox();
    }
    else if ( source == otherBtn )
    {
      browseBtn.setEnabled( true );
      comboModel.removeAllElements();
    }
    else if ( source == browseBtn )
    {
      File inputDir = new File( RemoteMaster.getWorkDir(), "XSight" );
      if ( chooser == null )
      {
        chooser = new JFileChooser( inputDir );
      }
      FileFilter filter = new FileFilter()
      {
        @Override
        public boolean accept( File f )
        {
          return RemoteMaster.isValidUpgradeSource( f, remote );
        }

        @Override
        public String getDescription()
        {
          return "Available source files";
        }
      };
      chooser.setFileFilter( filter );
      if ( outFile != null )
      {
        chooser.setSelectedFile( outFile );
      }
      
      while ( true )
      {
        int returnVal = chooser.showOpenDialog( this );
        if ( returnVal == RMFileChooser.APPROVE_OPTION )
        {
          outFile = chooser.getSelectedFile();

          if ( !outFile.exists() )
          {
            JOptionPane.showMessageDialog( this, outFile.getName() + " doesn't exist.", "File doesn't exist.",
                JOptionPane.ERROR_MESSAGE );
          }
          else if ( outFile.isDirectory() )
          {
            JOptionPane.showMessageDialog( this, outFile.getName() + " is a directory.", "File doesn't exist.",
                JOptionPane.ERROR_MESSAGE );
          }
          else
          {
            break;
          }
        }
        else
        {
          return;
        }
      }
      if ( outFile != null )
      {
        String name = outFile.getName();
        fileField.setText( name );
        setLanguageBox();
      }
    }
  }
  
  public File getSource()
  {
    if ( defaultBtn.isSelected() )
      return null;
    else
      return outFile;
  }
  
  public LanguageDescriptor getSelectedLanguage()
  {
    return selectedLanguage;
  }
  
  private Remote remote = null;
  private JTextField fileField = new JTextField( 20 );
  JRadioButton defaultBtn = new JRadioButton( "System upgrade" );
  JRadioButton otherBtn = new JRadioButton( "File:" );
  private JButton browseBtn = new JButton( "Browse" );
  private JButton okBtn = new JButton( "OK" );
  private JButton cancelBtn = new JButton( "Cancel" );
  private JFileChooser chooser = null;
  private File inFile = null;
  private File outFile = null;
  private LanguageDescriptor selectedLanguage = null;
  private JComboBox< LanguageDescriptor > languageBox = null;
  private DefaultComboBoxModel< LanguageDescriptor > comboModel = null;

}

