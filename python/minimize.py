#!/usr/bin/env python
import os
import sys
import csv
import itertools
import scipy.stats
import scipy.special
from pylab import *
from numpy import *
from optparse import OptionParser
from matplotlib import pyplot
from mpl_toolkits.mplot3d import Axes3D
from numpy.polynomial import Polynomial

def display(x, y, freq):
	fig = figure()
	ax = Axes3D(fig)
	ax.plot_trisurf(x, y, freq)
	pyplot.show()

def process_csv(fh):
	x = []
	y = []
	freq = []
	reader = csv.reader(fh, delimiter=',', quotechar='"')
	for row in reader:
		# TODO: Switch to DictReader.
		freq.append(float(row.pop()))
		y.append(float(row.pop()))
		x.append(float(row.pop()))
	return array(x), array(y), array(freq)

def minimize(fh):
	x, y, freq = process_csv(fh)
	xx = x * x
	xxx = x * x * x
	logx = log(x)
	yy = y * y
	yyy = y * y * y
	logy = log(y)
	coords = transpose(array([ones(x.shape),x,xx,y,yy]))
	model, resid, rank, s = linalg.lstsq(coords, freq)
	fig = figure()
	ax = Axes3D(fig)
	Xs = np.arange(0, 1000, 10)
	Ys = np.arange(0, 1500, 10)
	Xs, Ys = np.meshgrid(Xs, Ys)
	Zs = model[0] + model[1]*Xs + model[2]*Xs**2 + model[3]*Ys + model[4]*Ys**2
	ax.plot(x, y, freq, linestyle="none", marker="o", mfc="none", markeredgecolor="red")
	ax.plot_surface(Xs, Ys, Zs, rstride=4, cstride=4, alpha=0.4, cmap=cm.jet)
	show()
	resid_var = resid[0] / freq.size
	print "%s, %s" % (model, resid_var)
	#resid_sqrt = sqrt(resid_var)
	#resid_norm = scipy.stats.norm(0, resid_sqrt)
	#x = .25
	#z_score = x/resid_sqrt
	#p_value = (1 - scipy.special.ndtr(z_score)) * 2
	#print p_value

def main():
	usage = "usage: %prog [options]\n\nImports wifi measurements and generates data.csv, test_set.csv & validation_set.csv"
	parser = option_parser(usage)
	(options, args) = parser.parse_args()
	minimize(sys.stdin)

def option_parser(usage):
	parser = OptionParser(usage=usage)
	return parser

if __name__ == "__main__":
	main()

