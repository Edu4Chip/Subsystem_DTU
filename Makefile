# Just remember commands, for now.

ROM ?= base.s
BUILD_DIR ?= generated

init:
	git submodule update --init --recursive

generate:
	sbt "runMain dtu.DtuSubsystem --target-dir $(BUILD_DIR)"

clean:
	rm -rf $(BUILD_DIR)

test:
	sbt test
	cd leros; make init
	cd leros; sbt test
	cd hello-morse; sbt test
