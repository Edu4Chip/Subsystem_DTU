# Just remember commands, for now.

init:
	git submodule update --init --recursive

generate:
	sbt run

test:
	sbt test
	cd leros; make init
	cd leros; sbt test
	cd hello-morse; sbt test
