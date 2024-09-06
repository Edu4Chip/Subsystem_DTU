![CI](https://github.com/Edu4Chip/Subsystem_DTU/actions/workflows/scala.yml/badge.svg)

# DTU Repository

The project at DTU is a small 32-bit processor, called Leros.
Leros is a tiny processor core for embedded systems.
See more documentation on the [website for Leros](https://leros-dev.github.io/).

The instruction memory shall be loaded from the IBEX over the APB or over UART interface, this is selectable using `boot` pin. 

Leros will be connected to the two PMOD IO pins, using a UART and blinking an LED.

Besides Leros, we will also have a tiny FSM to blink (and beep) "Hello World" in Morse code.

## Diagram

![Alt text](doc/figures/DTU_Subsystem_Diagram.png)


## Pin Table

| Name              | Direction           | Function                   |
| ------------------| --------------------| -------------------------- |
| `clock`           | input               | clock                      |
| `reset`           | input               | reset signal (active high) |
| `P<signal>`       | input/output        | APB interface              |
| `boot`            | input               | select boot source         |
| `uart_tx_prog`    | output              | UART program interface     |
| `uart_rx_prog`    | input               | UART program interface     |
| `uart_tx_leros`   | output              | Leros UART interface       |
| `uart_rx_leros`   | input               | Leros UART interface       |
| `led`             | output              | Led output                 |
| `morse`           | output              | Morse output               |

## Instructions
Note that this project includes Leros and a tiny FSM as submodules. Therefore, you need to update with:

```
git submodule update --init --recursive
```

## TODO

 - [x] Add CI
 - [ ] Testing in an FPGA


