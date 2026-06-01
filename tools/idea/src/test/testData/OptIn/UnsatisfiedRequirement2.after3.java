import java.lang.annotation.*;

class UnsatisfiedRequirement {

    @com.osmerion.optin.RequiresOptIn
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyMarker {}

    @MyMarker
    public class MarkedClass {}

    @MyMarker
    public void foo() {
        new Object() {

            {
                MarkedClass markedClass = new MarkedClass();
            }

        };
    }

}
