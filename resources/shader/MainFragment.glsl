#version 120


uniform sampler2D texture;
uniform sampler2D lut;
uniform mat4 modelView;
uniform mat4 transformation;
uniform mat4 layerTransformation;
uniform float physicalImageWidth;
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

struct Sphere{
    vec3 center;
    float radius;
};

struct Ray{
    vec3 origin;
    vec3 direction;
};

struct Plane{
    vec3 normal;
    float d;
};

Sphere sphere = Sphere(vec3(0,0,0),695700000);
Plane plane = Plane(cross(vec3(1,0,0),vec3(0,1.,0)),-0.);

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


/* Mexican-hat 7x7 filter
 0  0 -1 -1 -1  0  0
 0 -1 -3 -3 -3 -1  0
 -1 -3  0  7  0 -3 -1
 -1 -3  7 24  7 -3 -1
 -1 -3  0  7  0 -3 -1
 0 -1 -3 -3 -3 -1  0
 0  0 -1 -1 -1  0  0
 */
vec4 sharpenValue(vec2 texPos){
    // grayscale value of filter
    vec3 tmp = vec3(0,0,0);
    float x = 1.0/imageResolution.x;
    float y = 1.0/imageResolution.y;
    
    tmp += texture2D(texture,texPos + vec2(-x, -3*y)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(0, -3*y)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(x, -3*y)).xyz * -1;
    
    tmp += texture2D(texture,texPos + vec2(-2*x, -2*y)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(-x, -2*y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(0, -2*y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(x, -2*y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(2*x, -2*y)).xyz * -1;
    
    tmp += texture2D(texture,texPos + vec2(-3*x, -y)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(-2*x, -y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(0, -y)).xyz * 7;
    tmp += texture2D(texture,texPos + vec2(2*x, -y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(-3*x, -y)).xyz * -1;
    
    tmp += texture2D(texture,texPos + vec2(-3*x, 0)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(-2*x, 0)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(-x, 0)).xyz * 7;
    tmp += texture2D(texture,texPos + vec2(0, 0)).xyz * 24;
    tmp += texture2D(texture,texPos + vec2(x, 0)).xyz * 7;
    tmp += texture2D(texture,texPos + vec2(2*x, 0)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(3*x, 0)).xyz * -1;
    
    tmp += texture2D(texture,texPos + vec2(-3*x, y)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(-2*x, y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(0, y)).xyz * 7;
    tmp += texture2D(texture,texPos + vec2(2*x, y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(-3*x, y)).xyz * -1;
    
    tmp += texture2D(texture,texPos + vec2(-2*x, 2*y)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(-x, 2*y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(0, 2*y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(x, 2*y)).xyz * -3;
    tmp += texture2D(texture,texPos + vec2(2*x, 2*y)).xyz * -1;
    
    tmp += texture2D(texture,texPos + vec2(-x, 3*y)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(0, 3*y)).xyz * -1;
    tmp += texture2D(texture,texPos + vec2(x, 3*y)).xyz * -1;
    
    return clamp(texture2D(texture,texPos) + vec4(tmp / 3.0 * sharpen, 0.0), 0.0, 1.0);
}

vec4 contrastValue(vec3 color){
    float value = min(clamp(0.5 + (color.x - 0.5) * (1 + contrast/10.0) ,0.0, 1.0), sqrt(color.x) + color.x);
    return vec4(value, value, value, 1.0);
}

void main(void)
{
    gl_FragColor = vec4(0,0,0,1);
    /* clear color */
    vec2 uv = gl_TexCoord[0].xy;
    /* calculate viewport */
    float width;
    float height;
    /* MV --> z */
    vec3 rayOrigin;
    vec3 rayDirection;
    
    //2D
    float zTranslation = (vec4(0,0,0,1) * transformation).z;
    if (cameraMode == 0){
        width = zTranslation * tan(fov);
        vec2 tmpWidth = (-1.0 +2.0*uv) * width;
        rayOrigin = (vec4(0,0,1,1) * transformation).xyz + vec3(tmpWidth, 0);
        rayDirection = vec3( 0, 0, -1.0);
    }
    //3D
    else{
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
    
    vec4 imageColor;
    gl_FragDepth = 1;
    
   	if (tSphere > 0.){
        vec3 posOri = rayRot.origin + tSphere*rayRot.direction;
        vec3 posRot = (vec4(posOri, 1) * layerTransformation).xyz;
        
        vec3 ray = ray1.origin + tSphere * ray1.direction;
        if (posRot.z >= 0.0){
            vec2 texPos = (posRot.xy/physicalImageWidth + 0.5) *vec2(1.,1.) + sunOffset;
            if (texPos.x > 1.0 || texPos.x < 0.0 || texPos.y > 1.0 || texPos.y < 0.0) {
                discard;
            }
            texPos = (texPos - imageOffset.xy) / imageOffset.zw;
            imageColor = sharpenValue(texPos);
            imageColor = contrastValue(imageColor.xyz);
        }
        
        float far = zTranslation - 4 * 695700000;
            
		gl_FragDepth = (1. /(zTranslation - ray.z) - 1. / near) / (1. /far - 1. / near);            
            
         //gl_FragDepth =  posRot.z;
            //imageColor = texture2D(texture,texPos);
        
    }
    
   	if (tPlane > 0. && (tPlane < tSphere|| tSphere < 0.)){
        vec3 posOri = rayRot.origin + tPlane*rayRot.direction;
        vec2 texPos = (posOri.xy/physicalImageWidth + 0.5) *vec2(1.,1.) + sunOffset;
        if ((texPos.x > 1.0 || texPos.x < 0.0 || texPos.y > 1.0 || texPos.y < 0.0) && tSphere < 0.) {
            discard;
        }
        texPos = (texPos - imageOffset.xy) / imageOffset.zw;
        
        vec4 coronaImageColor = sharpenValue(texPos);
        coronaImageColor = contrastValue(coronaImageColor.xyz);
        //vec4 coronaImageColor = texture2D(texture,texPos);
        if (opacityCorona > 0.){
            imageColor = imageColor + vec4(coronaImageColor.xyz * opacityCorona, coronaImageColor.a);
        }
    }
    
    vec2 pos = vec2(imageColor.y,lutPosition/256.);
    if (lutInverted != 0){
        pos.x = 1. - pos.x;
    }
    vec4 lutColor = texture2D(lut, pos);
    lutColor.x = pow(lutColor.x, gamma);
    lutColor.y = pow(lutColor.y, gamma);
    lutColor.z = pow(lutColor.z, gamma);
    gl_FragColor = lutColor * vec4(redChannel,greenChannel,blueChannel,opacity);
    //gl_FragColor = vec4(gl_FragDepth, gl_FragDepth, gl_FragDepth, 1);
}

