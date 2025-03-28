# Validation of DTU Subsystem on Basys3

This folder contains the necessary files to validate the DTU subsystem on the Basys3 board. The DTU subsystem is wrapped and the Apb bus us connected to a UART to Apb bridge.

### Generate, Synthesize, and Program FPGA
```bash
make gen synth prog
```

### Running a Program
```bash
make upload PROG=leros/asm/???.s
```