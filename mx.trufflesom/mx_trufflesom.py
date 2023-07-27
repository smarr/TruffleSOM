import os
import sys
from argparse import ArgumentParser

import mx


suite = mx.suite("trufflesom")
LABS_JDK_ID = suite.suiteDict["libraries"]["LABS_JDK"]["id"]


def ensure_core_lib_is_available():
    if not os.path.exists("core-lib/.git"):
        git = mx.GitConfig()
        git.run(["git", "submodule", "update", "--init", "--recursive"])


def ensure_labsjdk():
    if not os.path.exists("libs/jvmci"):
        mx.run_mx(
            [
                "--quiet",
                "fetch-jdk",
                "--strip-contents-home",
                "--jdk-id",
                LABS_JDK_ID,
                "--alias",
                suite.dir + "/libs/jvmci",
            ]
        )


bn_parser = ArgumentParser(
    prog="mx build-native", description="Build TruffleSOM native images"
)


bn_parser.add_argument(
    "-bt",
    "--build-trufflesom",
    action="store_true",
    dest="build_trufflesom",
    help="build trufflesom first.",
    default=False,
)
bn_parser.add_argument(
    "-bn",
    "--build-native-image-tool",
    action="store_true",
    dest="build_native_image_tool",
    help="build the native-image tool first.",
    default=False,
)
bn_parser.add_argument(
    "-g",
    "--graalvm",
    action="store",
    dest="graalvm",
    default=None,
    help="path to GraalVM distribution",
    metavar="<path>",
)

bn_parser.add_argument(
    "-t",
    "--type",
    action="store",
    dest="type",
    default="AST",
    metavar="<type>",
    help="interpreter type, one of AST, BC. Default=AST",
)
bn_parser.add_argument(
    "-d",
    "--with-debugger",
    action="store_true",
    dest="with_debugger",
    help="wait for Java debugger to attach.",
    default=False,
)
bn_parser.add_argument(
    "-J",
    "--no-jit",
    action="store_true",
    dest="without_jit",
    default=False,
    help="disables the JIT compiler, creating an interpreter-only binary.",
)

bn_parser.add_argument(
    "-m",
    "--dump-method",
    action="store",
    dest="method_filter",
    default=None,
    metavar="<method-filter>",
    help="dump compiler graphs for the selected methods. For the syntax "
    + "see https://github.com/oracle/graal/blob/master/compiler/"
    + "src/org.graalvm.compiler.debug/src/org/graalvm/compiler/"
    + "debug/doc-files/MethodFilterHelp.txt",
)


def get_svm_path():
    truffle_suite = mx.suite("truffle", fatalIfMissing=False)
    return truffle_suite.dir.replace("/truffle", "/substratevm")


def get_output_name(opt):
    output_name = "/som-native"

    if opt.without_jit:
        output_name += "-interp"

    if opt.type == "AST":
        output_name += "-ast"
    elif opt.type == "BC":
        output_name += "-bc"
    else:
        print(
            "Unknown interpreter type selected %s. Instead choose one of: AST, BC"
            % opt.type
        )
        sys.exit(1)

    return output_name


@mx.command(
    suite.name,
    "build-native",
    usage_msg="[options]",
    doc_function=bn_parser.print_help,
)
def build_native(args, **kwargs):
    """build TruffleSOM native images"""
    ensure_labsjdk()

    opt = bn_parser.parse_args(args)
    output_name = get_output_name(opt)

    svm_path = get_svm_path()

    if opt.build_trufflesom:
        mx.run_mx(["build"], suite)
    if opt.build_native_image_tool:
        mx.run_mx(["build"], svm_path)

    if opt.graalvm:
        cmd = [opt.graalvm + "/bin/native-image"]
        output_name += "-ee"
    else:
        cmd = ["native-image"]

    if opt.with_debugger:
        cmd += [
            "-J-Xdebug",
            "-J-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000",
        ]

    cmd += [
        "--macro:truffle",
        "--no-fallback",
        "--allow-incomplete-classpath",
        "-H:+ReportExceptionStackTraces",
        "-H:-DeleteLocalSymbols",
        "-Dsom.interp=" + opt.type,
    ]

    if opt.method_filter:
        cmd += [
            "--initialize-at-build-time=bd,tools,trufflesom,org.graalvm.graphio",
            "-H:Dump=",
            "-H:PrintGraph=File",
            "-H:MethodFilter=" + opt.method_filter,
        ]
    else:
        cmd += ["--initialize-at-build-time=bd,tools,trufflesom"]

    if opt.without_jit:
        cmd += ["-Dsom.jitCompiler=false"]

    # -H:+PrintAnalysisCallTree
    # -H:+PrintRuntimeCompileMethods
    # -H:+PrintMethodHistogram
    # -H:+RuntimeAssertions
    # -H:+EnforceMaxRuntimeCompileMethods

    cmd += [
        "-cp",
        suite.dir + "/mxbuild/dists/trufflesom.jar",
        "-o",
        suite.dir + output_name,
    ]

    if opt.graalvm:
        cmd += ["--gc=G1"]

    cmd += ["trufflesom.vm.Universe"]

    mx.run_mx(cmd, svm_path)


@mx.command(suite.name, "build-native-obj-test")
def build_native_obj_test(args, **kwargs):
    """build native object storage test image"""
    ensure_labsjdk()
    svm_path = get_svm_path()
    cmd = [
        "native-image",
        "--macro:truffle",
        "--no-fallback",
        "--initialize-at-build-time",
        "-H:+ReportExceptionStackTraces",
        "-cp",
        suite.dir
        + "/mxbuild/dists/trufflesom.jar:"
        + suite.dir
        + "/mxbuild/dists/trufflesom-test.jar",
        "-o",
        suite.dir + "/som-obj-storage-tester",
        "trufflesom.intepreter.objectstorage.BasicStorageTester",
    ]
    mx.run_mx(cmd, svm_path)


ensure_core_lib_is_available()
