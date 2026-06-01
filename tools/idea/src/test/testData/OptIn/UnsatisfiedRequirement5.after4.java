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

    public class Dog implements Animal {

        @MyMarker
        @Override
        void eat<caret>();

    }

}
