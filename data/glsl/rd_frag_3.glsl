#define PROCESSING_TEXTURE_SHADER

//varying vec2 vUv;
varying vec4 vertTexCoord;

uniform sampler2D texture;

void main()
{
    vec2 vUv = vertTexCoord.st;
    vec3 tex = texture2D(texture, vUv).rgb;
    float r = smoothstep( 0.2, 0.9, tex.r );
    float g = smoothstep( 0.2, 0.9, tex.g );
    float b = smoothstep( 0.2, 0.9, tex.b );
    //float r = tex.r;
    //float g = tex.g;
    //float b = tex.b;
    gl_FragColor = vec4(r,g,b,1.0);
}