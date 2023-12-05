import os
import shutil
import sys

from typing import List, Final
from termcolor import colored

examples_dir: Final[str] = "../examples"
plugin_dir: Final[str] = "../jumbotrace-plugin"
injected_dir: Final[str] = "../jumbotrace-injected"
injectedgen_dir: Final[str] = "../jumbotrace-injectedgen"


def log(colored_str: str, uncolored_str: str = ""):
    print(colored("[automation] " + colored_str, "magenta") + uncolored_str)


def limit_str_length(s: str, max_len: int) -> str:
    if len(s) <= max_len:
        return s
    else:
        return s[:max_len] + "..."


def cmd(command: str, msg: str):
    log(f"{msg} > ", limit_str_length(command, 150))
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


def compile_example_project(example_name: str):
    all_java_files = find_all_java_files(f"{examples_dir}/{example_name}/src")
    compile_plugin()
    compile_injected()
    copy_injection_to_example(f"{example_name}/target/classes")
    cmd(
        f"javac -g -cp {plugin_dir}/target/classes -Xplugin:JumboTrace -d {examples_dir}/{example_name}/target/classes {" ".join(all_java_files)}",
        f"compiling example {example_name}"
    )


def run_example_project(example_name: str):
    main_class, args = read_config_file(examples_dir + "/" + example_name)
    log(f"read main class from config file: {main_class}")
    log(f"read args from config file: {args}")
    compile_example_project(example_name)
    cmd(
        f"java -cp {examples_dir}/{example_name}/target/classes {main_class} \"{args}\"",
        f"running example {example_name}"
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


def read_config_file(directory: str):
    with open(f"{directory}/jumbotrace.config", mode="r") as f:
        lines = f.read().split('\n')
        lines = [line.strip() for line in lines if len(line.strip()) > 0]
        assert len(lines) == 2
        k1, v1 = lines[0].split("=", maxsplit=1)
        k2, v2 = lines[1].split("=", maxsplit=1)
        assert k1 == "mainclass"
        assert k2 == "args"
        return v1, v2


def find_all_java_files(directory: str) -> List[str]:
    java_files = []
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                java_files.append(root + "/" + file)
    return java_files


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
        case ["compile", "exproj", ex]:
            compile_example_project(ex)
        case ["run", "exproj", ex]:
            run_example_project(ex)
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
