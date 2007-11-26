/*
 *  OSCResponderNode.java
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
 *		30-Sep-06	modified to comply with new NetUtil version
 */

package de.sciss.jcollider;

import java.io.IOException;
import java.net.SocketAddress;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;

/**
 *	Similar operation as the SClang class
 *	of the same name, but slightly different implementation,
 *	based on the new <code>OSCMultiResponder</code> class
 *	and <code>NetUtil</code>.
 *	<P>
 *	<B>As of v0.29</B> the creator signature had to be changed to use a <code>Server</code> instead
 *	of a network address, unfortunately (a side effect of using <code>OSCClient</code> in <code>Server</code>
 *	and <code>OSCMultiResponder</code>). So you may need to update old code.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.31, 08-Oct-07
 */
public class OSCResponderNode
implements OSCListener	// , Runnable
{
//	private final InetSocketAddress		addr;
	private final Server				server;
	private final String				cmdName;
	private final OSCListener			action;
	private OSCMultiResponder			multi			= null;
	private boolean						removeWhenDone	= false;

//	private final List					collMessages	= Collections.synchronizedList( new ArrayList() );	// elements = IncomingMessage instances

	/**
	 *	Creates a new responder node to listen
	 *	for messages coming in from the given server.
	 *	Filters out messages different from
	 *	the specified command name. Upon reception,
	 *	the provided action is invoked.
	 *	<p>
	 *	After creating the responder, the <code>add</code>
	 *	method has to be called separately to actually start
	 *	the listening process.
	 *
	 *	@param	s			server of incoming messages
	 *	@param	cmdName		name of the OSC command at whose
	 *						arrival the action is invoked
	 *	@param	action		the action's <code>messageReceived</code>
	 *						method is called upon message reception.
	 *						Note that just as specified in the
	 *						<code>OSCListener</code> interface, the action
	 *						should not assume to be in any particular thread.
	 *						The current implementation calls the action in the
	 *						OSC listening thread, but this is not guaranteed.
	 *						Calls to Swing components should be deferred appropriately.
	 *
	 *	@see	Server
	 *	@see	#add()
	 */
	public OSCResponderNode( Server s, String cmdName, OSCListener action )
	{
		this.server		= s;
		this.cmdName	= cmdName;
		this.action		= action;
	}

	/**
	 *	Queries the name which is
	 *	used as the message filter
	 *
	 *	@return	the name of the OSC command to which this
	 *			responder listens
	 */
	public String getCommandName()
	{
		return cmdName;
	}

	/**
	 *	Adds the node to the list of actively listening nodes.
	 *	If you are uncertain about the node's state, check
	 *	<code>isListening</code> first, since this method will
	 *	throw an <code>IllegalStateException</code> if you try
	 *	to add it twice.
	 *
	 *	@return		the responder node (for convenience)
	 *
	 *	@see		#remove()
	 *	@see		#isListening()
	 *
	 *	@throws		IllegalStateException	if the node has already been added
	 */
	public OSCResponderNode add()
	throws IOException
	{
		if( isListening() ) throw new IllegalStateException( "OSCResponderNode.add() : duplicate call" );
		multi = server.getMultiResponder();
		multi.addNode( this );
		return this;
	}
	
	/**
	 *	Queries the node's state.
	 *
	 *	@return		<code>true</code> if the node is active
	 *				(was added), <code>false</code> otherwise
	 *				(newly created node or removed)
	 */
	public boolean isListening()
	{
		return( multi != null );
	}

	/**
	 *	Tags the node to remove itself after the next
	 *	unfiltered message arrived. If the node shall
	 *	receive exactly one message, a clean code must
	 *	call this method before calling the <code>add</code>
	 *	method.
	 *
	 *	@return		the responder node (for convenience)
	 */
	public OSCResponderNode removeWhenDone()
	{
		removeWhenDone	= true;
		return this;
	}
	
	/**
	 *	This method is called as part of
	 *	the implementation of the <code>OSCListener</code>
	 *	interface. It dispatches the message to
	 *	the action. If <code>removeWhenDone</code>
	 *	was called, it will remove the node after
	 *	the action returns.
	 *
	 *	@see	#removeWhenDone()
	 */
	public void messageReceived( OSCMessage msg, SocketAddress sender, long time )
	{
		if( isListening() ) {
			action.messageReceived( msg, sender, time );
			if( removeWhenDone ) {
				try {
					remove();	// OSCMultiResponder will take care of thread issues
				}
				catch( IOException e1 ) {
					e1.printStackTrace( Server.getPrintStream() );
				}
//				collMessages.clear();
			}
		}
	}

	/**
	 *	Removes the node from the list of actively
	 *	listening nodes. If the node was already removed,
	 *	this method does nothing.
	 *
	 *	@return		the responder node (for convenience)
	 *
	 *	@see		#add()
	 */
	public OSCResponderNode remove()
	throws IOException
	{
		final OSCMultiResponder m = multi;
	
		if( m != null ) {
			try {
				m.removeNode( this );
			}
			finally {
				multi = null;
			}
		}
		return this;
	}
	
//	// ----------- internal classes -----------
//
//	private static class IncomingMessage
//	{
//		private final OSCMessage		msg;
//		private final SocketAddress		sender;
//		private final long				time;
//		
//		private IncomingMessage( OSCMessage msg, SocketAddress sender, long time )
//		{
//			this.msg 	= msg;
//			this.sender	= sender;
//			this.time	= time;
//		}
//	}
}