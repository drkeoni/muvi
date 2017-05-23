#define PROCESSING_COLOR_SHADER

// Redefine below to see the tiling...
//#define SHOW_TILING

#define TAU 6.28318530718
#define PERIOD 13.0
#define MAX_ITER 5

varying vec4 vertColor;
varying vec4 vertTexCoord;

uniform float     iGlobalTime;           // shader playback time (in seconds)

float discreteColor = 300.0;

/*
   Original GLSL code is here: https://www.shadertoy.com/view/MdlXz8

   2/24/17 Modified by Jon Sorenson to be a processing shader, changed colors, tiling parameters

   Water turbulence effect by joltz0r 2013-07-04, improved 2013-07-07
*/
void main()
{
	float time = iGlobalTime * .1+23.0;
    // uv should be the 0-1 uv of texture
    vec2 uv = vertTexCoord.xy;

    vec2 p = mod(uv*PERIOD, PERIOD)-200.0;
    vec2 i = vec2(p);
	float c = 1.0;
	float inten = .005;

	for (int n = 0; n < MAX_ITER; n++)
	{
		float t = time * (1.0 - (3.5 / float(n+1)));
		i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
		c += 1.0/length(vec2(p.x / (sin(i.x+t)/inten),p.y / (cos(i.y+t)/inten)));
	}
	c /= float(MAX_ITER);
	c = 1.17-pow(c, 1.4);
	vec3 colour = floor((0.2 * vec3(pow(abs(c), 10.0)))*discreteColor)/discreteColor;
    colour = clamp(colour + vec3(0.3, 0.55, 0.7), 0.0, 0.8);

	gl_FragColor = vec4(colour, 1.0);
}