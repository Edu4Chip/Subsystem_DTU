# Just remember commands, for now.

PROG ?= leros-asm/didactic_rt.s
BUILD_DIR ?= generated

init:
	git submodule update --init --recursive

generate:
	sbt "runMain dtu.DtuSubsystem didacticSram $(PROG) --target-dir $(BUILD_DIR)"

caravel:
	sbt "runMain caravel.DtuSubsystemCaravel $(PROG) --target-dir $(BUILD_DIR)"

generate-caravel:
	sbt "runMain caravel.LerosCaravel registerRam $(PROG) --target-dir $(BUILD_DIR)/caravel"

clean:
	rm -rf $(BUILD_DIR)
	$(MAKE) -C openlane clean
	$(MAKE) -C basys3 clean

test:
	sbt test
	cd leros; make init
	cd leros; sbt test
	cd hello-morse; sbt test
