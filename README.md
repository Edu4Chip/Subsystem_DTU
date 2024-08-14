# Repository template (DTU)

The project at DTU is a small 32-bit processor, called Leros.
Leros is a tiny processor core for embedded systems.
See more documentation on the [website for Leros](https://leros-dev.github.io/).

The instruction memory shall be loaded from the IBEX over the APB.

Leros will be connected to the two PMOD IO pins, using a UART and blinking an LED.

Note that this project includes Leros and a tiny FSM as submodules. Therefore, you need to update with:

```
git submodule init
git submodule update
```

