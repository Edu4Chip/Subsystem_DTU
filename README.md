![CI](https://github.com/Edu4Chip/Subsystem_DTU/actions/workflows/scala.yml/badge.svg)

# DTU Subsystem Edu4Chip

The project at DTU is a small 32-bit processor, called Leros. Leros is a tiny processor core for embedded systems.
See more documentation on the [website for Leros](https://leros-dev.github.io/).

Leros is able to execute a fixed program from a read-only memory or a program can be loaded to a programmable instruction memory. The instruction memory is programmed via an APB interface which is connected to the Ibex core in the staff area as well as to an UART-to-APB bridge. The UART-to-APB bride uses a serial protocol for performing APB read and write transactions from a host machine.

Leros is controlled through a register mapped into the APB memory space which controls the boot source and Leros' reset pin. It has access to a UART interface and 4 GPIO pins.

Cross Core Registers facilite communication between Leros and Ibex. They consist of two sets of registers, the first set is readable from APB and writeable from Leros, the second set is writeable from APB and readable from Leros. For now system features 4 cross core registers in each direction. Cross core registers are 32-bit wide.

![DTU Toplevel Diagram](doc/figures/toplevel-dtu.svg)

## Building the DTU Subsystem

Initialize git submodules:

```shell
git submodule update --init --recursive
```

Generate SystemVerilog:

```shell
make generate
```

Run tests:

```shell
make test
```

## FPGA Validation on Basys3

Synthesize the design with the specified program in ROM.

```shell
make -C basys3 gen synth PROG=leros-asm/??.s
```

Upload the bitstream to the FPGA:

```shell
make -C basys3 prog
```

Upload a program to the instruction memory:

```shell
make -C basys3 upload PROG=leros-asm/??.s
```

## Hardening the Subsystem with OpenLane

```shell
make -C openlane gen harden
```

## Hardening the Subsystem with LibreLane

```shell
make -C openlane gen librelane
```

## Subsystem Configuration

The DTU subsystem is configured using `DtuSubsystemConfig`. An example configuarion is shown below:

```scala
new DtuSubsystem(
  DtuSubsystemConfig(
    romProgramPath = "leros-asm/didactic_rt.s",
    instructionMemorySize = 1 << 11, // 2kB
    dataMemorySize = 1 << 8, // 256 bytes
    lerosSize = 32, // 32-bit accumulator
    lerosMemAddrWidth = 16, // 16-bit address space
    crossCoreRegisters = 4,
    frequency = 100000000, // 1MHz
    lerosBaudRate = 115200,
    ponteBaudRate = 921600,
    apbAddrWidth = 12,
    apbDataWidth = 32
  )
)
```

## Pin Table

| Name              | Direction           | Function                   |
| ------------------| --------------------| -------------------------- |
| `clock`           | input               | clock                      |
| `reset`           | input               | reset signal (active high) |
| `p<signal>`       | input/output        | APB interface              |
| `irq`             | output              | unused                     |
| `irqEn`           | input               | unused                     |
| `ssCtrl`          | input               | unused                     |
| `pmod[0][0]`      | output              | Tx for Uart to Apb bridge  |
| `pmod[0][1]`      | input               | Rx for Uart to Apb bridge  |
| `pmod[0][2]`      | output              | Tx for Leros Uart          |
| `pmod[0][3]`      | input               | Rx for Leros Uart          |
| `pmod[1]`         | input/output        | Leros GPIO                 |

## Memory Map

![Memory Maps](doc/figures/toplevel-dtu-addrmap.svg)

## Development Environment

The DTU subsystem development can be done with either Docker or Nix/Lix.

### Installation Options

- **Lix** (Recommended): [https://lix.systems/install/](https://lix.systems/install/)
  - A modern implementation of the Nix language with improved usability
  - Commited to maintaining purpose for open source community than commercial interests.
  - Built for better error messages and developer experience
  - Fully compatible with Nix packages and flakes
  - Designed to evolve while maintaining backward compatibility

- **Nix**: [https://nixos.org/download](https://nixos.org/download)
  - The original package manager that Lix is based on

- **Docker**: [https://docs.docker.com/engine/install/](https://docs.docker.com/engine/install/)
  - Container-based alternative if you prefer not to install Nix/Lix

### Quick Start with Nix/Lix

1. Enter the development environment (assuming from project root):
```zsh
nix develop open-source-hardware-development-environment/
```

2. You'll now have access to all development tools including:
   - Openlane 2 with all dependencies
   - Verilator for Verilog simulation
   - SBT for Scala/Chisel development
   - Leros LLVM toolchain (x86 platforms only)
   - ZSH with developer-friendly configuration

### Using Docker Alternative

```zsh
# Build the Docker image
docker build -t hw-dev-env open-source-hardware-development-environment/

# Run the Docker container with the development environment
# in the current directory
docker run -it -rm \
  -v .:/home/developer/workdir/ \
  hw-dev-env \
  nix develop
```

Remember to change the local path to your codebase in the Docker command if it is needed elsewhere.

For more detailed instructions, see the [development environment README from the project](open-source-hardware-development-environment/README.md) or from the [repository page](https://codeberg.org/Kodalem/open-source-hardware-development-environment) of the submodule 
