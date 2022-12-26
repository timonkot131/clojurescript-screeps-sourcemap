[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.timonkot13/clojurescript-screeps-sourcemap.svg)](https://clojars.org/org.clojars.timonkot13/clojurescript-screeps-sourcemap)
# Requirements
Make sure that you have `"source-map": "^0.6.1"` in your npm's dependencies.
# How to use
Pass your source-map module into an `enable` function. The module format must look like this: `module.exports = {"version": ...}`, in order to be accepted by the screeps.
