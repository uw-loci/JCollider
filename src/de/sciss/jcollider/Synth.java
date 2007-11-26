/*
 *  Synth.java
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
 *		02-Oct-05	removed all setGroup statements. to have the group
 *					set correctly, use a NodeWatcher instead
 */

package de.sciss.jcollider;

import java.io.IOException;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;

import de.sciss.net.OSCBundle;
import de.sciss.net.OSCMessage;

/**
 *	Mimics SCLang's Synth class,
 *	that is, it's a client side
 *	representation of a synth in the synthesis graph
 *
 *	@warning	this is a quick direct translation from SClang
 *				which is largely untested. before all methods have been
 *				thoroughly verified, excepted some of them to be wrong
 *				or behave different than expected. what certainly works
 *				is instantiation and new-messages
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.31, 08-Oct-07
 */
public class Synth
extends Node
{
	private final String defName;

	// immediately sends
	public Synth( String defName, String[] argNames, float[] argValues, Node target )
	throws IOException
	{
		this( defName, argNames, argValues, target, kAddToHead );
	}

	// immediately sends
	public Synth( String defName, String[] argNames, float[] argValues, Node target, int addAction )
	throws IOException
	{
		this( defName, target.getServer(), target.getServer().nextNodeID() );
		
		getServer().sendMsg( newMsg( target, argNames, argValues, addAction ));
	}
	
	// doesn't send
	private Synth( String defName, Server server, int nodeID )
	{
		super( server, nodeID );
		
		this.defName	= defName;
	}
	
	public String getDefName()
	{
		return defName;
	}

	public OSCMessage newMsg()
	{
		return newMsg( getServer().asTarget() );
	}

	public OSCMessage newMsg( Node target )
	{
		return newMsg( target, null, null );
	}

	public OSCMessage newMsg( Node target, String[] argNames, float[] argValues )
	{
		return newMsg( target, argNames, argValues, kAddToHead );
	}

	public OSCMessage newMsg( Node target, String[] argNames, float[] argValues, int addAction )
	{
		if( target == null ) target = getServer().getDefaultGroup();
	
// removed 02-oct-05
//		this.setGroup( addAction == kAddToHead || addAction == kAddToTail ?
//			(Group) target : target.getGroup() );
			
		final int		argNum	= argNames == null ? 0 : argNames.length;
		final Object[]	allArgs	= new Object[ argNum * 2 + 4 ];
		
		allArgs[ 0 ]			= getDefName();
		allArgs[ 1 ]			= new Integer( getNodeID() );
		allArgs[ 2 ]			= new Integer( addAction );
		allArgs[ 3 ]			= new Integer( target.getNodeID() );
		
		for( int i = 0, j = 4; i < argNum; i++ ) {
			allArgs[ j++ ]		= argNames[ i ];
			allArgs[ j++ ]		= new Float( argValues[ i ]);
		}
			
		return new OSCMessage( "/s_new", allArgs );
	}

	public static Synth newPaused( String defName, String[] argNames, float[] argValues, Node target )
	throws IOException
	{
		return Synth.newPaused( defName, argNames, argValues, target, kAddToHead );
	}

	public static Synth newPaused( String defName, String[] argNames, float[] argValues, Node target, int addAction )
	throws IOException
	{
		final Synth		synth	= Synth.basicNew( defName, target.getServer() );
		final OSCBundle	bndl	= new OSCBundle( 0.0 );
		
		bndl.addPacket( synth.newMsg( target, argNames, argValues, addAction ));
		bndl.addPacket( synth.runMsg( false ));

		synth.getServer().sendBundle( bndl );
		return synth;
	}

	// does not send	(used for bundling)
	public static Synth basicNew( String defName, Server server )
	{
		return Synth.basicNew( defName, server, server.nextNodeID() );
	}

	public static Synth basicNew( String defName, Server server, int nodeID )
	{
		return new Synth( defName, server, nodeID );
	}

	public static Synth after( Node aNode, String defName )
	throws IOException
	{
		return Synth.after( aNode, defName, null, null );
	}

	public static Synth after( Node aNode, String defName, String[] argNames, float[] argValues )
	throws IOException
	{
		return new Synth( defName, argNames, argValues, aNode, kAddAfter );
	}

	public static Synth before( Node aNode, String defName )
	throws IOException
	{
		return Synth.before( aNode, defName, null, null );
	}

	public static Synth before( Node aNode, String defName, String[] argNames, float[] argValues )
	throws IOException
	{
		return new Synth( defName, argNames, argValues, aNode, kAddBefore );
	}

	public static Synth head( Group aGroup, String defName )
	throws IOException
	{
		return Synth.head( aGroup, defName, null, null );
	}

	public static Synth head( Node aGroup, String defName, String[] argNames, float[] argValues )
	throws IOException
	{
		return new Synth( defName, argNames, argValues, aGroup, kAddToHead );
	}

	public static Synth tail( Group aGroup, String defName )
	throws IOException
	{
		return Synth.tail( aGroup, defName, null, null );
	}

	public static Synth tail( Node aGroup, String defName, String[] argNames, float[] argValues )
	throws IOException
	{
		return new Synth( defName, argNames, argValues, aGroup, kAddToTail );
	}

	public static Synth replace( Node nodeToReplace, String defName )
	throws IOException
	{
		return Synth.replace( nodeToReplace, defName, null, null );
	}

	public static Synth replace( Node nodeToReplace, String defName, String[] argNames, float[] argValues )
	throws IOException
	{
		return new Synth( defName, argNames, argValues, nodeToReplace, kAddReplace );
	}

	public OSCMessage addToHeadMsg( Group aGroup, String[] argNames, float[] argValues )
	{
		return newMsg( aGroup, argNames, argValues, kAddToHead );
	}

	public OSCMessage addToTailMsg( Group aGroup, String[] argNames, float[] argValues )
	{
		return newMsg( aGroup, argNames, argValues, kAddToTail );
	}
	
	public OSCMessage addAfterMsg( Node aNode, String[] argNames, float[] argValues )
	{
		return newMsg( aNode, argNames, argValues, kAddAfter );
	}

	public OSCMessage addBeforeMsg( Node aNode, String[] argNames, float[] argValues )
	{
		return newMsg( aNode, argNames, argValues, kAddBefore );
	}
	
	public OSCMessage addReplaceMsg( Node aNode, String[] argNames, float[] argValues )
	{
		return newMsg( aNode, argNames, argValues, kAddReplace );
	}

//	// nodeID -1 
//	*grain { arg defName, args, target, addAction=\addToHead;
//		var server;
//		target = target.asTarget;
//		server = target.server;
//		server.sendMsg(9, defName.asDefName, -1, addActions[addAction], target.nodeID, *args);
//			//"/s_new"
//		^nil;
//	}
	
//	get { arg index, action;
//		OSCpathResponder(server.addr,['/n_set',nodeID,index],{ arg time, r, msg; 
//			action.value(msg.at(3)); r.remove }).add;
//		server.sendMsg(44, nodeID, index);	//"/s_get"
//	}
//	getMsg { arg index;
//		^[44, nodeID, index];	//"/s_get"
//	}
//	
//	getn { arg index, count, action;
//		OSCpathResponder(server.addr,['/n_setn',nodeID,index],{arg time, r, msg;
//			action.value(msg.copyToEnd(4)); r.remove } ).add; 
//		server.sendMsg(45, nodeID, index, count); //"/s_getn"
//	}
//	getnMsg { arg index, count;
//		^[45, nodeID, index, count]; //"/s_getn"
//	}

	public String toString()
	{
		if( getName() == null ) {
			return( "Synth(" + getNodeID() + ",\"" + defName + "\")" );
		} else {
			return( "Synth::" + getName() + "(" + getNodeID() + ",\"" + defName + "\")" );
		}
	}

// -------------- TreeNode interface --------------

	public TreeNode getChildAt( int childIndex )
	{
		return null;
	}

	public int getChildCount()
	{
		return 0;
	}
	
	public int getIndex( TreeNode node )
	{
		return -1;
	}
	
	public boolean getAllowsChildren()
	{
		return false;
	}
	
	public boolean isLeaf()
	{
		return true;
	}
	
	public Enumeration children()
	{
		return null;	// XXX allowed?
	}
}