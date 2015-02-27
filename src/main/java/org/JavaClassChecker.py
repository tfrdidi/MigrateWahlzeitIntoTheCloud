import fnmatch
import os
import re

importPattern = re.compile("import .+;")

def checkImportsInFile(path):
	for i, line in enumerate(open(path)):
         for match in re.finditer(importPattern, line):
            if not "org.wahlzeit" in line: 
               javaClassImport = line.split( )[1][:-1]
               if javaClassImport not in javaClassImports:
                  javaClassImports.append(javaClassImport)


matches = []
javaClassImports = []

for root, dirnames, filenames in os.walk('wahlzeit'):
  for filename in fnmatch.filter(filenames, '*.java'):
      path = os.path.join(root, filename)
      matches.append(path)
      checkImportsInFile(path)

for imp in javaClassImports:
	print imp
#print matches
