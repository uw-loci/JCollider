/*
 *  UGenInfo.java
 *  JCollider
 *
 *  Copyright (c) 2004-2007 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de , or visit http://www.sciss.de/jcollider
 *
 *
 *	JCollider is closely modelled after SuperCollider Language,
 *	often exhibiting a direct translation from Smalltalk to Java.
 *	SCLang is a software originally developed by James McCartney,
 *	which has become an Open Source project.
 *	See http://www.audiosynth.com/ for details.
 *
 *
 *  Changelog:
 *		04-Sep-05	created
 */

package de.sciss.jcollider;

//import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
//import java.io.RandomAccessFile;
//import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *	As stated elsewhere, it was decided to not implement separate
 *	classes for the different unit generators in JCollider. Instead
 *	there is one monolithic <code>UGen</code> class. To facilitate
 *	the visual represenation of UGens and to allow syntax checking
 *	and automatic default value assignment, the <code>UGenInfo</code>
 *	class was created which keeps records of all known ugen 
 *	&quot;classes&quot;. The database needs to be read in once
 *	explictly, usually you will do this once your programme launches.
 *	The database is merged with the JCollider library .jar file
 *	(<code>ugendefs.xml</code>, along with the document type
 *	descriptor <code>ugendefs.dtd</code>), and can be easily
 *	edited as to provide more ugens newly added to the supercollider
 *	application. This database file would also naturally be the
 *	place to put new attributes such as icon files if you want
 *	to build a graphic synthesis system, or links to help file and such.
 *	<p>
 *	Once you have initialized the database, using the <code>readDefinitions</code>
 *	method, you don't deal with <code>UGenInfo</code> any more, since
 *	the UGen constructor methods will query the database automatically.
 *	This in turn means, you cannot use <code>UGen.ar( ... )</code> for example,
 *	unless the database has been initialized. However there is nothing
 *	wrong with using JCollider without the UGen constructors (for example
 *	using only pre-stored synth defs), and in this case you may skip
 *	the database initialization, saving some startup time.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.31, 15-Oct-07
 *
 *	@see		#readDefinitions
 *	@see		UGen#ar( String )
 */
