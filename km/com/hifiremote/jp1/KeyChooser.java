package com.hifiremote.jp1;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

// TODO: Auto-generated Javadoc
/**
 * The Class KeyChooser.
 */
public class KeyChooser
  extends JDialog
  implements ActionListener
{
  
  /** The dialog. */
  private static KeyChooser dialog;
  
  /** The value. */
  private static Integer value; 
  
  /** The button box. */
  private JComboBox buttonBox = new JComboBox();
  
  /** The shift box. */
  private JCheckBox shiftBox = new JCheckBox();
  
  /** The x shift box. */
  private JCheckBox xShiftBox = new JCheckBox();
  
  private Button[] availableButtons = null;

  /**
   * Show dialog.
   * 
   * @param locationComp the location comp
   * @param remote the remote
   * @param initialKeyCode the initial key code
   * 
   * @return the integer
   */
  public static Integer showDialog( Component locationComp,
                                    Remote remote,
                                    Integer initialKeyCode,
                                    int type )
  {
    if ( dialog == null )
    {
      dialog = new KeyChooser( locationComp );
    }
    
    dialog.setRemote( remote, type );
    dialog.setKeyCode( initialKeyCode );
    dialog.setLocationRelativeTo( locationComp );
                              
    dialog.setVisible( true );
    return value;
  }

  /**
   * Instantiates a new key chooser.
   * 
   * @param c the c
   */
  private KeyChooser( Component c ) 
  {
    super(( JFrame )SwingUtilities.getRoot( c ));
    setTitle( "Key Chooser" );
    setModal( true );

    JButton cancelButton = new JButton( "Cancel" );
    cancelButton.addActionListener( this );

    JButton setButton = new JButton( "Set" );
    setButton.setActionCommand( "Set" );
    setButton.addActionListener( this );
    getRootPane().setDefaultButton( setButton );
    
    KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    getRootPane().registerKeyboardAction( this, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );

    JPanel panel = new JPanel();
    panel.add( new JLabel( "Key:" ));
    panel.add( buttonBox );
    panel.add( shiftBox );
    panel.add( xShiftBox );

    buttonBox.addActionListener( this );
    shiftBox.addActionListener( this );
    xShiftBox.addActionListener( this );

    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.TRAILING ));
    buttonPanel.add( setButton );
    buttonPanel.add( cancelButton );

    //Put everything together, using the content pane's BorderLayout.
    Container contentPane = getContentPane();
    contentPane.add( panel, BorderLayout.NORTH );
    contentPane.add( buttonPanel, BorderLayout.PAGE_END );

    pack();
  }

  /** The remote. */
  private Remote remote = null;
  
  private int type = Button.MOVE_BIND;
  
  /**
   * Sets the remote.
   * 
   * @param remote the new remote
   */
  public void setRemote( Remote remote, int type )
  {
    this.remote = remote;
    this.type = type;
    if ( type == Button.MACRO_BIND )
    {
      availableButtons = remote.getMacroButtons();
    }
    else if ( type == Button.LEARN_BIND )
    {
      availableButtons = remote.getLearnButtons();
    }
    else if ( type >= 0 )
    {
      availableButtons = remote.getBaseUpgradeButtons();
    }
    else
    {
      // Negative value indicates use of digit buttons only, from 0 to (-type)
      availableButtons = new Button[ 1 - type ];
      for ( int i = 0; i <= -type; i++ )
      {
        availableButtons[ i ] = remote.getButton( "" + i );
      }
    }
    
    buttonBox.setModel( new DefaultComboBoxModel( availableButtons ) );
    shiftBox.setText( remote.getShiftLabel());
    shiftBox.setVisible( type >= 0 && remote.getShiftEnabled() );
    xShiftBox.setText( remote.getXShiftLabel());
    xShiftBox.setVisible( type >= 0 && remote.getXShiftEnabled() );
    pack();
  }

  /**
   * Gets the key code.
   * 
   * @return the key code
   */
  public Integer getKeyCode()
  {
    Button b = ( Button )buttonBox.getSelectedItem();
    int code = b.getKeyCode();
    if ( shiftBox.isSelected())
      code |= remote.getShiftMask();
    else if ( xShiftBox.isSelected())
      code |= remote.getXShiftMask();

    return new Integer( code );
  }

  /**
   * Sets the key code.
   * 
   * @param keyCode the new key code
   */
  public void setKeyCode( Integer keyCode )
  {
    value = keyCode;
    int code = keyCode.intValue();
    Button b = remote.getButton( code );
    if( !Arrays.asList( availableButtons ).contains( b ) )
    {
      b = null;
    }
    shiftBox.setSelected( false );
    xShiftBox.setSelected( false );
    
    if ( b == null )
    {
      int base = code & 0x3F;
      if ( base != 0 )
      {
        b = remote.getButton( base );
        if (( base | remote.getShiftMask()) == code )
          shiftBox.setSelected( true );
        if (( base | remote.getXShiftMask()) == code )
          xShiftBox.setSelected( true );
      }
      else
      {
        b = remote.getButton( code & ~remote.getShiftMask());
        if ( b != null )
          shiftBox.setSelected( true );
        else
        {
          b = remote.getButton( code ^ ~remote.getXShiftMask());
          if ( b != null )
            xShiftBox.setSelected( true );
        }
      }
    }
      
    shiftBox.setEnabled( b.allowsShiftedKeyMove());
    xShiftBox.setEnabled( b.allowsXShiftedKeyMove());

    if ( b.getIsXShifted())
      xShiftBox.setSelected( true );      
    else if ( b.getIsShifted())
      shiftBox.setSelected( true );

    buttonBox.removeActionListener( this );
    buttonBox.setSelectedItem( b );  
    buttonBox.addActionListener( this );
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == buttonBox )
    {
      Button b = ( Button )buttonBox.getSelectedItem();
      if ( type >= 0 )
      {
        b.setShiftBoxes( type, shiftBox, xShiftBox );
      }
    }
    else if ( source == shiftBox )
    {
      if ( shiftBox.isSelected())
        xShiftBox.setSelected( false );
    }
    else if ( source == xShiftBox )
    {
      if ( xShiftBox.isSelected())
        shiftBox.setSelected( false );
    }
    else
    {
      if ( "Set".equals( e.getActionCommand())) 
        value = getKeyCode();
      else
        value = null;

      setVisible( false );
    }
  }
}

