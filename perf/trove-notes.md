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

  - Implement custom thread pool and properly parallellize file loading;
  - cache WebMercator projections.

## Updated plan of action

Parallelizing reading the file appears to only make things slower. Giving up on that.
