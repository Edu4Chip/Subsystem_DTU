APB target (slave) implementation in chisel. Implements AMBA APB4. It connects a number of registers to the APB4 interface. 

The optional protection signal `pprot`and strobe signal `pstrb` are not implemented. 

The implementation can be customzed with following arguments:
* `dataWidth`
* `addrWidth`
* `registerCount`
* `baseAddr`

The implementations raises error signal `pslverr` when an invalid address is supplied. Byte level adressing is used. 

A testbench is included which can performs a number of read and writes, and compares the content of registers after each read/write with a software simulation. 
The number of read/writes executed can be changed by setting the `testCases` variable. 