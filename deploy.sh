function snapshot() {
  sbt publishSigned
}

function release() {
  sbt -Drevision=$REVISION publishSigned sonatypeRelease
}