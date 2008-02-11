//
//  Demo.java
//
//	This demonstrates some features of JCollider. This demo
//	class is absolutely free and comes with absolutely no
//	warranties. JColider itself is released under the GNU GPL
//	(see separate license file).
//
//	To launch the compiled class, CD into the JColider folder
//	and run the JSuperColiderDemo.sh shell script.
//
//  Created by Hanns Holger Rutz on 10.09.05.
//

package de.sciss.jcollider.test;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

//import net.roydesign.mac.MRJAdapter;

import de.sciss.jcollider.*;
import de.sciss.jcollider.gui.*;

/**
 *	This class demonstrates some
 *	of JCollider's features, namely
 *	loading and building synth defs,
 *	along with instantiating and
 *	controlling a server, showing
 *	GUI elements like a server window
 *	and a synthdef diagram.
 *	<p>
 *	A frame is opened with three tables,
 *	the leftmost showing all synthdefs
 *	found in the folder &quot;synthdefs&quot;
 *	inside the cwd. The middle table
 *	shows a list of synthdefs that have
 *	been internally build using JCollider.
 *	The right most table will carry defs
 *	which have been drag-and-dropped from
 *	the finder.
 *	<p>
 *	A GUI server window is opened for the localhost,
 *	using default server options. Before booting,
 *	check that the application path to scsynth is
 *	correctly specified in the main frame.
 *	<p>
 *	Buttons at the bottom of the main frame will
 *	create a synth for the selected def, stop all
 *	synths, open a synthdef diagram and print the
 *	def to the system console. A tree view of the node graph
 *	can be created.
 *
 *	@version	0.31, 08-Oct-07
 *	@author		Hanns Holger Rutz
 *
 *	@todo		the demo and the NodeWatcher get irritated if
 *				a server is already running and containing
 *				nodes when the demo is launched
 *	@todo		quitting the application from the apple menu
 *				will not perform clean-ups, notably the server
 *				will not be shut down. please use main window's
 *				close gadget for now
 *	@todo		should be possible to drag + drop nodes in
 *				the tree panel, create subgroups etc.
 */
public class Demo
extends JFrame
implements FileFilter, ServerListener, Constants
{
	public static Font	fntGUI	= ServerPanel.fntGUI;

	private final SynthDefTable[] defTables;
	private SynthDefTable selectedTable	= null;
	
	private static final Comparator synthDefNameComp = new SynthDefNameComp();
	
	private Server		server	= null;
	private NodeWatcher	nw		= null;
	private Group		grpAll;
	
	private static final String[] tableNames = { "JCollider", "Drop Zone" };

	private final Demo enc_this	= this;
	
	public Demo()
	{
		super( "JCollider Demo" );

		final Box			b			= Box.createHorizontalBox();
		final Box			b2			= Box.createHorizontalBox();
		final Container		cp			= getContentPane();
		final JTextField	ggAppPath	= new JTextField( 32 );
		final String		fs			= File.separator;
		JScrollPane			ggScroll;
		JLabel				lb;
		JFrame				spf			= null;
		
		defTables = new SynthDefTable[ 2 ];
		for( int i = 0; i < 2; i++ ) {
			defTables[ i ]	= new SynthDefTable( tableNames[ i ]);
			ggScroll		= new JScrollPane( defTables[ i ]);
			b.add( ggScroll );
			defTables[ i ].getSelectionModel().addListSelectionListener( new TableSelListener( i ));
			if( i == 1 ) {
				ggScroll.setTransferHandler( new SynthDefFileTransferHandler( 1 ));
				ggScroll.setToolTipText( "Drop SynthDef Files from the Finder here" );
			}
		}

		try {
			cp.setLayout( new BorderLayout() );
			cp.add( b, BorderLayout.CENTER );

			server = new Server( "localhost" );
//			loadDefs();
			createDefs();

			File f = findFile( JCollider.isWindows ? "scsynth.exe" : "scsynth", new String[] {
				fs + "Applications" + fs + "SuperCollider_f",
				fs + "Applications" + fs + "SC3",
				fs + "usr" + fs + "local" + fs + "bin",
				fs + "usr" + fs + "bin",
				"C:\\Program Files\\SC3",
				"C:\\Program Files\\SuperCollider_f"
			});
//			if( (f == null) && JCollider.isMacOS ) {
//				try {
//					f = MRJAdapter.findApplication( "SCjm" );
//					if( f != null ) f = new File( f.getParentFile(), "scsynth" );
//				}
//				catch( IOException e1 ) {}
//			}
			if( f != null ) Server.setProgram( f.getAbsolutePath() );

			ggAppPath.setText( Server.getProgram() );
			ggAppPath.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e )
				{
					Server.setProgram( ggAppPath.getText() );
				}
			});
			lb = new JLabel( "Server App Path :" );
			lb.setBorder( BorderFactory.createEmptyBorder( 2, 6, 2, 4 ));
			b2.add( lb );
			b2.add( ggAppPath );
			cp.add( b2, BorderLayout.NORTH );
			cp.add( createButtons(), BorderLayout.SOUTH );

			server.addListener( this );
			try {
				server.start();
				server.startAliveThread();
			}
			catch( IOException e1 ) {}
