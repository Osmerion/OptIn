import com.osmerion.optin.OptIn;

import java.lang.annotation.*;

@OptIn(UnsatisfiedRequirement.MyMarker.class)
class UnsatisfiedRequirement {

    @com.osmerion.optin.RequiresOptIn
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyMarker {}

    @MyMarker
    public class MarkedClass {}

    {
        MarkedClass<caret> markedClass = new MarkedClass();
    }

}
