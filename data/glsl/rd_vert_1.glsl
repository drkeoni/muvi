#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform mat4 transform;
attribute vec4 vertex;

void main()
{
    //vUv = uv;
    gl_Position = transform * vertex;
}