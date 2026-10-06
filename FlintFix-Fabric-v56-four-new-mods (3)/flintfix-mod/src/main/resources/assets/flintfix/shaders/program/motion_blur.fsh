#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;
uniform float BlurFactor;

in vec2 texCoord;

out vec4 fragColor;

// Blends the new frame with the previous blended frame. BlurFactor is how much
// of the previous frame stays, so higher values leave longer trails.
void main() {
    vec3 current = texture(DiffuseSampler, texCoord).rgb;
    vec3 previous = texture(PrevSampler, texCoord).rgb;
    fragColor = vec4(mix(current, previous, BlurFactor), 1.0);
}
