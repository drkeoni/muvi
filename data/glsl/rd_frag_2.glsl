#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define PROCESSING_TEXTURE_SHADER

varying vec4 vertColor;
varying vec4 vertTexCoord;

uniform sampler2D texture;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 color5;

void main()
{
    vec2 vUv = vertTexCoord.st;
    float value = texture2D(texture, vUv).g;
    //int step = int(floor(value));
    //float a = fract(value);
    float a;
    vec3 col;
    float a2 = 1.0;

    if(value <= color1.a) {
        col = color1.rgb;
        a2 = 0.8;
    }
    if(value > color1.a && value <= color2.a)
    {
        a = (value - color1.a)/(color2.a - color1.a);
        col = mix(color1.rgb, color2.rgb, a);
    }
    if(value > color2.a && value <= color3.a)
    {
        a = (value - color2.a)/(color3.a - color2.a);
        col = mix(color2.rgb, color3.rgb, a);
    }
    if(value > color3.a && value <= color4.a)
    {
        a = (value - color3.a)/(color4.a - color3.a);
        col = mix(color3.rgb, color4.rgb, a);
    }
    if(value > color4.a && value <= color5.a)
    {
        a = (value - color4.a)/(color5.a - color4.a);
        col = mix(color4.rgb, color5.rgb, a);
    }
    if(value > color5.a) {
        col = color5.rgb;
        a2 = 0.5;
    }

    gl_FragColor = vec4(col.r, col.g, col.b, a2);
}