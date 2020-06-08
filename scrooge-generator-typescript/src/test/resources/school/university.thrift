#@namespace scala com.gu.thriftTest.school
#@namespace typescript _at_guardian.school

include "shared.thrift"

struct School {
  1: optional string schoolName
  2: required list<shared.Student> students
  5: required list<set<list<shared.Student>>> crazyNestedList
  6: required map<string,shared.Student> classes
  10: required map<i32,shared.Student> otherMapOfPeople
  11: optional i64 ageInMs
}