suite = {
    "name": "trufflesom",
    "mxversion": "6.18.0",
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
                "version": "888e43559fc6975c00d3aa7278093d941cb52479",
                "urls": [{"url": "https://github.com/oracle/graal", "kind": "git"}],
            },
        ]
    },
    "libraries": {
        "CHECKSTYLE_10.9.3": {
            "urls": [
                "https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.9.3/checkstyle-10.9.3-all.jar"
            ],
            "sha1": "5bd65d2d7e0eefe73782c072db2066f6832d5b43",
            "licence": "LGPLv21",
        },
        "LABS_JDK": {
            "id": "labsjdk-ce-21",
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
            "checkstyleVersion": "10.9.3",
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
                "requires": ["jdk.unsupported"],  # sun.misc.Unsafe
                "requiresConcealed": {
                    "org.graalvm.truffle": [
                        "com.oracle.truffle.api",
                        "com.oracle.truffle.api.instrumentation",
                    ],
                },
            },
            "dependencies": ["trufflesom"],
            "distDependencies": ["truffle:TRUFFLE_API"],
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
