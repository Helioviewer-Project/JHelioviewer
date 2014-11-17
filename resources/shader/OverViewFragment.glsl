uniform sampler2D texture;
uniform sampler2D lut;
uniform float currentLut;
void main(void)
{
	vec4 imageColor = texture2D(texture, gl_TexCoord[0].xy);
	vec2 pos = vec2(imageColor.y,currentLut/256.);
	vec4 lutColor = texture2D(lut, pos);
    gl_FragColor = lutColor;
}