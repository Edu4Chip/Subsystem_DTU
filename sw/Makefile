# Makefile for building Leros programs
# Uses the Leros LLVM toolchain for compilation

# Directory structure
SRC_DIR = src
BUILD_DIR = build
BIN_DIR = bin

# Toolchain commands
# Note: Ensure Leros LLVM is in path or use absolute paths
CC = clang
LD = ld.lld
OBJCOPY = llvm-objcopy
OBJDUMP = llvm-objdump

# Flags
CFLAGS = -target leros32 -O2
LDFLAGS = -Wl,-T,leros.ld

# Default program to build
PROG ?= fibonacci_sequence

# List of all source files
SRCS = $(wildcard $(SRC_DIR)/*.c)
PROGS = $(basename $(notdir $(SRCS)))

# Rule to make all example programs
all: $(PROGS)

# Create directories
$(BUILD_DIR) $(BIN_DIR):
	mkdir -p $@

# Pattern rule for building any program
%: $(SRC_DIR)/%.c | $(BUILD_DIR) $(BIN_DIR)
	@echo "Building $@..."
	$(CC) $(CFLAGS) $< -o $(BUILD_DIR)/$@.out $(LDFLAGS)
	$(OBJCOPY) $(BUILD_DIR)/$@.out -O binary $(BIN_DIR)/$@.bin
	$(OBJDUMP) -d $(BUILD_DIR)/$@.out > $(BUILD_DIR)/$@.dis

# Generate disassembly
disassemble: $(BUILD_DIR)/$(PROG).out
	$(OBJDUMP) -d $< > $(BUILD_DIR)/$(PROG).dis

# Clean build artifacts
clean:
	rm -rf $(BUILD_DIR) $(BIN_DIR)

# Create src directory if it doesn't exist
$(SRC_DIR):
	mkdir -p $@
	@echo "Created source directory. Add your .c files here."

# Phony targets
.PHONY: all clean disassemble list new

# Default linker script
leros.ld:
	@echo "ENTRY(_start)" > $@
	@echo "" >> $@
	@echo "SECTIONS" >> $@
	@echo "{" >> $@
	@echo "    . = 0x0;" >> $@
	@echo "    .text : { *(.text) }" >> $@
	@echo "    .data : { *(.data) }" >> $@
	@echo "    .bss : { *(.bss) }" >> $@
	@echo "}" >> $@

# Help target
help:
	@echo "Leros Build System"
	@echo "==================="
	@echo ""
	@echo "Available targets:"
	@echo "  make PROG=name    Build specific program"
	@echo "  make all          Build all programs"
	@echo "  make clean        Remove build artifacts"
	@echo "  make disassemble   Disassemble the program"
	@echo "  make help         Show this help message"
	@echo ""
	@echo "Output files are placed in:"
	@echo "  - $(BUILD_DIR)/ - ELF files and disassembly"
	@echo "  - $(BIN_DIR)/   - Binary files for loading into Leros"
