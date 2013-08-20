#!/usr/bin/env python
import os
import sys
import csv
import itertools
import scipy.stats
import scipy.special
import Image
from pylab import *
from numpy import *
from optparse import OptionParser
from matplotlib import pyplot
from mpl_toolkits.mplot3d import Axes3D
from numpy.polynomial import Polynomial

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

def aggregate(measurements, field):
	rethash = {}
	for measurement in measurements:
		rechash = rethash.setdefault(measurement[field], {})
		recarr = rechash.setdefault('measurements', [])
		recarr.append(measurement)
	return rethash

def generate_data(fh):
	measurements = process_csv(fh)
	bssids = aggregate(measurements, 'bssid')
	points = aggregate(measurements, 'point_id')
	return bssids, points

def process_bssid(bssid):
	x = []
	y = []
	level = []
	for measurement in bssid['measurements']:
		level.append(float(measurement['level']))
		y.append(float(measurement['y']))
		x.append(float(measurement['x']))
	return array(x), array(y), array(level)

def minimize(bssid, map_x, map_y):
	x, y, level = process_bssid(bssid)
	xy = x * y
	xx = x * x
	xxx = x * x * x
	logx = log(x)
	yy = y * y
	yyy = y * y * y
	logy = log(y)
	#coords = transpose(array([ones(x.shape),x,xx,y,yy]))
	coords = transpose(array([ones(x.shape),x,xx,xxx,y,yy,yyy,xy]))
	model, resid, rank, s = linalg.lstsq(coords, level)
	Xs = np.arange(0, map_x, 5)
	Ys = np.arange(0, map_y, 5)
	Xs, Ys = np.meshgrid(Xs, Ys)
	#Zs = model[0] + model[1]*Xs + model[2]*Xs**2 + model[3]*Ys + model[4]*Ys**2
	Zs = model[0] + model[1]*Xs + model[2]*Xs**2 + model[3]*Xs**3 + model[4]*Ys + model[5]*Ys**2 + model[6]*Ys**3 + model[7]*Ys*Xs
	fig = figure()
	ax = Axes3D(fig)
	ax.plot(x, y, level, linestyle="none", marker="o", mfc="none", markeredgecolor="red")
	ax.plot_surface(Xs, Ys, Zs, rstride=4, cstride=4, alpha=0.4, cmap=cm.jet)
	show()
	resid_var = resid[0] / level.size
	bssid_model = bssid.setdefault('model', {})
	bssid_model['const'] = model[0]
	bssid_model['x'] = model[1]
	bssid_model['xx'] = model[2]
	bssid_model['y'] = model[3]
	bssid_model['yy'] = model[4]
	bssid_model['resid_sq'] = resid_var
	bssid_model['predictions'] = Zs

	#resid_sqrt = sqrt(resid_var)
	#resid_norm = scipy.stats.norm(0, resid_sqrt)
	#x = .25
	#z_score = x/resid_sqrt
	#p_value = (1 - scipy.special.ndtr(z_score)) * 2
	#print p_value

def create_models(bssids, base_map):
	for bssid in bssids:
		if len(bssids[bssid]['measurements']) >= 20:
			minimize(bssids[bssid], base_map.size[0], base_map.size[1])
	return bssids

def find_location(bssids, point, base_map):
	print "probs"
	Xs = np.arange(0, base_map.size[0], 5)
	Ys = np.arange(0, base_map.size[1], 5)
	Xs, Ys = np.meshgrid(Xs, Ys)
	aggregate = None
	for measurement in point['measurements']:
		bssid = bssids[measurement['bssid']]
		if bssid.has_key('model'):
			model = bssid['model']
			predictions = model['predictions']
			resid_sq = model['resid_sq']
			resid_sqrt = sqrt(resid_sq)
			z_scores = abs(predictions - float(measurement['level'])) / resid_sqrt
			p_values = (1 - scipy.special.ndtr(z_scores)) * 2
			if aggregate == None:
				aggregate = p_values
			else:
				aggregate = aggregate * p_values
			#fig = figure()
			#ax = Axes3D(fig)
			#ax.plot_surface(Xs, Ys, p_values, rstride=4, cstride=4, alpha=0.4, cmap=cm.jet)
			#show()
	y, x = unravel_index(aggregate.argmax(), aggregate.shape)
	difference = math.sqrt((x - float(measurement['x'])) ** 2 + (y - float(measurement['y'])) ** 2)
	fig = figure()
	ax = Axes3D(fig)
	ax.plot([float(measurement['x'])], [float(measurement['y'])], [0], linestyle="none", marker="o", mfc="none", markeredgecolor="red")
	ax.plot_surface(Xs, Ys, aggregate, rstride=4, cstride=4, alpha=0.4, cmap=cm.jet)
	show()
	print "(%s, %s) -> (%s, %s) = %s" % (measurement['x'], measurement['y'], x, y,difference)
	print "---------"
	return difference

def find_locations(bssids, points, base_map):
	difference = []
	for point in points:
		difference.append(find_location(bssids, points[point], base_map))
	print mean(difference)

def main():
	usage = "usage: %prog [options] [image]\n\nImports wifi measurements, generates models and predicts one point in the set"
	parser = option_parser(usage)
	(options, args) = parser.parse_args()
	base_map = Image.open(args[0])
	bssids, points = generate_data(sys.stdin)
	bssids = create_models(bssids, base_map)
	find_locations(bssids, points, base_map)

def option_parser(usage):
	parser = OptionParser(usage=usage)
	return parser

if __name__ == "__main__":
	main()

