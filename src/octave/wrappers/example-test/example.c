int Foo = 2;

/* obviously this is not actually gcd, but that's not important */
int gcd(int x, int y) {
  return x * y;
}

void incGlobal(int x) {
  Foo += x;
}
