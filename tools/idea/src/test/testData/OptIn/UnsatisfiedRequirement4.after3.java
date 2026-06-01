import com.osmerion.optin.OptIn;

import java.lang.annotation.*;

class UnsatisfiedRequirement {

    @com.osmerion.optin.RequiresOptIn
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyMarker {}

    @MyMarker
    public class MarkedClass {}

    @OptIn(MyMarker.class)
    public void foo() {
        class LocalClass {

            public void bar() {
                MarkedClass<caret> markedClass = new MarkedClass();
            }

        }
    }

}
