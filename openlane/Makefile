GEN_DIR := generated/openlane
PROG := leros-asm/didactic_rt.s

gen:
	cd .. && sbt "runMain dtu.DtuSubsystem OpenRamSky130 $(PROG) --target-dir $(GEN_DIR)"

harden-docker:
	cd .. && python3 -m openlane --dockerized --run-tag dtu openlane/config.yaml

harden:
	cd .. && python3 -m openlane --run-tag dtu openlane/config.yaml

librelane:
	cd .. && librelane openlane/config.yaml

view:
	cd .. && librelane --last-run --flow openinklayout openlane/config.yaml

clean:
	rm -rf $(GEN_DIR)
	rm -rf runs
