package com.hifiremote.jp1;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.NumberFormatter;

import com.hifiremote.jp1.RemoteConfiguration.KeySpec;

public class MacroDefinitionBox extends Box implements ActionListener, ListSelectionListener,
PropertyChangeListener, RMSetter< Object >
{
  public MacroDefinitionBox()
  {
    this( false );
    itemStyleFromRemote = true;
  }
  
  public MacroDefinitionBox( boolean itemStyle )
  {
    super( BoxLayout.X_AXIS );
    this.itemStyle = itemStyle;
    macroButtons.setModel( macroButtonModel );
    setBorder( BorderFactory.createTitledBorder( "Macro Definition" ) );

    creationPanel = new JPanel( new CardLayout() );
    
    JPanel availableBox = new JPanel( new BorderLayout() );
    add( availableBox );
    availableBox.add(  creationPanel, BorderLayout.CENTER );
    JPanel availablePanel = new JPanel( new BorderLayout() );
    availablePanel.add( new JLabel( "Available keys:" ), BorderLayout.NORTH );
    availableButtons.setFixedCellWidth( 100 );
    
    availablePanel.add( new JScrollPane( availableButtons ), BorderLayout.CENTER );

    availableButtons.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    availableButtons.addListSelectionListener( this );
    availableButtons.setToolTipText( "<html>Double-click to add/insert.<br>" 
        + "Shift/double-click to add/insert shifted.<br>"
        + "This will insert if a macro key is selected, add otherwise.<br>"
        + "Right-click (either box) to clear macro key selection.</html>");
       
    double b = 5; // space between rows and around border
    double c = 10; // space between columns
    double pr = TableLayout.PREFERRED;
    double pf = TableLayout.FILL;
    double size[][] =
      {
        {
          b, pr, c, pf, b
        }, // cols
        {
          b, pr, b, pr, b, pr, b, pr, pr, b
        }  // rows
      };

    JPanel ssdPanel = new JPanel( new BorderLayout() );
    ssdPanel.add( new JLabel( "Specify macro item:"), BorderLayout.PAGE_START );
    itemPanel = new JPanel( new TableLayout( size ) );
    ssdPanel.add( itemPanel, BorderLayout.CENTER );
    deviceBox = new JComboBox();
    functionBox = new JComboBox();
    buttonBox = new JComboBox();
    
    creationPanel.add( availablePanel, "Normal");
    creationPanel.add( ssdPanel, "SSD");
    
    panel = new JPanel( new GridLayout( 3, 2, 2, 2 ) );
    panel.setBorder( BorderFactory.createEmptyBorder( 2, 0, 0, 0 ) );
    availableBox.add( panel, BorderLayout.SOUTH );
    add.addActionListener( this );
    panel.add( add );
    insert.addActionListener( this );
    panel.add( insert );
    addShift.addActionListener( this );
    panel.add( addShift );
    insertShift.addActionListener( this );
    panel.add( insertShift );
    addXShift.addActionListener( this );
    panel.add( addXShift );
    insertXShift.addActionListener( this );
    panel.add( insertXShift );
    
    add( Box.createHorizontalStrut( 20 ) );

    JPanel keysBox = new JPanel( new BorderLayout() );
    add( keysBox );
    keysBox.add( new JLabel( "Macro Keys:" ), BorderLayout.NORTH );
    macroButtons.setCellRenderer( macroButtonRenderer );    
    macroButtons.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    macroButtons.addListSelectionListener( this );
    macroButtons.setFixedCellWidth( 100 );
    macroButtons.addFocusListener( new FocusListener()
    {
      @Override
      public void focusLost( FocusEvent e )
      {
        enableButtons();
      }
      
      @Override
      public void focusGained( FocusEvent e )
      {
        enableButtons();
      }
    } );
    
    keysBox.add( new JScrollPane( macroButtons ), BorderLayout.CENTER );
    
    JPanel buttonPanel = new JPanel( new BorderLayout() );
    keysBox.add( buttonPanel, BorderLayout.SOUTH );

    b = 2;
    double size2[][] =
      {
        {
          pf, b, pf
        }, // cols
        {
          pr, b, pr, b, pr
        }  // rows
      };

    JPanel pushbtnBox = new JPanel( new TableLayout( size2 ) );
    buttonPanel.add(  pushbtnBox, BorderLayout.PAGE_START );
    buttonPanel.setBorder( BorderFactory.createEmptyBorder( 2, 0, 0, 0 ) );
    
    moveUp.addActionListener( this );
    pushbtnBox.add( moveUp, "0,0" );
    moveDown.addActionListener( this );
    pushbtnBox.add( moveDown, "2,0" );
    remove.addActionListener( this );
    remove.setToolTipText( "Remove selected item.  Key: DEL" );
    remove.setFocusable( false );
    pushbtnBox.add( remove, "0,2" );
    clear.addActionListener( this );
    pushbtnBox.add( clear, "2,2" );
    deselect.addActionListener( this );
    deselect.setToolTipText( "Deselects current selection.  Mouse: Right-click box" );
    pushbtnBox.add( deselect, "0,4,2,4" );
    
    macroButtons.addKeyListener( new KeyAdapter()
    {
      public void keyPressed( KeyEvent e )
      {
        if ( e.getKeyCode() == KeyEvent.VK_DELETE && remove.isVisible() && remove.isEnabled() )
        {
          remove.doClick();
        }
      }    
    } );
    
    availableButtons.addMouseListener( new MouseAdapter()
    {
      @Override
      public void mouseClicked( MouseEvent e )
      {
        if ( e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 )
        {
          if ( macroButtons.getSelectedValue() != null )
          {
            if ( ( e.getModifiers() & KeyEvent.SHIFT_MASK ) != 0 )
            {
              if ( insertShift.isVisible() && insertShift.isEnabled() )
              {
                insertShift.doClick();
              }
            }
            else if ( insert.isVisible() && insert.isEnabled() )
            {
              insert.doClick();
            }
          }       
          else if ( ( e.getModifiers() & KeyEvent.SHIFT_MASK ) != 0 )
          {
            if ( addShift.isVisible() && addShift.isEnabled() )
            {
              addShift.doClick();
            }
          }
          else if ( add.isVisible() && add.isEnabled() )
          {
            add.doClick();
          }
        }
        else if ( ( e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3 ) 
            && deselect.isVisible() && deselect.isEnabled() )
        {
          deselect.doClick();
        }
      }
    } );
    
    macroButtons.addMouseListener( new MouseAdapter()
    {
      @Override
      public void mouseClicked( MouseEvent e )
      {
        if ( ( e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3 ) 
            && deselect.isVisible() && deselect.isEnabled() )
        {
          deselect.doClick();
        }
      } 
    } );
  }

  private class XFormattedTextField extends JFormattedTextField
  {
    XFormattedTextField( NumberFormatter formatter )
    {
      super( formatter );
      setFocusLostBehavior( JFormattedTextField.COMMIT_OR_REVERT );
    }
    
    @Override
    protected void processFocusEvent( FocusEvent e ) 
    {
      super.processFocusEvent( e );
      if ( e.getID() == FocusEvent.FOCUS_GAINED )
      {  
        selectAll();
      }  
    }
  }

  public void setButtonEnabler( ButtonEnabler buttonEnabler )
  {
    this.buttonEnabler = buttonEnabler;
  }

  @Override
  public void setRemoteConfiguration( RemoteConfiguration config )
  {
    this.config = config;
    Remote remote = config.getRemote();
    if ( itemStyleFromRemote )
    {
      itemStyle = config.getRemote().usesEZRC();
    }
    macroButtonRenderer.setRemote( remote );
//    durationPanel.setVisible( false );
    
    java.util.List< Button > buttons = remote.getButtons();
    for ( Button b : buttons )
    {
      if ( buttonEnabler.isAvailable( b ) )
      {  
        availableButtonModel.addElement( b );
      }
    }
    availableButtons.setModel( availableButtonModel );
    CardLayout cl = ( CardLayout)creationPanel.getLayout();
    cl.show( creationPanel, itemStyle ? "SSD" : "Normal" );
    
    if ( itemStyle )
    {
      NumberFormatter formatter = new NumberFormatter( new DecimalFormat( "0.0" ) );
      formatter.setValueClass( Float.class );
      delay = new XFormattedTextField( formatter );
      delay.addPropertyChangeListener( "value", this );
      holdCheck = new JCheckBox( "Hold?" );
      
      formatter = new NumberFormatter( new DecimalFormat( remote.usesEZRC() ? "0.0" : "0.00" ) );
      formatter.setValueClass( Float.class );
      duration = new XFormattedTextField( formatter );
      duration.setColumns( 4 );
      duration.addActionListener( this );
      
      if ( remote.usesEZRC() )
      {
        remote.setDeviceComboBox( deviceBox );
        holdCheck.addActionListener( this );
        itemPanel.add( new JLabel( "Device:" ), "1, 1" );
        itemPanel.add( deviceBox, "3, 1" );
        itemPanel.add( new JLabel( "Function:" ), "1, 3" );
        deviceBox.addActionListener( this );
        itemPanel.add( functionBox, "3, 3"  );
        itemPanel.add( new JLabel( "Pause after (secs):" ), "1, 5" );
        itemPanel.add( delay, "3, 5" );
        delay.setValue( 0.3f );
        itemPanel.add( holdCheck, "1, 7" );
        durationLabel.setText( "Hold for (secs):" );
        itemPanel.add( durationLabel, "1, 8" );
        itemPanel.add( duration, "3, 8" );
        duration.setValue( 0.0f );
        duration.setEnabled( false );
        durationLabel.setEnabled( false );
        int ndx = deviceBox.getSelectedIndex();
        if ( ndx >= 0 )
        {
          // Reset this index to activate its action to set functionBox model
          deviceBox.setSelectedIndex( ndx );
        }
        //      duration.setFocusLostBehavior( JFormattedTextField.COMMIT_OR_REVERT );
      }
      else
      {
        holdCheck.setSelected( true );
        durationLabel.setText( "Hold for (secs):" );
        itemPanel.add( durationLabel, "1, 3" );
        itemPanel.add( duration, "3, 3" );
        itemPanel.add( new JLabel( "Button:" ), "1, 5" );
        itemPanel.add( buttonBox, "3, 5"  );
        itemPanel.add( new JLabel( "Pause after (secs):" ), "1, 7" );
        itemPanel.add( delay, "3, 7" );
        delay.setValue( 0.3f );
        duration.setValue( 0.0f );
        DefaultComboBoxModel model = new DefaultComboBoxModel( remote.getMacroButtons() );
        buttonBox.setModel( model );
      }
    }
  }  
  
  public void actionPerformed( ActionEvent event )
  {
    Object source = event.getSource();
    Remote remote = config.getRemote();
    if ( source == add )
    {
      addKey( 0 );
    }
    else if ( source == insert )
    {
      insertKey( 0 );
    }
    else if ( source == addShift )
    {
      if ( addShift.getText().equals( "Replace" ) )
      {
        replaceKey( 0 );
      }
      else
      {
        addKey( remote.getShiftMask() );
      }
    }
    else if ( source == insertShift )
    {
      insertKey( remote.getShiftMask() );
    }
    else if ( source == addXShift )
    {
      addKey( remote.getXShiftMask() );
    }
    else if ( source == insertXShift )
    {
      insertKey( remote.getXShiftMask() );
    }
    else if ( source == moveUp )
    {
      int index = macroButtons.getSelectedIndex();
      swap( index, index - 1 );
    }
    else if ( source == moveDown )
    {
      int index = macroButtons.getSelectedIndex();
      swap( index, index + 1 );
    }
    else if ( source == remove )
    {
      int index = macroButtons.getSelectedIndex();
      macroButtonModel.removeElementAt( index );
      int last = macroButtonModel.getSize() - 1;
      if ( index > last )
        index = last;
      macroButtons.setSelectedIndex( index );
    }
    else if ( source == clear )
    {
      macroButtonModel.clear();
    }
    else if ( source == deselect )
    {
      macroButtons.clearSelection();
    }
    else if ( source == deviceBox )
    {
      DeviceButton db = ( DeviceButton )deviceBox.getSelectedItem();
      DefaultComboBoxModel model = new DefaultComboBoxModel( db.getUpgrade().getGeneralFunctionList().toArray() );
      functionBox.setModel( model );
    }
    else if ( source == holdCheck )
    {
      duration.setEnabled( holdCheck.isSelected() );
      durationLabel.setEnabled( holdCheck.isSelected() );
      if ( !holdCheck.isSelected() )
      {
        duration.setValue( 0.0f );
      }
    }
    enableButtons();
  }
  
  private KeySpec getKeySpec()
  {
    KeySpec ks = null;
    double durMult= 10.0;
    if ( config.getRemote().usesEZRC() )
    {
      DeviceButton db = ( DeviceButton )deviceBox.getSelectedItem();
      GeneralFunction f = ( GeneralFunction )functionBox.getSelectedItem();
      ks = new KeySpec( db, f );
    }
    else
    {
      // db is not actually used, but needs to be non-null to prevent spurious errors
      DeviceButton db = config.getRemote().getDeviceButtons()[ 0 ];
      Button btn = ( Button )buttonBox.getSelectedItem();
      ks = new KeySpec( db, btn );
      ks.setEZRC( false );
      durMult = 400.0;
    }

    Float fv = ( Float )delay.getValue();
    ks.delay = fv == null ? 0 : ( int )( 10.0 * fv + 0.5 );
    if ( holdCheck.isSelected() )
    {
      fv = ( Float )duration.getValue();
      ks.duration = fv == null ? 0 : ( int )( durMult * fv + 0.5 );
    }
    else
    {
      ks.duration = -1;
    }
    return ks;
  }
  
  /**
   * Adds the key.
   * 
   * @param mask
   *          the mask
   */
  private void addKey( int mask )
  {
    Remote remote = config.getRemote();
    if ( itemStyle )
    {
      macroButtonModel.addElement( getKeySpec() );
      return;
    }
//    if ( remote.usesEZRC() )
//    {
//      // minimum duration is 0 for hold buttons but 0.1 for others
//      Button btn = ( Button )availableButtons.getSelectedValue();
//      mask |= isHold( btn ) ? 0 : 0x100;
//    }
    Integer value = new Integer( getSelectedKeyCode() | mask );
    macroButtonModel.addElement( value );
  }

  private void replaceKey( int mask )
  {
    int index = macroButtons.getSelectedIndex();
    if ( index == -1 )
    {
      return;
    }
    if ( itemStyle )
    {
      KeySpec value = getKeySpec();
      macroButtonModel.setElementAt( value, index );
    }
    else
    {
      Integer value = new Integer( getSelectedKeyCode() | mask );
      macroButtonModel.setElementAt( value, index );
    }
    macroButtons.setSelectedIndex( index );
    macroButtons.ensureIndexIsVisible( index );
  }

  private void insertKey( int mask )
  {
    int index = macroButtons.getSelectedIndex();
    if ( itemStyle )
    {
      KeySpec value = getKeySpec();
      macroButtonModel.add( index, value );
    }
    else
    {
      Integer value = new Integer( getSelectedKeyCode() | mask );
      if ( index == -1 )
        macroButtonModel.add( 0, value );
      else
        macroButtonModel.add( index, value );
    }
    macroButtons.setSelectedIndex( index + 1 );
    macroButtons.ensureIndexIsVisible( index + 1 );
  }
  
  /**
   * Swap.
   * 
   * @param index1
   *          the index1
   * @param index2
   *          the index2
   */
  private void swap( int index1, int index2 )
  {
    Object o1 = macroButtonModel.get( index1 );
    Object o2 = macroButtonModel.get( index2 );
    macroButtonModel.set( index1, o2 );
    macroButtonModel.set( index2, o1 );
    macroButtons.setSelectedIndex( index2 );
    macroButtons.ensureIndexIsVisible( index2 );
  }
  
  /**
   * Gets the selected key code.
   * 
   * @return the selected key code
   */
  private int getSelectedKeyCode()
  {
    return ( ( Button )availableButtons.getSelectedValue() ).getKeyCode();
  }
  
  public boolean isMoreRoom( int limit )
  {
    return macroButtonModel.getSize() < limit;
  }

  public boolean isEmpty()
  {
    return macroButtonModel.getSize() == 0;
  }
  
//  private boolean isHold( Button btn )
//  {
//    Remote remote = config.getRemote();
//    if ( remote.usesEZRC() )
//    {
//      LinkedHashMap< String, List< Button >> groups = remote.getButtonGroups();
//      List< Button > holdList = groups != null ? groups.get( "Hold" ) : null;
//      return holdList != null && holdList.contains( btn );
//    }
//    return false;
//  }
  
  public JList getMacroButtons()
  {
    return macroButtons;
  }

  @Override
  public Object getValue()
  {
    int length = macroButtonModel.getSize();
    if ( itemStyle )
    {
      List< KeySpec > items = new ArrayList< KeySpec >();
      for ( int i = 0; i < length; ++i )
      {  
        items.add( ( ( KeySpec )macroButtonModel.elementAt( i ) ) );
      }  
      return items;
    }
    else
    {
      short[] keyCodes = new short[ length ];
      for ( int i = 0; i < length; ++i )
      {  
        keyCodes[ i ] = ( ( Number )macroButtonModel.elementAt( i ) ).shortValue();
      }  
      return new Hex( keyCodes );
    }
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
   */ 
  public void valueChanged( ListSelectionEvent e )
  {
    if ( e.getValueIsAdjusting() )
      return;

    enableButtons();
    if ( itemStyle )
    {
      KeySpec ks = ( KeySpec)macroButtons.getSelectedValue();
      if ( ks == null )
      {
        return;
      }
      if ( config.getRemote().usesEZRC() )
      {
        deviceBox.setSelectedItem( ks.db );
        GeneralFunction gf = ks.fn; 
        functionBox.getModel().setSelectedItem( null );
        functionBox.setSelectedItem( gf );
        boolean showDuration = ks.duration >= 0;
        holdCheck.setSelected( showDuration );
        duration.setEnabled( showDuration );
        durationLabel.setEnabled( showDuration );
        duration.setValue( showDuration ? ks.duration * 0.1f : 0f );
      }
      else
      {
        buttonBox.getModel().setSelectedItem( null );
        buttonBox.setSelectedItem( ks.btn );
        duration.setValue( ks.duration * 0.0025f );
      }
      delay.setValue( ks.delay / 10.0f );
    }
  }
  
  @Override
  public void setValue( Object value )
  {
    macroButtonModel.clear();
    if ( value == null )
    {
      return;
    }

    if ( value instanceof List< ? > )
    {
      @SuppressWarnings( "unchecked" )
      List< KeySpec > list = ( List< KeySpec > )value;
      for ( KeySpec ks : list )
      {
        macroButtonModel.addElement( ks );
      }
    }
    else
    {
      Hex hex = ( Hex )value;
      availableButtons.setSelectedIndex( -1 );
      short[] data = hex.getData();
      for ( int i = 0; i < data.length; ++i )
        macroButtonModel.addElement( new Integer( data[ i ] ) );
      macroButtons.setSelectedIndex( -1 );
    }
  }

  /**
   * Enable buttons.
   */
  public void enableButtons()
  {
    boolean usesEZRC = config.getRemote().usesEZRC();
    int selected = macroButtons.getSelectedIndex();
    moveUp.setEnabled( selected > 0 );
    moveDown.setEnabled( ( selected != -1 ) && ( selected < ( macroButtonModel.getSize() - 1 ) ) );
    remove.setEnabled( macroButtons.isFocusOwner() && selected != -1 );
    clear.setEnabled( macroButtonModel.getSize() > 0 );
    deselect.setEnabled( selected != -1 );
    Button baseButton = ( Button )( usesEZRC ? null : itemStyle ? buttonBox.getSelectedItem() 
        : availableButtons.getSelectedValue() );
    buttonEnabler.enableButtons( baseButton, this );
  }

  /** The add. */
  protected JButton add = new JButton( "Add" );

  /** The insert. */
  protected JButton insert = new JButton( "Insert" );

  /** The add shift. */
  protected JButton addShift = new JButton( "Add Shift" );

  /** The insert shift. */
  protected JButton insertShift = new JButton( "Ins Shift" );

  /** The add x shift. */
  protected JButton addXShift = new JButton( "Add xShift" );

  /** The insert x shift. */
  protected JButton insertXShift = new JButton( "Ins xShift" );
  
  private JButton moveUp = new JButton( "Move up" );

  /** The move down. */
  private JButton moveDown = new JButton( "Move down" );

  /** The remove. */
  private JButton remove = new JButton( "Remove" );

  /** The clear. */
  private JButton clear = new JButton( "Clear" );
  private JButton deselect = new JButton( "Clear selection" );
  
  private JComboBox deviceBox = null;
  private JComboBox functionBox = null;
  private JComboBox buttonBox = null;
  
//  private JPanel durationPanel = new JPanel( new BorderLayout() );
  
  private JPanel creationPanel = null;
  private JPanel itemPanel = null;
  
  private JLabel durationLabel = new JLabel( "Duration:  " );
  
  private XFormattedTextField duration = null;
  private XFormattedTextField delay = null;
  private JCheckBox holdCheck = null;

  /** The config. */
  private RemoteConfiguration config = null;

  /** The available buttons. */
  private JList availableButtons = new JList();
  
  private DefaultListModel availableButtonModel = new DefaultListModel();
  
  /** The macro buttons. */
  private JList macroButtons = new JList();
  
  private JPanel panel = null;
  
  private ButtonEnabler buttonEnabler = null;
  
  /** The macro button model. */
  private DefaultListModel macroButtonModel = new DefaultListModel();
  
  /** The macro button renderer. */
  private MacroButtonRenderer macroButtonRenderer = new MacroButtonRenderer();
  
  private boolean itemStyle = false;
  private boolean itemStyleFromRemote = false;

  @Override
  public void propertyChange( PropertyChangeEvent e )
  {
    Object source = e.getSource();
    if ( source == delay ) 
    {
      Float f = ( Float )delay.getValue();
      if ( config != null && config.getRemote().usesEZRC() )
      {
        if ( f < 0.1 )
        {
          delay.setValue( 0.1f );
        }
        if ( f > 10.0 )
        {
          delay.setValue( 10.0f );
        }
      }
    }
  }
}
