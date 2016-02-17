# Rust implementation of Tracker Commons

Rust is a hybrid high-level/low-level language with advanced compile-time checking to ensure an absence of the kinds of errors that usually plague traditional low-level languages such as C.

## Implementation notes

Rust is reported to have an extremely fast parser combinator library, [`nom`](https://github.com/Geal/nom), which should mean that parsing can be extremely fast without having to push characters around by hand.

It also has built-in JSON support in its [`rustc-serialize` library](https://crates.io/crates/rustc-serialize). May wish to test whether hand-rolled JSON support is faster or slower than a nom parser. Also, need to test whether `rustc-serialize` will do sane things with floating-point values (i.e. produce JSON, not something that looks like JSON but has junk in it).

Rust also has a workable Foreign Function Interface (FFI) system to generate C bindings.  Thus, the Rust implementation could be the back-end for other implementations (e.g. R) that may be slower.  It could also be the back-end to a C or C++ implementation, but would provide stronger guarantees about safety than hand-written C.