//			if( server.isRunning() ) initServer();
			spf = ServerPanel.makeWindow( server, ServerPanel.MIMIC | ServerPanel.CONSOLE | ServerPanel.DUMP );
		}
		catch( IOException e1 ) {
			JOptionPane.showMessageDialog( this, "Failed to create a server :\n" + e1.getClass().getName() +
				e1.getLocalizedMessage(), this.getTitle(), JOptionPane.ERROR_MESSAGE );
		}

		JCollider.setDeepFont( cp, fntGUI );

		addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e )
			{
				if( nw != null ) {
					nw.dispose();
					nw = null;
				}
				if( server != null ) {
					try {
						if( server.didWeBootTheServer() ) server.quitAndWait();
						else if( grpAll != null ) grpAll.free();
						server = null;
					}
					catch( IOException e1 ) {
						reportError( e1 );
					}
				}
				setVisible( false );
				dispose();
				System.exit( 0 );
			}
		});
		
		setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
		
//		pack();
		if( spf != null ) setLocation( spf.getX() + spf.getWidth() + 24, spf.getY() );
		setSize( 512, 512 );
		setVisible( true );
		toFront();
	}
	
//	private void loadDefs()
//	{
//		final File[]			defFiles	= new File( "synthdefs" ).listFiles( this );
//		SynthDef[]				defs;
//		final java.util.List	collDefs	= new ArrayList();
//	
//		for( int i = 0; i < defFiles.length; i++ ) {
//			try {
//				defs = SynthDef.readDefFile( defFiles[ i ]);
//				for( int j = 0; j < defs.length; j++ ) {
//					collDefs.add( defs[ j ]);
//				}
//			}
//			catch( IOException e1 ) {
//				System.err.println( defFiles[ i ].getName() + " : " + e1.getClass().getName() +
//					" : " + e1.getLocalizedMessage() );
//			}
//		}
//		
//		Collections.sort( collDefs, synthDefNameComp );
//		defTables[ 0 ].addDefs( collDefs );
//	}
	
	private JComponent createButtons()
	{
		final Box	b	= Box.createHorizontalBox();
		JButton		but;
		
		but	= new JButton( new actionPlayClass() );
		but.setToolTipText( "Play Selected SynthDef" );
		b.add( but );
		but = new JButton( new actionStopClass() );
		but.setToolTipText( "Stop All Synths" );
		b.add( but );
		but = new JButton( new actionDiagramClass() );
		but.setToolTipText( "Open Diagram For Selected SynthDef" );
		b.add( but );
		but = new JButton( new actionDumpClass() );
		but.setToolTipText( "Dump Selected SynthDef To The System Console" );
		b.add( but );
		but = new JButton( new actionSynthDefApiExClass() );
		but.setToolTipText( "Demo code from SynthDef API doc" );
		b.add( but );
		but = new JButton( new actionNodeTreeClass() );
		but.setToolTipText( "View a Tree of all Nodes" );
		b.add( but );
		
		return b;
	}
	
	private void createDefs()
	{
		try {
//			UGenInfo.readDefinitions();
			UGenInfo.readBinaryDefinitions();

			final java.util.List collDefs = DemoDefs.create();
			Collections.sort( collDefs, synthDefNameComp );
//			defTables[ 1 ].addDefs( collDefs );
			defTables[ 0 ].addDefs( collDefs );
		}
		catch( IOException e1 ) {
			e1.printStackTrace();
//			reportError( e1 );
		}
	}

	private void initServer()
	throws IOException
	{
		sendDefs();
		if( !server.didWeBootTheServer() ) {
			server.initTree();
			server.notify( true );
		}
//		if( nw != null ) nw.dispose();
		nw		= NodeWatcher.newFrom( server );
		grpAll	= Group.basicNew( server );
		nw.register( server.getDefaultGroup() );
		nw.register( grpAll );
		server.sendMsg( grpAll.newMsg() );
	}

	private void sendDefs()
	{
		java.util.List	defs;
		SynthDef		def;
	
		for( int i = 0; i < defTables.length; i++ ) {
			defs = defTables[ i ].getDefs();
			for( int j = 0; j < defs.size(); j++ ) {
				def = (SynthDef) defs.get( j );
				try {
					def.send( server );
				}
				catch( IOException e1 ) {
					System.err.println( "Sending Def " + def.getName() + " : " +
						e1.getClass().getName() + " : " + e1.getLocalizedMessage() );
				}
			}
		}
	}

	private static File findFile( String fileName, String[] folders )
	{
		File f;
	
		for( int i = 0; i < folders.length; i++ ) {
			f = new File( folders[ i ], fileName );
			if( f.exists() ) return f;
		}
		return null;
	}

    public static void main( String args[] )
	{
		SwingUtilities.invokeLater( new Runnable() {
			public void run()
			{
				new Demo();
			}
		});
	}

	private static void reportError( Exception e ) {
		System.err.println( e.getClass().getName() + " : " + e.getLocalizedMessage() );
	}

