import java.lang.annotation.*;

@UnsatisfiedRequirement.MyMarker
class UnsatisfiedRequirement {

    @com.osmerion.optin.RequiresOptIn
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyMarker {}

    @MyMarker
    public class MarkedClass {}

    public void foo() {
        new Object() {

            {
                MarkedClass markedClass = new MarkedClass();
            }

        };
    }

}
