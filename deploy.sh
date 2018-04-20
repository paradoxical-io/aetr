function snapshot() {
  sbt publishSigned
}

function release() {
  sbt -Dversion=$REVISION publishSigned sonatypeRelease docker dockerPush
}