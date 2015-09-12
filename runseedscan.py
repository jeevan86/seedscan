#!/usr/bin/env python

import os
import shutil
import datetime
import glob
import argparse

from time import gmtime, strftime,sleep
from subprocess import Popen

###################################################################################################
#Code for running seedscan on a nightly cron with backfilling
#by Adam Ringler
#
#Methods
#startParser()
#modifyBuild()
#writeScanLog()
#scanYear()
#setupLogFileName()
#scanback()
#saveLogFiles()
#runSQLanalyzer()
#
###################################################################################################

#Here is the directory we are working in
seedscandir = '/home/asluser/seedscancron'

#These are the different types of logging we are using
logTypes = ['DEBUG','INFO','WARN','ERROR']

#Here is where we are saving the plots
plotsdir = '/TEST_ARCHIVE/seedscanplots'

#Start year for backscanning
startyear = 1988

#Here is our parser funcation
def startParser():

	parser = argparse.ArgumentParser(description='Program to manage seedScan runs')


	parser.add_argument('-v','--verbose',action = "store_true", dest = "debug", \
		default = False, help="Run in verbose mode")

	parser.add_argument('-oneday',action = "store_true", dest = "oneday", \
		default = False, help="Run just a one day scan")

	#The following has not been implemented
	'''
	parser.add_argument('-sta',type = str,action = "store", dest = "station", \
		default = "",help="Station to scan: NN_SSSS", required = False)
	
	parser.add_argument('-syear',type = int, action = "store", dest = "syear", \
		default = 2001, help="Start year: YYYY", required = False)

	parser.add_argument('-sday',type = int, action = "store", dest = "sday", \
		default = 1, help="Start day: ddd", required = False)

	parser.add_argument('-numday',type = int, action = "store", dest = "scandays", \
		default = 1, help="Number of days to scan: n", required = False)
	'''	

	parserVal = parser.parse_args()

	return parserVal

def writeScanLog(scanyear, scanday,daysToScan,seedscandir):
	if debug:
		print 'We are writing our scanned dates to the log'
	f = open(seedscandir + '/logs/scansRUN','a')
	f.write(strftime("%Y", gmtime()) + ',' + strftime("%j",gmtime()).zfill(3) + ',' + \
		str(scanyear) + ',' + scanday.zfill(3) + ',' + str(daysToScan) + '\n')
	f.close()

'''
def addStation(station):
	if debug:
		print 'We are going to add the following station: ' + station
'''

# Get the scan years
def scanYear(startyear,seedscandir):
	#Lets get the current year and day and setup our scan years
	curyear = int(strftime("%Y", gmtime()))
	curjday = int(strftime("%j",gmtime()))
	yeararray = range(startyear,curyear + 1)
	if debug:
		print 'Here is the yeararray:' + str(yeararray)
	if os.path.isfile(seedscandir + '/lastlog'):
		if debug:
			print 'We have an old log file so lets use it'
			print 'Here is the lastlog ' + seedscandir + '/lastlog'
		fo = open(seedscandir + '/lastlog')
		lastscan = fo.readlines()
		fo.close()
		os.remove(seedscandir + '/lastlog')
		if debug:
			print 'Here is the last scan date' + str(lastscan[0].strip())
		yearind = yeararray.index(int(lastscan[0].strip()))
	#No old log file so lets start from scratch
	else:
		if debug:
			print 'We do not have an old log file so we lets start a new one'
		yearind = 0
	logfile = open(seedscandir + '/lastlog','w')	
	if debug:
		print 'Here are the number of years ' + str(len(yeararray))
	year1 = yeararray[(yearind + 1)%len(yeararray)] 
	if debug:
		print 'Year to scan:' + str(year1) 
	logfile.write(str(year1) + '\n')
	logfile.close()
	return curyear,year1

# Scanback (creates new config.xml and runs a scan using startday)
def scanback(daysToScan, startday):
	if debug:
		print 'We are in the scanback function for ' + str(daysToScan) + \
			' starting on  ' + str(startday)
	#Open the old config file
	fo = open(seedscandir + '/config.xml.base')
	configfile = fo.read()
	fo.close()
	configfilenew = str(configfile)
	#Replace the start day and days to scan
	sdaystr='<cfg:start_day>' + str(startday) + '</cfg:start_day>'
	daystscstr='<cfg:days_to_scan>' + str(daysToScan) + '</cfg:days_to_scan>'
	configfilenew = str(configfile).replace('<cfg:start_day>365</cfg:start_day>',sdaystr)
	configfilenew = configfilenew.replace('<cfg:days_to_scan>1</cfg:days_to_scan>',daystscstr)
	scanfile = open(seedscandir + '/config.xml' ,'w')
	scanfile.write(configfilenew)
	scanfile.close()
	#Copy the config file and execute the seedscan process
	curyear = int(strftime("%Y", gmtime()))
	curjday = int(strftime("%j",gmtime()))
	curdate = datetime.datetime.now()
	scandate = curdate - datetime.timedelta(days=startday)
	scanyear = str(scandate.year)
	scanday = str(scandate.timetuple().tm_yday).zfill(3)
	
	os.chdir(seedscandir)
	buildstr = 'java -jar seedscan.jar'
	logstr = ' | tee ' + seedscandir + '/logs/log.sd' + str(scanyear) + '_' + str(scanday) + 'ts' + \
		str(daysToScan) + 'ds' + str(curyear) + '_' + str(curjday).zfill(3)
	if debug:	
		print "Here is the build str: " + buildstr + logstr
	os.system(buildstr + logstr)
	writeScanLog(scanyear, scanday, daysToScan, seedscandir)
	os.remove('config.xml')

# Main 
if __name__=="__main__":
	#Here we start the parser
	parserVal = startParser()
	if parserVal.debug:
		debug = True
	else:
		debug = False
	if parserVal.oneday:
		oneDayScan = True
	else:
		oneDayScan = False
	curdire = os.getcwd()
	if debug:
		print 'Current directory' + curdire
	os.chdir(seedscandir)
	if debug:
		print 'Moved to directory' + str(seedscandir)

	#Lets now get the current year and the year to scan
	curyear,year1=scanYear(startyear, seedscandir)
	if debug:
		print 'Here is our current year: ' + str(curyear)
		print 'Here is our scanback year: ' + str(year1)

	#Now we have our scan years so we need to scan
	if oneDayScan:
		if debug:
			print 'We are doing a one day scan'
		scanback(1,1)

	else:
		scanback(7,2)
		scanback(7,19)
		scanback(1,60)
		daysback = (curyear - year1)*366 + 1 - int(strftime("%j",gmtime()))
		for dayInd in range(1,366,30):
			scanback(30,daysback + (dayInd-1))

