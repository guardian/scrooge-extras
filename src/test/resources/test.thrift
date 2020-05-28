#@namespace typescript _at_guardian.contententity

include "shared.thrift"
include "external/zoo.thrift"

struct School {
  1: optional string schoolName
  2: required list<shared.Person> students
  3: optional shared.Person principal
  5: required list<set<list<shared.Person>>> crazyStuff
  6: required map<string,shared.Person> mapOfPeople
  10: required map<i32,shared.Person> otherMapOfPeople
  23: optional shared.SomeEnum someEnumProperty
  13: optional i32 valueWithDefault = shared.A_BIG_NUMBER
//  34: required zoo.Animal mascot
}

union Either {
  1: string left
  2: i32 right
}