#define PROCESSING_TEXTURE_SHADER

varying vec4 vertTexCoord;

uniform float screenWidth;
uniform float screenHeight;
uniform sampler2D texture;
uniform float delta;
uniform float feed;
uniform float kill;

float step_x = 1.0/screenWidth;
float step_y = 1.0/screenHeight;

void main()
{
    vec2 vUv = vertTexCoord.st;

    vec2 uv = texture2D(texture, vUv).rg;
    vec2 uv0 = texture2D(texture, vUv+vec2(-step_x, 0.0)).rg;
    vec2 uv1 = texture2D(texture, vUv+vec2(step_x, 0.0)).rg;
    vec2 uv2 = texture2D(texture, vUv+vec2(0.0, -step_y)).rg;
    vec2 uv3 = texture2D(texture, vUv+vec2(0.0, step_y)).rg;

    vec2 lapl = (uv0 + uv1 + uv2 + uv3 - 4.0*uv);
    vec2 uv4 = vec2( lapl.r - uv.r*uv.g*uv.g + feed*(1.0 - uv.r),
                     lapl.g + uv.r*uv.g*uv.g - (feed+kill)*uv.g );
    vec2 dst = uv + delta*uv4;

    gl_FragColor = vec4(dst.r, dst.g, 0.0, 1.0);
}