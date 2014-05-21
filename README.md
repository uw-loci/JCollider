# JCollider
-- version 0.37 (built 11-jan-10) --

### statement

JCollider is a java library to build clients for the SuperCollider server architecture. While staying rather compact, it provides a lot of the functionality found in the SuperCollider Language (`sclang`) application. JCollider uses the same concept of mirroring the server (`scsynth`) objects on the client side as `sclang`, and its API is fairly similar to classes found in `sclang`. Some additional GUI helper classes are provided.

JCollider is (C)opyright 2005-2010 by Hanns Holger Rutz. All rights reserved. JCollider is released under the [GNU General Public License](licenses/JCollider-License.txt) which protects your freedom and ensures that products built using this library are also free software. The software comes with absolutely no warranties.

To contact the author, send an email to `contact at sciss.de`.
For project status, API and current version, visit [www.sciss.de/jcollider](http://www.sciss.de/jcollider/).

### requirements / installation

JCollider is platform independant, but requires a [Java](http://java.sun.com)&trade; 1.4 SE runtime environment (JRE) or development kit (JDK). since it is designed to control a supercollider server, you will have to install one, if you haven't yet done so. the main supercollider portal is [supercollider.sf.net](http://supercollider.sf.net). JCollider uses the NetUtil OSC library which is licensed under the [GNU Lesser General Public License](licenses/NetUtil-License.txt) and which is included in this release. To access the source code of NetUtil, please download the full package from [www.sciss.de/netutil](http://www.sciss.de/netutil/).

### download

The current version can be downloaded here:

### 
documentation

Documentation comes in the form of JavaDoc. The generate the docs, open a terminal, `cd` into the JCollider folder and run:

```
$ ant doc
```

there is no real tutorial or manual at the moment, however if you are familiar with `sclang`, it is straight forward to get started. the API documentation (see below) is not yet complete but already quite robust.

### 
compilation / usage

_Please read the [DevelopersNeeded](DevelopersNeeded!.txt) note._

The downloaded archive comes already with a built version of JCollider, that is `build/JCollider.jar` to you may not need to compile it yourself.

The source code can be compiled using Eclipse SDK 3.2+ ([www.eclipse.org](http://www.eclipse.org)) and Apache Ant 1.6.5+ ([ant.apache.org](http://ant.apache.org/)). You need to install [Ant-Contrib](http://ant-contrib.sourceforge.net) as well (first install Ant if you do not have it, then download Ant-Contrib and copy `ant-contrib.jar` into Ant's `lib` folder). The Eclipse project file is configured to run the regular Ant build file. You can make a clean build just with Ant from the terminal:

```
$ ant clean jar
$ ant test
```

The `"test"` will just launch the JCollider demo so you can see if the build works; you can leave away that task. By default, NetUtil is included in the resulting file `build/JCollider.jar`. If wish to link externally to it, you can build like this:

```
$ ant -Dplain=true clean jar
```

the original demo project is included in this build and can be executed as follows:

```
$ java -jar build/JCollider.jar --test1
```

the demo opens two frames, one containing a list of synth defs, the other one being a small server window just like you know from `sclang`. first check, that the path name to the application is correct, alternatively start the server manually before launching the demo. now select a synth definition from the tables and press the play button. to stop all synths, press the stop button. to view the synth def, press one of the next two buttons. to see a tree of all known nodes, press the right most button.

note that some synth defs will only run on mac os because they use mac-only ugens (e.g. `MouseX`). also note that `CombDist` and `RingMod` are insert effects which have to be started _before_ starting an oscillator synth def.

Alternatively, `--test2` runs a demo for generating a SynthDef with controls which can be operated from a GUI.

### projects using jcollider

Please let me know when you want to have your projects added here.

### to-do / known issues

### change history

* v0.37 (jan 2010 - SVN rev. 27)
* v0.36 (oct 2009 - SVN rev. 25)
* v0.35 (sep 2009 - SVN rev. 19)
* v0.34 (jul 2009 - SVN rev. 16)
* v0.32 (feb 2008 - SVN rev. 3)
* v0.31 (nov 2007)
* v0.30 (jul 2007)
* v0.29 (oct 2006)
* v0.28 (jul 2006)
* v0.27 (feb 2006)
* v0.26 (nov 2005)
* v0.24 (oct 2005)
* v0.23 (oct 2005)
* v0.21 (sep 2005)
* v0.2 (sep 2005)
