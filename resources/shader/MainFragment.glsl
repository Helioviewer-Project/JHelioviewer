#version 120


uniform sampler2D texture;
uniform sampler2D lut;
uniform mat4 modelView;
uniform mat4 transformation;
uniform mat4 layerTransformation;
uniform mat4 layerInv;
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
uniform float fov;
uniform vec2 imageResolution;
uniform int cameraMode;
uniform float near;
uniform float far;

struct Sphere
{
    vec3 center;
    float radius;
};

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

Sphere sphere = Sphere(vec3(0,0,0),695700000);
Plane plane = Plane((vec4(0,0,1,0) * layerTransformation).xyz,-0.);
//Plane plane = Plane(cross(vec3(1,0,0),vec3(0,1.,0)),-0.);

float intersectSphere(in Ray ray, in Sphere sphere)
{
    vec3 oc = ray.origin - sphere.center;
    float b = 2.0 *dot(oc, ray.direction);
    float c = dot(oc,oc) - sphere.radius*sphere.radius;
    float determinant = b*b - 4.0 *c;
    /*no intersection*/
    if(determinant <0.0) return -1.0;
    
    float t = (-b - sqrt(determinant))/ 2.0;
    return t;
}

float intersectPlane(in Ray ray, in Plane plane)
{
    /*equation of a plane, y=0 = ro.y+t*rd.y*/
    return -(plane.d + dot(ray.origin,plane.normal)) / dot(ray.direction,plane.normal);
}

void intersect(in Ray ray, out float tSphere, out float tPlane)
{
    /* intersect with a sphere */
    tSphere = intersectSphere(ray, sphere);
    /* intersect with a plane */
    tPlane = intersectPlane(ray, plane);
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
    gl_FragColor = vec4(0,0,0,1);
    vec2 uv = gl_TexCoord[0].xy;
    /* calculate viewport */
    float width;
    float height;
    /* MV --> z */
    vec3 rayOrigin;
    vec3 rayDirection;
    
    float zTranslation = (vec4(0,0,0,1) * transformation).z;
    if (cameraMode == 0)
    {
	    //2D
        width = zTranslation * tan(fov);
        vec2 tmpWidth = (-1.0 +2.0*uv) * width;
        rayOrigin = (vec4(0,0,1,1) * transformation).xyz + vec3(tmpWidth, 0);
        rayDirection = vec3( 0, 0, -1.0);
    }
    else
    {
	    //3D
        width = tan(fov);
        height = tan(fov);
        rayOrigin = (vec4(0,0,1,1) * transformation).xyz;
        rayDirection = normalize(vec3( (-1.0 +2.0*uv) *vec2(width, height), -1.0));
    }
    
    Ray ray1 = Ray(rayOrigin, rayDirection);
    /* MV --> rotation */
    vec3 rayORot = (vec4(rayOrigin,0) * transformation).xyz;
    vec3 rayDRot = (vec4(rayDirection,0) * transformation).xyz;
    Ray rayRot = Ray(rayORot, rayDRot);
    
    float tSphere, tPlane;
    intersect (rayRot, tSphere, tPlane);
    
    float sphereIntensity = 0.;
    float coronaIntensity = 0.;
    gl_FragDepth = 1;
    
   	if (tSphere > 0.)
   	{
        vec3 posOri = rayRot.origin + tSphere*rayRot.direction;
        vec3 posRot = (vec4(posOri, 0) * (layerInv)).xyz;
        
        vec3 ray = ray1.origin + tSphere * ray1.direction;
        if (posRot.z >= 0.0)
        {
            vec2 texPos = (posRot.xy/physicalImageSize + 0.5) + sunOffset;
            if (texPos.x >= 1.0 || texPos.x < 0.0 || texPos.y >= 1.0 || texPos.y < 0.0)
               discard;
               
            texPos = (texPos - imageOffset.xy) / imageOffset.zw;
            sphereIntensity = contrastValue(sharpenValue(texPos));
        }
        
		gl_FragDepth = (1./(zTranslation - ray.z) - 1./near) / (1./far - 1./near);            
    }
    
   	if (tPlane > 0. && (tPlane < tSphere|| tSphere < 0.))
   	{
        vec3 posOri = rayRot.origin + tPlane*rayRot.direction;
        vec3 posRot = (vec4(posOri, 0) * (layerInv)).xyz;
        
        vec2 texPos = (posRot.xy/physicalImageSize + 0.5) + sunOffset;
        if ((texPos.x >= 1.0 || texPos.x < 0.0 || texPos.y >= 1.0 || texPos.y < 0.0) && tSphere < 0.)
            discard;
        
        if (opacityCorona > 0.)
        {
	        texPos = (texPos - imageOffset.xy) / imageOffset.zw;
            coronaIntensity = contrastValue(sharpenValue(texPos)) * opacityCorona;
        }
    }
    
    vec3 lutColor = lutLookup(coronaIntensity) + lutLookup(sphereIntensity);
    gl_FragColor = vec4(lutColor,1) * vec4(redChannel,greenChannel,blueChannel,opacity);
}
