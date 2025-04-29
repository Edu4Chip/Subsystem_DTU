# Leros: The Return of the Accumulator Machine

An FPGA-optimized tiny processor core for utility functions
(e.g., SW UART). The challenge is to get the resources below
500 LC and use just 2 RAM blocks. The processor is named after
the Greek island [Leros](https://en.wikipedia.org/wiki/Leros)
where the architecture was designed.

The Leros project is hosted at GitHub in [https://github.com/leros-dev](https://github.com/leros-dev).

Leros is documented in the following publications and documents:

1. Martin Schoeberl.
   [Leros: A Tiny Microcontroller for FPGAs](http://www.jopdesign.com/doc/leros.pdf).
   In Proceedings of the 21st International Conference on Field Programmable Logic and Applications (FPL 2011), Chania, Crete, Greece, September 2011.
2. James Caska and Martin Schoeberl.
   [Java dust: How small can embedded Java be?](http://www.jopdesign.com/doc/lerosjvm.pdf)
   In Proceedings of the 9th International Workshop on Java Technologies for Real-Time and Embedded Systems (JTRES 2011), York, UK, ACM, September 2011.
3. Morten Borup Petersen.
   [A Compiler Backend and Toolchain for the Leros Architecture](https://findit.dtu.dk/en/catalog/2443128784)
   BSc. thesis, Technical University of Denmark (2019)
4. Martin Schoeberl and Morten Borup Petersen.
   [Leros: The return of the accumulator machine.](https://www.jopdesign.com/doc/leros32.pdf)
   Architecture of Computing Systems - ARCS 2019 - 32nd International Conference, Proceedings, 115-127, May, 2019.



A work-in-progress handbook is available as LaTeX sources at [Leros Handbook](https://github.com/leros-dev/leros-doc/tree/master/handbook)

## Architecture

Leros is an accumulator machine with a register file. Memory is accessed
via indirect load and store instructions.

## Leros Aims

*An accumulator instruction that does less than a typical RISC
instruction is probably more RISC than the typical load/store
register-based RISC architecture.*

* A simple architecture
    * Results in a cheap FPGA implementation
    * Easy to use in teaching
    * Just a few instructions
* Different bit width
    * 16-bit version for tiny microcontroller
    * 32-bit version as a reasonable target for C
    * 64-bit version for a Linux port

Further aims:

* Serve as an example of a small Chisel project for the Chisel book
* Use in teaching in fall 2018
* Provide virtual memory (with paging) for a Linux port
* Use for student projects
* Use for manycore experiments (NoC with more than 9 cores on a DE2-115)


## Instruction Set Architecture

The instructions of Leros can be categorized into the following types:

* ALU operation with the accumulator and an immediate
* ALU operation with the accumulator and a register
* Load and store
* Indirect load and store
* Conditional branches
* Jump and link
* Arithmetic shift right
* (Input and output)


### List of Instructions

| Opcode    | Function               | Description                                       |
| --------- | ---------------------- | ------------------------------------------------- |
| add       | A = A + Rn             | Add register Rn to A                              |
| addi      | A = A + i              | Add immediate value i to A (sign extend i)        |
| sub       | A = A - Rn             | Subtract register Rn from A                       |
| subi      | A = A - i              | Subtract immediate value i from A (sign extend i) |
| sra       | A = A >> 1             | Shift A arithmetic right                          |
| and       | A = A and Rn           | And register Rn with A                            |
| andi      | A = A and i            | And immediate value i with A                      |
| or        | A = A or Rn            | Or register Rn with A                             |
| ori       | A = A or i             | Or immediate value i with A                       |
| xor       | A = A xor Rn           | Xor register Rn with A                            |
| xori      | A = A xor i            | Xor immediate value i with A                      |
| load      | A = Rn                 | Load register Rn into A                           |
| loadi     | A = i                  | Load immediate value i into A (sign extend i)     |
| loadhi    | A$_{31-8}$ = i         | Load immediate into second byte (sign extend i)   |
| loadh2i   | A$_{31-16}$ = i        | Load immediate into third byte (sign extend i)    |
| loadh3i   | A$_{31-24}$ = i        | Load immediate into fourth byte (sign extend i)   |
| store     | Rn = A                 | Store A into register Rn                          |
| jal       | PC = A, Rn = PC + 2    | Jump to A and store return address in Rn          |
| ldaddr    | AR = Rn                | Load address register AR with Rn                  |
| loadind   | A = mem[AR+(i << 2)]   | Load a word from memory into A                    |
| loadindb  | A = mem[AR+i]$_{7-0}$  | Load a byte signe extending from memory into A    |
| storeind  | mem[AR+(i << 2)] = A   | Store A into memory                               |
| storeindb | mem[AR+i]$_{7-0}$ = A  | Store a byte into memory                          |
| br        | PC = PC + o            | Branch                                            |
| brz       | if A == 0 PC = PC + o  | Branch if A is zero                               |
| brnz      | if A != 0 PC = PC + o  | Branch if A is not zero                           |
| brp       | if A >= 0 PC = PC + o  | Branch if A is positive                           |
| brn       | if A < 0 PC = PC + o   | Branch if A is negative                           |
| scall     | scall A                | System call (simulation hook)                     |

### Instruction Encoding

Instructions are 16 bits wide. The higher byte is used to encode the
instruction, the lower byte contains either an immediate value, a
register number, or a branch offset (part of the branch offset uses
also bits in the upper byte).

```
+--------+--------+
|iiiiiiii|nnnnnnnn|
+--------+--------+
```

For example `00001001.00000010` is an add immediate instruction that
adds 2 to the accumulator, where `00001000.00000011` adds the content
of R3 to the accumulator. For branches, we use 3 of the instruction bits
for larger offsets.


The following table shows all currently defined instructions (21, if you include
all conditional branch variations).

Not all instruction bits are currently used (unused are marked with `-`).
Bit 0 selects between immediate and using a register. The following list
is the complete instruction set.


```
+--------+----------+
|00000---| nop      |
|000010-0| add      |
|000010-1| addi     |
|000011-0| sub      |
|000011-1| subi     |
|00010---| sra      |
|00011---| -        |
|00100000| load     |
|00100001| loadi    |
|00100010| and      |
|00100011| andi     |
|00100100| or       |
|00100101| ori      |
|00100110| xor      |
|00100111| xori     |
|00101001| loadhi   |
|00101010| loadh2i  |
|00101011| loadh3i  |
|00110---| store    |
|001110-?| out      |
|000001-?| in       |
|01000---| jal      |
|01001---| -        |
|01010---| ldaddr   |
|01100-00| ldind    |
|01100-01| ldindb   |
|01100-10| ldindh   |
|01110-00| stind    |
|01110-01| stindb   |
|01110-10| stindh   |
|1000nnnn| br       |
|1001nnnn| brz      |
|1010nnnn| brnz     |
|1011nnnn| brp      |
|1100nnnn| brn      |
|11111111| scall    |
+--------+----------+
```

#### Comments

`loadh` makes only sense for immediate values.

Can easily be extended to 64 bits when ignoring the immediate bit.
Load function from ALU could be dropped.

Load address and following load/store should be emitted as pair as they are
dependent. Possible interrupts should be disabled between those two instructions.

`ldindb/ldindh` sign extends.

Why do we have a nop? addi 0 can serve as nop if needed.

## Getting Started

To run all test execute:
```bash
sbt test
```

More targets (e.g., synthesize for an FPGA) can be found in the Makefile.

### LLVM Toolchain
Initially, pull and build the [leros-llvm](https://github.com/leros-dev/leros-llvm) by executing the `build.sh` script in the root repository directory.
The LLVM toolchain provides all the binary utilities from GNU Binutils. Following are a couple of examples on how the toolchain may be used in a development process:

*Note*: If an LLVM installation is already present on your machine, ensure that the executables within the build directory of the Leros toolchain are executed instead of the LLVM executables accessible through the `PATH`.

To compile a C source file for the Leros architecture, execute:
```bash
clang -target leros32 -c foo.c -o foo.o
```
This will create an unlinked ELF object file containing Leros machine code.
To check that actual Leros instructions were emitted, `objdump` may be used to disassemble the object file:
```bash
llvm-objdump -d foo.o
```

The Leros toolchain assumes a number of constants to be present in certain registers when compiling a C program. These registers are initialized in the Leros [crt0.leros.c](https://github.com/leros-dev/leros-lib/blob/master/runtime/crt0.leros.c) file. For more information on crt0 files, refer to: https://en.wikipedia.org/wiki/Crt0.
The crt0 object file as well as the runtime library functions are built by the `build.sh` script and placed inside the toolchain library folders. These object files are automatically linked whenever using the Leros linker.

For compiling a Leros program and linking it with the crt0 object file, execute:
```bash
clang -target leros32 foo.c -o foo.out
```
This will emit an executable ELF file, which may be executed by the Leros simulator (https://github.com/leros-dev/leros-sim).
If a flat binary version of an executable is needed, the `llvm-objcopy` may be used:
```bash
llvm-objcopy foo.out -O binary foo.out foo.bin
```
This will dump all of the ELF sections to a flat binary file, suitable for running on simulators or used to initialize hardware ROMs. Note, that this will emit the various program sections at some default address. When executing on hardware, it may be desired to emit code at a specific address placement. For this, a linker script is needed.
As an example, it is desired for a programs entry point (and .text segment) to be emitted at address `0x0`.
A linker script for this may be:
```ld
# file: leros.ld
ENTRY(_start)

SECTIONS
{
    . = 0x0;
   .text : { *(.text) }
}
```
Here, we refer to the `_start` symbol specified in the [crt0.leros.c](https://github.com/leros-dev/leros-lib/blob/master/runtime/crt0.leros.c) file, as well as specify that the .text section - the instructions of the program - are to be emitted from address 0x0.
The linker script may be passed as an argument to the linker through clang, by specifying:
```bash
clang -target leros32 -Xlinker leros.ld foo.c -o foo.out
```
The flat binary may then be extracted from the `foo.out` ELF file.

For compiling a Leros program to assembly, execute:
```bash
clang -target leros32 -S foo.c -o foo.s
```

## Leros Versions and Compilers

### Initial Version

The initial version of Leros was designed as a 16-bit accumulator
machine and written in VHDL. Besides writing assembly programs
a Java JVM for microcontroller has been ported to support Leros.
Also a software simulator written in Java is available.

### Current Development Version

To provide a reasonable target for C programs, we will extend Leros
to 32 bits and rewrite the hardware description in Chisel.
We will try to make Leros to be configurable being 16 or 32 bits.
LLVM will be adapted for Leros32 and feedback from this compiler
backend may result in changes in the instruction set.
This may break the compatibility with the VHDL version of Leros
and the Java compiler.

We aim to provide enough documentation and simulators so that this
version can be used in the teaching of basic computer architecture.