#!/bin/sh

APPNAME=JCollider
APPVERSION=0.32

echo "=========================================="
echo "= this script creates html javadoc files ="
echo "= in a subfolder 'doc' in the current    ="
echo "= work directory. Be sure to cd into the ="
echo "= $APPNAME folder before running this"
echo "= script!!!                              ="
echo "=========================================="
echo
echo "NOTE : white space characters in path names must be replaced by %20"
echo

PACKAGES="de.sciss.jcollider de.sciss.jcollider.gui de.sciss.jcollider.test"
CLASSPATH="libraries/NetUtil.jar"
JAVADOC_OPTIONS="-quiet -use -tag synchronization -tag todo -tag warning -source 1.4 -version -author -sourcepath src/ -d doc/api"
WINDOW_TITLE="$APPNAME v$APPVERSION API"

GLOBAL_NETUTIL=http://www.sciss.de/netutil/doc/api/
GLOBAL_JAVA=http://java.sun.com/j2se/1.4.2/docs/api/

REFER_OFFLINE=0
read -er -p "Let javadoc use local API copies when creating docs (y,N)? "
for f in Y y j J; do if [ "$REPLY" = $f ]; then REFER_OFFLINE=1; fi done
LINK_OFFLINE=0
read -er -p "Should the resulting HTML files link to local API copies (y,N)? "
for f in Y y j J; do if [ "$REPLY" = $f ]; then LINK_OFFLINE=1; fi done

if [ $(($REFER_OFFLINE|LINK_OFFLINE)) != 0 ]; then
  LOCAL_JAVA="/Developer/ADC%20Reference%20Library/documentation/Java/Reference/1.4.2/doc/api"
  read -er -p "Local Java 1.4.2 API folder ('$LOCAL_JAVA')? "
  if [ "$REPLY" != "" ]; then LOCAL_JAVA="$REPLY"; fi
  LOCAL_NETUTIL="/Users/rutz/Documents/devel/NetUtil/doc/api"
  read -er -p "Local NetUtil API folder ('$LOCAL_NETUTIL')? "
  if [ "$REPLY" != "" ]; then LOCAL_NETUTIL="$REPLY"; fi
# LOCAL_MRJ="/Volumes/Claude/Developer/MRJAdapter/Documentation"
# read -er -p "Local MRJAdapter API folder ('$LOCAL_MRJ')? "
# if [ "$REPLY" != "" ]; then LOCAL_MRJ="$REPLY"; fi
  
  if [ $LINK_OFFLINE != 0 ]; then
#  	LINK_OPTIONS="-link \"file://$LOCAL_JAVA\" -link \"file://$LOCAL_NETUTIL\""
  	LINK_OPTIONS="-link file://$LOCAL_JAVA -link file://$LOCAL_NETUTIL"
  else
#	LINK_OPTIONS="-linkoffline $GLOBAL_JAVA \"file://$LOCAL_JAVA\" -linkoffline $GLOBAL_NETUTIL \"file://$LOCAL_NETUTIL\""
    LINK_OPTIONS="-linkoffline $GLOBAL_JAVA file://$LOCAL_JAVA -linkoffline $GLOBAL_NETUTIL file://$LOCAL_NETUTIL"
  fi;
else
	LINK_OPTIONS="-link $GLOBAL_JAVA -link $GLOBAL_NETUTIL"
fi

# CMD="javadoc $JAVADOC_OPTIONS -windowtitle \"$WINDOW_TITLE\" $LINK_OPTIONS -classpath $CLASSPATH $PACKAGES"
CMD="javadoc $JAVADOC_OPTIONS $LINK_OPTIONS -classpath $CLASSPATH $PACKAGES"
echo $CMD
$CMD

echo "---------- done ----------"
