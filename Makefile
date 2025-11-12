# Just remember commands, for now.

PROG ?= leros-asm/didactic_rt.s
BUILD_DIR ?= generated
MEM_TYPE ?= RtlSyncMemory
IMEM ?= 1024
DMEM ?= 1024

init:
	git submodule update --init --recursive

generate:
	sbt "runMain dtu.DtuSubsystem DidacticSram $(PROG) --target-dir $(BUILD_DIR)"

generate-caravel:
	sbt "runMain caravel.LerosCaravel $(MEM_TYPE) $(PROG) $(IMEM) $(DMEM) --target-dir $(BUILD_DIR)/caravel"

generate-caravel-top:
	sbt "runMain caravel.CaravelTop"

clean:
	rm -rf $(BUILD_DIR)
	$(MAKE) -C openlane clean
	$(MAKE) -C basys3 clean

test:
	sbt test
	cd leros; make init
	cd leros; sbt test
	cd hello-morse; sbt test


.PHONY: tools
tools:
	cd ponte-cli; cargo build --release
	cp ponte-cli/target/release/ponte-cli tools/