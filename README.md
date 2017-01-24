# muvi

![screenshot of VinylSketch2](/data/images/vinyl_example_01_medium.png)

The muvi code base is a home project to explore generative video production
based on music that I've been writing in the last few years.  The tech
stack uses a mix of processing APIs for music and graphics and scala as the
JVM language.

## Sketches

### Lindenmayer

![screenshot of Lindenmayer](/data/images/lindenmayer_example_01_medium.png)

### SnowFall

### VinylSketch2

![screenshot of VinylSketch2](/data/images/vinyl_example_05.png)

### GrayScottSketch2

![screenshot of GrayScottSketch2](/data/images/grayscott_example_01.png)

This sketch generates a video by solving a Gray-Scott reaction-diffusion _plus_ drift system
and displaying the
resulting concentrations as colors. The resulting videos exhibit
a lot of flexibility made possible by
using different
GLSL shaders, colors, filters, PDE parameters, 
camera parameters, and drift parameters.

A live example of this sketch with accompanying music can be found [here.](https://youtu.be/TB2K7XTwpBE)

## Architecture

Over time a modular and reusable architecture has been forming in this code base.  The main
technology stack is the processing 3 library, supported usually by libraries from the toxiclibs
and minim projects.  

All of the processing applets implement the trait `MusicVideoApplet` which provides support for
application logging and  playing an audio file chosen from the databank included in the code base (the
music files under `/data/`).

All of the current sketches include video elements which respond to the music.  The general system
for doing this is contained in the `MusicVideoSystem` singleton class that a sketch
initializes upon setup.  Sketches can register `Agent`s which
receive musical events over time such as volume levels and
the current [Mel-frequency cepstrum](https://en.wikipedia.org/wiki/Mel-frequency_cepstrum) coefficients.
The drawing action of an `Agent` is completely up to the sketch.

## Credits

All of the music and code found here was created by Jon Sorenson and is licensed under a
[Creative Commons License.](https://creativecommons.org/licenses/)
The original inspiration for VinylSketch2 came from the javascript applet
[spins.](http://mattdesl.github.io/spins/) The original GLSL shader code for
solving and plotting the Gray-Scott reaction-diffusion 
equations came from [here.](https://github.com/pmneila/jsexp)

CC by Jon Sorenson &copy;2017