![CI](https://github.com/Edu4Chip/Subsystem_DTU/actions/workflows/scala.yml/badge.svg)

# DTU Repository

The project at DTU is a small 32-bit processor, called Leros.
Leros is a tiny processor core for embedded systems.
See more documentation on the [website for Leros](https://leros-dev.github.io/).

The instruction memory shall be loaded from the IBEX over the APB.

Leros will be connected to the two PMOD IO pins, using a UART and blinking an LED.

Besides Leros, we will also have a tiny FSM to blink (and beep) "Hello World" in Morse code.

Note that this project includes Leros and a tiny FSM as submodules. Therefore, you need to update with:

```
git submodule update --init --recursive
```

## TODO

 - [x] Add CI
 - [ ] Testing in an FPGA


