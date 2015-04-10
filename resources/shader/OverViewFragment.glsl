uniform sampler2D texture;
uniform sampler2D lut;
uniform float currentLut;
uniform int inverted;
void main(void)
{
	vec4 imageColor = texture2D(texture, gl_TexCoord[0].xy);
	vec2 pos = vec2(imageColor.y,currentLut/256.);
    if (inverted != 0){
    	pos.x = 1. - pos.x;
    }
	vec4 lutColor = texture2D(lut, pos);
    gl_FragColor = lutColor;
}