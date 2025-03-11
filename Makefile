# Just remember commands, for now.

PROG_DIR ?= leros/asm/test
ROM ?= base.s
APB_BASE_ADDR ?= 01050000
BUILD_DIR ?= generated

init:
	git submodule update --init --recursive

generate:
	sbt "runMain dtu.DtuSubsystem"

clean:
	rm -rf $(BUILD_DIR)/*.sv 
	rm -rf $(BUILD_DIR)/*.json 
	rm -rf $(BUILD_DIR)/*.f 
	rm -rf $(BUILD_DIR)/*.fir 

test:
	sbt test
	cd leros; make init
	cd leros; sbt test
	cd hello-morse; sbt test
