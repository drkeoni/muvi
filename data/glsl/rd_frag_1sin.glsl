#define PROCESSING_TEXTURE_SHADER

#define M_PI 3.1415926535897932384626433832795
#define M_2PI 6.28318530718

varying vec4 vertColor;
varying vec4 vertTexCoord;

uniform float screenWidth;
uniform float screenHeight;
uniform sampler2D texture;
uniform float delta;
uniform float feed;
uniform float kill;

uniform float modMult;
uniform float modPct;
uniform float modOffset;

uniform float feedLowMult;
uniform float feedHighMult;

float step_x = 1.0/screenWidth;
float step_y = 1.0/screenHeight;
float feed_low = feed*feedLowMult;
float feed_high = feed*feedHighMult;
vec2 D = vec2( 0.2099, 0.1105 );

//
// my starting point was https://github.com/pmneila/jsexp
//
void main()
{
    vec2 vUv = vertTexCoord.st;

    vec2 uv = texture2D(texture, vUv).rg;
    vec2 uv0 = texture2D(texture, vUv+vec2(-step_x, 0.0)).rg;
    vec2 uv1 = texture2D(texture, vUv+vec2(step_x, 0.0)).rg;
    vec2 uv2 = texture2D(texture, vUv+vec2(0.0, -step_y)).rg;
    vec2 uv3 = texture2D(texture, vUv+vec2(0.0, step_y)).rg;

    // the feed parameter changes linearly based on squared
    // distance from the center
    float distsq = dot(vUv-0.5,vUv-0.5);
    float falpha = distsq / 0.25;
    float f = feed_low + (feed_high-feed_low)*falpha;

    vec2 lapl = (uv0 + uv1 + uv2 + uv3 - 4.0*uv);
    vec2 d_mod = modMult * D * lapl * (0.5*modPct*(sin(M_2PI*vUv/modPct) + 1.0) + modOffset);
    vec2 uv4 = vec2( d_mod.r - uv.r*uv.g*uv.g + f*(1.0 - uv.r),
                     d_mod.g + uv.r*uv.g*uv.g - (f+kill)*uv.g );
    vec2 dst = uv + delta*uv4;

    gl_FragColor = vec4(dst.r, dst.g, 0.0, 0.7);
}