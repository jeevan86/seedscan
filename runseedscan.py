#!/usr/bin/env python

import os
import shutil
import datetime

from time import gmtime, strftime,sleep
from subprocess import Popen

###################################################################################################
#Code for running seedscan on a nightly cron with backfilling
#by Adam Ringler
#
#Methods
#scanback()
#
#
#
###################################################################################################

debug=True
seedscandire = '/home/asluser/seedscancron'
plotsdire =  '/TEST_ARCHIVE/seedscanplots'
startyear = 1988

def scanback(daysToScan, startday):
	if debug:
		print 'We are in the scanback function for ' + str(daysToScan) + ' starting on  ' + str(startday)
#Open the old config file
	fo = open(seedscandire + '/config.xml')
	configfile = fo.read()
	fo.close()
	configfilenew = str(configfile)

#Replace the start day and days to scan
	sdaystr='<cfg:start_day>' + str(startday) + '</cfg:start_day>'
	daystscstr='<cfg:days_to_scan>' + str(daysToScan) + '</cfg:days_to_scan>'
	configfilenew = str(configfile).replace('<cfg:start_day>365</cfg:start_day>',sdaystr)
	configfilenew = configfilenew.replace('<cfg:days_to_scan>1</cfg:days_to_scan>',daystscstr)
	scanfile = open(seedscandire + '/config.xml.temp' ,'w')
	scanfile.write(configfilenew)
	scanfile.close()

#Copy the config file and execute the seedscan process
	curyear = int(strftime("%Y", gmtime()))
	curjday = int(strftime("%j",gmtime()))
	curdate = datetime.datetime.now()
	scandate = curdate - datetime.timedelta(days=startday)
	scanyear = str(scandate.year)
	scanday = str(scandate.timetuple().tm_yday).zfill(3)
	shutil.copy(seedscandire + '/config.xml.temp' , seedscandire + '/seedscan/config.xml')
	os.remove(seedscandire + '/config.xml.temp')
	buildstr = 'ant seedscan -f ' + seedscandire + '/seedscan/build.xml'
	logstr = ' > ' + seedscandire + '/log.sd' + str(scanyear) + '_' + str(scanday) + 'ts' + str(daysToScan) + 'ds' + \
		str(curyear) + '_' + str(curjday).zfill(3)
	print "Here is the build str" + buildstr + logstr
	os.system(buildstr + logstr)



if __name__=="__main__":
	curdire = os.getcwd()
	if debug:
		print 'Current directory' + curdire
	os.chdir(seedscandire)
	if debug:
		print 'Moved to directory' + str(os.getcwd())

#Pull the nightly seedscan and get it all setup
	os.system("rm -rf seedscan")
	os.system('git clone https://github.com/asl-usgs/seedscan.git seedscan')
	os.chdir(seedscandire + '/seedscan')
#Modify the build file to allow for the correct memory usage
	buildFin = open("build.xml", "r")
	buildConfig = buildFin.readlines()
	buildFin.close()
	buildFo = open("build.xml", "w")
	for line in buildConfig:
		lineIndex = line.find("-Xmx", 0)
		if (lineIndex >= 0):
			buildFo.write("<jvmarg value=\"-Xmx25g\"/>\n")
		else:
			buildFo.write(line)
	buildFo.close()
	os.chdir(seedscandire)
#Lets get the current year and day and setup our scan years
	curyear = int(strftime("%Y", gmtime()))
	curjday = int(strftime("%j",gmtime()))
	yeararray = range(startyear,curyear + 1)
	if debug:
		print 'Here is the yeararray:' 
		print yeararray
	if os.path.isfile(seedscandire + '/lastlog'):
		if debug:
			print 'We have an old log file so lets use it'
			print 'Here is the lastlog ' + seedscandire + '/lastlog'
		fo = open(seedscandire + '/lastlog')
		lastscan = fo.readlines()
		fo.close()
		os.remove(seedscandire + '/lastlog')
		if debug:
			print str(lastscan[0].strip())
		yearind = yeararray.index(int(lastscan[0].strip()))
#No old log file so lets start from scratch
	else:
		yearind = 0
	logfile = open(seedscandire	 + '/lastlog','w')	
	if debug:
		print 'Here are the number of years ' + str(len(yeararray))
	year1 = yeararray[(yearind + 1)%len(yeararray)] 
	if debug:
		print 'Years to scan' + str(year1) 
	
	logfile.write(str(year1) + '\n')
	logfile.close()
#Now we have our scan years so we need to scan
	scanback(2,1)
	scanback(1,60)
	scanback(1,7)
	daysback = (curyear - year1)*366 + 1 - int(strftime("%j",gmtime()))
	for dayInd in range(1,366,30):
		scanback(30,daysback + (dayInd-1))
	
	os.system('mv log.* logfiles')
	os.chdir(seedscandire + '/seedscan/null/')
	os.system('cp -R --parents * ' + plotsdire )
	os.chdir(curdire)
	os.chdir(seedscandire)
	os.system('psql -h aslpdf01.cr.usgs.gov -f analyzer.sql dataq_prod')
#buildstr = 'ant seedscan -f seedscan/build.xml'
#logstr = ' > logcur'
#subp = Popen(buildstr + logstr, shell = True)
