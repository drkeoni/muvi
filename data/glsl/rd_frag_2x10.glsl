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
uniform vec4 color6;
uniform vec4 color7;
uniform vec4 color8;
uniform vec4 color9;
uniform vec4 color10;

void main()
{
    vec2 uv = texture2D(texture, vertTexCoord.st).rg;
    float value = dot(uv,uv);
    //float value =  texture2D(texture, vertTexCoord.st).g;
    float a2 = 1.0;
    vec3 col;

    if(value <= color1.a) {
        col = color1.rgb;
        a2 = 0.7;
    } else if(value > color10.a) {
        col = color10.rgb;
        a2 = 0.5;
    } else {
        vec4 colorA;
        vec4 colorB;
        if(value > color1.a && value <= color2.a)
        {
            colorA = color1;
            colorB = color2;
        }
        if(value > color2.a && value <= color3.a)
        {
            colorA = color2;
            colorB = color3;
        }
        if(value > color3.a && value <= color4.a)
        {
            colorA = color3;
            colorB = color4;
        }
        if(value > color4.a && value <= color5.a)
        {
            colorA = color4;
            colorB = color5;
        }
        if(value > color5.a && value <= color6.a)
        {
            colorA = color5;
            colorB = color6;
        }
        if(value > color6.a && value <= color7.a)
        {
            colorA = color6;
            colorB = color7;
        }
        if(value > color7.a && value <= color8.a)
        {
            colorA = color7;
            colorB = color8;
        }
        if(value > color8.a && value <= color9.a)
        {
            colorA = color8;
            colorB = color9;
        }
        if(value > color9.a && value <= color10.a)
        {
            colorA = color9;
            colorB = color10;
        }
        float a = (value - colorA.a)/(colorB.a - colorA.a);
        col = mix(colorA.rgb, colorB.rgb, a);
    }
    gl_FragColor = vec4(col.r, col.g, col.b, a2);
}