// ------------- ServerListener interface -------------

	public void serverAction( ServerEvent e )
	{
		switch( e.getID() ) {
		case ServerEvent.RUNNING:
			try {
				initServer();
			}
			catch( IOException e1 ) {
				reportError( e1 );
			}
			break;
		
		case ServerEvent.STOPPED:
			// re-run alive thread
			final javax.swing.Timer t = new javax.swing.Timer( 1000, new ActionListener() {
				public void actionPerformed( ActionEvent e )
				{
					try {
						if( server != null ) server.startAliveThread();
					}
					catch( IOException e1 ) {
						reportError( e1 );
					}
				}
			});
			t.setRepeats( false );
			t.start();
			break;
		
		default:
			break;
		}
	}

// ------------- FileFilter interface -------------

	public boolean accept( File f )
	{
		try {
			return SynthDef.isDefFile( f );
		}
		catch( IOException e1 ) {
			return false;
		}
	}
	
// ------------- internal classes -------------
	
	private static class SynthDefTable
	extends JTable
	{
		private final SynthDefTableModel tm;
	
		private SynthDefTable( String name )
		{
			super();
			tm = new SynthDefTableModel( name );
			setModel( tm );
			getColumnModel().getColumn( 0 ).setPreferredWidth( 128 );
			setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		}
		
//		private void addDef( SynthDef def )
//		{
//			tm.addDef( def );
//		}

		private void addDefs( java.util.List defs )
		{
			tm.addDefs( defs );
		}
		
		private SynthDef getSelectedDef()
		{
			final int row = getSelectedRow();
			if( row >= 0 ) return tm.getDef( row );
			else return null;
		}

		private java.util.List getDefs()
		{
			return tm.getDefs();
		}
	}
	
	private static class SynthDefTableModel
	extends AbstractTableModel
	{
		private final java.util.List collDefs = new ArrayList();
		private final String name;

		private SynthDefTableModel( String name )
		{
			super();
			this.name = name;
		}
		
		public String getColumnName( int col )
		{
			return name;
		}

		public int getRowCount()
		{
			return collDefs.size();
		}
		
		public int getColumnCount()
		{
			return 1;
		}
		
		public Object getValueAt( int row, int column )
		{
			if( row < collDefs.size() ) {
				return ((SynthDef) collDefs.get( row )).getName();
			} else {
				return null;
			}
		}

//		private void addDef( SynthDef def )
//		{
//			collDefs.add( def );
//			fireTableRowsInserted( collDefs.size() - 1, collDefs.size() - 1 );
//		}

		private void addDefs( java.util.List defs )
		{
			if( defs.isEmpty() ) return;
		
			final int startRow = collDefs.size();
			collDefs.addAll( defs );
			fireTableRowsInserted( startRow, collDefs.size() - 1 );
		}

		private SynthDef getDef( int idx )
		{
			return (SynthDef) collDefs.get( idx );
		}

//		private int getNumDefs()
//		{
//			return collDefs.size();
//		}

		private java.util.List getDefs()
		{
			return new ArrayList( collDefs );
		}
	}
	
	private class actionPlayClass
	extends AbstractAction
	{
		private actionPlayClass()
		{
			super( "Play" );			
		}
	
		public void actionPerformed( ActionEvent e )
		{
			if( selectedTable == null ) return;
			
			final SynthDef	def		= selectedTable.getSelectedDef();
			final Synth		synth;
			
			if( (def != null) && (grpAll != null) && (server != null) ) {
				try {
					synth	= Synth.basicNew( def.getName(), server );
					if( nw != null ) nw.register( synth );
					server.sendMsg( synth.newMsg( grpAll ));
				}
				catch( IOException e1 ) {
					JCollider.displayError( enc_this, e1, "Play" );
				}
			}
		}
	}

	private class actionStopClass
	extends AbstractAction
	{
		private actionStopClass()
		{
			super( "Stop All" );
		}
	
		public void actionPerformed( ActionEvent e )
		{
			if( grpAll != null ) {
				try {
					grpAll.freeAll();
				}
				catch( IOException e1 ) {
					JCollider.displayError( enc_this, e1, "Stop" );
				}
			}
		}
	}
	
	private class TableSelListener
	implements ListSelectionListener
	{
		int idx;
	
		private TableSelListener( int idx )
		{
			this.idx	= idx;
		}

		public void valueChanged( ListSelectionEvent e )
		{
			if( defTables[ idx ].getSelectedRowCount() > 0 ) {
				selectedTable = defTables[ idx ];
				for( int i = 0; i < defTables.length; i++ ) {
					if( (i != idx) && defTables[ i ].getSelectedRowCount() > 0 ) {
						defTables[ i ].clearSelection();
					}
				}
			}
		}
	}

	private class actionDiagramClass
	extends AbstractAction
	{
		private actionDiagramClass()
		{
			super( "Def Diagram" );			
		}
	
		public void actionPerformed( ActionEvent e )
		{
			if( selectedTable == null ) return;
			
			final SynthDef def = selectedTable.getSelectedDef();
			if( def != null ) {
				new SynthDefDiagram( def );
			}
		}
	}
	
	private class actionDumpClass
	extends AbstractAction
	{
		private actionDumpClass()
		{
			super( "Def Dump" );			
		}
	
		public void actionPerformed( ActionEvent e )
		{
			if( selectedTable == null ) return;
			
			final SynthDef def = selectedTable.getSelectedDef();
			if( def != null ) {
				def.printOn( System.out );
			}
		}
	}

	private class actionSynthDefApiExClass
	extends AbstractAction
	{
		private actionSynthDefApiExClass()
		{
			super( "API Ex" );			
		}
	
		public void actionPerformed( ActionEvent e )
		{
			DemoDefs.synthDefApiExample( server );	// doesn't inform nodewatcher though
		}
	}

	private class actionNodeTreeClass
	extends AbstractAction
	{
		private actionNodeTreeClass()
		{
			super( "Node Tree" );			
		}
	
		public void actionPerformed( ActionEvent e )
		{
			if( (server == null) || (nw == null) || (grpAll == null) ) return;
		
			final NodeTreePanel		ntp			= new NodeTreePanel( nw, grpAll );
			final JFrame			treeFrame	= ntp.makeWindow();
			
			treeFrame.addWindowListener( new WindowAdapter() {
				public void windowClosing( WindowEvent e )
				{
					treeFrame.setVisible( false );
					treeFrame.dispose();
					ntp.dispose();
				}
			});
		}
	}

	private static class SynthDefNameComp
	implements Comparator
	{
		public int compare( Object def1, Object def2 )
		{
			return( ((SynthDef) def1).getName().compareTo( ((SynthDef) def2).getName() ));
		}
	}

	private class SynthDefFileTransferHandler
	extends TransferHandler
	{
		private final int idx;
	
		private SynthDefFileTransferHandler( int idx )
		{
			this.idx = idx;
		}

		/**
		 * Overridden to import a Pathname if it is available.
		 */
		public boolean importData( JComponent c, Transferable t )
		{
			final Object			o;
			final java.util.List	fileList;
			final java.util.List	collDefs;
			File					f;
			SynthDef[]				defs;
		
			try {
				if( t.isDataFlavorSupported( DataFlavor.javaFileListFlavor )) {
					o =  t.getTransferData( DataFlavor.javaFileListFlavor );
					if( o instanceof java.util.List ) {
						fileList	= (java.util.List) o;
						collDefs	= new ArrayList();
						for( int i = 0; i < fileList.size(); i++ ) {
							f = (File) fileList.get( i );
							try {
								if( SynthDef.isDefFile( f )) {
									defs = SynthDef.readDefFile( f );
									for( int j = 0; j < defs.length; j++ ) {
										collDefs.add( defs[ j ]);
									}
								} else {
									System.err.println( "Not a synth def file : " + f.getName() );
								}
							}
							catch( IOException e1 ) {
								JCollider.displayError( enc_this, e1, "Drop File" );
							}
						}
						if( !collDefs.isEmpty() ) {
							Collections.sort( collDefs, synthDefNameComp );
							defTables[ idx ].addDefs( collDefs );
							return true;
						}
					}
				}
			}
			catch( UnsupportedFlavorException e1 ) {}
			catch( IOException e2 ) {
				JCollider.displayError( enc_this, e2, "Drop File" );
			}

			return false;
		}
		
		public boolean canImport( JComponent c, DataFlavor[] flavors )
		{
			for( int i = 0; i < flavors.length; i++ ) {
				if( flavors[i].equals( DataFlavor.javaFileListFlavor )) return true;
			}
			return false;
		}
	} // class PathTransferHandler
}