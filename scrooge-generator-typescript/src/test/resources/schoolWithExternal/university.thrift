#@namespace typescript _at_guardian.schoolWithExternal

include "../external/shared.thrift"

struct School {
  1: optional string schoolName
  2: required list<shared.Student> students
  5: required list<set<list<shared.Student>>> crazyNestedList
  6: required map<string,shared.Student> classes
  10: required map<i32,shared.Student> otherMapOfPeople
}