all: compile copy

compile:
	cpp -P ___JumboTracer___raw.java -o ___JumboTracer___.java
	javac -g ___JumboTracer___.java

copy:
	python copy_to_examples.py
