package com.hifiremote.jp1;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.NumberFormatter;

import com.hifiremote.jp1.assembler.CommonData;

public class ProtocolDataPanel extends JPanel
{
  public enum EnableOps
  { SAVE, RESTORE, DISABLE }
  
  public enum Mode
  {
    ASM, DISASM
  }
  
  public class RMFormattedTextField extends JFormattedTextField implements KeyListener
  {
    private Format fmt;
    private DefaultFormatter ff;
    private String lastText = "";
    private ActionListener al = null;

    public RMFormattedTextField( Format fmt, ActionListener al )
    {
      super( fmt );
      this.fmt = fmt;
      this.al = al;
      addKeyListener( this );
      setFocusLostBehavior( JFormattedTextField.COMMIT );
    }

    public RMFormattedTextField( DefaultFormatter ff, ActionListener al )
    {
      super( ff );
      this.ff = ff;
      this.al = al;
      addKeyListener( this );
      setFocusLostBehavior( JFormattedTextField.COMMIT );
    }

    @Override
    protected void processFocusEvent( FocusEvent e )
    {
      super.processFocusEvent( e );
      if ( e.getID() == FocusEvent.FOCUS_GAINED )
      {
        selectAll();
      }
      else if ( e.getID() == FocusEvent.FOCUS_LOST )
      {
        endEdit();
      }
    }

    protected void setValue( String text )
    {
      try
      {
        Object obj = ( fmt == null ) ? ff.stringToValue( text ) : fmt.parseObject( text );
        lastText = obj == null ? "" : obj.toString();
        setText( lastText );
        commitEdit();
        if ( al != null && al == pfpdListener )
          update();
      }
      catch ( ParseException e1 )
      {
        e1.printStackTrace();
      }
    }

