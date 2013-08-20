#!/usr/bin/env python
import sys
import os
import csv
import math
from optparse import OptionParser

# From:
# sqlite3 -csv -header locus.db "SELECT m._id as measurement_id, m.point_id, m.bssid, m.ssid, m.frequency, m.level, m.timestamp, p.map_id, p.x, p.y FROM measurements as m INNER JOIN points p ON m.point_id = p._id" > measurements.csv
def process_csv_row(header, row):
	i = 0
	record = {}
	for val in row:
		record[header[i]] = val
		i += 1
	return record

def process_csv(fh):
	retarr = []
	reader = csv.reader(fh, delimiter=',', quotechar='"')
	try:
		header = reader.next()
	except StopIteration:
		return
	for row in reader:
		# TODO: Switch to DictReader.
		record = process_csv_row(header, row)
		if record == {}:
			continue
		retarr.append(record)
	return retarr

def aggregate_bssid(measurements):
	bssids = {}
	for measurement in measurements:
		bssidarr = bssids.setdefault(measurement['bssid'], [])
		bssidarr.append(measurement)
	return bssids

def generate_bssid_distances(bssids):
	fn = bssids[0]['bssid'].replace(':', '_') + ".csv"
	with open(fn, 'wb') as csvfile:
		for bssid in bssids:
			writer = csv.writer(csvfile, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
			writer.writerow([float(bssid['x']), float(bssid['y']), bssid['level']])

def abs_diff(x, y):
	return math.fabs(float(x) - float(y))

def generate_distances(bssids):
	retarr = []
	for bssid in bssids:
		generate_bssid_distances(bssids[bssid])

def generate_data(fh):
	measurements = process_csv(fh)
	bssids = aggregate_bssid(measurements)
	distances = generate_distances(bssids)

def main():
	usage = "usage: %prog [options]\n\nImports wifi measurements and generates data.csv, test_set.csv & validation_set.csv"
	parser = option_parser(usage)
	(options, args) = parser.parse_args()
	generate_data(sys.stdin)

def option_parser(usage):
	parser = OptionParser(usage=usage)
	return parser

if __name__ == "__main__":
	main()
