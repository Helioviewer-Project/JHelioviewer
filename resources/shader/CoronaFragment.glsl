#version 120

uniform sampler2D texture;
uniform sampler2D lut;
uniform mat4 modelView;
uniform mat4 transformation;
uniform vec2 physicalImageSize;
uniform float opacity;
uniform float sharpen;
uniform float gamma;
uniform float contrast;
uniform float lutPosition;
uniform int lutInverted;
uniform int redChannel;
uniform int greenChannel;
uniform int blueChannel;
uniform vec2 sunOffset;
uniform vec4 imageOffset;
uniform float opacityCorona;
uniform float tanFOV;
uniform vec2 imageResolution;
uniform int cameraMode;
uniform float near;
uniform float far;

struct Ray
{
    vec3 origin;
    vec3 direction;
};

struct Plane
{
    vec3 normal;
    float d;
};

Plane plane = Plane(vec3(0,0,1), -0.);

float intersectPlane(in Ray ray, in Plane plane)
{
    /*equation of a plane, y=0 = ro.y+t*rd.y*/
    return -(plane.d + dot(ray.origin,plane.normal)) / dot(ray.direction,plane.normal);
}

const mat3 kernel = mat3(-0.1,-0.2,-0.1,-0.2,1.2,-0.2,-0.1,-0.2,-0.1);


/* shapen 5x5 filter
  0  -4  -6  -4  0  14
 -4 -16 -24 -16 -4  64
 -6 -24 220 -24 -6  60
 -4 -16 -24 -16 -4  64
  0  -4  -6  -4  0  14
  */
float sharpenValue(vec2 texPos)
{
	if(sharpen == 0.)
		return texture2D(texture,texPos).r;

    // grayscale value of filter
    float tmp = 0.0;
    float x = 1.0/imageResolution.x;
    float y = 1.0/imageResolution.y;
    
    //tmp -= texture2D(texture,texPos + vec2(-x-x, -y-y)).r;
    tmp -= texture2D(texture,texPos + vec2(-  x, -y-y)).r * 4;
    tmp -= texture2D(texture,texPos + vec2(   0, -y-y)).r * 6;
    tmp -= texture2D(texture,texPos + vec2(   x, -y-y)).r * 4;
    //tmp -= texture2D(texture,texPos + vec2( x+x, -y-y)).r;
    
    tmp -= texture2D(texture,texPos + vec2(-x-x,   -y)).r * 4;
    tmp -= texture2D(texture,texPos + vec2(-  x,   -y)).r * 16;
    tmp -= texture2D(texture,texPos + vec2(   0,   -y)).r * 24;
    tmp -= texture2D(texture,texPos + vec2(   x,   -y)).r * 16;
    tmp -= texture2D(texture,texPos + vec2( x+x,   -y)).r * 4;
    
    tmp -= texture2D(texture,texPos + vec2(-x-x,    0)).r * 6;
    tmp -= texture2D(texture,texPos + vec2(-  x,    0)).r * 24;
    tmp += texture2D(texture,texPos + vec2(   0,    0)).r * 216;
    tmp -= texture2D(texture,texPos + vec2(   x,    0)).r * 24;
    tmp -= texture2D(texture,texPos + vec2( x+x,    0)).r * 6;
    
    tmp -= texture2D(texture,texPos + vec2(-x-x,    y)).r * 4;
    tmp -= texture2D(texture,texPos + vec2(-  x,    y)).r * 16;
    tmp -= texture2D(texture,texPos + vec2(   0,    y)).r * 24;
    tmp -= texture2D(texture,texPos + vec2(   x,    y)).r * 16;
    tmp -= texture2D(texture,texPos + vec2( x+x,    y)).r * 4;
    
    //tmp -= texture2D(texture,texPos + vec2(-x-x,  y+y)).r;
    tmp -= texture2D(texture,texPos + vec2(-  x,  y+y)).r * 4;
    tmp -= texture2D(texture,texPos + vec2(   0,  y+y)).r * 6;
    tmp -= texture2D(texture,texPos + vec2(   x,  y+y)).r * 4;
    //tmp -= texture2D(texture,texPos + vec2( x+x,  y+y)).r;
    
    return clamp(texture2D(texture,texPos).r + tmp / 3.0 * sharpen, 0.0, 1.0);
}

float contrastValue(float color)
{
	if(contrast == 0.)
		return color;
    return min(clamp(0.5 + (color - 0.5) * (1 + contrast/10.0), 0.0, 1.0), sqrt(color) + color);
}

vec3 lutLookup(float intensity)
{
    vec2 pos = vec2(intensity,(lutPosition + 0.5) / 256.);
    if (lutInverted != 0)
        pos.x = 1. - pos.x;
   	
   	vec3 res = texture2D(lut, pos).rgb;
    res.r=pow(res.r,gamma);
    res.g=pow(res.g,gamma);
    res.b=pow(res.b,gamma);
    return res;
}

void main(void)
{
    vec2 uv = gl_TexCoord[0].xy;
    
    /* MV --> z */
    Ray ray;
    float zTranslation = (transformation * vec4(0,0,0,1)).z;
    if (cameraMode == 0)
    {
	    //2D
        vec2 center = uv * zTranslation * tanFOV;
        ray.origin = (transformation * vec4(0,0,1,1)).xyz + vec3(center, 0);
        ray.direction = vec3(0, 0, -1.0);
    }
    else
    {
	    //3D
        ray.origin = (transformation * vec4((transformation * vec4(0,0,0,1)).xyz,0)).xyz;
        ray.direction = (transformation * normalize(vec4(uv * tanFOV, -1.0, 0))).xyz;
    }
    
    vec3 lutColor;
    
    float tPlane = intersectPlane(ray, plane);
   	if (tPlane <= 0.)
   		discard;
   		
    vec2 pos = ray.origin.xy + tPlane*ray.direction.xy;
    vec2 texPos = pos/physicalImageSize + 0.5 + sunOffset;
    if (texPos.x >= 1.0 || texPos.x < 0.0 || texPos.y >= 1.0 || texPos.y < 0.0)
    	discard;
    
    texPos = (texPos - imageOffset.xy) / imageOffset.zw;
    gl_FragColor = vec4(lutLookup(contrastValue(sharpenValue(texPos))) * opacityCorona,1) * vec4(redChannel,greenChannel,blueChannel,opacity);
    
    vec3 origin = vec4(transformation * vec4(0,0,0,1)).xyz;
    vec3 direction = normalize(vec3(uv * tanFOV, -1.0)).xyz;
    float z = origin.z + tPlane * direction.z;
	gl_FragDepth = (1./(zTranslation - z) - 1./near) / (1./far - 1./near);            
}
