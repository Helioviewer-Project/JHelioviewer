[
	{"instrument":"HMI", 	"color":"Gray"},
	{"instrument":"MDI", 	"color":"Gray"},
 	{"instrument":"EIT", 	"measurement":"171",  "color":"SOHO EIT 171 Å"},
	{"instrument":"EIT", 	"measurement":"195",  "color":"SOHO EIT 195 Å"},
	{"instrument":"EIT", 	"measurement":"284",  "color":"SOHO EIT 284 Å"},
	{"instrument":"EIT",  	"measurement":"304",  "color":"SOHO EIT 304 Å"},
	{"instrument":"LASCO",  "detector":"C2",      "color":"Red Temperature"},
	{"instrument":"LASCO",  "detector":"C3",      "color":"Blue/White Linear"},
	{"instrument":"AIA", 	"measurement":"94",   "color":"SDO-AIA 94 Å"},
	{"instrument":"AIA", 	"measurement":"131",  "color":"SDO-AIA 131 Å"},
	{"instrument":"AIA", 	"measurement":"171",  "color":"SDO-AIA 171 Å"},
	{"instrument":"SWAP",	"measurement":"174",  "color":"SDO-AIA 171 Å"},
	{"instrument":"AIA", 	"measurement":"193",  "color":"SDO-AIA 193 Å"},
	{"instrument":"AIA", 	"measurement":"211",  "color":"SDO-AIA 211 Å"},
	{"instrument":"AIA", 	"measurement":"304",  "color":"SDO-AIA 304 Å"},
	{"instrument":"AIA", 	"measurement":"335",  "color":"SDO-AIA 335 Å"},
	{"instrument":"AIA",  	"measurement":"1600", "color":"SDO-AIA 1600 Å"},
	{"instrument":"AIA", 	"measurement":"1700", "color":"SDO-AIA 1700 Å"},
	{"instrument":"AIA", 	"measurement":"4500", "color":"SDO-AIA 4500 Å"},
	{"instrument":"SECCHI", "measurement":"171",  "color":"STEREO EUVI 171 Å"},
	{"instrument":"SECCHI", "measurement":"195",  "color":"STEREO EUVI 195 Å"},
	{"instrument":"SECCHI", "measurement":"284",  "color":"STEREO EUVI 284 Å"},
	{"instrument":"SECCHI", "measurement":"304",  "color":"STEREO EUVI 304 Å"}
]

// Configuration about the used default color tables
// This must be an array and the fist matching rule will be used
// A rule can query "observatory", "instrument", "detector", "measurement"
// which must be equal to the meta information of the layer. Leaving out
// will match to any layer.
//
// The case will be ignored
//
// The key "color" is the name of the color table to apply