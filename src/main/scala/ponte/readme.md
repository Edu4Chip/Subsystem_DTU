# Ponte - UART-to-APB Bridge

`Ponte` is an UART-to-APB bridge that allows us to communicate with the APB network of the DTU Subsystem using a UART interface. The targeted APB bus has 16-bit addresses and 32-bit data words.

The system clock frequency and the baud rate of the UART interface are parameters to the module. The system has been tested at 100MHz and 921600 baud.

The `src/main/scala/ponte/PonteSerialDriver.scala` class provides a wrapper around a serial port to provide write and read transaction methods for a host computer to communicate with the `Ponte` module.

## IO Ports

| **Port Name** | **Direction** | **Width** | **Description**            |
|---------------|---------------|-----------|----------------------------|
| `clock`       | Input         | 1         | System clock signal        |
| `reset`       | Input         | 1         | System reset signal        |
| `uart_rx`     | Input         | 1         | UART receive signal        |
| `uart_tx`     | Output        | 1         | UART transmit signal       |
| `apb_paddr`   | Output        | 16        | APB address bus            |
| `apb_psel`    | Output        | 1         | APB select signal          |
| `apb_penable` | Output        | 1         | APB enable signal          |
| `apb_pwrite`  | Output        | 1         | APB write signal           |
| `apb_pwdata`  | Output        | 32        | APB write data bus         |
| `apb_prdata`  | Input         | 32        | APB read data bus          |
| `apb_pready`  | Input         | 1         | APB ready signal           |
| `apb_pslverr` | Input         | 1         | APB slave error signal     |

## Protocol

The serial protocol uses three special bytes:

- `0xAA` signals the start of a write-transaction.
- `0xAB` signals the start of a read-transaction.
- `0x5A` is the escape byte, signaling that the next byte is to be interpreted as data and should be XORed with `0x20` to retrieve the original byte.

A write-transaction follows the following pattern:

- send `0xAA`
- send the 16-bit start address (word-aligned)
- send 4 bytes of write data
- continue sending 4 bytes of write data (the address is auto-incremented by 4)

A read-transaction follows the following pattern:

- send `0xAB`
- send *one* byte for the number of words to be read *Minus One*
  - e.g. sending `0` will read one word
  - up to 256 words can be read in a single transaction
- send the 16-bit start address (word-aligned)
- receive `n * 4` bytes of read data on the rx line

An ongoing write-transaction will be aborted if a new read- or write-transaction is started. Writes are committed to the APB bus every 4 bytes and are always completed. 

Read transactions can't be aborted. The full number of specified words will be sent back to the host before new transactions are accepted.

If `0xAA`, `0xAB`, or `0x5A` occur as part of the address, read length or data, they are escaped by sending `0x5A` followed by the original byte XORed with `0x20`.

## Resource Utilization on Basys3 FPGA

Synthesizing the `Ponte` module for the Basys3 FPGA results in the following resource utilization:

| **Resource** | **Ponte Protocol Decoder** | **Uart Transceiver** | **Total** |
|--------------|----------------------------|----------------------|-----------|
| **LUTs**     | 97                         | 109                  | 206       |
| **FFs**      | 63                         | 69                   | 132       |
| **Carry**    | 4                          | 10                   | 14        |
