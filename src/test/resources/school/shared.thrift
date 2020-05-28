#@namespace typescript _at_guardian.school.common

const i32 MAX_STUDENTS_PER_CLASS = 50;
const map<string,i32> DISTRIBUTION = {"Maths": 45, "French": MAX_STUDENTS_PER_CLASS}

typedef i32 Age

struct Student {
  1: required Denomination denomination
  2: required Age age
  3: optional set<i32> grades = [0, 4]
  5: optional Type type
}

enum Type {
  ROBOT
  HUMAN
  DOG
  CAT
}

union Denomination {
  1: string fullName
  2: string nickName
  3: i32 barcode
}