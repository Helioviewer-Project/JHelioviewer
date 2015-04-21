uniform sampler2D texture;
uniform sampler2D lut;
uniform mat4 modelView;
uniform mat4 transformation;
uniform mat4 layerTransformation;
uniform float sunRadius;
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
uniform float opacityCorona;
uniform float fov;

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

Sphere sphere = Sphere(vec3(0,0,0),sunRadius);
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

void main(void)
{	
	/* clear color */
	float z = 4.015;
	vec2 uv = gl_TexCoord[0].xy * vec2(1.,-1.) + vec2(0,1);
	/* calculate viewport */
	float width = tan(fov);
	float height = tan(fov);
	/* MV --> z */
	
	vec3 rayOrigin = (vec4(0,0,1,1) * transformation).xyz; 
	
	vec3 rayDirection = normalize(vec3( (-1.0 +2.0*uv) *vec2(width, height), -1.0));
	
	Ray ray1 = Ray(rayOrigin, rayDirection);
	/* MV --> rotation */ 
	vec3 rayORot = (vec4(rayOrigin,0) * transformation).xyz;
	vec3 rayDRot = (vec4(rayDirection,0) * transformation).xyz;
	Ray rayRot = Ray(rayORot, rayDRot);

	float tSphere, tPlane;
    intersect (rayRot, tSphere, tPlane);
	
	vec4 imageColor;
   	if (tSphere > 0.){	
		vec3 posOri = rayRot.origin + tSphere*rayRot.direction;
		vec3 posRot = (vec4(posOri, 1) * layerTransformation).xyz;
		if (posRot.z >= 0.0){
	    	vec2 texPos = (posRot.xy/physicalImageWidth + 0.5) *vec2(1.,1.) + sunOffset;
			imageColor = texture2D(texture,texPos);
		}
	}

   	if (tPlane > 0. && (tPlane < tSphere|| tSphere < 0.)){
		vec3 posOri = rayRot.origin + tPlane*rayRot.direction;
		//if (posOri.z >= 0.0){
	    vec2 texPos = (posOri.xy/physicalImageWidth + 0.5) *vec2(1.,1.) + sunOffset;
	    vec4 coronaImageColor = texture2D(texture,texPos);
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
    
}

