# Just remember commands, for now.

generate:
	sbt run

test:
	sbt test
	cd leros; make init
	cd leros; sbt test
	cd hello-morse; sbt test
