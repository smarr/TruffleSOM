suite = {
    "name": "trufflesom",
    "mxversion": "7.36.3",
    "versionConflictResolution": "latest",
    "version": "0.0.1",
    "release": False,
    "groupId": "trufflesom",
    "url": "https://github.com/SOM-st/TruffleSOM",
    "scm": {
        "url": "https://github.com/SOM-st/TruffleSOM",
        "read": "https://github.com/SOM-st/TruffleSOM.git",
        "write": "git@github.com:SOM-st/TruffleSOM.git",
    },
    "imports": {
        "suites": [
            {
                "name": "truffle",
                "subdir": True,
                "version": "8a6fac2143cdd80a060d85dea06409bc2e6c5ec6",
                "urls": [{"url": "https://github.com/oracle/graal", "kind": "git"}],
            },
        ]
    },
    "libraries": {
        "CHECKSTYLE_10.21.0": {
            "urls": [
                "https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.21.0/checkstyle-10.21.0-all.jar"
            ],
            "digest": "sha512:401940e1475a333afee636535708fa842b1a11b30f9fd43518589aaf94c2cf601b24f83176e95ffc171e2befe968267262b24a0a3931a009b39531a6fe570e60",
            "licence": "LGPLv21",
        },
        "LABS_JDK": {
            "id": "labsjdk-ce-latest",
            # I am just using the suite.py to store the info
            # so but manage it in mx_trufflesom.py
            "path": ".",
        },
    },
    "projects": {
        "trufflesom": {
            "subDir": "src",
            "sourceDirs": ["src"],
            "dependencies": ["truffle:TRUFFLE_API"],
            "requires": [
                "java.logging",
                "java.management",
                "jdk.management",
                "jdk.unsupported",  # sun.misc.Unsafe
            ],
            "requiresConcealed": {
                "java.base": ["jdk.internal.module"],
            },
            "checkstyleVersion": "10.21.0",
            "jacoco": "include",
            "javaCompliance": "17+",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "TruffleSOM",
        },
        "tests": {
            "dir": ".",
            "sourceDirs": ["tests"],
            "requires": [
                "java.logging",
            ],
            "dependencies": ["truffle:TRUFFLE_API", "TRUFFLESOM", "mx:JUNIT"],
            "checkstyle": "trufflesom",
            "jacoco": "include",
            "javaCompliance": "17+",
            "workingSets": "TruffleSOM",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "testProject": True,
        },
    },
    "distributions": {
        "TRUFFLESOM": {
            "description": "TruffleSOM",
            "moduleInfo": {
                "name": "trufflesom",
                "exports": [
                    "trufflesom.* to org.graalvm.truffle",
                ],
                "requires": [
                    "jdk.unsupported",
                    "org.graalvm.collections",
                    "org.graalvm.polyglot",
                ],
            },
            "dependencies": ["trufflesom"],
            "distDependencies": [
                "truffle:TRUFFLE_API"
            ],  # , "tools:TRUFFLE_COVERAGE", "tools:TRUFFLE_PROFILER"
        },
        "TRUFFLESOM_TEST": {
            "description": "TruffleSOM JUnit Tests",
            "javaCompliance": "17+",
            "dependencies": ["tests"],
            "exclude": ["mx:JUNIT", "mx:HAMCREST"],
            "distDependencies": ["TRUFFLESOM", "truffle:TRUFFLE_TEST"],
            "testDistribution": True,
        },
    },
}
