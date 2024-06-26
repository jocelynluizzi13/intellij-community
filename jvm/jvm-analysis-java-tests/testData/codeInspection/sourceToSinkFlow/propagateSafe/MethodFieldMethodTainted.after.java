import org.checkerframework.checker.tainting.qual.*;

class Simple {

    @Untainted
    String field = "safe";

  void test() {
    String s = foo();
    sink(<caret>s);
  }

    @Untainted
    String foo() {
    return field;
  }

  void setFieldToSource() {
    this.field = source();
  }

  @Tainted String source() {
    return "unsafe";
  }

  void sink(@Untainted String s) {}
}