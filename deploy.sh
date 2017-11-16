function snapshot() {
  sbt publish
}

function release() {
  sbt -Drevision=$REVISION publish
}