## Jotting down ideas

I like the vinyl-inspired audio reactive video [here](http://mattdesl.github.io/spins/).  First could do the same thing
with color and MFCCs.  Thinking about having the line change direction as it spirals around (clockwise vs. counter-clockwise).
Multiple spirals?  Particle interactions?  3D?

An array of hexagons, like lily pads.  Light up with MFCC signals.

I really like the effect where foreground images are very sharp (and photorealistic ideally) and the background is
heavily blurred (depth of focus effect).

Wrapping the musical line around a faux music box cylinder,
rotating in the middle of the screen.  Lines of "music" spin off.

Go back to a crystal simulation I programmed in England.  Have a landscape
growing from falling snow crystals or sand or water drops, where the
drops hit some of them grow and become Lindenmayer ferns.

#### 02/21/2016

I'm really interested in art from reaction-diffusion systems.
[Jonathan McCabe](https://www.flickr.com/photos/jonathanmccabe/sets) has
amazing generative art---based on various algorithms---with reaction-diffusion
systems playing an important role.

To map music to a 2D art canvas, start with the song as a trajectory
in time, MFCC coefficients, and loudness.  How do we map this trajectory
to space?  And for generative systems that require significant time to evolve a
 picture (RD systems and cellular automata), how does this interrelate with
 the motion described by
the song?

For example, we can map the MFCC coefficients (call them 12) to 12 regions on
the canvas.  As the song proceeds, pictures evolve in the region corresponding
to the dominant MFCC coefficient.

Or time can correspond to the regions and we draw different pictures at different
times corresponding to the local volume and MFCC coefficients (I've implemented
this one before).

Some of the best art from McCabe is multi-scale application of the same ideas,
which introduces another way to proceed.  Allow the current scale of the
are to change based on...time?

