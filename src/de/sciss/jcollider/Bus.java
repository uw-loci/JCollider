/*
 *  Bus.java
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
 *		04-Aug-05	created
 */

package de.sciss.jcollider;

import java.io.PrintStream;

/**
 *	Mimics SCLang's Bus class,
 *	that is, it's a client side
 *	representation of an audio or control bus
 *
 *	@warning	this is a quick direct translation from SClang
 *				which is largely untested. before all methods have been
 *				thoroughly verified, excepted some of them to be wrong
 *				or behave different than expected. what certainly works
 *				is instantiation
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.31, 08-Oct-07
 *
 *	@todo		missing methods (set, setn, fill, get, getn ...)
 */
public class Bus
implements Constants
{
	private final Server	server;

	private Object			rate;
	private int				index;
	private int				numChannels;

	/**
	 *	Creates an mono audio bus on the server at index 0.
	 *	This does not use the server's allocators.
	 *
	 *	@param	server	the <code>Server</code> on which the bus resides
	 */
	public Bus( Server server )
	{
		this( server, kAudioRate );
	}

	public Bus( Server server, Object rate )
	{
		this( server, rate, 0 );
	}

	public Bus( Server server, Object rate, int index )
	{
		this( server, rate, index, 1 );
	}

	public Bus( Server server, Object rate, int index, int numChannels )
	{
		this.rate			= rate;
		this.index			= index;
		this.numChannels	= numChannels;
		this.server			= server;
	}

	public static Bus control( Server server )
	{
		return Bus.control( server, 1 );
	}

	public static Bus control( Server server, int numChannels )
	{
		final int alloc	= server.getControlBusAllocator().alloc( numChannels );
		
		if( alloc == -1 ) {
			Server.getPrintStream().println(
				"Bus.control: failed to get a control bus allocated. " +
				"numChannels: " + numChannels + "; server: " + server.getName() );
			return null;
		} else {
			return new Bus( server, kControlRate, alloc, numChannels );
		}
	}

	public static Bus audio( Server server )
	{
		return Bus.audio( server, 1 );
	}
	
	public static Bus audio( Server server, int numChannels )
	{
		final int alloc	= server.getAudioBusAllocator().alloc( numChannels );
		
		if( alloc == -1 ) {
			Server.getPrintStream().println(
				"Bus.audio: failed to get a audio bus allocated. " +
				"numChannels: " + numChannels + "; server: " + server.getName() );
			return null;
		} else {
			return new Bus( server, kAudioRate, alloc, numChannels );
		}
	}

	public static Bus alloc( Server server, Object rate )
	{
		return Bus.alloc( server, rate, 1 );
	}

	public static Bus alloc( Server server, Object rate, int numChannels )
	{
		if( rate == kAudioRate ) {
			return Bus.audio( server, numChannels );
		} else if( rate == kControlRate ) {
			return Bus.control( server, numChannels );
		} else {
			throw new IllegalArgumentException( rate.toString() );
		}
	}
	
	public String toString()
	{
		return( "Bus(" + server.getName() + ", " + getRate() + ", " + getIndex() + ", " + getNumChannels() + ")" );
	}

	public Object getRate()
	{
		return rate;
	}
	
	public int getNumChannels()
	{
		return numChannels;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public Server getServer()
	{
		return server;
	}
	
	private void setRate( Object rate )
	{
		this.rate	= rate;
	}
	
	private void setNumChannels( int numChannels )
	{
		this.numChannels = numChannels;
	}
	
	private void setIndex( int index )
	{
		this.index	= index;
	}

//	set { arg ... values; // shouldn't be larger than this.numChannels
//		server.sendBundle(nil,(["/c_set"] 
//			++ values.collect({ arg v,i; [index + i ,v] }).flat));
//	}
//	setMsg { arg ... values;
//		^["/c_set"] 
//			++ values.collect({ arg v,i; [index + i ,v] }).flat
//	}
//	
//	setn { arg values;
//		// could throw an error if values.size > numChannels
//		server.sendBundle(nil,
//			["/c_setn",index,values.size] ++ values);
//	}
//	setnMsg { arg values;
//		^["/c_setn",index,values.size] ++ values;
//	}
//	get { arg action;
//		OSCpathResponder(server.addr,['/c_set',index], { arg time, r, msg; 
//			action.value(msg.at(2)); r.remove }).add;
//		server.listSendMsg(["/c_get",index]);
//	}	
//	getn { arg count, action;
//		OSCpathResponder(server.addr,['/c_setn',index],{arg time, r, msg; 
//			action.value(msg.copyToEnd(3)); r.remove } ).add; 
//		server.listSendMsg(["/c_getn",index, count]);
//	}
//	getMsg {
//		^["/c_get",index];
//	}
//	getnMsg { arg count, action;
//		^["/c_getn",index, count ? numChannels];
//	}
//
//	fill { arg value,numChans;
//		// could throw an error if numChans > numChannels
//		server.sendBundle(nil,
//			["/c_fill",index,numChans,value]);
//	}
//	
//	fillMsg { arg value;
//		^["/c_fill",index,numChannels,value];
//	}
//
	
	public void free()
	{
		if( getIndex() == -1 ) {
			printOn( Server.getPrintStream() );
			Server.getPrintStream().println( " has already been freed" );
			return;
		}

		if( getRate() == kAudioRate ) {
			getServer().getAudioBusAllocator().free(index);
		} else if( getRate() == kControlRate ) {
			getServer().getControlBusAllocator().free(index);
		} else {
			throw new IllegalStateException( getRate().toString() );
		}

		setIndex( -1 );
		setNumChannels( -1 );
	}
	
	// allow reallocation
	public void alloc()
	{
		if( getRate() == kAudioRate ) {
			setIndex( getServer().getAudioBusAllocator().alloc( getNumChannels() ));
		} else if( getRate() == kControlRate ) {
			setIndex( getServer().getControlBusAllocator().alloc( getNumChannels() ));
		} else {
			throw new IllegalStateException( getRate().toString() );
		}
	}
	
	public void realloc()
	{
		if( getIndex() == -1 ) return;
	
		final Object	oldRate	= getRate();
		final int		oldCh	= getNumChannels();
		
		free();
		setRate( oldRate );
		setNumChannels( oldCh );
		alloc();
	}

//	// alternate syntaxes
//	setAll { arg value;
//		this.fill(value,numChannels);
//	}
//	
//	value_ { arg value;
//		this.fill(value,numChannels);
//	}
	
	public void printOn( PrintStream stream )
	{ 
		stream.print( this.getClass().getName() + "(" + getServer().getName() + "," + getRate() + "," +
			getIndex() + "," + getNumChannels() + ")" );
	}
	
	public boolean equals( Object o )
	{
		if( o instanceof Bus ) {
			final Bus aBus = (Bus) o;
			return( this.getIndex()			== aBus.getIndex() &&
					this.getNumChannels()	== aBus.getNumChannels() &&
					this.getRate()			== aBus.getRate() &&
					this.getServer()		== aBus.getServer() );
		} else {
			return false;
		}
	}
	
	public int hashCode()
	{
		return( getIndex() ^ -getNumChannels() ^ getRate().hashCode() ^ getServer().hashCode() );
	}
	
	/**
	 *	Queries whether this bus is playing audio
	 *	onto the hardware audio interface channels.
	 *
	 *	@return	<code>true</code> if this bus plays audio on audible interface channels,
	 *			<code>false</code> otherwise
	 */
	public boolean isAudioOut()
	{
		return( (rate == kAudioRate) && (getIndex() < getServer().getOptions().getFirstPrivateBus()) );
	}
	
//	ar {
//		if(rate == \audio,{
//			^In.ar(index,numChannels)
//		},{
//			//"Bus converting control to audio rate".inform;
//			^K2A.ar( In.kr(index,numChannels) )
//		})
//	}
//	
//	kr {
//		if(rate == \audio,{
//			^A2K.kr(index,numChannels)
//		},{
//			^In.kr(index,numChannels)
//		})
//	}

//	play { arg target=0, outbus, fadeTime, addAction=\addToTail;
//		if(this.isAudioOut.not,{ // returns a Synth
//			^{ this.ar }.play(target, outbus, fadeTime, addAction);
//		});
//	}
}

