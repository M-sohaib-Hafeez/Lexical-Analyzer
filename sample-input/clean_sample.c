/* A program with no lexical errors at all. */

int gcd(int a, int b) {
    while (b != 0) {
        int t = b;
        b = a % b;
        a = t;
    }
    return a;
}

int main() {
    char sep = '\t';
    char *msg = "gcd(48, 18) = %d\n";
    int result = gcd(48, 18);
    return 0;
}
