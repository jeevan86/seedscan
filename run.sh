# There are several ways to run seedscan:
#
# 0. >ant - default behavior is to compile the source files
#           into the ./build class file dir
#
# 1. Using ant + build.xml to set the classpath and run seedscan from
#    the ./build class files:
#    >ant test
#
java -Xms512M -Xmx512M -jar SeedScan.jar
