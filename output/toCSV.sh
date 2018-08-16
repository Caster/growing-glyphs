#!/bin/bash

if [[ $# -ne 1 ]]; then
  echo "need to pass directory as only argument"
  exit 1
fi

echo -e "dataset\trunning time (s)\tQuadTree cells min\tQuadTree cells avg\tQuadTree cells max\tQuadTree cells n\t\
QuadTree leaves min\tQuadTree leaves avg\tQuadTree leaves max\tQuadTree leaves n\t\
QuadTree height min\tQuadTree height avg\tQuadTree height max\tQuadTree height n\t\
QuadTree joins\tQuadTree splits\tQuadTree inserts\tQuadTree inserts actual\t\
QuadTree removes\tQuadTree removes actual\t\
out of cell events handled\tout of cell events discarded\t\
merge events handled\tmerge events discarded"

for dataset in $1/*; do
  basename="$(basename "$dataset")"
  if [[ "$basename" == "log.txt" ]]; then
    continue;
  fi

  echo -ne "$basename\t"

  echo -ne "$(awk '$3 == "clustering" && $4 == "took" {print $5}' "$1-no-stats/$basename")\t"

  echo -ne "$(awk '$3 == "QuadTree" && $4 == "cells" {print $12}' "$dataset")\t"
  echo -ne "$(awk '$3 == "QuadTree" && $4 == "cells" {print $6}' "$dataset")\t"
  max="$(awk '$3 == "QuadTree" && $4 == "cells" {print $14}' "$dataset")"
  echo -ne "${max%?}\t"
  echo -ne "$(awk '$3 == "QuadTree" && $4 == "cells" {print $16}' "$dataset")\t"

  echo -ne "$(awk '$3 == "QuadTree" && $4 == "leaves" {print $12}' "$dataset")\t"
  echo -ne "$(awk '$3 == "QuadTree" && $4 == "leaves" {print $6}' "$dataset")\t"
  max="$(awk '$3 == "QuadTree" && $4 == "leaves" {print $14}' "$dataset")"
  echo -ne "${max%?}\t"
  echo -ne "$(awk '$3 == "QuadTree" && $4 == "leaves" {print $16}' "$dataset")\t"

  echo -ne "$(awk '$3 == "QuadTree" && $4 == "height" {print $12}' "$dataset")\t"
  echo -ne "$(awk '$3 == "QuadTree" && $4 == "height" {print $6}' "$dataset")\t"
  max="$(awk '$3 == "QuadTree" && $4 == "height" {print $14}' "$dataset")"
  echo -ne "${max%?}\t"
  echo -ne "$(awk '$3 == "QuadTree" && $4 == "height" {print $16}' "$dataset")\t"

  echo -ne "$(awk '$3 == "QuadTree" && $4 == "join" {print $7}' "$dataset")\t"

  echo -ne "$(awk '$3 == "QuadTree" && $4 == "split" {print $7}' "$dataset")\t"

  echo -ne "$(awk '$3 == "QuadTree" && $4 == "insert" && $5 == "occurred" {print $6}' "$dataset")\t"
  echo -ne "$(awk '$3 == "QuadTree" && $4 == "insert" && $5 != "occurred" {print $5}' "$dataset")\t"

  echo -ne "$(awk '$3 == "QuadTree" && $4 == "remove" && $5 == "occurred" {print $6}' "$dataset")\t"
  echo -ne "$(awk '$3 == "QuadTree" && $4 == "remove" && $5 != "occurred" {print $5}' "$dataset")\t"

  events="$(awk '$5 == "out" {print $9,"\t",$11}' "$dataset")"
  echo -ne "${events:1}\t"

  events="$(awk '$5 == "merge" {print $7,"\t",$9}' "$dataset")"
  echo -e "${events:1}"
done
