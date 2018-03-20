#!/usr/bin/env python3

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

df = pd.read_csv(args.inputFile)

t = df["time"][5:]
xacc = df["x accel"][5:]
xma = df["x ma"][5:]
yacc = df["y accel"][5:]
yma = df["y ma"][5:]
zacc = df["z accel"][5:]
zma = df["z ma"][5:]

plt.figure(figsize=(15,7.5))
plt.plot(t, xacc, "ro", t, xma, "b-")
plt.savefig("xma.png")

plt.figure(figsize=(15,7.5))
plt.plot(t, xacc, "ro", t, xma, "b-")
plt.savefig("yma.png")

plt.figure(figsize=(15,7.5))
plt.plot(t, xacc, "ro", t, xma, "b-")
plt.savefig("zma.png")
