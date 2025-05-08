# Verilator simulation Makefile for DTU Subsystem

# Directories
GEN_DIR := ../generated
OBJ_DIR := ./obj_dir

# Verilator settings
VERILATOR_FLAGS := --cc -Wno-unused
TOP_MODULE := DtuSubsystem

# Main SystemVerilog file
TOP_FILE := $(GEN_DIR)/$(TOP_MODULE).sv

# C++ testbench file
TB_CPP := ./src/tb_simple.cpp

# Default target
all: verilate

verilate:
	@echo "Verilating $(TOP_FILE)"
	verilator --Mdir $(OBJ_DIR) --cc $(TOP_FILE) --exe $(TB_CPP) --trace

clean:
	@echo "Cleaning up"
	rm -rf $(OBJ_DIR)

test: verilate
	@echo "Running testbench"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk
	./$(OBJ_DIR)/V$(TOP_MODULE)

help:
	@echo "DTU Subsystem Verilator Simulation"
	@echo "=================================="
	@echo ""
	@echo "Available targets:"
	@echo "  make				Run Verilator and build the simulation"
	@echo "  make verilate			Verilate the design and build the simulation"
	@echo "  make test 			Run the testbench"
	@echo "  make clean			Clean up generated files"