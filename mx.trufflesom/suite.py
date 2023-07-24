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
        "suites": [{
            "name": "truffle",
            "subdir": True,
            "version": "ef253f24988ea66c63fe92cda4a8a555bc02fe38",
            "urls": [{
                "url": "https://github.com/oracle/graal",
                "kind": "git"
            }],
        }]
    },
    "libraries": {
        "CHECKSTYLE_10.9.3" : {
            "urls" : [
                "https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.9.3/checkstyle-10.9.3-all.jar"
            ],
            "sha1": "5bd65d2d7e0eefe73782c072db2066f6832d5b43",
            "licence" : "LGPLv21"
        },
    },
    "projects": {
        "trufflesom": {
            "subDir": "src",
            "sourceDirs": [".", "../bdt", "../tools"],
            "dependencies": [
                # "TRUFFLESQUEAK_SHARED",
                # "BOUNCY_CASTLE_CRYPTO_LIB",
                "truffle:TRUFFLE_API"
                # "truffle:TRUFFLE_NFI",
            ],
            "requires": [
                # "java.datatransfer",
                # "java.desktop",
                "java.logging",
                "java.management",
                "jdk.management",
                "jdk.unsupported" # sun.misc.Unsafe
            ],
            "requiresConcealed" : {
                "java.base": ["jdk.internal.module"],
            },
            "checkstyleVersion": "10.9.3",
            "jacoco": "include",
            "javaCompliance": "17+",
            "annotationProcessors": ["truffle:TRUFFLE_DSL_PROCESSOR"],
            "workingSets": "TruffleSOM",
        },
        # "bdt": {
#              "subDir": "src",
#              "sourceDirs": ["."],
#              "dependencies": [
#                  "truffle:TRUFFLE_API"
#                  # "sdk:GRAAL_SDK",
#              ],
#              "checkstyle": "trufflesom",
#              "jacoco": "include",
#              "javaCompliance": "17+",
#              "workingSets": "TruffleSOM",
#          },
#          "tools": {
#               "subDir": "src",
#               "sourceDirs": ["."],
#               "dependencies": [
#                   "truffle:TRUFFLE_API"
#                   # "sdk:GRAAL_SDK",
#               ],
#               "checkstyle": "trufflesom",
#               "jacoco": "include",
#               "javaCompliance": "17+",
#               "workingSets": "TruffleSOM",
#           },
        "tests": {
            "subDir": "tests",
            "sourceDirs": ["."],
            "dependencies": [
                "trufflesom",
                "mx:JUNIT"
            ],
            "checkstyle": "trufflesom",
            "jacoco": "include",
            "javaCompliance": "17+",
            "workingSets": "TruffleSOM",
            "testProject": True,
        },
    },
    "distributions": {
         "TRUFFLESOM": {
             "description": "TruffleSOM",
             "moduleInfo": {
                 "name": "trufflesom",
                 "exports": [
                     "trufflesom to org.graalvm.truffle",
                 ],
                 "requires": [
                     "jdk.unsupported" # sun.misc.Unsafe
                 ],
                 "requiresConcealed": {
                     "org.graalvm.truffle": [
                         "com.oracle.truffle.api",
                         "com.oracle.truffle.api.instrumentation",
                     ],
                 },
             },
             "dependencies": [
                 "trufflesom",
             ],
             "distDependencies": [
                 # "TRUFFLESQUEAK_SHARED",
                 "truffle:TRUFFLE_API",
                 # "truffle:TRUFFLE_NFI",
             ],
             # "javaProperties": {
             #     "org.graalvm.language.smalltalk.home": "<path:TRUFFLESQUEAK_HOME>",
             # },
         },
         # "TRUFFLESOM_LIBS": {
#              "description": "In-repo libraries ",
#              "moduleInfo": {
#                  "name": "de.hpi.swa.trufflesqueak.shared",
#              },
#              "dependencies": [
#                  "de.hpi.swa.trufflesqueak.shared",
#              ],
#              "distDependencies": [
#                  "sdk:GRAAL_SDK",
#              ],
#          },
     }
}