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

## Trove

    INFO    |  loaded 7.758 locations
    INFO    |  read 15.772.243 entries and ignored 438.657
    FINE    |  QuadTree has 5.909 nodes and height 15
    FINE    |  created 39.439 events initially, for 7.754 glyphs
    FINE    |  created 8.335.893 events, handled 8.118 and discarded 7.896.508; 431.267 events were never considered
    FINE    |  created 340.814 out of cell events (365 handled, 235.931 discarded)
    FINE    |  created 7.995.079 merge events (7.753 handled, 7.660.577 discarded)
    FINE    |  clustering took 409,628 seconds (wall clock time)
    FINE    |  queue operations took 11,18 seconds (wall clock time, 16.240.519 timings)
    FINE    |  queue size was 1.887.522,031 on average and always between 1 and 2.855.186, over 16.240.519 measurements
    FINE    |  glyphs per cell was 1.593,515 on average and always between 567 and 2.412, over 4.432 measurements

Clearly, the QuadTree needs to be subdivided adaptively while the glyphs are growing.

## Risse (used for debugging + optimizing)

    INFO    |  loaded 447 locations
    INFO    |  read 6.924 entries and ignored 0
    FINE    |  QuadTree has 341 nodes and height 9
    FINE    |  created 2.246 events initially, for 447 glyphs
    FINE    |  created 27.972 events, handled 2.647 and discarded 23.232; 2.093 events were never considered
    FINE    |  → 18.942 out of cell events (2.201 handled, 15.061 discarded)
    FINE    |  → 9.030 merge events (446 handled, 8.171 discarded)
    FINE    |  QuadTree has 341 nodes and height 9 now
    FINE    |  clustering took 0,392 seconds (wall clock time)
    FINE    |  queue operations took 0,021 seconds (wall clock time, 53.851 timings)
    FINE    |  queue size was 8.148,914 on average and always between 1 and 13.036, over 53.851 measurements
    FINE    |  glyphs per cell was 137,625 on average and always between 0 and 295, over 256 measurements

As one can see, the logging format changed a bit while working on the implementation. Adaptive subdivision resulted in the below result.

    INFO    |  loaded 447 locations
    INFO    |  read 6.924 entries and ignored 0
    FINE    |  QuadTree has 341 nodes and height 9
    FINE    |  created 2.246 events initially, for 447 glyphs
    FINE    |  created 25.643 events, handled 3.220 and discarded 20.342; 2.081 events were never considered
    FINE    |  → 17.660 out of cell events (2.774 handled, 13.211 discarded)
    FINE    |  → 7.983 merge events (446 handled, 7.131 discarded)
    FINE    |  QuadTree has 629 nodes and height 11 now
    FINE    |  clustering took 0,569 seconds (wall clock time)
    FINE    |  queue operations took 0,021 seconds (wall clock time, 49.205 timings)
    FINE    |  queue size was 6.983,72 on average and always between 1 and 10.834, over 49.205 measurements
    FINE    |  glyphs per cell was 0,511 on average and always between 0 and 5, over 544 measurements

    INFO    |  loaded 7.758 locations
    INFO    |  read 15.772.243 entries and ignored 438.657
    FINE    |  QuadTree has 5.909 nodes and height 15, having at most 5 glyphs per cell and cell size at least 0,001
    FINE    |  created 39.439 events initially, for 7.754 glyphs
    FINE    |  created 8.528.420 events, handled 8.121 and discarded 8.080.127; 440.172 events were never considered
    FINE    |  → 341.260 out of cell events (368 handled, 236.552 discarded)
    FINE    |  → 8.187.160 merge events (7.753 handled, 7.843.575 discarded)
    FINE    |  QuadTree has 6.277 nodes and height 15 now
    FINE    |  clustering took 460,273 seconds (wall clock time)
    FINE    |  queue operations took 12,694 seconds (wall clock time, 16.616.668 timings)
    FINE    |  queue size was 1.918.751,305 on average and always between 1 and 2.900.394, over 16.616.668 measurements
    FINE    |  glyphs per cell was 0,17 on average and always between 0 and 5, over 4.793 measurements

Seems to have gotten a bit slower, but changing the maximum number of glyphs per cell does have a big impact on performance.
