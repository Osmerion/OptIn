import com.osmerion.optin.OptIn;

import java.lang.annotation.*;

class UnsatisfiedRequirement {

    @com.osmerion.optin.RequiresOptIn
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyMarker {}

    @MyMarker
    public class MarkedClass {}

    public void foo() {
        class LocalClass {

            @OptIn(MyMarker.class)
            public void bar() {
                MarkedClass<caret> markedClass = new MarkedClass();
            }

        }
    }

}
