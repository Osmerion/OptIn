import com.osmerion.optin.OptIn;

import java.lang.annotation.*;

class UnsatisfiedRequirement {

    @com.osmerion.optin.RequiresOptIn
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyMarker {}

    public interface Animal {

        @MyMarker
        void eat();

    }

    @OptIn(MyMarker.class)
    public class Dog implements Animal {

        @Override
        void eat<caret>();

    }

}
