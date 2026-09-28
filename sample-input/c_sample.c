/* CS-3205 Phase 1 demo source
   this comment intentionally spans two lines */

int main() {
    int _count = 42;
    char grade = 'A';
    char newline = '\n';
    char *name = "Sohaib \"the\" student\n";

    if (_count >= 10) {
        _count = _count + 1;
    }

    return 0;
}

/* ---- everything below intentionally triggers one of the required errors ---- */

int bad_char = 'XY';
char *unterminated_string = "this string never closes on this line
int weird = 5 ~ 3;
char nothing = '';

/* this final comment is left unterminated on purpose
   so you can see the "Unterminated comment" error too
