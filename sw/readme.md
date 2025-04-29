# Leros Software Development

This folder contains scripts and makefiles for building executables for the Leros processor architecture. The toolchain leverages the LLVM infrastructure customized for Leros.

***Important Note:*** The Leros LLVM toolchain is not compatible with the standard LLVM toolchain. It is specifically 
designed for the Leros architecture and should not be used interchangeably with other LLVM-based projects. 
As for now, 2025-04-29, the only systems who support this are x86 based systems.

## LLVM Toolchain for Leros

The Leros LLVM toolchain provides a complete development environment for compiling C code to Leros machine code. The toolchain includes:

- `clang`: C/C++ compiler frontend
- `llc`: LLVM static compiler (IR to machine code)
- `lld`: Linker
- `llvm-objdump`: Disassembler
- `llvm-objcopy`: Binary utilities

*Note: this assumes that Leros unique LLVM binaries are your system default*

## Building Programs

You can use the provided Makefile to build programs for Leros:

```zsh
# Compile a specific program (will search in src directory)
make PROG=hello

# Build all example programs
make all

# Clean build artifacts
make clean
```

## Output Formats

The build process generates several files:
- `.o`: Object files
- `.out`: ELF executable
- `.bin`: Raw binary (for loading into Leros memory)
- `.dis`: Disassembly listing (for debugging)

## Examples

Fibonacci sequence example with the already compiled binaries, code and dump are located in the `src`, `build` and `bin` directory.

For detailed information about the Leros architecture and instruction set, see the [documentation website](https://leros-dev.github.io/).
