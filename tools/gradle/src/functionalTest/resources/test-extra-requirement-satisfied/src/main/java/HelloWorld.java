import com.osmerion.optin.*;

public class HelloWorld {

    @Marker
    public void foo() {}

    @OptIn(Marker.class)
    public void bar() {
        foo();
    }

}
