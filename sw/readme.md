# Leros Software Development

This folder contains scripts and makefiles for building executables for the Leros processor architecture. The toolchain leverages the LLVM infrastructure customized for Leros.

## LLVM Toolchain for Leros

The Leros LLVM toolchain provides a complete development environment for compiling C code to Leros machine code. The toolchain includes:

- `clang`: C/C++ compiler frontend
- `llc`: LLVM static compiler (IR to machine code)
- `lld`: Linker
- `llvm-objdump`: Disassembler
- `llvm-objcopy`: Binary utilities

## Building Programs

You can use the provided Makefile to build programs for Leros:

```bash
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

## Memory Layout

The default memory layout places code at address 0x0 and initializes stack and other segments according to the Leros ABI. Custom memory layouts can be specified using linker scripts.

## Examples

Example programs are located in the `src` directory. These demonstrate basic functionality and Leros-specific features.

For detailed information about the Leros architecture and instruction set, see the [docs.md](docs.md) file.
