# Initial running time
This is the output of the first run on clustering Trove (`trove-dump-uniq.tsv`).

    INFO    |  created 8,335,897 events, handled 8,118 and discarded 7,896,507; 431,272 events were never considered
    INFO    |  took 432,318 ms (wall clock time)

## Plan of action

  - Try to reduce time it takes to load file (not in above output, but it is considerable);
  - focus on determining as fast as possible whether an event should be discarded;
  - more general profiling.

# After adding more logging

    FINE    |  ENTRY into CsvIO#read()
    INFO    |  counting lines took 6,147 ms (wall clock time)
    INFO    |  constructing HashMap took 0 ms (wall clock time)
    FINE    |  RETURN from CsvIO#read()
    INFO    |  reading file took 42,971 ms (wall clock time)
    INFO    |  read 15,772,243 entries and ignored 438,657

# After parallelizing reading file with a Stream

...no improvement. It appears that using streams is not as easy as I thought it would be.

## Plan of action

Implement custom thread pool and properly parallellize file loading.

## Updated plan of action

Parallelizing reading the file appears to only make things slower. Giving up on that.

    INFO    |  reading file took 35,798 ms (wall clock time)
    INFO    |  read 15,772,243 entries and ignored 438,657
    INFO    |  created 8,335,893 events, handled 8,118 and discarded 7,896,507; 431,268 events were never considered
    INFO    |  clustering took 415,210 ms (wall clock time)

# Parallellize clustering?

General idea: process events within a cell of the QuadTree. As soon as an OUT_OF_CELL event is generated, pause. This gives a partial clustering and a set of alive glyphs. Four of these partial clusterings can be combined to get the partial clustering of the parent of four QuadTree cells. That would require a possibility to "roll back" a partial clustering, so that the clustering can continue from the earliest point in time of the four partial clusterings. QuadTree cells can track whether they have been clustered already.

Overall approach: have a queue with "work packages". Each package is either a cell and some glyphs (initial state), or four partially clustered cells and the assignment to combine them. When clustering of a cell has completed, it should be checked if this was the last cell of its siblings to complete. If so, the parent should be added to the queue as a work package. Clustering completes when the root has been clustered. Because work packages are disjoint, this can be parallellized.

The hope is that not too much work needs to be undone when combining partial clusterings.

# More debug information

Now logging more detailed logging information: specific operations, nanosecond accuracy, number of events per type, queue size, ...

    INFO    |  loaded 447 locations
    INFO    |  reading file took 0,13 seconds (wall clock time)
    INFO    |  read 6.924 entries and ignored 0
    INFO    |  created 27.972 events, handled 2.647 and discarded 23.232; 2.093 events were never considered
    INFO    |  clustering took 0,388 seconds (wall clock time)
    INFO    |  queue operations took 0,023 seconds (wall clock time, 53.851 timings)
    INFO    |  queue size was 8.148,914 on average, over 53.851 measurements
