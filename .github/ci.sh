#!/bin/bash

build_with_libgraal() {
  ./som --setup labsjdk
  mx sforceimport
  mx --env libgraal build
}

ensure_core_lib_and_its_remotes() {
  (cd core-lib && git remote add smarr https://github.com/smarr/SOM.git 2>/dev/null || true; git fetch --all)
  git submodule update --init
}

ensure_mx_on_path() {
  PATH=${PATH}:$(pwd)/../mx:/opt/local/bin
  export PATH
}

ensure_labsjdk_and_graal_version() {
  ./som --setup labsjdk
  mx sforceimport
  rm libs/jvmci || true
  ./som --setup labsjdk
  (cd ../graal && mx --root-suites clean --aggressive --all || true)
}

reown_graal() {
  if [ -d "../graal/compiler/mxbuild" ]; then
    (cd ../graal/compiler/mxbuild && sudo reown-project.sh) || true
  fi
}

reset_graal_to_imported_version() {
  if [ -d "../graal" ]; then
    COMMIT_ID=$(./som --setup truffle-commit-id)
    (cd ../graal && git fetch --all && git reset --hard "$COMMIT_ID") || true
  fi
}

install_dependencies() {
  ~/.asdf/bin/asdf install awfy "$GRAALEE_VERSION"
  ~/.asdf/bin/asdf install java "$JAVA_VERSION"
  GRAALEE_HOME="$HOME/.asdf/installs/awfy/$GRAALEE_VERSION"
  export GRAALEE_HOME

  ECLIPSE_EXE="$HOME/.local/eclipse/eclipse"
  export ECLIPSE_EXE

  JAVA_HOME="$HOME/.asdf/installs/java/$JAVA_VERSION"
  export JAVA_HOME

  if [ -d "$GRAALEE_HOME/Contents/Home" ]; then
    GRAALEE_HOME="$GRAALEE_HOME/Contents/Home"
    export GRAALEE_HOME
  fi
}

compress_and_upload() {
  lz4 "$1" "$1.lz4"
  sftp tmp-artifacts << EOF
    -mkdir incoming/${CI_PIPELINE_ID}/
    put $1.lz4 incoming/${CI_PIPELINE_ID}/
EOF
}

download_and_decompress() {
  sftp "tmp-artifacts:incoming/${CI_PIPELINE_ID}/$1.lz4"
  lz4 -d "$1.lz4" "$1"
}
