#!/usr/bin/env python3

import sys
import pandas as pd
import matplotlib.pyplot as plt
import argparse

# parse command-line args
argument_parsing_description="""
Produce nice plots from csv files of sensor data.
The first line should be column names:
    time,x accel,y accel,z accel,x ma,y ma,z ma
Only plot the middle quantile of the data
"""
argparser = argparse.ArgumentParser(description=argument_parsing_description)
argparser.add_argument("inputFile", help="csv file to read from")
argparser.add_argument("quantileStart", type=int, help="starting quantile to plot")
argparser.add_argument("quantileEnd", type=int, help="ending quantile to plot")
args = argparser.parse_args()

# read csv
df = pd.read_csv(args.inputFile)
# don't want first 5 values since moving averag has window size 5
t = df["time"][5:]

# command-line arg error conditions
startRow = int(t.shape[0] * (args.quantileStart / 100))
endRow = int(t.shape[0] * (args.quantileEnd / 100))
if not endRow > startRow:
    print("ERROR:\n"+
          "quantileStart must be less than quantileEnd such that at least\n" +
          "one data row is in range [qauntileStart,quantileEnd]\n")
    argparser.print_help()
    sys.exit(1)

# select only the chosen quantile range of data
t = t.iloc[startRow:endRow]
xacc = df["x accel"][5:].iloc[startRow:endRow]
xma = df["x ma"][5:].iloc[startRow:endRow]
yacc = df["y accel"][5:].iloc[startRow:endRow]
yma = df["y ma"][5:].iloc[startRow:endRow]
zacc = df["z accel"][5:].iloc[startRow:endRow]
zma = df["z ma"][5:].iloc[startRow:endRow]

# add vertical lines  to graph
def addVerticalLines():
    for index, value in t.iteritems():
        if index % 8 == 0:
            plt.axvline(x=value, alpha=0.25, color="g", lw=4)
        elif index % 4 == 0:
            plt.axvline(x=value, alpha=0.25, color="y", lw=4)

# plot x raw data and x ma (moving average) against time
plt.figure(figsize=(15,7.5))
plt.plot(t, xacc, "ro", t, xma, "b-")
addVerticalLines()
plt.savefig("xma.png")

# plot y raw data and y ma (moving average) against time
plt.figure(figsize=(15,7.5))
plt.plot(t, yacc, "ro", t, yma, "b-")
addVerticalLines()
plt.savefig("yma.png")

# plot z raw data and z ma (moving average) against time
plt.figure(figsize=(15,7.5))
plt.plot(t, zacc, "ro", t, zma, "b-")
addVerticalLines()
plt.savefig("zma.png")

# plot z,y,z moving averages together
plt.figure(figsize=(15,7.5))
plt.plot(t, xma, "r-", label="x MA")
plt.plot(t, yma, "b-", label="y MA")
plt.plot(t, zma, "c-", label="z MA")
addVerticalLines()
plt.legend()
plt.savefig("all_ma.png")
