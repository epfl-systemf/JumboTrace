import os
import shutil
import sys

from typing import List, Final
from termcolor import colored

examples_dir: Final[str] = "../examples"
plugin_dir: Final[str] = "../jumbotrace-plugin"
injected_dir: Final[str] = "../jumbotrace-injected"
injectedgen_dir: Final[str] = "../jumbotrace-injectedgen"

test_excluded_examples: Final[List[str]] = [
    "Chemistry",    # currently no support for tests based on whole projects
    "Counters",     # FIXME this one should work but does not
    "Exceptions",   # should fail
    "Casts"         # should fail
]


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


def compile_example(example_name: str, test_mode: bool):
    compile_plugin()
    compile_injected(test_mode)
    copy_injection_to_example(example_name)
    cmd(
        f"javac -g -cp {plugin_dir}/target/classes -Xplugin:JumboTrace {examples_dir}/{example_name}/*.java",
        f"compiling example {example_name} [instrumented version]"
    )


def compile_example_raw(example_name: str):
    cmd(
        f"javac -g {examples_dir}/{example_name}/*.java",
        f"compiling example {example_name} [non-instrumented version]"
    )


def run_example(example_name: str, main_class_name: str = "Main",
                stdout_target: str | None = None, stderr_target: str | None = None,
                test_mode: bool = False):
    compile_example(example_name, test_mode)
    command = f"java -cp {examples_dir}/{example_name} {main_class_name}"
    msg = f"running example {example_name} [instrumented] (mainclass=\'{main_class_name}\')"
    if stdout_target is not None:
        command += " > " + stdout_target
        msg += f" stdout redirected to {stdout_target}"
    if stderr_target is not None:
        command += " 2> " + stderr_target
        msg += f" stderr redirected to {stderr_target}"
    cmd(command, msg)


def run_example_raw(example_name: str, main_class_name: str = "Main",
                    stdout_target: str | None = None, stderr_target: str | None = None):
    compile_example_raw(example_name)
    command = f"java -cp {examples_dir}/{example_name} {main_class_name}"
    msg = f"running example {example_name} [not instrumented] (mainclass=\'{main_class_name}\')"
    if stdout_target is not None:
        command += " > " + stdout_target
        msg += f" stdout redirected to {stdout_target}"
    if stderr_target is not None:
        command += " 2> " + stderr_target
        msg += f" stderr redirected to {stderr_target}"
    cmd(command, msg)


def compile_example_project(example_name: str, test_mode: bool):
    all_java_files = find_all_java_files(f"{examples_dir}/{example_name}/src")
    compile_plugin()
    compile_injected(test_mode)
    copy_injection_to_example(f"{example_name}/target/classes")
    cmd(
        f"javac -g -cp {plugin_dir}/target/classes -Xplugin:JumboTrace -d {examples_dir}/{example_name}/target/classes {" ".join(all_java_files)}",
        f"compiling example {example_name} (using plugin)"
    )


def run_example_project(example_name: str, test_mode: bool = False):
    main_class, args = read_config_file(examples_dir + "/" + example_name)
    log(f"read main class from config file: {main_class}")
    log(f"read args from config file: {args}")
    compile_example_project(example_name, test_mode)
    cmd(
        f"java -cp {examples_dir}/{example_name}/target/classes {main_class} \"{args}\"",
        f"running example {example_name} [not instrumented]"
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


def run_injectedgen(test_mode: bool):
    compile_injectedgen()
    args = "___JumboTrace___.java"
    if test_mode:
        args = "#test " + args
    cmd(
        f"mvn -f {injectedgen_dir} -Dexec.args=\"{args}\" exec:java",
        "running injectedgen"
    )


def compile_injected(test_mode: bool):
    run_injectedgen(test_mode)
    cmd(
        f"mvn -f {injected_dir}/pom.xml clean:clean compiler:compile",
        f"compiling injected code (test mode {"enabled" if test_mode else "disabled"})"
    )


def copy_injection_to_example(example_name: str):
    example_dir = f"{examples_dir}/{example_name}"
    src = f"{injected_dir}/target/classes/com"
    dst = f"{examples_dir}/{example_dir}/com"
    log(f"copying {src} -> {dst}")
    shutil.copytree(src=src, dst=dst, dirs_exist_ok=True)


def run_tests():
    log("Running tests")
    examples = os.listdir(examples_dir)
    examples = [ex for ex in examples if ex not in test_excluded_examples]
    n_tests = len(examples)
    log("Example programs found: " + ", ".join(examples))
    for test_idx, example in enumerate(examples):
        log(f"TEST {example} STARTS (test {test_idx+1} out of {n_tests})")
        raw_stdout_target = f"{examples_dir}/{example}/test-raw-stdout.testout"
        raw_stderr_target = f"{examples_dir}/{example}/test-raw-stderr.testout"
        instr_stdout_target = f"{examples_dir}/{example}/test-instr-stdout.testout"
        instr_stderr_target = f"{examples_dir}/{example}/test-instr-stderr.testout"
        run_example_raw(example, stdout_target=raw_stdout_target, stderr_target=raw_stderr_target)
        run_example(example, stdout_target=instr_stdout_target, stderr_target=instr_stderr_target, test_mode=True)
        assert_files_equal(raw_stdout_target, instr_stdout_target)
        assert_files_equal(raw_stderr_target, instr_stderr_target)
        log(f"TEST {example} PASSED (test {test_idx+1} out of {n_tests})")
    log("Finished running tests: " + ", ".join(examples))
    log("Tests finished: no error detected")


def assert_files_equal(raw_out, instr_out):
    with open(raw_out, mode="r") as raw, open(instr_out, mode="r") as instr:
        raw_lines = raw.readlines()
        instr_lines = instr.readlines()
        for line_idx in range(min(len(raw_lines), len(instr_lines))):
            assert raw_lines[line_idx] == instr_lines[line_idx], f"outputs differ starting at line {line_idx}"
        assert len(raw_lines) == len(instr_lines), "output lengths differ"


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
            compile_example(ex, test_mode=False)
        case ["run", "example", ex]:
            run_example(ex)
        case ["run", "example", ex, maincl]:
            run_example(ex, maincl)
        case ["compile", "exproj", ex]:
            compile_example_project(ex, test_mode=False)
        case ["run", "exproj", ex]:
            run_example_project(ex)
        case ["compile", "plugin"]:
            compile_plugin()
        case ["compile", "injectedgen"]:
            compile_injectedgen()
        case ["run", "injectedgen"]:
            run_injectedgen(test_mode=False)
        case ["compile", "injected"]:
            compile_injected(test_mode=False)
        case ["test"]:
            run_tests()
        case _:
            print("unknown command: " + " ".join(args), file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1:])
