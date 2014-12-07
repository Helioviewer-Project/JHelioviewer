import os
import glob
import sys
import array
import subprocess

path = '.'
suffixes = 'dll','so','jnilib'
subdirs = os.listdir(path)

for subdir in subdirs :
    for suff in suffixes :
        for infile in glob.glob( os.path.join(path, subdir, '*.' + suff) ):
            md5hash = subprocess.check_output(['md5','-q', infile]).rstrip()
            print "<file name=\"" + os.path.basename(infile) + "\" srcDir=\"jogl2/" + subdir + "\" destDir=\".\" loadLibrary=\"true\" md5=\"" + md5hash + "\"/>"
    print ""
