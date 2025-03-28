PROG?=leros/asm/didactic_rt.s

gen:
	cd .. && sbt "runMain basys3.DtuSubsystemBasys3 leros/asm/didactic_rt.s --target-dir generated"

synth:
	./vivado_synth \
	  -p xc7a35tcpg236-1 \
		-x pins.xdc \
		-t DtuSubsystemBasys3 \
		-o ../build \
		../generated/DtuSubsystemBasys3.sv

prog:
	openocd -f basys3_prog.f

upload:
	cd .. && sbt "runMain basys3.ProgramDtuSubsystemBasys3 $(PROG)"