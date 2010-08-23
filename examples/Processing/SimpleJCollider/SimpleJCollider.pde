/**
 *  This simple Processing sketch shows you how you
 *  can use JCollider inside Processing. It assumes
 *  you launched scsynth on udp port 57110, for example
 *  directly from the terminal as
 *
 *  $ scsynth -u 57110
 *
 *  or by booting it from within the SuperCollider application.
 *  As you can see the SynthDef creation is rather painfull,
 *  so mostly you will want to pre-create your SynthDefs in
 *  the SuperCollider language.
 */

import de.sciss.jcollider.*;

Server s;
Group group;
int xx = 0;
int inc = 1;

void setup() {
  try {
    // use client ID 1, so we don't conflict with nodeIDs of sclang...
    s = new Server( "default", new InetSocketAddress( "127.0.0.1", 57110 ),
     new ServerOptions(), 1 );
    s.start();
    UGenInfo.readBinaryDefinitions();
    createDefs();
    group = Group.basicNew( s, 0 ); // the root node
  } catch( IOException e ) { println( "Failed to contact server"); }
  size(200,200);
  stroke(255);
}

void createDefs() throws IOException {
  // simple filtered noise perc
  GraphElem f;
  Control c = Control.kr( new String[] { "freq" }, new float[] { 400f });
  UGenChannel freq = c.getChannel( 0 );
  f = UGen.ar( "Resonz", UGen.array(
    UGen.ar( "WhiteNoise" ), UGen.ar( "WhiteNoise" )),
    freq, UGen.ir( 0.1f ));
  f = UGen.ar( "*", f, UGen.ar( "Lag", UGen.ar( "pow",
     UGen.ar( "Line", UGen.ir( 1 ), UGen.ir( 0 ), UGen.ir( 1 ), UGen.ir( 2 )),
     UGen.ir( 2 )),
     UGen.ir( 0.1 )));
  f = UGen.ar( "Out", UGen.ir( 0 ), f );
  // sending the SynthDef is asynchronous, so we
  // should better make sure this command replies,
  // but for simplicity we just assume that the
  // def is ready on the server when the line first
  // hits the right margin...
  new SynthDef( "test", f ).send( s );
}

void draw() {
  background(0);
  line( xx, 0, xx, height - 1 );
  xx += inc;
  if( xx <= 0 ) {
    xx = 0;
    ding( 400 );
    inc *= -1;
  } else if( xx >= width - 1 ) {
    xx = width - 1;
    ding( 600 );
    inc *= -1;
  }
}

public void stop(){
  s.dispose();
  super.stop();
}

void ding( float freq ) {
  try {
    Synth.head( group, "test",
      new String[] { "freq" }, new float[] { freq });
  } catch( IOException e ) { println( "Failed to spawn synth"); }
}

