#@namespace typescript _at_guardian.contententity.common

const i32 A_BIG_NUMBER = 1234;    // 1
const map<string,string> A_COMPLEX_THING = {"hello": "world", "goodnight": "moon"}

typedef i32 Age

struct Person {
  1: required string fullName
  2: required Age age = A_BIG_NUMBER
  3: optional set<i32> grades
  4: required i32 finally; // attempt at a reserved keyword
}

enum SomeEnum {
  ONE
  TWO = 3
  THREE_PLUS_FOUR = 7
  strangeCase = 9
}