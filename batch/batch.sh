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
    points/big-glyph-120k
    points/big-glyph-140k
    points/big-glyph-160k
    points/big-glyph-180k
HERE
#     csv/glottovis.tsv
#     points/big-glyph-5k
#     points/big-glyph-10k
#     points/big-glyph-15k
#     points/big-glyph-20k
#     points/big-glyph-25k
#     points/big-glyph-30k
#     points/big-glyph-35k
#     points/big-glyph-40k
#     points/big-glyph-45k
#     points/big-glyph-50k
#     points/big-glyph-60k
#     points/big-glyph-70k
#     points/big-glyph-80k
#     points/big-glyph-90k
#     points/big-glyph-100k
#     points/big-glyph-120k
#     points/big-glyph-140k
#     points/big-glyph-160k
#     points/big-glyph-180k
#     points/big-glyph-200k
#     points/glottovis-20k
#     points/glottovis-30k
#     points/glottovis-40k
#     points/glottovis-50k
#     points/glottovis-60k
#     points/glottovis-70k
#     points/glottovis-80k
#     points/glottovis-90k
#     points/glottovis-100k
#     points/glottovis-120k
#     points/glottovis-140k
#     points/glottovis-160k
#     points/glottovis-180k
#     points/glottovis-200k
#     points/trove
#     points/trove-50k
#     points/trove-100k
#     points/trove-200k
#     points/uniform-10k
#     points/uniform-50k
#     points/uniform-100k
#     points/uniform-200k

read -r -d '' ALGORITHMS <<-'HERE'
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
