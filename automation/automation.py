import os
import shutil
import sys

from typing import List, Final
from termcolor import colored

examples_dir: Final[str] = "../examples"
plugin_dir: Final[str] = "../jumbotrace-plugin"
injected_dir: Final[str] = "../jumbotrace-injected"
injectedgen_dir: Final[str] = "../jumbotrace-injectedgen"


def log(msg: str):
    print(colored("[automation] " + msg, "magenta"))


def cmd(command: str, msg: str):
    log(msg)
    ret_code = os.system(command)
    if ret_code != 0:
        print(colored(" !!! AUTOMATION: FAILED !!!", "red"), file=sys.stderr)
        exit(ret_code)


def compile_example(example_name: str):
    compile_plugin()
    compile_injected()
    copy_injection_to_example(example_name)
    cmd(
        f"javac -g -cp {plugin_dir}/target/classes -Xplugin:JumboTrace {examples_dir}/{example_name}/*.java",
        f"compiling example {example_name}"
    )


def run_example(example_name: str, main_class_name: str = "Main"):
    compile_example(example_name)
    cmd(
        f"java -cp {examples_dir}/{example_name} {main_class_name}",
        f"running example {example_name} (mainclass=\'{main_class_name}\')"
    )


def compile_plugin():
    cmd(
        f"mvn -f {plugin_dir}/pom.xml clean:clean compiler:compile resources:copy-resources",
        "compiling javac plugin"
    )


def compile_injectedgen():
    cmd(
        f"mvn -f {injectedgen_dir}/pom.xml clean:clean compiler:compile",
        "compiling injectedgen"
    )


def run_injectedgen():
    compile_injectedgen()
    cmd(
        f"mvn -f {injectedgen_dir} exec:java",
        "running injectedgen"
    )


def compile_injected():
    run_injectedgen()
    cmd(
        f"mvn -f {injected_dir}/pom.xml clean:clean compiler:compile",
        "compiling injected code"
    )


def copy_injection_to_example(example_name: str):
    example_dir = f"{examples_dir}/{example_name}"
    src = f"{injected_dir}/target/classes/com"
    dst = f"{examples_dir}/{example_dir}/com"
    log(f"copying {src} -> {dst}")
    shutil.copytree(src=src, dst=dst, dirs_exist_ok=True)


def main(args: List[str]):
    if len(args) == 0:
        print("No argument given to automation script. Exiting", file=sys.stderr)
        exit(0)
    match args:
        case ["compile", "example", ex]:
            compile_example(ex)
        case ["run", "example", ex]:
            run_example(ex)
        case ["run", "example", ex, maincl]:
            run_example(ex, maincl)
        case ["compile", "plugin"]:
            compile_plugin()
        case ["compile", "injectedgen"]:
            compile_injectedgen()
        case ["run", "injectedgen"]:
            run_injectedgen()
        case ["compile", "injected"]:
            compile_injected()
        case _:
            print("unknown command: " + " ".join(args), file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1:])
