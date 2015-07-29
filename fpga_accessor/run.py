from __future__ import print_function
import argparse
import sys
import csv
import numpy as np

import os

__author__ = 'Chick Markley chick@eecs.berkeley.edu U.C. Berkeley'


class FpgaRunner(object):
    def __init__(self):
        parser = argparse.ArgumentParser()
        parser.add_argument('-cf', '--convolution_file', help='csv file containing convolution convolution',
                            default='convolution')
        parser.add_argument('-if', '--image_file', help='csv file containing image data',
                            default='image')
        parser.add_argument('-cd', '--convolution_device', help='device used to set fpga convolution',
                            default='/dev/xillybus_coeff_write')
        parser.add_argument('-fi', '--fpga_in', help='device used to load image to fpga',
                            default='/dev/xillybus_data_write')
        parser.add_argument('-fo', '--fpga_out', help='device used to read image from fpga',
                            default='/dev/xillybus_data_read')
        parser.add_argument('-ci', '--convolved_image', help='where to save the convolved image',
                            default='convolved_image')
        parser.add_argument('-v', '--verbose', help='turn on narration of activities', action='store_true',
                            default=False)

        self.configuration = parser.parse_args(sys.argv[1:])

        self.convolution_file = self.configuration.convolution_file
        self.image_file = self.configuration.image_file
        self.convolution_device = self.configuration.convolution_device
        self.fpga_in = self.configuration.fpga_in
        self.fpga_out = self.configuration.fpga_out
        self.convolved_image = self.configuration.convolved_image
        self.verbose = self.configuration.verbose
        self.open_write_handles = []

    @staticmethod
    def read_file(file_name):
        with open(file_name, 'r') as f:
            data = np.loadtxt(f, dtype=np.float32)
        f.close()
        return data

    @staticmethod
    def write_file(file_name, matrix):
        np.savetxt(file_name, matrix)

    def write_device(self, device_name, matrix):
        byte_stream = matrix.tobytes()
        with open(device_name, 'wb') as f:
            f.write(byte_stream)
        self.open_write_handles.append(f)

    def read_from_device(self, device_name):
        with open(device_name, 'rb') as f:
            print("Before f.read on " + device_name)            
            #buffer = f.read(1600)
            buffer = f.read()
            print("After f.read")
        self.open_write_handles.append(f)
        matrix = np.fromstring(buffer, dtype=np.float32)
        return matrix

    def shutdown(self):
        for device in self.open_write_handles:
            device.close()
        self.open_write_handles = []

    def chatter(self, message):
        if self.verbose:
            print(message)

    def run(self):

        pid = os.fork()

        if not pid:
            convolution = FpgaRunner.read_file(self.convolution_file)
            self.chatter("convolution\n{}".format(convolution))

            image = FpgaRunner.read_file(self.image_file)
            self.chatter("image\n{}".format(image))

            self.write_device(self.convolution_device, convolution)

            self.write_device(self.fpga_in, image)

        else:
            convolved_data = self.read_from_device(self.fpga_out)

            FpgaRunner.write_file(self.convolved_image, convolved_data)

            self.shutdown()


if __name__ == '__main__':
    FpgaRunner().run()