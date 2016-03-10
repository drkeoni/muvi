uniform mat4 projmodelviewMatrix;

attribute vec4 inVertex;
attribute vec4 inColor;

varying vec4 vertColor;

void main() {
  // Applying modelview+projection transformation to incoming vertex:
  gl_Position = projmodelviewMatrix * inVertex;

  // Passing unmodified vertex color to the fragment shader.
  vertColor = inColor;
}