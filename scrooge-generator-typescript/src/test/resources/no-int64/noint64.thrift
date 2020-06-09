#@namespace typescript _at_guardian.no_int64

struct NoInt64 {
  1: optional string someAttribute
  2: required YesInt64 itsATrap
}

struct YesInt64 {
  1: required i64 someLargeNumber
}