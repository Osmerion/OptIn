import com.osmerion.optin.*;

public class HelloWorld {

    @Marker
    public void foo() {}

    public void bar() {
        foo();
    }

    @SubtypingMarker
    class Base {}

    class Child extends Base {}

}
