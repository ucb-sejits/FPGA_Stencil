#!/usr/bin/env bash

python run.py -cf ../data/convolution0.txt -if ../data/sample_image_20x20.txt -cd /dev/xillybus_coeff_write -fi /dev/xillybus_data_write -fo /dev/xillybus_data_read -ci convolved_image -v