public class UGenInfo
implements Constants, Comparable
{
	private static final String UGENDEFS_DTD = "ugendefs.dtd";

	private static final EntityResolver dtdResolver = new DTDResolver();
	
	/**
	 *	This field contains a read-only map
	 *	which maps String (ugen class names and
	 *	alias names such as the unary/binary operator names)
	 *	to <code>UGenInfo</code> elements).
	 *	<p>
	 *	For example <code>UGenInfo.infos.get( &quot;PlayBuf&quot; )</code>
	 *	will return the dataset for the PlayBuf ugen,
	 *	<code>UGenInfo.infos.get( &quot;reciprocal&quot; )</code>
	 *	will return the dataset for the UnaryOpUGen ugen
	 *	(there is only one dataset for all the operators).
	 */
	public static Map	infos;

	/**
	 *	Value for <code>outputType</code> : the ugen
	 *	has a fixed number of outputs
	 */
	public static final int	OUTPUT_FIXED		= 0;	// a constant value
	/**
	 *	Value for <code>outputType</code> : the ugen
	 *	has a variable number which needs to be
	 *	specified explictly when constructing the
	 *	ugen (example : <code>PanAz</code>)
	 */
	public static final int	OUTPUT_ARG			= 1;	// explicitly argument for the constructor
	/**
	 *	Value for <code>outputType</code> : the ugen
	 *	has a variable number of outputs depending
	 *	on the length of it's array argument
	 *	(example : <code>Demand</code>)
	 */
	public static final int	OUTPUT_ARRAYSIZE	= 2;	// size of a ugen input array

	/**
	 *	Name of the ugen class
	 */
	public final String		className;
	/**
	 *	Array of the input argument definitions 
	 */
	public final Arg[]		args;
	/**
	 *	Set of all allowed rates at which the ugen can run
	 *
	 *	@see	Constants#kAudioRate
	 *	@see	Constants#kControlRate
	 */
	public final Set		rates;
	/**
	 *	Maps special names (<code>String</code>s) to
	 *	specialIndex values (<code>Integer</code>s).
	 *	For example, the BinaryOpUGen will have mappings
	 *	like &quot;absdif&quot; -> Integer( 38 ) etc.
	 *	For UGens which do not deal with special indices,
	 *	this field is <code>null</code>
	 */
	public final Map		specials;		// maps String special name to Integer( specialIndex ) ; may be null
	/**
	 *	Defines how the number of outputs is
	 *	determined. One of <code>FIXED</code>, <code>ARG</code> or <code>ARRAYSIZE<code>
	 */
	public final int		outputType;		// (fixed|arg|arraySize|pre)
	/**
	 *	For fixed output ugens, the number of outputs.
	 *	For <code>ARG</code> type ugens, the default
	 *	number of outputs (<code>-1</code> if no defaults exist).
	 *	For <code>ARRAYSIZE</code> type ugens, the argument
	 *	index of the array argument to use. 
	 *
	 *	@warning	for now, arrays are only allowed as the
	 *				last argument of the ugen. no more than one
	 *				array is allowed per ugen. as of september 2005,
	 *				all known supercollider ugens fulfill this requirement
	 */
	public final int		outputVal;		// FIXED : # of outputs, ARG : default, ARRAYSIZE : arg idx
	/**
	 *	A tricky workaround invented for <code>LagControl</code> until
	 *	i recognized that this isn't a real ugen. This could be used
	 *	to multiply the array size for <code>ARRAYSIZE</code> output type
	 *	ugens by a constant, say two or one half. So for now, ignore it,
	 *	it will have the value of one for all ugens. This field
	 *	may be deleted in one of the next versions
	 */
	public final float		outputMul;
	
	private UGenInfo( String className, Arg[] args, Set rates, Map specials,
					  int outputType, int outputVal, float outputMul )
	{
		this.className	= className;
		this.args		= args;
		this.rates		= rates;
		this.specials	= specials == null ? specials : Collections.unmodifiableMap( specials );
		this.outputType	= outputType;
		this.outputVal	= outputVal;
		this.outputMul	= outputMul;
	}

	public int compareTo(Object o)
	{
		if( o instanceof UGenInfo ) {
			return this.className.compareTo( ((UGenInfo) o).className );
		} else {
			throw new ClassCastException();
		}
	}
	
	/**
	 *	Returns a string ready to display
	 *	to the user. This will return the ugens
	 *	class name or the operator name for
	 *	unary/binary ops
	 *
	 *	@todo	should find a way to present controls better
	 *			to the user with the different control names accessible
	 */
	public String getDisplayName( UGen ugen )
	{
		if( specials == null ) return className;
		
		final Integer	specialIndex	= new Integer( ugen.getSpecialIndex() );
		String			specialName;
		
		for( Iterator iter = specials.keySet().iterator(); iter.hasNext(); ) {
			specialName = iter.next().toString();
			if( specials.get( specialName ).equals( specialIndex )) return specialName;
		}
		
		return className;
	}
	
	/**
	 *	Returns the name for one of the ugens inputs.
	 *	This can be used to visualize the ugen, and furthermore
	 *	to create a keyword constructor for the ugen
	 *	(to be done).
	 */
	public String getArgNameForInput( UGen ugen, int argIdx )
	{
		if( args.length == 0 ) return null;
		if( argIdx < args.length ) {
			if( args[ argIdx ].isArray ) {
				return( args[ argIdx ].name + "[0]" );
			} else {
				return args[ argIdx ].name;
			}
		} else {
			if( args[ args.length - 1 ].isArray ) {
				return( args[ args.length - 1 ].name + '[' + (argIdx - args.length + 1) + ']' );
			} else {
				return null;
			}
		}
	}

	/**
	 *	Given the number of instantiating arguments,
	 *	returns the number of output channels the
	 *	ugen will have. For fixed type ugens, returns
	 *	simply the <code>outputVal</code>, for array
	 *	type ugens, calculates the number of outputs from
	 *	<code>numArgs</code>, for ugens that require an
	 *	explicit number-of-channels argument, returns
	 *	<code>pre</code> (if given) or the default value
	 *	<code>outputVal</code>) (if specified).
	 *
	 *	@param	numArgs	the number of arguments which will be used
	 *					to instantiate the ugen
	 *	@param	pre		pre-specified number of outputs (or <code>-1</code>)
	 *	@return	the number of output channels or <code>-1</code>
	 *			if this method is unable to determine the number of channels
	 *			(missing <code>pre</code> argument)
	 */
	public int getNumOutputs( int numArgs, int pre )
	{
		switch( outputType ) {
		case OUTPUT_FIXED:
			return outputVal;
		
		case OUTPUT_ARRAYSIZE:
			return( (int) ((numArgs - this.args.length + 1) * outputMul) );

		case OUTPUT_ARG:
			if( pre == -1 ) return outputVal; else return pre;
			
		default:
			assert false : outputType;
			return -1;
		}
	}

	/**
	 *	Prints a textual representation of this
	 *	dataset onto the given stream
	 */
	public void printOn( PrintStream out )
	{
		boolean		b = false;
	
		out.print( "UGenInfo(\"" + className + "\")\n rates: " );
		for( Iterator iter = rates.iterator(); iter.hasNext(); b = true ) {
			if( b ) out.print( ", " );
			out.print( iter.next() );
		}
		out.print( "\n args: [ " );

		for( int i = 0; i < args.length; i++ ) {
			if( i > 0 ) out.print( ", " );
			if( args[i].isArray ) out.print( "... " );
			out.print( args[i].name );
			if( !Float.isNaN( args[i].def )) {
				out.print( " = " + args[i].def );
			}
		}
		out.println( " ]" );
	}

//	public UGen ar( Object[] args )
//	{
//		if( !rates.contains( kAudioRate )) throw new IllegalArgumentException( kAudioRate );
//	}
//
//	public UGen kr( Object[] args )
//	{
//		if( !rates.contains( kControlRate )) throw new IllegalArgumentException( kControlRate );
//	}
//
//	public UGen ir( Object[] args )
//	{
//		if( !rates.contains( kScalarRate )) throw new IllegalArgumentException( kScalarRate );
//	}
//
//	public UGen dr( Object[] args )
//	{
//		if( !rates.contains( kDemandRate )) throw new IllegalArgumentException( kDemandRate );
//	}

	/**
	 *	Reads in the ugen definition database
	 *	from a text resource inside the libraries jar file
	 *	(<code>ugendefs.xml</code>). Call this method
	 *	once before using the database, i.e. before
	 *	constructing UGens or showing a synth def diagram.
	 *	<p>
	 *	When this method returns, the database is available
	 *	from the static <code>infos</code> field.
	 *
	 *	@throws	IOException	if the definitions file couldn't be read.
	 *						this should never happen if you don't touch the
	 *						library. however, if you modify the xml file as to
	 *						include new ugens, this error may occur if the xml
	 *						file is malformed
	 *
	 *	@see	#infos
	 */
	public static void readDefinitions()
	throws IOException
	{
		final Document					domDoc;
		final DocumentBuilderFactory	builderFactory;
		final DocumentBuilder			builder;
		final NodeList					ugenList;
		final Map						map			= new HashMap();
		Element							node, elem;
		UGenInfo						ui;

		try {
			builderFactory	= DocumentBuilderFactory.newInstance();
			builderFactory.setValidating( true );
			builder			= builderFactory.newDocumentBuilder();
			builder.setEntityResolver( dtdResolver );
			domDoc			= builder.parse( ClassLoader.getSystemClassLoader().getResourceAsStream( "ugendefs.xml" ));
			node			= domDoc.getDocumentElement();
			ugenList		= node.getElementsByTagName( "ugen" );
			
			for( int i = 0; i < ugenList.getLength(); i++ ) {
				elem		= (Element) ugenList.item( i );
				ui			= decodeUGenNode( domDoc, elem );
				map.put( ui.className, ui );
				if( ui.specials != null ) {
					for( Iterator iter = ui.specials.keySet().iterator(); iter.hasNext(); ) {
						map.put( iter.next(), ui );	// alias entry
					}
				}
			}
		}
		catch( ParserConfigurationException e1 ) {
			throw new IOException( e1.getClass().getName() + " : " + e1.getLocalizedMessage() );
		}
		catch( SAXParseException e2 ) {
			throw new IOException( e2.getClass().getName() + " : " + e2.getLocalizedMessage() );
		}
		catch( SAXException e3 ) {
			throw new IOException( e3.getClass().getName() + " : " + e3.getLocalizedMessage() );
		}
		
		UGenInfo.infos = map;
	}
	
//	public static void writeBinaryDefinitions( File path )
//	throws IOException
//	{
//		final Object[]			keys 	= infos.keySet().toArray();
//		final RandomAccessFile	raf		= new RandomAccessFile( path, "wb" );
//		UGenInfo				info;
//	
//		raf.writeInt( );
//		
//		Arrays.sort( keys, null );
//		
//		for( int i = 0; i < keys.length; i++ ) {
//			info	= (UGenInfo) infos.get( keys[ i ]);
//			info.className
//		}
//	}

	private static UGenInfo decodeUGenNode( Document domDoc, Element node )
	{
		final Set				rates		= new HashSet();
		final String			className	= node.getAttribute( "class" );
		final NodeList			argList		= node.getElementsByTagName( "arg" );
		final NodeList			outList		= node.getElementsByTagName( "outputs" );
		final NodeList			specialList	= node.getElementsByTagName( "special" );
		final Arg[]				args		= new Arg[ argList.getLength() ];
		final Map				specials;
		
		Element					elem;
		String					val, name;
		int						n, outputType, outputVal;
		boolean					b;
		float					defaultValue, outputMul;
		
		val	= node.getAttribute( "rates" );
		if( val.indexOf( "audio" ) >= 0 )	rates.add( kAudioRate );
		if( val.indexOf( "control" ) >= 0 )	rates.add( kControlRate );
		if( val.indexOf( "scalar" ) >= 0 )	rates.add( kScalarRate );
		if( val.indexOf( "demand" ) >= 0 )	rates.add( kDemandRate );
		
		for( int i = 0; i < argList.getLength(); i++ ) {
			elem			= (Element) argList.item( i );
			name			= elem.getAttribute( "name" );
			val				= elem.getAttribute( "type" );
			b				= val.equals( "array" );
			if( b && (i != argList.getLength() - 1) ) {
				throw new IllegalArgumentException( className + "(arg:" + name + ") : array not allowed here" );
			}
			val				= elem.getAttribute( "def" );
			defaultValue	= Float.NaN;
			
			if( val.length() > 0 ) {
				try {
					defaultValue	= Float.parseFloat( val );
				}
				catch( NumberFormatException e1 ) {
					System.err.println( className + "(arg:" + name + ") : " + e1.getClass().getName() +
						" : " + e1.getLocalizedMessage() );
				}
			}
			
			args[ i ] = new Arg( name, defaultValue, b );
		}
		
		// #IMPLIED
		outputType	= OUTPUT_FIXED;
		outputVal	= -1;
		outputMul	= 1.0f;
		if( outList.getLength() > 0 ) {
			try {
				elem	= (Element) outList.item( 0 );
				val		= elem.getAttribute( "type" );
				if( val.length() > 0 ) {
					if( val.equals( "fixed" )) {
						outputType	= OUTPUT_FIXED;
					} else if( val.equals( "arg" )) {
						outputType	= OUTPUT_ARG;
					} else if( val.equals( "arraySize" )) {
						outputType	= OUTPUT_ARRAYSIZE;
					}
				}
				val		= elem.getAttribute( "val" );
				if( val.length() > 0 ) {
					if( outputType == OUTPUT_ARRAYSIZE ) {
						for( int i = 0; i < args.length; i++ ) {
							if( args[ i ].name.equals( val )) {
								outputVal = i;
								break;
							}
						}
						if( outputVal == -1 ) System.err.println( className + " (outputs) : illegal ref " + val );
					} else {
						outputVal	= Integer.parseInt( val );
					}
				}
				val		= elem.getAttribute( "mul" );
				if( val.length() > 0 ) {
					outputMul	= Float.parseFloat( val );
				}
			}
			catch( NumberFormatException e1 ) {
					System.err.println( className + " (outputs) : " + e1.getClass().getName() +
						" : " + e1.getLocalizedMessage() );
			}
		}
		if( (outputType == OUTPUT_FIXED) && (outputVal == -1) ) outputVal = 1;	// default
		
		if( specialList.getLength() > 0 ) {
			specials = new HashMap();
			for( int i = 0; i < specialList.getLength(); i++ ) {
				elem	= (Element) specialList.item( i );
				name	= elem.getAttribute( "name" );
				val		= elem.getAttribute( "idx" );
				try {
					n	= Integer.parseInt( val );
					specials.put( name, new Integer( n ));
				}
				catch( NumberFormatException e1 ) {
					System.err.println( className + "(arg:" + name + ") : " + e1.getClass().getName() +
						" : " + e1.getLocalizedMessage() );
				}
			}
		} else {
			specials = null;
		}
		
		return new UGenInfo( className, args, rates, specials, outputType, outputVal, outputMul );
	}
	
	/**
	 *	Descriptor for a ugen input argument.
	 */
	public static class Arg
	{
		/**
		 *	Name of the argument (same as in SClang).
		 */
		public final String		name;
		/**
		 *	Allowed range of its value and default value.
		 *	The min/max fields are currently unused and
		 *	are set to <code>Float.NEGATIVE_INFINITY</code>
		 *	and <code>Float.POSITIVE_INFINITY</code>
		 *	respectively. Could be used in a future version.
		 */
		public final float		min, max, def;
		/**
		 *	If <code>true</code>, this argument requires
		 *	an array of values. As of this version, this flag
		 *	is only allowed once and for the last of all ugen
		 *	input argument.
		 */
		public final boolean	isArray;
		
		private Arg( String name, float min, float max, float def, boolean isArray )
		{
			this.name		= name;
			this.min		= min;
			this.max		= max;
			this.def		= def;
			this.isArray	= isArray;
		}

		private Arg( String name, float def, boolean isArray )
		{
			this( name, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, def, isArray );
		}

//		private Arg( String name, float def )
//		{
//			this( name, def, false );
//		}
//
//		private Arg( String name )
//		{
//			this( name, Float.NaN, false );
//		}
	}

	private static class DTDResolver
	implements EntityResolver
	{
		/**
		 *  This Resolver can be used for loading documents.
		 *	If the required DTD is the Meloncillo session DTD
		 *	("ichnogram.dtd"), it will return this DTD from
		 *	a java resource.
		 *
		 *  @param  publicId	ignored
		 *  @param  systemId	system DTD identifier
		 *  @return				the resolved input source for
		 *						the Meloncillo session DTD or <code>null</code>
		 *
		 *	@see	javax.xml.parsers.DocumentBuilder#setEntityResolver( EntityResolver )
		 */
		public InputSource resolveEntity( String publicId, String systemId )
		throws SAXException
		{
//System.err.println( "hier : "+publicId+"; "+systemId );

			if( systemId.endsWith( UGENDEFS_DTD )) {	// replace our dtd with java resource
				InputStream dtdStream = getClass().getClassLoader().getResourceAsStream( UGENDEFS_DTD );
				InputSource is = new InputSource( dtdStream );
				is.setSystemId( UGENDEFS_DTD );
				return is;
			}
			return null;	// unknown DTD, use default behaviour
		}
	}
}