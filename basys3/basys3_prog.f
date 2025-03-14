# File: 7series.txt
adapter driver ftdi
ftdi device_desc "Digilent USB Device"
ftdi vid_pid 0x0403 0x6010
# channel 1 does not have any functionality
ftdi channel 0
# just TCK TDI TDO TMS, no reset
ftdi layout_init 0x0088 0x008b
reset_config none
adapter speed 10000

source [find cpld/xilinx-xc7.cfg]
source [find cpld/jtagspi.cfg]
init

puts [irscan xc7.tap 0x09]
puts [drscan xc7.tap 32 0]  

puts "Programming FPGA..."
pld load 0 ../build/Basys3Top.bit
exit