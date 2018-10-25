#!/bin/bash

function interrupt {
    EXIT=1
    tput el1 # clear line
    echo -ne "\r" # move to beginning of line
    echo "Interrupt! Finishing up, then stopping ..."
}

trap interrupt INT # properly exit on Ctrl+C


EXIT=0
HOME="/home/thom/Code/src/growing-glyphs"

read -r -d '' INPUTS <<-'HERE'
     points/big-glyph-50k
     points/big-glyph-100k
     points/big-glyph-200k
HERE
#     csv/glottovis.tsv
#     points/trove
#     points/big-glyph-10k
#     points/uniform-10k
#     points/big-glyph-50k
#     points/uniform-50k
#     points/glottovis-50k
#     points/trove-50k
#     points/big-glyph-100k
#     points/uniform-100k
#     points/glottovis-100k
#     points/trove-100k
#     points/big-glyph-200k
#     points/uniform-200k
#     points/glottovis-200k
#     points/trove-200k

read -r -d '' ALGORITHMS <<-'HERE'
    plus
    big
HERE
#     naive
#     quad
#     plus
#     big

read -r -d '' GROWFUNCTIONS <<-'HERE'
    linear-squares
HERE
#     linear-squares
#     lineararea-squares
#     logarithmic-squares
#     linear-circles
#     lineararea-circles
#     logarithmic-circles

ITERATIONS=5


for INPUT in $INPUTS; do
for GROWFUNCTION in $GROWFUNCTIONS; do
for ALGORITHM in $ALGORITHMS; do
for ((i=1;i<=ITERATIONS;i++)); do
    timeout 300 java -Djava.util.logging.config.file=$HOME/logging.properties \
         -Xverify:none -Xmx3072M -jar GG.CLI.jar \
         "$HOME/input/$INPUT" \
         "$HOME/batch/output.txt" \
         $ALGORITHM $GROWFUNCTION n
    if [[ $? -eq 124 ]]; then
        TIME="timed out"
    else
        TIME=$(grep clustering output.txt | grep took | awk -F' ' '{print $5}')
    fi
    echo -e "$INPUT\t$GROWFUNCTION\t$ALGORITHM\t$TIME" >> "$HOME/batch/results.tsv"
    rm "$HOME/batch/output.txt"
    if [[ $EXIT -eq 1 ]]; then
        exit 130
    fi
done
done
done
done
