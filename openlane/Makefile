GEN_DIR := generated/openlane
PROG := leros-asm/didactic_rt.s

gen:
	cd .. && sbt "runMain dtu.DtuSubsystem sky130Sram $(PROG) --target-dir $(GEN_DIR)"

harden-docker:
	cd .. && python3 -m openlane --dockerized --run-tag dtu openlane/config.yaml

harden:
	cd .. && python3 -m openlane --run-tag dtu openlane/config.yaml

clean:
	rm -rf $(GEN_DIR)
	rm -rf runs