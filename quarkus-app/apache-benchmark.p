#set datafile separator '\t'

set title TITLE
set style fill solid border lt -1
set style textbox opaque noborder
set boxwidth 1.0 abs

# Where to place the legend/key
set key left top

set term png size 1400, 800
set terminal png

if (exists("IMAGE")) \
	set output IMAGE; \
	PLOT=0;
if (PLOT!=1 && PLOT!=2 && PLOT!=3) \
   IMAGE = "benchmark.png";
if (PLOT==1) \
   IMAGE = "benchmark-1.png";
if (PLOT==2) \
   IMAGE = "benchmark-2.png";   
if (PLOT==3) \
   IMAGE = "benchmark-3.png";
if (PLOT==4) \
   IMAGE = "benchmark-4.png";

if (PLOT!=0) \
    set output IMAGE;

set size 1,1
set grid y
set grid x
set xlabel "Time"
set ylabel "Response time (ms)"

set style line 2 lc rgb 'blue' # cross
#set style line 1 lc rgb 'blue' pt 5   # square
#set style line 2 lc rgb 'blue'  pt 7   # circle
#set style line 3 lc rgb 'blue' pt 9   # triangle

#set xtics nomirror rotate by -45

set style textbox opaque border lc "blue"

# Specify that the x-series data is time data
set xdata time
# Specify the *input* format of the time data
set timefmt "%H:%M:%S"
# Specify the *output* format for the x-axis tick labels
set format x "%H:%M:%S"

msg = sprintf("Ploting...   PLOT=%d  IMAGE=%s", PLOT, IMAGE)
print msg

# Ploting all Requests by Point
if (PLOT == 0 || PLOT == 1 ) \
    plot "data.tsv" every ::2 using 4:9 title LINE w p ls 2 pointsize 1.2 linewidth 1.5;

#Ploting a Interpolation and Aproximation Data (using smoth)
if (PLOT == 2 ) \
    plot "data.tsv" every ::2 using 4:9 smooth sbezier title LINE with points linewidth 2;
if (PLOT == 3 ) \
    plot "data.tsv" every ::2 using 4:9 smooth unique title LINE with lines linewidth 2;
if (PLOT == 4 ) \
    plot "data.tsv" every ::2 using 4:9 smooth csplines title LINE with lines linewidth 2;