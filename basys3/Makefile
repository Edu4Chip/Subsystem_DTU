PROG?=leros-asm/didactic_rt.s
DEV=/dev/ttyUSB3

sources = $(wildcard ../generated/basys3/*.sv)

all: gen synth prog

gen:
	cd .. && sbt "runMain basys3.DtuSubsystemBasys3 $(PROG) --target-dir generated/basys3"

synth:
	./vivado_synth \
	  -p xc7a35tcpg236-1 \
		-x pins.xdc \
		-t DtuSubsystemBasys3 \
		-o ../build \
		${sources} 

prog:
	openocd -f basys3_prog.f

upload:
	cd .. && sbt "runMain basys3.ProgramDtuSubsystemBasys3 $(DEV) $(PROG)"

clean:
	rm -rf ../generated/basys3
	rm -rf ../build