    protected void update()
    {
      al.actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, "" ) );
    }
    
    private void endEdit()
    {
      if ( !isEditValid() )
      {
        showWarning( getText() + " : Invalid value" );
        setText( getValue() == null ? "" : getValue().toString() );
      }
      else
        try
        {
          commitEdit();
        }
        catch ( ParseException ex )
        {
          ex.printStackTrace();
        }
      if ( !getText().equals( lastText ) )
      {
        lastText = getText();
        update();
      }
    }

    private void showWarning( String message )
    {
      JOptionPane.showMessageDialog( this, message, "Invalid Value", JOptionPane.ERROR_MESSAGE );
    }

    @Override
    public void keyPressed( KeyEvent e )
    {
      int key = e.getKeyCode();
      if ( key == KeyEvent.VK_ENTER ) 
      {
        endEdit();
      }
    }

    @Override
    public void keyReleased( KeyEvent e ){}

    @Override
    public void keyTyped( KeyEvent e ){}
    
  }
  
  /**
   * A subclass of NumberFormatter that allows values to be null.
   */
  private static class RMNumberFormatter extends NumberFormatter
  {
    RMNumberFormatter( NumberFormat nf )
    {
      super( nf );
    }

    @Override
    public Object stringToValue( String string ) throws ParseException
    {
      if ( string == null || string.isEmpty() )
      {
        setEditValid( true );
        return null;
      }
      else
        return super.stringToValue( string );
    }

    @Override
    public String valueToString( Object value ) throws ParseException
    {
      if ( value == null )
        return "";
      else
        return super.valueToString( value );
    }
  }
  
  public static class DisplayArea extends JTextArea
  {
    public DisplayArea( String text, List< JTextArea > areas )
    {
      super( text );
      JLabel label = new JLabel();
      setLineWrap( true );
      setWrapStyleWord( true );
      setFont( label.getFont() );
      setBackground( label.getBackground() );
      setEditable( false );
      if ( areas != null )
      {
        areas.add( this );
      }
    }
  }
  
  public class PFMainPanel extends JPanel implements ActionListener, ItemListener
  {
    public PFMainPanel()
    {
      setLayout( new BorderLayout() );
      pfPanel = new JPanel();
      pfScrollPane = new JScrollPane( pfPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
      pfScrollPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      add( pfScrollPane, BorderLayout.CENTER );

      JPanel headerPanel = new JPanel( new BorderLayout() );
      headerPanel.setBorder( BorderFactory.createLineBorder( Color.GRAY ) );
      JPanel pfChoice = new JPanel( new GridLayout( 1, CommonData.pfData.length ) );
      headerPanel.add( pfChoice, BorderLayout.PAGE_START );
      String text = "Bits per byte, current protocol values selected";
      headerPanel.add( new JLabel( text, SwingConstants.CENTER ), BorderLayout.PAGE_END );
      add( headerPanel, BorderLayout.PAGE_START );
      ButtonGroup grp = new ButtonGroup();
      pfButtons = new JRadioButton[ CommonData.pfData.length ];
      pfBoxes = new JComboBox[ CommonData.pfData.length ][];
      pfValues = new Short[ CommonData.pfData.length ];
      for ( int i = 0; i < pfButtons.length; i++ )
      {
        pfButtons[ i ] = new JRadioButton( "PF" + i, false );
        pfButtons[ i ].addItemListener( this );
        pfChoice.add( pfButtons[ i ] );
        grp.add( pfButtons[ i ] );
        pfBoxes[ i ] = new JComboBox[ CommonData.pfData[ i ].length ];
        boxCount += pfBoxes[ i ].length;
      }
      enableStates = new boolean[ boxCount ];
      Arrays.fill( enableStates, true );
      
    }
    
    public void set()
    {
      if ( !isActive )
      {
        return;
      }
      doBoxEnableStates( EnableOps.RESTORE );
      int n = -1;
      for ( int i = 0; i < pfButtons.length; i++ )
      {
        pfButtons[ i ].setEnabled( pfValues[ i ] != null );
        if ( pfButtons[ i ].isSelected() )
          n = i;
      }
      if ( n < 0 )
      {
        n = 0;
        pfButtons[ 0 ].setSelected( true );
      }
      List< JTextArea > areas = new ArrayList< JTextArea >();
      double size[][] =
      {
          {
             ProtocolDataPanel.this.getWidth( "0-0__" ), TableLayout.FILL
          }, null
      };
      setTableLayout( size, CommonData.pfData[ n ], true );
      pfPanel.setLayout( new TableLayout( size ) );

      int bitPos = 0;
      int row = 1;
      int m = 0;
      isSettingPF = true;
      for ( String[] data : CommonData.pfData[ n ] )
      {
        DisplayArea label = new DisplayArea( data[ 1 ], areas );
        label.setFocusable( false );
        pfPanel.add( label, "0, " + row + ", l, t" );

        DisplayArea area = new DisplayArea( data[ 2 ], areas );
        area.setFocusable( false );
        pfPanel.add( area, "1, " + row++ );

        JComboBox combo = new JComboBox();
        pfBoxes[ n ][ m++ ] = combo;
        combo.addActionListener( this );
        String text = data[ 3 ];
        while ( true )
        {
          int pos = text.indexOf( "\n" );
          if ( pos >= 0 )
          {
            combo.addItem( text.substring( 0, pos ) );
            text = text.substring( pos + 1 );
          }
          else
          {
            combo.addItem( text.substring( 0 ) );
            break;
          }
        }
        ;

        pfPanel.add( combo, "1, " + row++ );
        if ( data.length > 4 )
        {
          area = new DisplayArea( data[ 4 ], areas );
          area.setFocusable( false );
          pfPanel.add( area, "1, " + row++ );
        }

        if ( pfValues[ n ] != null )
        {
          int len = Integer.parseInt( data[ 0 ] );
          int val = ( pfValues[ n ] >> bitPos ) & ( ( 1 << len ) - 1 );
          bitPos += len;
          for ( int i = 0;; i++ )
          {
            text = ( String )combo.getModel().getElementAt( i );
            if ( text.startsWith( "" + val + " =" ) || i == combo.getModel().getSize() - 1 /* other */)
            {
              combo.setSelectedIndex( i );
              break;
            }
          }
        }
        row++ ;
      }
      isSettingPF = false;
      pfPanel.validate();    
      doBoxEnableStates( EnableOps.SAVE );
      if ( mode == Mode.DISASM )
      {
        doBoxEnableStates( EnableOps.DISABLE );
      }
      for ( JTextArea area : areas )
      {
        Dimension d = area.getPreferredSize();
        d.width = 100;
        area.setPreferredSize( d );
      }
      javax.swing.SwingUtilities.invokeLater( new Runnable()
      {
        public void run()
        {
          pfScrollPane.getVerticalScrollBar().setValue( 0 );
        }
      } );
    }
    
    public void setPFData( Processor proc, Hex hex )
    {
      int pfCount = assemblerModel.getPfCount();
      short[] data = hex.getData();
      Arrays.fill( basicValues, null );
      Arrays.fill( pfValues, null );
      Arrays.fill( pdValues, null );
      for ( int i = 0; i < proc.getStartOffset(); i++ )
      {
        basicValues[ i ] = data[ i ]; 
      }
      for ( int i = proc.getStartOffset(); i < 3; i++ )
      {
        basicValues[ i ] = data[ i + 2 ];
      }
      for ( int i = 0; i < pfCount; i++ )
      {
        pfValues[ i ] = data[ 5 + i ];
      }
      for ( int i = 0; i < assemblerModel.getPdCount(); i++ )
      {
        pdValues[ i ] = data[ 5 + pfCount + i ];
      }
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
      if ( !isActive )
      {
        return;
      }
      Object source = e.getSource();
      int m = -1;
      int n = 0;
      for ( ; n < pfButtons.length; n++ )
        if ( pfButtons[ n ].isSelected() )
          break;
      if ( n < pfButtons.length )
        m = Arrays.asList( pfBoxes[ n ] ).indexOf( source );
      if ( m >= 0 && !isSettingPF )
      {
        int bitStart = Integer.parseInt( CommonData.pfData[ n ][ m ][ 1 ].substring( 0, 1 ) );
        int bitCount = Integer.parseInt( CommonData.pfData[ n ][ m ][ 0 ] );
        int mask = ~( ( ( 1 << bitCount ) - 1 ) << bitStart );
        int val = 0;
        JComboBox combo = pfBoxes[ n ][ m ];
        String text = ( String )combo.getSelectedItem();
        if ( Character.isDigit( text.charAt( 0 ) ) )
        {
          val = Integer.parseInt( text.substring( 0, 1 ) );
        }
        else
        // Handle "other"
        {
          for ( ; val < combo.getItemCount() - 1; val++ )
          {
            text = ( String )combo.getModel().getElementAt( val );
            if ( val != Integer.parseInt( text.substring( 0, 1 ) ) )
              break;
          }
        }
        pfValues[ n ] = ( short )( ( pfValues[ n ] & mask ) | ( val << bitStart ) );
        if ( bitStart == 7 )
        {
          isSettingPF = true;
          if ( val == 0 )
            for ( int i = n + 1; i < pfButtons.length; i++ )
            {
              pfButtons[ i ].setEnabled( false );
              pfValues[ i ] = null;
            }
          else
          {
            pfButtons[ n + 1 ].setEnabled( true );
            pfValues[ n + 1 ] = 0;
          }
          isSettingPF = false;
        }
      }
    }

    @Override
    public void itemStateChanged( ItemEvent e )
    {
      Object source = e.getSource();
      int n = 0;
      if ( ( n = Arrays.asList( pfButtons ).indexOf( source ) ) >= 0 )
      {
        if ( pfButtons[ n ].isSelected() )
          set();
        return;
      }
    }
    
    public boolean isActive()
    {
      return isActive;
    }

    public void setActive( boolean isActive )
    {
      this.isActive = isActive;
    }

    private JPanel pfPanel = null;
    private JScrollPane pfScrollPane = null;
    private JRadioButton[] pfButtons = null;
    private boolean isActive = false;
  }
  
  public class PDMainPanel extends JPanel implements ActionListener
  {
    public PDMainPanel()
    {
      setLayout( new BorderLayout() );
      pdPanel = new JPanel();
      pdScrollPane = new JScrollPane( pdPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
      pdScrollPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      add( pdScrollPane, BorderLayout.CENTER );
      pdHeaderPanel = new JPanel( new BorderLayout() );
      add( pdHeaderPanel, BorderLayout.PAGE_START );
      int n = 0;
      for ( int i = 0; i < CommonData.pdData.length; i++ )
      {
        if ( CommonData.pdData[ i ][ 0 ] != null )
          n += Integer.parseInt( CommonData.pdData[ i ][ 0 ] );
      }
      pdValues = new Short[ n ];
      pdFields = new ArrayList< RMFormattedTextField >();
      pdSizes = new ArrayList< int[] >();
    }
    
    public void set()
    {
      if ( !isActive )
      {
        return;
      }
      doBoxEnableStates( EnableOps.RESTORE );
      List< JTextArea > areas = new ArrayList< JTextArea >();
      double size[][] =
      {
          {
              ProtocolDataPanel.this.getWidth( "PD00/PD00__" ), 
              ProtocolDataPanel.this.getWidth( "$" ), 
              ProtocolDataPanel.this.getWidth( "FF_FF_" ), 
              ProtocolDataPanel.this.getWidth( " -> " ),
              ProtocolDataPanel.this.getWidth( "999999_" ), 
              TableLayout.FILL
          }, null
      };
      setTableLayout( size, CommonData.pdData, true );
      pdPanel.setLayout( new TableLayout( size ) );
      RMFormattedTextField tf = null;
      pdFields.clear();
      pdSizes.clear();

      int pdNum = 0;
      int pdLastNum = 0;
      int row = 1;
      for ( String[] data : CommonData.pdData )
      {
        int n = data[ 0 ] == null ? -1 : Integer.parseInt( data[ 0 ] );
        n = ( n == -1 ) ? 0 : ( n == 0 ) ? -1 : n;
        int type = Integer.parseInt( data[ 1 ] );
        int pdNdx = ( n > 0 ) ? pdNum : pdLastNum;
        String text = "";
        for ( int i = 0; i < n; i++ )
        {
          if ( i > 0 )
            text += "/";
          text += String.format( "PD%02X", pdNum + i );
        }

        if ( n > 0 )
        {
          DisplayArea label = new DisplayArea( text, areas );
          label.setFocusable( false );
          pdPanel.add( label, "0, " + row + ", l, t" );
        }

        DisplayArea area = new DisplayArea( data[ 2 ], areas );
        area.setFocusable( false );
        if ( n >= 0 )
        {
          pdPanel.add( area, "1, " + row + ", 5, " + row );
          JLabel label = new JLabel( "$" );
          label.setFocusable( false );
          pdPanel.add( new JLabel( "$" ), "1, " + ++row );

          tf = new RMFormattedTextField( new HexFormat( -1, type == 1 ? 1 : 2 ), this );
          pdFields.add( tf );
          pdPanel.add( tf, "2, " + row );
          for ( int i = 3; i < data.length; i++ )
          {
            label = new JLabel( " -> ", SwingConstants.CENTER );
            label.setFocusable( false );
            pdPanel.add( label, "3, " + row );
            NumberFormat nf = NumberFormat.getInstance();
            nf.setGroupingUsed( false );
            nf.setParseIntegerOnly( type != 4 );
            tf = new RMFormattedTextField( new RMNumberFormatter( nf ), this );
            pdFields.add( tf );
            pdPanel.add( tf, "4, " + row );
            label = new JLabel( "  " + data[ i ] );
            label.setFocusable( false );
            pdPanel.add( label, "5, " + row++ );
          }

          int val = 0;
          int index = pdSizes.size();
          pdSizes.add( new int[]
          {
              pdNdx, type == 1 ? 1 : 2
          } );
          pdSizes.add( new int[]
          {
              pdNdx, 0x10 + type
          } );
          if ( type == 4 )
            pdSizes.add( new int[]
            {
                pdNdx, 0x24
            } );
          if ( pdValues[ pdNdx ] != null )
          {
            if ( type > 1 && pdValues[ pdNdx + 1 ] != null )
            {
              val = pdValues[ pdNdx ] * 0x100 + pdValues[ pdNdx + 1 ];
              pdFields.get( index ).setValue( String.format( "%04X", val ) );
            }
            else
            {
              pdFields.get( index ).setValue( String.format( "%02X", pdValues[ pdNdx ] ) );
            }
            pdFields.get( index ).update();
          }
          else
          {
            pdFields.get( index ).setValue( "" );
          }
          row++ ;
          if ( n > 0 )
            pdLastNum = pdNum;
          pdNum += n;
        }
        else
        {
          pdHeaderPanel.removeAll();
          pdHeaderPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
          pdHeaderPanel.add( area, BorderLayout.CENTER );
        }
      }
      validate();
      doBoxEnableStates( EnableOps.SAVE );
      if ( mode == Mode.DISASM )
      {
        doBoxEnableStates( EnableOps.DISABLE );
      }
      for ( JTextArea area : areas )
      {
        Dimension d = area.getPreferredSize();
        d.width = 100;
        area.setPreferredSize( d );
        area.setMinimumSize( new Dimension( 10, 10 ) );
      }
      javax.swing.SwingUtilities.invokeLater( new Runnable()
      {
        public void run()
        {
          pdScrollPane.getVerticalScrollBar().setValue( 0 );
        }
      } );
    }
    
    @Override
    public void actionPerformed( ActionEvent e )
    {
      if ( !isActive )
      {
        return;
      }
      
      Object source = e.getSource();
      int n = pdFields.indexOf( source );
      if ( n >= 0 )
      {
        int index = pdSizes.get( n )[ 0 ];
        if ( pdSizes.get( n )[ 1 ] >> 4 == 0 )
        {
          Hex hex = ( Hex )pdFields.get( n ).getValue();
          Arrays.fill( pdValues, index, index + pdSizes.get( n )[ 1 ], null );
          for ( int i = 0; i < hex.length(); i++ )
            pdValues[ index + i ] = hex.getData()[ i ];

          int p = 0;
          for ( ; pdSizes.get( p )[ 0 ] != index; p++ )
            ;
          for ( ; p < pdSizes.size() && pdSizes.get( p )[ 0 ] == index; p++ )
            if ( pdSizes.get( p )[ 1 ] >> 4 == 0 )
            {
              Short val1 = pdValues[ index ];
              Short val2 = pdValues[ index + 1 ];
              int type = pdSizes.get( p )[ 1 ];
              if ( p != n )
                pdFields.get( p ).setValue(
                    ( type == 2 && val2 != null ) ? String.format( "%04X", val1 * 0x100 + val2 )
                        : ( val1 != null ) ? String.format( "%02X", val1 ) : "" );
              hex = ( Hex )pdFields.get( p ).getValue();
              type = pdSizes.get( p + 1 )[ 1 ] & 0xF;
              if ( type == 1 && hex.length() == 1 )
              {
                pdFields.get( p + 1 ).setValue( "" + hex.getData()[ 0 ] );
              }
              else if ( type == 4 && hex.length() >= 1 )
              {
                double m = ( dataStyle == 0 ) ? 2.0 : 0.0;
                double d = ( dataStyle == 0 ) ? 8.0 : 4.0;
                pdFields.get( p + 1 ).setValue( "" + ( hex.getData()[ 0 ] + m ) / d );
                if ( hex.length() > 1 )
                  pdFields.get( p + 2 ).setValue( "" + ( hex.getData()[ 1 ] + m ) / d );
              }
              else if ( hex.length() == 2 )
              {
                int time = 2 * hex.get( 0 ) + ( ( type == 3 && dataStyle == 0 ) ? 40 : 0 );
                pdFields.get( p + 1 ).setValue( "" + time );
              }
              else
                for ( int i = 1; p + i < pdSizes.size() && pdSizes.get( p + i )[ 1 ] >> 4 == i; i++ )
                {
                  pdFields.get( p + i ).setValue( "" );
                }
            }
        }
        else
        {
          int type = pdSizes.get( n )[ 1 ] & 0xF;
          int pos = pdSizes.get( n )[ 1 ] >> 4;
          Object obj = pdFields.get( n ).getValue();
          RMFormattedTextField pdField = pdFields.get( n - pos );
          if ( obj == null )
          {
            Hex hex = ( Hex )pdField.getValue();
            pdField.setValue( hex.subHex( 0, Math.min( hex.length(), pos - 1 ) ).toString() );
          }
          else if ( type == 1 )
          {
            pdField.setValue( String.format( "%02X", ( Long )obj & 0xFF ) );
          }
          else if ( type == 4 )
          {
            Hex hex = ( Hex )pdFields.get( n - pos ).getValue();
            double val = ( obj instanceof Long ) ? ( Long )obj : ( Double )obj;
            val *= ( dataStyle == 0 ) ? 8.0 : 4.0;
            val -= ( dataStyle == 0 ) ? 2.0 : 0.0;
            hex = new Hex( hex, 0, Math.max( hex.length(), pos ) );
            hex.set( ( short )( ( int )( val + 0.5 ) & 0xFF ), pos - 1 );
            pdField.setValue( hex.toString() );
          }
          else
          // types 2, 3
          {
            long val = ( Long )obj;
            val -= ( type == 3 && dataStyle == 0 ) ? 40 : 0;
            val /= 2;
            pdField.setValue( String.format( "%04X", val & 0xFFFF ) );
          }
          pdField.update();
        }
      }
    }
    
    public boolean isActive()
    {
      return isActive;
    }

    public void setActive( boolean isActive )
    {
      this.isActive = isActive;
    }

    private JPanel pdHeaderPanel = null;
    private JPanel pdPanel = null;
    private JScrollPane pdScrollPane = null;
    private List< int[] > pdSizes = null;
    private boolean isActive = false;
   
  }
  
  public class FunctionMainPanel extends JPanel
  {
    public FunctionMainPanel()
    {
      setLayout( new BorderLayout() );
      fnPanel = new JPanel();
      fnScrollPane = new JScrollPane( fnPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
      fnScrollPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
      add( fnScrollPane, BorderLayout.CENTER );
      fnHeaderPanel = new JPanel( new BorderLayout() );
      add( fnHeaderPanel, BorderLayout.PAGE_START );
    }
    
    public void set()
    {
      if ( !isActive )
      {
        return;
      }
      List< JTextArea > areas = new ArrayList< JTextArea >();
      int n = 0;
      for ( String[] data : CommonData.fnData )
      {
        if ( processor.getZeroAddresses().keySet().contains( data[ 0 ] )
            || processor.getAbsAddresses().keySet().contains( data[ 0 ] ) || data[ 0 ].equals( "" ) )
        {
          n++ ;
        }
      }
      String functions[][] = new String[ n ][ 3 ];
      n = 0;
      for ( String[] data : CommonData.fnData )
      {
        Integer address = processor.getZeroAddresses().get( data[ 0 ] );
        if ( data[ 0 ].equals( "" ) )
        {
          functions[ n ][ 0 ] = "0";
        }
        else if ( address != null )
        {
          functions[ n ][ 0 ] = String.format( "%02X    ", address );
        }
        else
        {
          address = processor.getAbsAddresses().get( data[ 0 ] );
          if ( address != null )
          {
            functions[ n ][ 0 ] = String.format( "%04X", address );
          }
          else
            continue;
        }
        functions[ n ][ 1 ] = data[ 0 ];
        functions[ n++ ][ 2 ] = data[ 1 ];
      }

      Arrays.sort( functions, new Comparator< String[] >()
      {
        @Override
        public int compare( String[] o1, String[] o2 )
        {
          int n1 = Integer.parseInt( o1[ 0 ].trim(), 16 );
          int n2 = Integer.parseInt( o2[ 0 ].trim(), 16 );
          if ( ( zeroUsed.contains( n1 ) || absUsed.contains( n1 ) )
              && !( zeroUsed.contains( n2 ) || absUsed.contains( n2 ) ) )
          {
            return -1;
          }
          else if ( !( zeroUsed.contains( n1 ) || absUsed.contains( n1 ) )
              && ( zeroUsed.contains( n2 ) || absUsed.contains( n2 ) ) )
          {
            return 1;
          }
          else
            return n1 - n2;
        }
      } );

      double size[][] =
        {
          {
            ProtocolDataPanel.this.getWidth( "$FFFF_*_" ), 
            TableLayout.FILL
          }, null
        };
      setTableLayout( size, functions, true );
      fnPanel.setLayout( new TableLayout( size ) );

      int row = 1;
      for ( String[] fn : functions )
      {
        String text = fn[ 0 ];
        n = Integer.parseInt( fn[ 0 ].trim(), 16 );
        if ( dataStyle > 0 || n < 0x100 )
        {
          text = ( dataStyle == 0 ? "R" : "$" ) + text;
        }
        else
        {
          text += "H";
        }

        if ( zeroUsed.contains( n ) || absUsed.contains( n ) )
        {
          text += " * ";
        }

        DisplayArea label = new DisplayArea( text, areas );

        text = fn[ 1 ] + ( fn[ 1 ].equals( "" ) ? "" : "\n" ) + fn[ 2 ];
        DisplayArea area = new DisplayArea( text, areas );
        if ( fn[ 1 ].equals( "" ) )
        {
          fnHeaderPanel.removeAll();
          fnHeaderPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
          fnHeaderPanel.add( area, BorderLayout.CENTER );
        }
        else
        {
          fnPanel.add( label, "0, " + row + ", l, t" );
          fnPanel.add( area, "1, " + row );
          row += 2;
        }
      }

      validate();
      for ( JTextArea area : areas )
      {
        Dimension d = area.getPreferredSize();
        d.width = 100;
        area.setPreferredSize( d );
        area.setMinimumSize( new Dimension( 10, 10 ) );
      }
      javax.swing.SwingUtilities.invokeLater( new Runnable()
      {
        public void run()
        {
          fnScrollPane.getVerticalScrollBar().setValue( 0 );
        }
      } );
    }
    
    public void setAbsUsed( List< Integer > absUsed )
    {
      this.absUsed = absUsed;
    }

    public void setZeroUsed( List< Integer > zeroUsed )
    {
      this.zeroUsed = zeroUsed;
    }

    public void setActive( boolean isActive )
    {
      this.isActive = isActive;
    }

    private JPanel fnHeaderPanel = null;
    private JPanel fnPanel = null;
    private JScrollPane fnScrollPane = null;
    private List< Integer > absUsed = null;
    private List< Integer > zeroUsed = null;
    private boolean isActive = false;
  }
  
  public ProtocolDataPanel( Component owner )
  {
    double b = 5; // space between rows and around border
    double c = 10; // space between columns
    double pr = TableLayout.PREFERRED;
    double pf = TableLayout.FILL;
    
    double size3[][] =
      {
          {
              b, pr, c, pf, b, pr, b
          }, // cols
          null
      // rows set later
      };

    setTableLayout( size3, dataLabels, false );
    setLayout( new TableLayout( size3 ) );
    prefSize = new JScrollPane( this ).getPreferredSize();
    
    basicValues = new Short[ 3 ];
    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed( false );
    frequency = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    dutyCycle = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    altFreq = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    altDuty = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );

    nf.setParseIntegerOnly( true );
    rptValue = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    burst1On = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    burst1Off = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    burst0On = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    burst0Off = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    afterBits = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    leadInOn = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    leadInOff = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    leadOutOff = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );
    altLeadOut = new RMFormattedTextField( new RMNumberFormatter( ( NumberFormat )nf.clone() ), pfpdListener );

    dataComponents = new Component[][]{ 
        { frequency }, { dutyCycle }, { sigStruct }, null,
        { devBytes }, { devBits1, devBits1lbl }, { devBits2, devBits2lbl }, { devBitDbl }, null,
        { cmdBytes }, { cmdBits1, cmdBits1lbl }, { cmdBits2, cmdBits2lbl }, { cmdBitDbl }, null,
        { rptType }, { rptHold }, { rptValue, rptValueLbl }, null,
        { burst1On }, { burst1Off }, null,
        { burst0On }, { burst0Off }, { xmit0rev }, null,
        { leadInStyle }, { leadInOn, leadInOnLbl }, { leadInOff, leadInOffLbl }, null,
        { leadOutStyle }, { leadOutOff }, { offAsTotal, offAsTotalLbl }, null,
        { useAltLeadOut }, { altLeadOut, altLeadOutLbl }, null,
        { useAltFreq }, { altFreq, altFreqLbl }, { altDuty, altDutyLbl }, null, null, null,
        { burstMidFrame, burstMidFrameLbl }, { afterBits, afterBitsLbl }
//        { chkByteStyle }, { bitsHeld }, null,
//        { miniCombiner }, { sigStyle }, null,
//        { vecOffset }, { dataOffset }, null,
//        { toggleBit }
    };
    
    boxCount = 0;
    JLabel label = null;
    for ( int i = 0; i < dataComponents.length; i++ )
    {
      if ( dataComponents[ i ] == null )
      {
        if ( dataLabels[ i ] == null )
          continue;
        label = new JLabel( dataLabels[ i ][ 0 ], SwingConstants.CENTER );
        label.setFocusable( false );
        add( label, "1, " + ( i + 1 ) + ", 5, " + ( i + 1 ) );
        continue;
      }
      if ( dataComponents[ i ].length > 1 )
      {
        label = ( JLabel )dataComponents[ i ][ 1 ];
      }
      else
      {
        label = new JLabel();
      }
      boxCount++;
      label.setFocusable( false );
      label.setText( dataLabels[ i ][ 0 ] );
      add( label, "1, " + ( i + 1 ) );
      add( dataComponents[ i ][ 0 ], "3, " + ( i + 1 ) );
      if ( dataLabels[ i ].length > 1 )
      {
        label = new JLabel( dataLabels[ i ][ 1 ] );
        label.setFocusable( false );
        add( label, "5, " + ( i + 1 ) );
      }
    }
    if ( owner instanceof ManualSettingsPanel )
    {
      assemblerModel = ( ( ManualSettingsPanel )owner ).getAssemblerModel();
    }
    
    pfMainPanel = new PFMainPanel();
    pdMainPanel = new PDMainPanel();
    fnMainPanel = new FunctionMainPanel();
  }
  
  public static void setTableLayout( double[][] size, String[][] data, boolean interleave )
  {
    double b = 5; // space between rows and around border
    double c = 10; // space between columns
    List< Double > rows = new ArrayList< Double >();
    rows.add( b );
    for ( int i = 0; i < data.length; i++ )
    {
      if ( data[ i ] != null && data[ i ][ 0 ] != null && data[ i ][ 0 ].equals( "0" ) )
        continue;
      if ( interleave )
      {
        for ( int j = 2; j < Math.max( data[ i ].length, 3 ); j++ )
          rows.add( TableLayout.PREFERRED );
      }
      else
      {
        rows.add( data[ i ] == null ? c : TableLayout.PREFERRED );
      }
      if ( i == data.length - 1 && data[ i ] != null || interleave )
        rows.add( b );
    }
    size[ 1 ] = new double[ rows.size() ];
    for ( int i = 0; i < rows.size(); i++ )
    {
      size[ 1 ][ i ] = rows.get( i );
    }
  }

  
  private ActionListener pfpdListener = new ActionListener()
  {
    @Override
    public void actionPerformed( ActionEvent e )
    {
      Object source = e.getSource();
      if ( source == frequency || source == dutyCycle )
      {
        Hex.put( getCarrierData( frequency.getValue(), dutyCycle.getValue() ), basicValues, 0 );
        if ( dataStyle > 2 && source == frequency )
        {
          if ( basicValues[ 0 ] == 0 ) setPFbits( 2, 16, 2, 5 );
        }
      }
      else if ( source == devBytes )
      {
        int val = devBytes.getSelectedIndex();
        boolean is2 = val == 2;
        if ( dataStyle > 2 )
          is2 = is2 && ( ( pfValues[ 0 ] & 0x58 ) != 0x08 );
        devBits1lbl.setText( is2 ? "Bits/Dev1" : "Bits/Dev" );
        devBits2lbl.setVisible( is2 );
        if ( !is2 )
          devBits2.setSelectedIndex( -1 );
        devBits2.setEnabled( is2 );
        if ( !isSettingPF )
        {
          if ( dataStyle < 3 )
            setPFbits( 0, Math.min( val, 3 ), 0, 2 );
          if ( basicValues[ 2 ] == null ) basicValues[ 2 ] = 0;
          basicValues[ 2 ] = ( short )( ( basicValues[ 2 ] & 0x0F ) | ( devBytes.getSelectedIndex() << 4 ) );
          if ( !is2 )
            pdValues[ dataStyle < 2 ? 0x10 : dataStyle < 3 ? 0x0E : 0x0D ] = null;
        }
      }
      else if ( source == cmdBytes )
      {
        int val = cmdBytes.getSelectedIndex();
        boolean is2 = val == 2 && ( dataStyle < 3 );
        cmdBits1lbl.setText( is2 ? "Bits/Cmd1" : "Bits/Cmd" );
        cmdBits2lbl.setVisible( is2 );
        if ( !is2 )
          cmdBits2.setSelectedIndex( -1 );
        cmdBits2.setEnabled( is2 );
        if ( !isSettingPF )
        {
          if ( dataStyle < 3 )
            setPFbits( 0, Math.min( val, 3 ), 2, 2 );
          if ( basicValues[ 2 ] == null )
            basicValues[ 2 ] = 0;
          basicValues[ 2 ] = ( short )( ( basicValues[ 2 ] & 0xF0 ) | cmdBytes.getSelectedIndex() );
          if ( !is2 && dataStyle < 3 ) pdValues[ dataStyle < 2 ? 0x12 : 0x10 ] = null;
        }
      }
      else if ( source == leadInStyle )
      {
        int index = leadInStyle.getSelectedIndex();
        leadInOn.setEnabled( index > 0 );
        leadInOff.setEnabled( index > 0 );
        leadInOnLbl.setEnabled( index > 0 );
        leadInOffLbl.setEnabled( index > 0 );

        if ( isSettingPF )
          return;

        if ( index == 0 )
        {
          burstMidFrame.setSelectedIndex( 0 );
          leadInOn.setValue( "" );
          leadInOff.setValue( "" );
        }
        else
        {

          if ( leadInOn.getValue() == null ) leadInOn.setValue( dataStyle == 2 ? "4" : dataStyle > 2 ? "" + Math.max( burstUnit/1000, 5 ) : "0" );
          if ( leadInOff.getValue() == null ) leadInOff.setValue( "0" );
//          if ( burstMidFrame.getSelectedIndex() == -1 ) burstMidFrame.setSelectedIndex( 0 );

        }
       
        if ( dataStyle < 3 ) 
        {
          setPFbits( 1, index, 2, 2 );
          if ( index == 0 )
          {
            burstMidFrame.setSelectedIndex( 0 );
          }
//          else if ( burstMidFrame.getSelectedIndex() == -1 )
//          {
//            burstMidFrame.setSelectedIndex( 0 );
//          }
        }
        
      }

      if ( dataStyle > 2 )
      {
        boolean rpt = rptType.getSelectedIndex() >= 0 && rptValue.getValue() != null && ( Long) rptValue.getValue() > 0;
        rptHold.setEnabled( !rpt );
        if ( rpt && rptType.getSelectedIndex() < rptHold.getModel().getSize() ) rptHold.setSelectedIndex( rptType.getSelectedIndex() );
        useAltLeadOut.setEnabled( true );
      }
      else
      {
        rptHold.setEnabled( true );
        boolean b = useAltFreq.getSelectedIndex() == 0 && burstMidFrame.getSelectedIndex() == 0;
        useAltLeadOut.setEnabled( b );
        altLeadOut.setEnabled( b );
        b = useAltLeadOut.getSelectedIndex() == 0 && burstMidFrame.getSelectedIndex() == 0;
        useAltFreq.setEnabled( b );
        altFreq.setEnabled( b );
        altDuty.setEnabled( b );
        b = useAltLeadOut.getSelectedIndex() == 0 && useAltFreq.getSelectedIndex() == 0 && leadInStyle.getSelectedIndex() > 0;
        burstMidFrame.setEnabled( b );
        afterBits.setEnabled( b );
      }
 
      if ( !isSettingPF )
      {
        if ( dataStyle > 2 )
        {
          boolean altLO = useAltLeadOut.getSelectedIndex() == 1 && altLeadOut.getValue() != null && ( Long )altLeadOut.getValue() > 0;
          setPFbits( 1, ( altLO || rptHold.getSelectedIndex() == 1 ) ? 1 : 0, 1, 1 );
          setPFbits( 1, ( altLO || leadInStyle.getSelectedIndex() == 3 ) ? 1 : 0, 2, 1 );
          setPFbits( 1, leadInStyle.getSelectedIndex() > 0 ? 1 : 0, 4, 1 );
          
          Long val = ( Long )rptValue.getValue();
          if ( rptType.getSelectedIndex() == 1 ) assemblerModel.setForcedRptCount( 0 );
          else assemblerModel.setForcedRptCount( val == null ? 0 : ( short )( long )val );

          Long t = ( Long )burst1On.getValue();
          if ( t == null ) t = ( Long )burst0On.getValue();
          setONtime34( t, 0, null );
          if ( burst0On.getValue() == null || burst1On.getValue() == null || testONtime34( ( Long )burst0On.getValue() ) == testONtime34( ( Long )burst1On.getValue() ) )
          {
            setPFbits( 2, 0, 3, 1 );  // signifies burst 0 and 1 on-times equal
            if ( leadInStyle.getSelectedIndex() == 3 )
            {
              if ( useAltLeadOut.getSelectedIndex() == 0 )
              {
                // set pd0E to lead-out off-time to indicate not using alternate lead-out
                t = ( Long )leadOutOff.getValue();
                if ( dataStyle == 3 ) setOFFtime34( t, 0x0E, CommonData.leadinOFFoffsets34, dataStyle );
                if ( dataStyle == 4 ) Hex.put( t == null ? null : ( int )( t / 4 + 10 ), pdValues, 0x0E );
                // set pd10 to half lead-in off-time
                t = ( Long )leadInOff.getValue();
                setOFFtime34( t == null ? null : ( long )( t / 2 ), 0x10, CommonData.burstOFFoffsets34, dataStyle );
              }
              else
              {
                errorMessage( 2 );
              }
            }
            else // leadInStyle < 3
            {
              Hex.put(  null, pdValues, 0x10 );
              if ( useAltLeadOut.getSelectedIndex() == 1 )
              {
                t = ( Long )altLeadOut.getValue();
                if ( dataStyle == 3 ) setOFFtime34( t, 0x0E, CommonData.leadinOFFoffsets34, dataStyle );
                if ( dataStyle == 4 ) Hex.put( t == null ? null : ( int )( t / 4 + 10 ), pdValues, 0x0E );
              }
              else
              {
                Hex.put(  null, pdValues, 0x0E );
              }
            }
          }
          else
          {
            if ( leadInStyle.getSelectedIndex() == 3 )
            {
              errorMessage( 0 );
            }
            else if ( useAltLeadOut.getSelectedIndex() == 1 )
            {
              errorMessage( 1 );
            }
            else
            {
              setPFbits( 2, 1, 3, 1 );
              t = ( Long )burst0On.getValue();
              setONtime34( t, 0x0E, null );
              pdValues[ 0x0F ] = null;
              Hex.put(  null, pdValues, 0x10 );
            }
          }
        }
        else  // dataStyle < 3
        {
          Long val = ( Long )rptValue.getValue();
          int n = ( dataStyle < 2 ) ? 0x11 : 0x0F;
          if ( rptType.getSelectedIndex() == 1 ) 
          {
            pdValues[ n ] = val == null ? 0 : ( short )( long )val;
            assemblerModel.setForcedRptCount( 0 );
          }
          else
          {
            pdValues[ n ] = null;
            assemblerModel.setForcedRptCount( val == null ? 0 : ( short )( long )val );
          }
        }
        if ( source == devBits1 )
        {
          short val = ( short )devBits1.getSelectedIndex();
          Short pdval = val == -1 ? null : val;
          if ( dataStyle < 3 )
            pdValues[ 0 ] = pdval;
          else if ( ( pfValues[ 0 ] & 0x58 ) != 0x08 )
            pdValues[ 1 ] = pdval;
          else
          {
            pdValues[ 0x0D ] = pdval;
            pdValues[ 1 ] = 0;
          }
        }
        else if ( source == cmdBits1 )
        {
          short val = ( short )cmdBits1.getSelectedIndex();
          Short pdval = val == -1 ? null : val;
          if ( dataStyle < 3 )
            pdValues[ 1 ] = pdval;
          else
            pdValues[ 2 ] = pdval;
        }
        else if ( source == devBits2 )
        {
          // code can generate this action, see source = sigStruct, hence need for test of isEnabled
          short val = ( short )devBits2.getSelectedIndex();
          if ( dataStyle < 3 )
            pdValues[ dataStyle < 2 ? 0x10 : 0x0E ] = val == -1 ? null : val;
          else if ( devBits2.isEnabled() )
            pdValues[ 0x0D ] = val == -1 ? null : val;
          else if ( ( pfValues[ 0 ] & 0x58 ) != 0x08 )
            pdValues[ 0x0D ] = null;
          // else pdValues[ 0x0D ] = ( ( pfValues[ 0 ] & 0x58 ) == 0x08 ) ? ( short )devBits1.getSelectedIndex() : null;
        }
        else if ( source == cmdBits2 )
        {
          if ( dataStyle < 3 )
          {
            short val = ( short )cmdBits2.getSelectedIndex();
            pdValues[ dataStyle < 2 ? 0x12 : 0x10 ] = val == -1 ? null : val;
          }
          // Disabled when dataStyle >= 3
        }
        else if ( source == sigStruct )
        {
          if ( dataStyle < 3 )
            setPFbits( 0, sigStruct.getSelectedIndex(), 4, 2 );
          else
          {
            String sig = ( String )sigStruct.getSelectedItem() + "-";
            String items[] =
            {
                "devs", "dev", "cmd", "!dev", "dev2", "cmd", "!cmd"
            };
            int key = 0;
            int p = 0;
            while ( true )
            {
              int n = sig.indexOf( '-' );
              if ( n < 0 )
                break;
              String item = sig.substring( 0, n );
              for ( ; p < items.length && !items[ p ].equals( item ); p++ )
                ;
              if ( p == items.length )
                break; // Should not occur
              key |= 1 << ( 6 - p );
              sig = sig.substring( n + 1 );
            }
            int val = 0;
            if ( ( key & 2 ) == 2 )
            {
              val |= 0x02;
              key ^= 0x12;
            }
            if ( ( key & 0x40 ) == 0x40 )
            {
              val |= 0x01;
              key ^= 0x60;
            }
            val |= ( ( key & 0x3C ) << 1 ) | ( key & 1 ) << 2;
            setPFbits( 0, val, 0, 7 );
            actionPerformed( new ActionEvent( devBytes, ActionEvent.ACTION_PERFORMED, "Internal" ) );
            actionPerformed( new ActionEvent( devBits1, ActionEvent.ACTION_PERFORMED, "Internal" ) );
            actionPerformed( new ActionEvent( devBits2, ActionEvent.ACTION_PERFORMED, "Internal" ) );
          }
        }
        else if ( source == devBitDbl )
        {
          if ( dataStyle < 3 )
            setPFbits( 2, devBitDbl.getSelectedIndex(), 0, 2 );
          else
          {
            setPFbits( 2, devBitDbl.getSelectedIndex(), 1, 1 );
            isSettingPF = true;
            cmdBitDbl.setSelectedIndex( devBitDbl.getSelectedIndex() );
            isSettingPF = false;
          }
        }
        else if ( source == cmdBitDbl )
        {
          if ( dataStyle < 3 )
            setPFbits( 2, cmdBitDbl.getSelectedIndex(), 2, 2 );
          else
          {
            setPFbits( 2, devBitDbl.getSelectedIndex(), 1, 1 ); // same as devBitDbl
            isSettingPF = true;
            devBitDbl.setSelectedIndex( cmdBitDbl.getSelectedIndex() );
            isSettingPF = false;
          }
        }
        else if ( source == rptType )
        {
          int index = rptType.getSelectedIndex();
          Long val = ( Long )rptValue.getValue();
          if ( !assemblerModel.testBuildMode( processor ) && val != null && val != 0 )
          {
            errorMessage( 3 );
          }
          if ( dataStyle > 2 ) return;
          
          setPFbits( 1, index, 4, 1 );
          int n = ( dataStyle < 2 ) ? 0x11 : 0x0F;
          pdValues[ n ] = ( index == 0 ) ? null : ( pdValues[ n ] == null || pdValues[ n ] == 0xFF ) ? 0 : pdValues[ n ];

        }
        else if ( source == rptValue )
        {
          int index = rptType.getSelectedIndex();
          if ( !assemblerModel.testBuildMode( processor ) && index != 1 )
          {
            errorMessage( 4 );
          }
        }
        else if ( source == rptHold )
        {
          if ( dataStyle < 3 ) setPFbits( 1, rptHold.getSelectedIndex(), 0, 2 );
        }
        else if ( source == xmit0rev )
        {
          if ( dataStyle < 3 )
            setPFbits( 2, xmit0rev.getSelectedIndex(), 4, 1 );
          else
            setPFbits( 2, xmit0rev.getSelectedIndex(), 2, 3 );
        }

        else if ( source == leadOutStyle )
        {
          if ( dataStyle < 3 )
            setPFbits( 1, leadOutStyle.getSelectedIndex(), 5, 2 );
          else
          {
            setPFbits( 1, leadOutStyle.getSelectedIndex() >> 1, 5, 1 );
            setPFbits( 1, 1 - offAsTotal.getSelectedIndex(), 6, 1 );
          }
        }
        else if ( source == offAsTotal )
        {
          if ( dataStyle < 3 )
            setPFbits( 0, offAsTotal.getSelectedIndex(), 6, 1 );
          if ( dataStyle == 4 )
          {
            setPFbits( 1, offAsTotal.getSelectedIndex(), 6, 1 );
            setPFbits( 2, offAsTotal.getSelectedIndex(), 0, 1 );
            actionPerformed( new ActionEvent( leadOutStyle, ActionEvent.ACTION_PERFORMED, "Internal" ) );
          }
        }
        else if ( source == burstMidFrame )
        {
          if ( dataStyle < 3 )
          {
            if ( burstMidFrame.getSelectedIndex() <= 0 )
            {
              afterBits.setValue( "" );
            }
            else if ( burstMidFrame.getSelectedIndex() == 1 && afterBits.getValue() == null )
            {
              afterBits.setValue( "0" );
            }
            assemblerModel.setMidFrameIndex( burstMidFrame.getSelectedIndex() );
          }
        }
        else if ( source == burst1On )
        {
          Long t = ( Long )burst1On.getValue();

          if ( dataStyle < 2 ) Hex.put( t == null ? null : ( int )( t / 2 ), pdValues, 2 );
          if ( dataStyle == 2 ) Hex.semiPut( t == null ? null : ( int )(( t / 4 + 255 ) % 256 ), pdValues, 2, 0 );
        }
        else if ( source == burst1Off )
        {
          Long t = ( Long )burst1Off.getValue();
          if ( dataStyle < 2 )
          {
            if ( t != null )
              t = Math.max( t - ( ( dataStyle == 0 ) ? 40 : 0 ), 0 );
            Hex.put( t == null ? null : ( int )( t / 2 ), pdValues, 4 );
          }
          if ( dataStyle == 2 )
            Hex.semiPut( t == null ? null : ( int )( t / 4 ), pdValues, 2, 1 );
          if ( dataStyle > 2 )
            setOFFtime34( t, 3, CommonData.burstOFFoffsets34, dataStyle );
        }
        else if ( source == burst0On )
        {
          Long t = ( Long )burst0On.getValue();
          if ( dataStyle < 2 ) Hex.put( t == null ? null : ( int )( t / 2 ), pdValues, 6 );
          if ( dataStyle == 2 ) Hex.semiPut( t == null ? null : ( int )(( t / 4 + 255 ) % 256 ), pdValues, 5, 0 );
        }
        else if ( source == burst0Off )
        {
          Long t = ( Long )burst0Off.getValue();
          if ( dataStyle < 2 )
          {
            if ( t != null )
              t = Math.max( t - ( ( dataStyle == 0 ) ? 40 : 0 ), 0 );
            Hex.put( t == null ? null : ( int )( t / 2 ), pdValues, 8 );
          }
          if ( dataStyle == 2 )
            Hex.semiPut( t == null ? null : ( int )( t / 4 ), pdValues, 5, 1 );
          if ( dataStyle > 2 )
            setOFFtime34( t, 5, CommonData.burstOFFoffsets34, dataStyle );
        }
        else if ( source == leadInOn )
        {
          Long t = ( Long )leadInOn.getValue();
          if ( dataStyle < 2 )
            Hex.put( t == null || leadInStyle.getSelectedIndex() == 0 ? null : ( int )( t / 2 ), pdValues, 0x0C );
          if ( dataStyle == 2 )
            Hex.semiPut( t == null || leadInStyle.getSelectedIndex() == 0 ? null : ( int )( ( t / 4 + 255 ) % 256 ),
                pdValues, 0x0B, 0 );
          if ( dataStyle > 2 )
            setONtime34( leadInStyle.getSelectedIndex() == 0 ? null : t, 9, 0x0C );

        }
        else if ( source == leadInOff )
        {
          Long t = ( Long )leadInOff.getValue();
          if ( dataStyle < 2 )
          {
            if ( t != null )
              t = Math.max( t - ( ( dataStyle == 0 ) ? 40 : 0 ), 0 );
            Hex.put( t == null || leadInStyle.getSelectedIndex() == 0 ? null : ( int )( t / 2 ), pdValues, 0x0E );
          }
          if ( dataStyle == 2 )
            Hex.semiPut( t == null || leadInStyle.getSelectedIndex() == 0 ? null : ( int )( t / 4 ), pdValues, 0x0B, 1 );
          if ( dataStyle > 2 )
          {
            isSettingPF = true;
            setOFFtime34( leadInStyle.getSelectedIndex() == 0 ? null : t, 0x0A, CommonData.leadinOFFoffsets34,
                dataStyle );
            actionPerformed( new ActionEvent( leadInStyle, ActionEvent.ACTION_PERFORMED, "Internal" ) );
            isSettingPF = false;
          }
        }
        else if ( source == leadOutOff )
        {
          Long t = ( Long )leadOutOff.getValue();
          if ( dataStyle < 2 )
            Hex.put( t == null ? null : ( int )( t / 2 ), pdValues, 0x0A );
          if ( dataStyle == 2 )
            Hex.put( t == null ? null : ( int )( t / 4 + 10 ), pdValues, 8 );
          if ( dataStyle == 3 )
            setOFFtime34( t, 7, CommonData.leadinOFFoffsets34, dataStyle );
          if ( dataStyle == 4 )
            Hex.put( t == null ? null : ( int )( t / 4 + 10 ), pdValues, 7 );
        }
        else if ( source == useAltLeadOut )
        {
          if ( dataStyle < 3 ) setPFbits( 3, useAltLeadOut.getSelectedIndex(), 5, 1 );
          if ( useAltLeadOut.getSelectedIndex() <= 0 ) altLeadOut.setValue( "" );
          if ( useAltLeadOut.getSelectedIndex() == 1 && altLeadOut.getValue() == null ) altLeadOut.setValue( "0" );
        }
        else if ( source == altLeadOut )
        {
          if ( useAltLeadOut.getSelectedIndex() == 1 || useAltLeadOut.getSelectedIndex() < 1
              && useAltFreq.getSelectedIndex() < 1 && burstMidFrame.getSelectedIndex() < 1 )
          {
            Long t = useAltLeadOut.getSelectedIndex() < 1 ? null : ( Long )altLeadOut.getValue();     
            if ( dataStyle < 2 ) Hex.put( t == null ? null : ( int )( t / 2 ), pdValues, 0x13 );
            if ( dataStyle == 2 ) Hex.put( t == null ? null : ( int )( t / 4 + 10 ), pdValues, 0x11 );
          }
        }
        else if ( source == useAltFreq )
        {
          if ( dataStyle > 2 ) return;
          setPFbits( 3, useAltFreq.getSelectedIndex(), 6, 1 );
          if ( useAltFreq.getSelectedIndex() <= 0 )
          {
            altFreq.setValue( "" );
            altDuty.setValue( "" );
          }
          else if ( altFreq.getValue() == null ) altFreq.setValue( "0" );
        }
        else if ( source == altFreq || source == altDuty )
        {
          if ( dataStyle > 2 ) return;
          if ( useAltFreq.getSelectedIndex() == 1 || useAltLeadOut.getSelectedIndex() < 1 && useAltFreq.getSelectedIndex() < 1 && burstMidFrame.getSelectedIndex() < 1 )
          {
            int ndx = dataStyle < 2 ? 0x13 : 0x11;
            Integer cd = useAltFreq.getSelectedIndex() < 1 ? null : useAltFreq.getSelectedIndex() <= 0 ? null
                : getCarrierData( altFreq.getValue(), altDuty.getValue() );
            if ( cd == null || cd == 0xFFFF )
            {
              pdValues[ ndx ] = null;
              pdValues[ ndx + 1 ] = null;
            }
            else
            {
              Hex.put( getCarrierData( altFreq.getValue(), altDuty.getValue() ), pdValues, dataStyle < 2 ? 0x13 : 0x11 );
            }
          }
        }
        else if ( source == afterBits )
        {
          Long val = ( Long )afterBits.getValue();
          if ( dataStyle < 3 && burstMidFrame.getSelectedIndex() == 1 )
          {
            pdValues[ 0x13 ] = ( val == null ) ? null : ( short )( long )( val + 1 );
          }
        }
      }
    }
  };
  
  private Integer getCarrierData( Object freq, Object duty )
  {
    if ( freq == null )
      return null;
    double f = ( freq instanceof Double ) ? ( Double )freq : ( Long )freq;
    if ( f == 0 || duty == null )
      return 0;
    double dc = ( duty instanceof Double ) ? ( Double )duty : ( Long )duty;
    burstUnit = ( int )( Math.round( 1000000 / f ) );
    int tot = ( int )( processor.getOscillatorFreq() / ( f * 1000 ) + 0.5 );
    int on = ( int )( dc * tot / 100 + 0.5 ) - processor.getCarrierOnOffset();
    return on * 0xFF + tot - processor.getCarrierTotalOffset();
  }
  
  private void setPFbits( int index, int value, int bitStart, int bitCount )
  {
    for ( int i = -1; i < index; i++ )
      if ( pfValues[ i + 1 ] == null )
      {
        if ( i >= 0 )
          pfValues[ i ] = ( short )( pfValues[ i ] | 0x80 );
        pfValues[ i + 1 ] = 0;
      }
    putPFbits( index, value, bitStart, bitCount );
    for ( int i = index; i > 0; i-- )
      if ( pfValues[ i ] == 0 )
      {
        pfValues[ i ] = null;
        pfValues[ i - 1 ] = ( short )( pfValues[ i - 1 ] & 0x7F );
      }
  }
  
  private void putPFbits( int index, int value, int bitStart, int bitCount )
  {
    int mask = ( ( 1 << bitCount ) - 1 ) << bitStart;
    pfValues[ index ] = ( short )( ( pfValues[ index ] & ~mask ) | ( ( value << bitStart ) & mask ) );
  }
  
  private void setOFFtime34( Long time, int pdIndex, int[] offsets, int dataStyle )
  {
    if ( time == null )
    {
      pdValues[ pdIndex ] = ns.isEmpty() ? null : ( short )1;
      ;
      pdValues[ pdIndex + 1 ] = ns.isEmpty() ? null : ( short )1;
      ;
    }
    else
    {
      double d = ( dataStyle == 3 ) ? 257 : 257.5;
      time = ( dataStyle == 3 ) ? ( time - offsets[ 0 ] ) / 3 : ( time - offsets[ 1 ] ) / 2;
      if ( time < 0 )
        time = ( long )0;
      int tHigh = ( int )( time / d );
      int tLow = ( int )( time - ( tHigh * d ) );
      tHigh = ( tHigh + 1 ) % 256;
      tLow = ( tLow + 1 ) % 256;
      pdValues[ pdIndex ] = ( short )tHigh;
      pdValues[ pdIndex + 1 ] = ( short )tLow;
    }
  }
  
  private void setONtime34( Long time, int pdIndex1, Integer pdIndex2 )
  {
    if ( time == null )
    {
      pdValues[ pdIndex1 ] = ns.isEmpty() ? null : ( short )1;
      if ( pdIndex2 != null )
        pdValues[ pdIndex2 ] = ns.isEmpty() ? null : ( short )1;
    }
    else if ( pfValues[ 2 ] != null && ( pfValues[ 2 ] & 0x7C ) == 0x40 )
    {
      time = Math.max( ( time - 2 ) / 3, 0 );
      pdValues[ pdIndex1 ] = ( short )( time % 256 );
    }
    else if ( burstUnit > 0 )
    {
      time = ( time * 1000 + burstUnit / 2 ) / burstUnit;
      if ( time == 0 ) time = 1L;
      time = ( time + 0xFFFF ) % 0x10000;
      int tHigh = ( ( int )( long )time / 256 + 1 ) % 256;
      int tLow = ( ( int )( long )time + 1 ) % 256;
      pdValues[ pdIndex1 ] = ( short )tLow;
      if ( pdIndex2 != null )
      {
        if ( tHigh == 1 )
        {
          pdValues[ pdIndex2 ] = null;
          setPFbits( 1, 0, 3, 1 );
        }
        else
        {
          pdValues[ pdIndex2 ] = ( short )tHigh;
          setPFbits( 1, 1, 3, 1 );
        }
      }
    }
    else
    {
      pdValues[ pdIndex1 ] = null;
      if ( pdIndex2 != null )
        pdValues[ pdIndex2 ] = null;
    }
  }
  
  private int testONtime34( Long time )
  {
    if ( time == null )
    {
      return -1;
    }
    else if ( pfValues[ 2 ] != null && ( pfValues[ 2 ] & 0x7C ) == 0x40 )
    {
      return ( int )Math.max( ( time - 2 ) / 3, 0 );

    }
    else if ( burstUnit > 0 )
    {
      return ( int )( ( time * 1000 + burstUnit/2 ) / burstUnit );
      
    }
    else
    {
      return -2;
    }
  }
  
  public void populateComboBox( Component component, Object[] array )
  {
    if ( !( component instanceof JComboBox ) )
    {
      return;
    }
    isSettingPF = true;
    JComboBox comboBox = ( JComboBox )component;
    ( ( DefaultComboBoxModel )comboBox.getModel() ).removeAllElements();
    if ( array == null )
    {
      isSettingPF = false;
      return;
    }
    for ( int i = 0; i < array.length; i++ )
    {
      comboBox.addItem( array[ i ] );
    }
    if ( comboBox.getActionListeners().length == 0 ) comboBox.addActionListener( pfpdListener );
    isSettingPF = false;
  }
  
  public void doBoxEnableStates( EnableOps op )
  {
    if( dataComponents == null ) 
    {
      return;
    }
    if ( op == EnableOps.DISABLE && nestLevel == 0 )
    {
      for ( int i = 0; i < dataComponents.length; i++ )
      {
        if ( dataComponents[ i ] == null ) continue;
        Component cpt = dataComponents[ i ][ 0 ];
        cpt.setEnabled( false );
      }
      for ( JComboBox[] cbArray : pfBoxes )
        for ( JComboBox cb : cbArray )
          if ( cb != null ) cb.setEnabled( false );
          
      for ( RMFormattedTextField field : pdFields )
        if ( field != null ) field.setEnabled( false );
      
    }
    else if ( op == EnableOps.SAVE && --nestLevel == 0 )
    {
      int n = 0;
      for ( int i = 0; i < dataComponents.length; i++ )
      {
        if ( dataComponents[ i ] == null ) continue;
        Component cpt = dataComponents[ i ][ 0 ];
        enableStates[ n++ ] = cpt.isEnabled();
      }
      for ( JComboBox[] cbArray : pfBoxes )
        for ( JComboBox cb : cbArray )
          if ( cb != null ) enableStates[ n++ ] = cb.isEnabled();
    }
    else if ( op == EnableOps.RESTORE && nestLevel++ == 0 )
    {
      int n = 0;
      for ( int i = 0; i < dataComponents.length; i++ )
      {
        if ( dataComponents[ i ] == null ) continue;
        Component cpt = dataComponents[ i ][ 0 ];
        cpt.setEnabled( enableStates[ n++ ] );
      }
      for ( JComboBox[] cbArray : pfBoxes )
        for ( JComboBox cb : cbArray )
          if ( cb != null ) cb.setEnabled( enableStates[ n++ ] );
      for ( RMFormattedTextField field : pdFields )
        if ( field != null ) field.setEnabled( true );
    }
  }


  private void errorMessage( int n )
  {
    if ( !showMessages || errorNumber >= 0 ) return;
    errorNumber = n;
    javax.swing.SwingUtilities.invokeLater( new Runnable()
    {
      public void run() 
      {
        if ( errorNumber < 0 ) return;
        String title = "Data Error";
        String message = null;
        switch ( errorNumber )
        {
          case 0:
            message = "Half-size leadout after first frame is not allowed when\n0-Burst and 1-Burst have different ON times";
            break;
          case 1:
            message = "Alternate lead-out is not allowed when 0-Burst and 1-Burst\nhave different ON times";
            break;
          case 2:
            message = "Half-size leadout after first frame and Alternate lead-out cannot both be selected.";
            break;
          case 3:
            message = "A change of Repeat Type between Minimum and Forced when the Repeat Count" +
                      "\nis nonzero will only be effective in Build mode, i.e. when the assembler" +
                      "\nlisting is empty or contains only directives.";
            break;
          case 4:
            message = "A change of Repeat Count when the Repeat Type is Forced will only be" + 
                      "\neffective in Build mode, i.e. when the assembler listing is empty or" +
                      "\ncontains only directives.";
            break;
          default:
            message = "Unknown error";
        }
        JOptionPane.showMessageDialog( RemoteMaster.getFrame(), message, title, JOptionPane.ERROR_MESSAGE );
        errorNumber = -1;
      }
    } );
  }
  
  private String getOFFtime34( int pdIndex, int[] offsets, int dataStyle )
  {
    if ( Hex.get( pdValues, pdIndex ) == null )
    {
      return ns;
    }
    else
    {
      int t = ( pdValues[ pdIndex + 1 ] + 255 ) % 256;
      t += ( ( pdValues[ pdIndex ] + 255 ) % 256 ) * ( ( dataStyle == 3 ) ? 257 : 257.5 );
      t = ( dataStyle == 3 ) ? 3 * t + offsets[ 0 ] : 2 * t + offsets[ 1 ];
      return "" + t;
    }
  }

  private String getONtime34( int pdIndex1, Integer pdIndex2 )
  {
    if ( pdValues[ pdIndex1 ] == null )
    {
      return ns;
    }
    else if ( pfValues[ 2 ] != null && ( pfValues[ 2 ] & 0x7C ) == 0x40 )
    {
      int t = ( pdValues[ pdIndex1 ] + 255 ) % 256 + 1;
      return "" + ( 3 * t + 2 );
    }
    else
    {
      int t = ( pdValues[ pdIndex1 ] + 255 ) % 256 + 1;
      if ( pdIndex2 != null && pfValues[ 1 ] != null && ( pfValues[ 1 ] & 0x08 ) == 0x08 && pdValues[ pdIndex2 ] != null )
      {
        t += ( ( pdValues[ pdIndex2 ] + 255 ) % 256 ) * 256;
      }
      return "" + burstUnit * t / 1000;
    }
  }
  
  public String getFrequency( int times )
  {
    burstUnit = 0;
    int on = times >> 8;
    int off = times & 0xFF;
    if ( on > 0 && off > 0 )
    {
      double f = processor.getOscillatorFreq() / ( on + off + processor.getCarrierTotalOffset() );
      burstUnit = ( int )( Math.round( 1000000000 / f ) );
      return String.format( "%.3f", f / 1000 );
    }
    else if ( on == 0 && off == 0 )
    {
      return "0";
    }
    else
    {
      return "** Error **";
    }
  }
  
  public String getDutyCycle( int times )
  {
    int on = times >> 8;
    int off = times & 0xFF;
    int totOffset = processor.getCarrierTotalOffset();
    int onOffset = processor.getCarrierOnOffset();
    if ( on > 0 && off > 0 )
    {
      double dc = 100.0 * ( on + onOffset ) / ( on + off + totOffset );
      return String.format( "%.2f", dc );
    }
    else if ( on == 0 && off == 0 )
    {
      return "";
    }
    else
    {
      return "** Error **";
    }
  }
  
  public void interpretPFPD( boolean force )
  {
    // DataStyle values:
    // 0 = S3C80, S3F80
    // 1 = HCS08
    // 2 = 6805-RC16/18, SST
    // 3 = 6805-C9
    // 4 = P8/740

    if ( processor == null || !force && !isActive )
    {
      return;
    }

    int ni = ns.isEmpty() ? -1 : 0;

    int dataStyle = processor.getDataStyle();
    if ( ( (DefaultComboBoxModel )devBits1.getModel() ).getSize() == 0 )
    {
      // Populate those combo boxes whose content is fixed
      populateComboBox( devBytes, CommonData.to15 );
      populateComboBox( cmdBytes, CommonData.to15 );
      populateComboBox( devBits1, CommonData.to8 );
      populateComboBox( cmdBits1, CommonData.to8 );
      populateComboBox( devBits2, CommonData.to8 );
      populateComboBox( cmdBits2, CommonData.to8 );
      populateComboBox( xmit0rev, CommonData.noYes );
      populateComboBox( leadInStyle, CommonData.leadInStyle );
      populateComboBox( offAsTotal, CommonData.noYes );
      populateComboBox( useAltLeadOut, CommonData.noYes );
      populateComboBox( useAltFreq, CommonData.noYes );
      populateComboBox( leadOutStyle, CommonData.leadOutStyle );
      populateComboBox( rptType, CommonData.repeatType );
      populateComboBox( burstMidFrame, CommonData.noYes );
    }

    populateComboBox( sigStruct, dataStyle < 3 ? CommonData.sigStructs012 : CommonData.sigStructs34 );
    populateComboBox( devBitDbl, dataStyle < 3 ? CommonData.bitDouble012 : CommonData.bitDouble34 );
    populateComboBox( cmdBitDbl, dataStyle < 3 ? CommonData.bitDouble012 : CommonData.bitDouble34 );
    populateComboBox( rptHold, dataStyle < 3 ? CommonData.repeatHeld012 : CommonData.noYes );

    isSettingPF = !force;
    doBoxEnableStates( EnableOps.RESTORE );
    
    Integer valI = Hex.get( basicValues, 0 );
    frequency.setValue( valI == null ? "35" : getFrequency( valI ) );
    dutyCycle.setValue( valI == null ? "30" : getDutyCycle( valI ) );
    if ( valI == null )
    {
      valI = Hex.get( basicValues, 0 );
      frequency.setValue( getFrequency( valI ) );
      dutyCycle.setValue( getDutyCycle( valI ) );
    }
    Short valS = basicValues[ 2 ];

    devBytes.setSelectedIndex( valS == null ? 0 : valS >> 4 );
    cmdBytes.setSelectedIndex( valS == null ? 0 : valS & 0x0F );
    burstMidFrame.setEnabled( dataStyle < 3 );
    if ( dataStyle >= 3 ) burstMidFrame.setSelectedIndex( 0 );
    afterBitsLbl.setEnabled( dataStyle < 3 );
    afterBits.setEnabled( dataStyle < 3 );
    useAltFreq.setEnabled( dataStyle < 3 );
    altFreqLbl.setEnabled( dataStyle < 3 );
    altFreq.setEnabled( dataStyle < 3 );
    altDutyLbl.setEnabled( dataStyle < 3 );
    altDuty.setEnabled( dataStyle < 3 );
    offAsTotal.setEnabled( dataStyle != 3 );
    
    if ( dataStyle < 3 )
    {
      devBits1.setSelectedIndex( ( pdValues[ 0 ] != null && pdValues[ 0 ] <= 8 ) ? pdValues[ 0 ] : ni );
      cmdBits1.setSelectedIndex( ( pdValues[ 1 ] != null && pdValues[ 1 ] <= 8 ) ? pdValues[ 1 ] : ni );
      int n = ( dataStyle < 2 ) ? 0x10 : 0x0E;
      devBits2.setSelectedIndex( ( pdValues[ n ] != null && pdValues[ n ] <= 8 ) ? pdValues[ n ] : -1 );
      n = ( dataStyle < 2 ) ? 0x12 : 0x10;
      cmdBits2.setSelectedIndex( ( pdValues[ n ] != null && pdValues[ n ] <= 8 ) ? pdValues[ n ] : -1 );
      sigStruct.setSelectedIndex( ( pfValues[ 0 ] != null ) ? ( pfValues[ 0 ] >> 4 ) & 0x03 : -1 );
      devBitDbl.setSelectedIndex( ( pfValues[ 2 ] != null ) ? pfValues[ 2 ] & 3 : 0 );
      cmdBitDbl.setSelectedIndex( ( pfValues[ 2 ] != null ) ? ( pfValues[ 2 ] >> 2 ) & 3 : 0 );
      n = ( dataStyle < 2 ) ? 0x11 : 0x0F;
      rptType.setSelectedIndex( ( pfValues[ 1 ] != null && ( ( pfValues[ 1 ] & 0x10 ) != 0 ) && pdValues[ n ] != null && pdValues[ n ] != 0xFF  ) ? 1 : 0 );
      rptValue.setValue( ( rptType.getSelectedIndex() == 1 ) ? ( pdValues[ n ] != null ) ? "" + pdValues[ n ] : "" : "" + assemblerModel.getForcedRptCount() );
      rptHold.setSelectedIndex( ( pfValues[ 1 ] != null ) ? pfValues[ 1 ] & 0x03 : 0 );
      xmit0rev.setSelectedIndex( ( pfValues[ 2 ] != null ) ? ( pfValues[ 2 ] >> 4 ) & 1 : 0 );
      leadInStyle.setSelectedIndex( ( pfValues[ 1 ] != null ) ? ( pfValues[ 1 ] >> 2 ) & 3 : 0 );
      boolean b;
      if ( burstMidFrame.isEnabled() )
      {
        b = assemblerModel.getMidFrameIndex() > 0;
        burstMidFrame.setSelectedIndex( b ? 1 : 0 );
        afterBits.setValue( ( b && pdValues[ 0x13 ] != null ) ? "" + ( pdValues[ 0x13 ] - 1 ) : "" );
        afterBits.setEnabled( b );
        afterBitsLbl.setEnabled( b );
      }
      else
      {
        burstMidFrame.setSelectedIndex( 0 );
        afterBits.setEnabled( false );
        afterBitsLbl.setEnabled( false );
      }
      leadOutStyle.setSelectedIndex( ( pfValues[ 1 ] != null ) ? ( pfValues[ 1 ] >> 5 ) & 3 : 0 );
      offAsTotal.setSelectedIndex( pfValues[ 0 ] != null ? ( pfValues[ 0 ] >> 6 ) & 1 : 0 );
      useAltLeadOut.setSelectedIndex( ( pfValues[ 3 ] != null ) ? ( pfValues[ 3 ] >> 5 ) & 1 : 0 );
      useAltFreq.setSelectedIndex( ( pfValues[ 3 ] != null ) ? ( pfValues[ 3 ] >> 6 ) & 1 : 0 );
      int ndx = dataStyle < 2 ? 0x13 : 0x11;
      b = useAltFreq.getSelectedIndex() > 0 && Hex.get( pdValues, ndx ) != null && Hex.get( pdValues, ndx ) != 0xFFFF;
      altFreq.setValue( b ? getFrequency( Hex.get( pdValues, ndx ) ) : "" );
      altDuty.setValue( b ? getDutyCycle( Hex.get( pdValues, ndx ) ) : "" );

      if ( dataStyle < 2 )
      {
        burst1On.setValue( ( Hex.get( pdValues, 2 ) != null /* && Hex.get( pdValues, 2 ) > 0 */) ? ""
            + Hex.get( pdValues, 2 ) * 2 : ns );
        burst1Off.setValue( ( Hex.get( pdValues, 4 ) != null /* && Hex.get( pdValues, 4 ) > 0 */) ? ""
            + ( Hex.get( pdValues, 4 ) * 2 + ( ( dataStyle == 0 ) ? 40 : 0 ) ) : ns );
        burst0On.setValue( ( Hex.get( pdValues, 6 ) != null /* && Hex.get( pdValues, 6 ) > 0 */) ? ""
            + Hex.get( pdValues, 6 ) * 2 : ns );
        burst0Off.setValue( ( Hex.get( pdValues, 8 ) != null /* && Hex.get( pdValues, 8 ) > 0 */) ? ""
            + ( Hex.get( pdValues, 8 ) * 2 + ( ( dataStyle == 0 ) ? 40 : 0 ) ) : ns );
        leadInOn.setValue( ( leadInStyle.getSelectedIndex() > 0 && Hex.get( pdValues, 0x0C ) != null && Hex.get(
            pdValues, 0x0C ) != 0xFFFF ) ? "" + Hex.get( pdValues, 0x0C ) * 2 : "" );
        leadInOff.setValue( ( leadInStyle.getSelectedIndex() > 0 && Hex.get( pdValues, 0x0E ) != null && Hex.get(
            pdValues, 0x0E ) != 0xFFFF ) ? "" + ( Hex.get( pdValues, 0x0E ) * 2 + ( ( dataStyle == 0 ) ? 40 : 0 ) )
            : "" );
        leadOutOff.setValue( ( Hex.get( pdValues, 0x0A ) != null /* && Hex.get( pdValues, 0x0A ) > 0 */) ? ""
            + Hex.get( pdValues, 0x0A ) * 2 : ns );
        altLeadOut.setValue( ( useAltLeadOut.getSelectedIndex() == 1 && Hex.get( pdValues, 0x13 ) != null 
            /* && Hex.get( pdValues, 0x13 ) > 0*/ ) ? "" + Hex.get( pdValues, 0x13 ) * 2 : "" );
      }
      else
      {
        int t = ( Hex.semiGet( pdValues, 2, 0 ) != null ) ? Hex.semiGet( pdValues, 2, 0 ) : -1;
        burst1On.setValue( t >= 0 ? "" + 4 * ( t + 1 ) : ns );
        t = ( Hex.semiGet( pdValues, 2, 1 ) != null ) ? Hex.semiGet( pdValues, 2, 1 ) : -1;
        burst1Off.setValue( t >= 0 ? "" + 4 * t : ns );
        t = ( Hex.semiGet( pdValues, 5, 0 ) != null ) ? Hex.semiGet( pdValues, 5, 0 ) : -1;
        burst0On.setValue( t >= 0 ? "" + 4 * ( t + 1 ) : ns );
        t = ( Hex.semiGet( pdValues, 5, 1 ) != null ) ? Hex.semiGet( pdValues, 5, 1 ) : -1;
        burst0Off.setValue( t >= 0 ? "" + 4 * t : ns );
        t = ( Hex.semiGet( pdValues, 0x0B, 0 ) != null ) ? Hex.semiGet( pdValues, 0x0B, 0 ) : -1;
        leadInOn.setValue( leadInStyle.getSelectedIndex() > 0 && t >= 0 ? "" + 4 * ( t + 1 ) : "" );
        t = ( Hex.semiGet( pdValues, 0x0B, 1 ) != null ) ? Hex.semiGet( pdValues, 0x0B, 1 ) : -1;
        leadInOff.setValue( leadInStyle.getSelectedIndex() > 0 && t >= 0 ? "" + 4 * t : "" );
        t = ( Hex.get( pdValues, 8 ) != null ) ? Hex.get( pdValues, 8 ) - 10 : -1;
        leadOutOff.setValue( t >= 0 ? "" + 4 * t : ns );
        t = ( Hex.get( pdValues, 0x11 ) != null ) ? Hex.get( pdValues, 0x11 ) - 10 : -1;
        altLeadOut.setValue( useAltLeadOut.getSelectedIndex() == 1 && t >= 0 ? "" + 4 * t : "" );
      }
    }
    else
    {
      if ( dataStyle == 3 )
      {
        offAsTotal.setSelectedIndex( -1 );
        offAsTotalLbl.setEnabled( false );
      }
      else
      {
        offAsTotalLbl.setEnabled( true );
      }
      if ( pfValues[ 0 ] != null && ( ( pfValues[ 0 ] & 0x58 ) == 0x08 ) )
      {
        devBits1.setSelectedIndex( ( pdValues[ 0x0D ] != null ) ? pdValues[ 0x0D ] : ni );
      }
      else
      {
        devBits1.setSelectedIndex( ( pdValues[ 1 ] != null ) ? pdValues[ 1 ] : ni );
        if ( devBits2.isEnabled() )
        {
          devBits2.setSelectedIndex( ( pdValues[ 0x0D ] != null ) ? pdValues[ 0x0D ] : ni );
        }
      }
      cmdBits1.setSelectedIndex( ( pdValues[ 2 ] != null ) ? pdValues[ 2 ] : ni );
      String sig = "";
      String items[] =
      {
          "devs", "dev", "cmd", "!dev", "dev2", "cmd", "!cmd"
      };
      if ( pfValues[ 0 ] != null )
      {
        int key = ( ( pfValues[ 0 ] >> 1 ) & 0x3C ) | ( ( pfValues[ 0 ] >> 2 ) & 1 );
        if ( ( pfValues[ 0 ] & 0x41 ) == 0x41 )
        {
          key ^= 0x60; // replace bit for "dev" by that for "devs"
        }
        if ( ( pfValues[ 0 ] & 0x22 ) == 0x22 )
        {
          key ^= 0x12; // replace bit for first "cmd" by that for second one
        }
        for ( int i = 0; i < 7; i++ )
        {
          if ( ( ( key << i ) & 0x40 ) == 0x40 )
          {
            sig += items[ i ] + "-";
          }
        }
        sig = sig.substring( 0, Math.max( sig.length() - 1, 0 ) );
        sigStruct.setSelectedItem( sig );
      }
      else
      {
        sigStruct.setSelectedIndex( -1 );
      }
      sigStruct.setSelectedItem( pfValues[ 0 ]  == null ? -1 : sig );
      devBitDbl.setSelectedIndex( ( pfValues[ 2 ] != null ) ? ( pfValues[ 2 ] >> 1 ) & 1 : 0 );
      cmdBitDbl.setSelectedIndex( ( pfValues[ 2 ] != null ) ? ( pfValues[ 2 ] >> 1 ) & 1 : 0 );
      int count = assemblerModel.getForcedRptCount();
      rptType.setSelectedIndex( count > 0 ? 0 : ( pfValues[ 1 ] != null && ( ( pfValues[ 1 ] & 0x02 ) != 0 ) ) ? 1 : -1 );
      if ( rptType.getSelectedIndex() == 0 ) rptValue.setValue( "" + count );
      rptHold.setSelectedIndex( ( pfValues[ 1 ] != null && ( ( pfValues[ 1 ] & 0x02 ) != 0 ) ) ? 1 : 0 );

      burst1On.setValue( getONtime34( 0, null ) );

      burst0On.setValue( ( pfValues[ 2 ] != null  && ( pfValues[ 2 ] & 0x08 ) == 0x08 ) ? getONtime34( 0x0E, null ) : getONtime34( 0, null ) );

      burst1Off.setValue( getOFFtime34( 3, CommonData.burstOFFoffsets34, dataStyle ) );
      burst0Off.setValue( getOFFtime34( 5, CommonData.burstOFFoffsets34, dataStyle ) );
      xmit0rev.setSelectedIndex( ( pfValues[ 2 ] != null && ( pfValues[ 2 ] & 0x1C ) == 0x04 ) ? 1 : 0 );
      leadInStyle.setSelectedIndex( ( pfValues[ 1 ] != null && (( pfValues[ 1 ] & 0x10 ) == 0x10 ) ) ? 
         (  ( pfValues[ 1 ] & 0x04 ) == 0x04 && ( /*force ||*/ Hex.get( pdValues, 0x10 ) != null && Hex.get( pdValues, 0x10 ) != Hex.get( pdValues, 0x0A ) ) ) ? 3 : 1 : 0 );

      leadInOn.setValue( leadInStyle.getSelectedIndex() > 0 ? getONtime34( 9, 0x0C ) : "" );
      leadInOff.setValue( leadInStyle.getSelectedIndex() > 0 ? getOFFtime34( 0x0A, CommonData.leadinOFFoffsets34,
          dataStyle ) : "" );
      offAsTotal.setSelectedIndex( ( dataStyle == 4 && pfValues[ 2 ] != null ) ? pfValues[ 2 ] & 1 : 0 );
      leadOutStyle.setSelectedIndex( pfValues[ 1 ] != null ? offAsTotal.getSelectedIndex() == ( pfValues[ 1 ] >> 6 & 1 ) ? -1 : ( pfValues[ 1 ] >> 4 & 2 ) : 0 );
      leadOutOff.setValue( ( dataStyle == 3 ) ? getOFFtime34( 7, CommonData.leadinOFFoffsets34, dataStyle ) : ( Hex.get( pdValues, 7 ) != null && Hex.get( pdValues, 7 ) > 0 ) ? "" + ( Math.max( Hex.get( pdValues, 7 ) * 4 - 40, 0 ) ) : "0" );

      boolean b = pfValues[ 1 ] != null && ( pfValues[ 1 ] & 4 ) == 4 && ( pfValues[ 2 ] == null || ( pfValues[ 2 ] & 8 ) == 0 );
      int i1 = Hex.get( pdValues, 0x0E ) == null ? -1 : Hex.get( pdValues, 0x0E );
      int i2 = Hex.get( pdValues, 0x07 ) == null ? -1 : Hex.get( pdValues, 0x07 );
      b = b && i1 > 0 && i2 > 0 && i1 != i2;
      int ndx = b ? 1 : 0;
      
      // For some peculiar reason the following line that combines the above does not work, giving value 1 at times when it should be 0.
      // int ndx = ( pfValues[ 1 ] != null && ( pfValues[ 1 ] & 4 ) == 4 && ( pfValues[ 2 ] == null || ( pfValues[ 2 ] & 8 ) == 0 ) && Hex.get( pdValues, 0x0E ) != null && Hex.get( pdValues, 0x0E ) != Hex.get( pdValues, 0x07 ) ) ? 1 : 0;
      
      useAltLeadOut.setSelectedIndex( ndx );
      altLeadOut.setValue( ( useAltLeadOut.getSelectedIndex() == 1  ) ? ( dataStyle == 3 ) ? getOFFtime34( 0x0E, CommonData.leadinOFFoffsets34, dataStyle ) : ( Hex.get( pdValues, 0x0E ) != null && Hex.get( pdValues, 0x0E ) > 0 ) ? "" + ( Hex.get( pdValues, 0x0E ) * 4 - 40 ) : "" : "" );
    }
    isSettingPF = false;
    doBoxEnableStates( EnableOps.SAVE );
    if ( mode == Mode.DISASM )
      doBoxEnableStates( EnableOps.DISABLE );
  }
  
  public void reset()
  {
    setDefaultInterpretations();
    restoreInterpretations();
  }
  
  public void refreshPD()
  {
    ns = "0";
    interpretPFPD( true );
    ns = "";
    int i = 0;
    for ( i = 0; i < pfValues.length && pfValues[ i ] != null; i++ );
    assemblerModel.setPfCount( i );
    pfpdListener.actionPerformed( new ActionEvent( sigStruct, ActionEvent.ACTION_PERFORMED, "Internal" ) );
    pfpdListener.actionPerformed( new ActionEvent( leadInStyle, ActionEvent.ACTION_PERFORMED, "Internal" ) );
    pfpdListener.actionPerformed( new ActionEvent( rptType, ActionEvent.ACTION_PERFORMED, "Internal" ) );
    pfpdListener.actionPerformed( new ActionEvent( devBytes, ActionEvent.ACTION_PERFORMED, "Internal" ) );
    pfpdListener.actionPerformed( new ActionEvent( cmdBytes, ActionEvent.ACTION_PERFORMED, "Internal" ) );
    pfpdListener.actionPerformed( new ActionEvent( useAltLeadOut, ActionEvent.ACTION_PERFORMED, "Internal" ) );
    pfpdListener.actionPerformed( new ActionEvent( useAltFreq, ActionEvent.ACTION_PERFORMED, "Internal" ) );
  }
  
  public void setAssemblerData( boolean refresh )
  {
    assemblerModel.setPfCount( getPfCount() );
    if ( refresh )
    {
      // Refresh pd values that could be null
      refreshPD();
    }

    boolean fill = false;
    short[] fillValues = CommonData.pdDefaults[ dataStyle ];
    int i = 0;
    for ( i = pdValues.length - 1; i >= 0; i-- )
    {
      if ( fill == false && pdValues[ i ] != null )
      {
        fill = true;
        assemblerModel.setPdCount( i + 1 );
      }
      if ( pdValues[ i ] == null && fill )
        pdValues[ i ] = i < fillValues.length ? fillValues[ i ] : 0;
    }

    Hex hex = new Hex( assemblerModel.getPdCount() + 10 );
    assemblerModel.setHex( hex );
    for ( i = 0; i < processor.getStartOffset(); i++ )
      hex.set( basicValues[ i ], i );
    for ( i = processor.getStartOffset(); i < 3; i++ )
      hex.set( basicValues[ i ], i + 2 );
    for ( i = 0; i < pfValues.length && pfValues[ i ] != null; i++ )
      hex.set( pfValues[ i ], i + 5 );
    for ( int j = 0; j < assemblerModel.getPdCount(); j++ )
      hex.set( pdValues[ j ], i + j + 5 );

  }
  public void setDefaultInterpretations()
  {
    interpretations = new Object[ dataComponents.length ];
    for ( int i = 0; i < dataComponents.length; i++ )
    {
      if ( dataComponents[ i ] == null ) continue;
      Component cpt = dataComponents[ i ][ 0 ];
      if ( cpt instanceof JComboBox )
      {
        interpretations[ i ] = 0;
      }
      else if ( cpt instanceof RMFormattedTextField )
      {
        interpretations[ i ] = "";
      }
      interpretations[ 0 ] = "35";
      interpretations[ 1 ] = "30";
      interpretations[ 4 ] = 1;
      interpretations[ 5 ] = 8;
      interpretations[ 9 ] = 1;
      interpretations[ 10 ] = 8;
      interpretations[ 14 ] = 1;
      interpretations[ 15 ] = 1;
    }
  }
  
  public void setInterpretations()
  {
    if ( interpretations == null )
    {
      setDefaultInterpretations();
      return;
    }

    interpretations = new Object[ dataComponents.length ];
    for ( int i = 0; i < dataComponents.length; i++ )
    {
      if ( dataComponents[ i ] == null ) continue;
      Component cpt = dataComponents[ i ][ 0 ];
      if ( cpt instanceof JComboBox )
      {
        interpretations[ i ] = ((JComboBox)cpt).getSelectedIndex();
      }
      else if ( cpt instanceof RMFormattedTextField )
      {
        interpretations[ i ] = ( ( RMFormattedTextField )cpt ).getText();
      }
    }
  }
  
  public void restoreInterpretations()
  {
    if ( interpretations == null )
    {
      return;
    }
    interpretPFPD( true );
    for ( int i = 0; i < dataComponents.length; i++ )
    {
      if ( dataComponents[ i ] == null ) continue;
      Component cpt = dataComponents[ i ][ 0 ];
      if ( !cpt.isEnabled() ) continue;
      if ( cpt instanceof JComboBox )
      {
        ((JComboBox)cpt).setSelectedIndex( ( Integer )interpretations[ i ] );         
      }
      else if ( cpt instanceof RMFormattedTextField )
      {
        ( ( RMFormattedTextField )cpt ).setValue ( ( String )interpretations[ i ] );
      }
    }
  }
  
  public int getRptType()
  {
    return rptType.getSelectedIndex();
  }
  
  public int getBurstMidFrame()
  {
    return burstMidFrame.getSelectedIndex();
  }

  private int getWidth( String text )
  {
    return ( new JLabel( text ) ).getPreferredSize().width + 4;
  }
  
  public Short[] getPdValues()
  {
    return pdValues;
  }

  public Short[] getPfValues()
  {
    return pfValues;
  }
  
  public int getPfCount()
  {
    int i = 0;
    if ( pfValues[ 0 ] == null )
      pfValues[ 0 ] = 0;
    for ( i = 0; i < pfValues.length && pfValues[ i ] != null; i++ );
    return i;
  }

  public Short[] getBasicValues()
  {
    return basicValues;
  }
  
  public void setMode( Mode mode )
  {
    this.mode = mode;
  }

  public void setActive( boolean isActive )
  {
    this.isActive = isActive;
  }

  public void setShowMessages( boolean showMessages )
  {
    this.showMessages = showMessages;
  }

  public int getDataStyle()
  {
    return dataStyle;
  }

  public void setDataStyle( int dataStyle )
  {
    this.dataStyle = dataStyle;
  }

  public void setProcessor( Processor processor )
  {
    this.processor = processor;
  }

  public Dimension getPrefSize()
  {
    return prefSize;
  }


  private Component[][] dataComponents = null;

  private String[][] dataLabels = { 
      { "Frequency", "kHz" }, { "Duty Cycle", "%" }, { "Signal Structure" }, null,
      { "Device Bytes" }, { "Bits/Dev1" }, { "Bits/Dev2" }, { "Dev Bit Doubling" }, null,
      { "Command Bytes" }, { "Bits/Cmd1" }, { "Bits/Cmd2" }, { "Cmd Bit Doubling" }, null,
      { "Repeat Type" }, { "Hold" }, { "Count" }, null,
      { "1 Burst ON", "uSec" }, { "OFF", "uSec" }, null,
      { "0 Burst ON", "uSec" }, { "OFF", "uSec" }, { "Xmit 0 Reversed" }, null,
      { "Lead-In Style" }, { "Lead-In ON", "uSec" }, { "OFF", "uSec" }, null,
      { "Lead-Out Style" }, { "Lead-Out OFF", "uSec" }, { "OFF as Total" }, null,
      { "Use Alt Lead-Out" }, { "Alt Lead-Out", "uSec" }, null,
      { "Use Alt Freq" }, { "Alt Freq", "kHz" }, { "Alt Duty", "%" }, null,
      { "*****    Active in Build mode only    *****" }, null,
      { "Burst Mid-Frame" }, { "After # of bits" },
//      { "Check Byte Style" }, { "# Bytes Checked" }, null,
//      { "Mini-Combiner" }, { "Signal Style" }, null,
//      { "Vector Offset" }, { "Data Offset" }, null,
//      { "Toggle Bit }"
  };
  
  public RMFormattedTextField frequency = null;
  public RMFormattedTextField dutyCycle = null;
  public JComboBox sigStruct = new JComboBox();

  public JComboBox devBytes = new JComboBox();
  public JComboBox devBits1 = new JComboBox();
  public JComboBox devBits2 = new JComboBox();
  public JComboBox devBitDbl = new JComboBox();

  public JComboBox cmdBytes = new JComboBox();
  public JComboBox cmdBits1 = new JComboBox();
  public JComboBox cmdBits2 = new JComboBox();
  public JComboBox cmdBitDbl = new JComboBox();

  public RMFormattedTextField rptValue = null;
  public JComboBox rptType = new JComboBox();
  public JComboBox rptHold = new JComboBox();

  public RMFormattedTextField burst1On = null;
  public RMFormattedTextField burst1Off = null;

  public RMFormattedTextField burst0On = null;
  public RMFormattedTextField burst0Off = null;
  public JComboBox xmit0rev = new JComboBox();

  public JComboBox leadInStyle = new JComboBox();
  public JComboBox burstMidFrame = new JComboBox();
  public RMFormattedTextField afterBits = null;
  public RMFormattedTextField leadInOn = null;
  public RMFormattedTextField leadInOff = null;

  public JComboBox leadOutStyle = new JComboBox();
  public RMFormattedTextField leadOutOff = null;
  public JComboBox offAsTotal = new JComboBox();

  public JComboBox useAltLeadOut = new JComboBox();
  public RMFormattedTextField altLeadOut = null;

  public JComboBox useAltFreq = new JComboBox();
  public RMFormattedTextField altFreq = null;
  public RMFormattedTextField altDuty = null;

  // public JComboBox toggleBit = new JComboBox();
  // public JComboBox chkByteStyle = new JComboBox();
  // public JTextField bitsHeld = new JTextField();
  //
  // public JComboBox miniCombiner = new JComboBox();
  // public JComboBox sigStyle = new JComboBox();
  //
  // public JTextField vecOffset = new JTextField();
  // public JTextField dataOffset = new JTextField();

  public JLabel devBits1lbl = new JLabel();
  public JLabel devBits2lbl = new JLabel();
  public JLabel cmdBits1lbl = new JLabel();
  public JLabel cmdBits2lbl = new JLabel();
  public JLabel burstMidFrameLbl = new JLabel();
  public JLabel afterBitsLbl = new JLabel();
  public JLabel altFreqLbl = new JLabel();
  public JLabel altDutyLbl = new JLabel();
  public JLabel rptValueLbl = new JLabel();
  public JLabel leadInOnLbl = new JLabel();
  public JLabel leadInOffLbl = new JLabel();
  public JLabel offAsTotalLbl = new JLabel();
  public JLabel altLeadOutLbl = new JLabel();
  
  private String ns = "";
  private Object[] interpretations = null;
  private Short[] basicValues = null;
  private Short[] pdValues = null;
  private List< RMFormattedTextField > pdFields = null;
  private Short[] pfValues = null;
  private JComboBox[][] pfBoxes = null;
  private boolean isSettingPF = false;
  private int boxCount = 0;
  private int dataStyle = 0;
  private int nestLevel = 0;
  private AssemblerTableModel assemblerModel = null;
  private int burstUnit = 0;
  private boolean[] enableStates = null;
  private int errorNumber = -1;
  private Processor processor = null;

  private Mode mode = null;
  private boolean isActive = false;
  private boolean showMessages = true;
  private Dimension prefSize = null;
  
  public PFMainPanel pfMainPanel = null;
  public PDMainPanel pdMainPanel = null;
  public FunctionMainPanel fnMainPanel = null;
}
