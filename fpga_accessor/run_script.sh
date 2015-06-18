#!/usr/bin/env bash

python run.py -cf ../data/convolution0.txt -if ../data/sample_image_20x20.txt -cd /dev/null -fi fpga_in -fo fpga_in -ci convolved_image -v
